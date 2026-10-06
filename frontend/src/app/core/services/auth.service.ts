import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PerfilActivoService } from '../../features/perfilesInfantiles/services/perfil-activo.service';
import { AuthResponse, LoginRequest, RegistroRequest, Rol, Usuario } from '../models/auth.models';

const TOKEN_KEY = 'ls_token';
const SESSION_KEY = 'ls_session';

interface Sesion {
  token: string;
  expiraEn: number;
  usuario: Usuario;
}

/** Ruta de inicio de cada rol tras autenticarse. */
export const HOME_POR_ROL: Record<Rol, string> = {
  CLIENTE: '/cliente',
  VENDEDOR: '/vendedor',
  ADMINISTRADOR: '/admin',
};

/**
 * Maneja la sesión del usuario. El backend es sin estado: toda la sesión vive en el JWT,
 * que se guarda en localStorage y se adjunta en cada petición mediante el authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly perfilActivo = inject(PerfilActivoService);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly sesion = signal<Sesion | null>(this.restaurarSesion());

  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  readonly rol = computed(() => this.sesion()?.usuario.rol ?? null);
  readonly autenticado = computed(() => this.sesion() !== null);

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, request)
      .pipe(tap((res) => this.guardarSesion(res)));
  }

  registrar(request: RegistroRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, request)
      .pipe(tap((res) => this.guardarSesion(res)));
  }

  perfil(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.baseUrl}/me`);
  }

  actualizarCuenta(request: ActualizarCuentaRequest): Observable<AuthResponse> {
    return this.http
      .put<AuthResponse>(`${this.baseUrl}/me`, request)
      .pipe(tap((response) => this.guardarSesion(response)));
  }

  desactivarCuenta(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/me`);
  }

  logout(redirigir = true): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(SESSION_KEY);
    this.sesion.set(null);
    this.perfilActivo.limpiar();
    if (redirigir) {
      this.router.navigate(['/auth/login']);
    }
  }

  /** Devuelve el token solo si no ha expirado; si expiró, cierra la sesión local. */
  getToken(): string | null {
    const actual = this.sesion();
    if (!actual) {
      return null;
    }
    if (Date.now() >= actual.expiraEn) {
      this.logout(false);
      return null;
    }
    return actual.token;
  }

  /** Verifica autenticación considerando la expiración del token. */
  estaAutenticado(): boolean {
    return this.getToken() !== null;
  }

  tieneRol(...roles: Rol[]): boolean {
    const rol = this.rol();
    return this.estaAutenticado() && rol !== null && roles.includes(rol);
  }

  rutaInicio(): string {
    const rol = this.rol();
    return rol && this.estaAutenticado() ? HOME_POR_ROL[rol] : '/auth/login';
  }

  private guardarSesion(res: AuthResponse): void {
    const sesion: Sesion = { token: res.token, expiraEn: res.expiraEn, usuario: res.usuario };
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(SESSION_KEY, JSON.stringify(sesion));
    this.sesion.set(sesion);
  }

  private restaurarSesion(): Sesion | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      const token = localStorage.getItem(TOKEN_KEY);
      if (!raw || !token) {
        return null;
      }
      const sesion = JSON.parse(raw) as Sesion;
      const expiraEn = decodificarExpiracion(token) ?? sesion.expiraEn;
      if (sesion.token !== token || !expiraEn || Date.now() >= expiraEn) {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(SESSION_KEY);
        this.perfilActivo.limpiar();
        return null;
      }
      return { ...sesion, expiraEn };
    } catch {
      return null;
    }
  }
}

/** Lee el claim "exp" del JWT (sin verificar firma; eso lo hace el backend). */
export function decodificarExpiracion(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    const exp = JSON.parse(json).exp;
    return typeof exp === 'number' ? exp * 1000 : null;
  } catch {
    return null;
  }
}
