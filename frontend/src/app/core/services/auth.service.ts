import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PerfilActivoService } from '../../features/perfilesInfantiles/services/perfil-activo.service';
import {
  ActualizarCuentaRequest,
  AuthResponse,
  LoginRequest,
  RegistroRequest,
  Rol,
  Usuario,
} from '../models/auth.models';

const TOKEN_KEY = 'ls_token';
const SESSION_KEY = 'ls_session';

interface Sesion {
  token: string;
  expiraEn: number;
  usuario: Usuario;
}

/**
 * Ruta de inicio de cada rol tras autenticarse.
 * @see UI-3
 * @see UI-18
 * @see UI-25
 */
export const HOME_POR_ROL: Record<Rol, string> = {
  CLIENTE: '/cliente',
  VENDEDOR: '/vendedor',
  ADMINISTRADOR: '/admin',
};

/**
 * Maneja la sesión del usuario. El backend es sin estado: toda la sesión vive en el JWT,
 * que se guarda en localStorage y se adjunta en cada petición mediante el authInterceptor.
 * @see US-05
 * @see ADR-04
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

  /**
   * Envía las credenciales y persiste la sesión cuando la autenticación responde correctamente.
   * @param request Credenciales de inicio de sesión.
   * @returns Flujo con la respuesta de autenticación.
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, request)
      .pipe(tap((res) => this.guardarSesion(res)));
  }

  /**
   * Envía los datos de registro y persiste la sesión creada.
   * @param request Datos del formulario de registro.
   * @returns Flujo con la respuesta de autenticación.
   */
  registrar(request: RegistroRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, request)
      .pipe(tap((res) => this.guardarSesion(res)));
  }

  /**
   * Solicita los datos del usuario autenticado.
   * @returns Flujo con los datos de la cuenta.
   */
  perfil(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.baseUrl}/me`);
  }

  /**
   * Actualiza la cuenta y sincroniza la sesión con la respuesta recibida.
   * @param request Campos de cuenta que se desean actualizar.
   * @returns Flujo con la respuesta de autenticación actualizada.
   */
  actualizarCuenta(request: ActualizarCuentaRequest): Observable<AuthResponse> {
    return this.http
      .put<AuthResponse>(`${this.baseUrl}/me`, request)
      .pipe(tap((response) => this.guardarSesion(response)));
  }

  /**
   * Solicita la desactivación de la cuenta autenticada.
   * @returns Flujo que finaliza sin contenido al completarse la solicitud.
   */
  desactivarCuenta(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/me`);
  }

  /**
   * Elimina la sesión local, limpia el perfil activo y opcionalmente navega al login.
   * @param redirigir Indica si debe navegarse a la pantalla de inicio de sesión.
   * @returns No devuelve un valor.
   */
  logout(redirigir = true): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(SESSION_KEY);
    this.sesion.set(null);
    this.perfilActivo.limpiar();
    if (redirigir) {
      this.router.navigate(['/auth/login']);
    }
  }

  /**
   * Devuelve el token solo si no ha expirado; si expiró, cierra la sesión local.
   * @returns Token vigente o `null` cuando no hay sesión válida.
   */
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

  /**
   * Verifica autenticación considerando la expiración del token.
   * @returns `true` si existe un token vigente.
   */
  estaAutenticado(): boolean {
    return this.getToken() !== null;
  }

  /**
   * Comprueba que el usuario autenticado tenga uno de los roles indicados.
   * @param roles Roles aceptados para la operación.
   * @returns `true` si el rol de la sesión coincide con alguno de los indicados.
   */
  tieneRol(...roles: Rol[]): boolean {
    const rol = this.rol();
    return this.estaAutenticado() && rol !== null && roles.includes(rol);
  }

  /**
   * Determina la ruta de inicio según el rol de la sesión vigente.
   * @returns Ruta del rol autenticado o la ruta de inicio de sesión.
   */
  rutaInicio(): string {
    const rol = this.rol();
    return rol && this.estaAutenticado() ? HOME_POR_ROL[rol] : '/auth/login';
  }

  /**
   * Persiste en el almacenamiento local y en la señal la sesión recibida.
   * @param res Respuesta de autenticación con token, vencimiento y usuario.
   * @returns No devuelve un valor.
   */
  private guardarSesion(res: AuthResponse): void {
    const sesion: Sesion = { token: res.token, expiraEn: res.expiraEn, usuario: res.usuario };
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(SESSION_KEY, JSON.stringify(sesion));
    this.sesion.set(sesion);
  }

  /**
   * Restaura una sesión local solo si el token coincide y no ha vencido.
   * @returns Sesión válida restaurada o `null` si no puede recuperarse.
   */
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

/**
 * Lee el claim "exp" del JWT (sin verificar firma; eso lo hace el backend).
 * @param token JWT cuya carga se analiza.
 * @returns Vencimiento en milisegundos o `null` si no puede obtenerse.
 */
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
