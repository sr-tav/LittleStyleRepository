import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Observable, finalize, tap } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { procesarErrorApi } from '../../auth/auth.validators';
import { AgregarItemCarritoRequest, Carrito } from '../models/carrito.models';

const CARRITO_CACHE_PREFIX = 'ls_carrito_usuario_';

@Injectable({ providedIn: 'root' })
export class CarritoService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/cliente/carrito`;

  private readonly estado = signal<Carrito | null>(null);
  private readonly usuarioId = signal<number | null>(null);
  private activeUserId: number | null | undefined;

  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly items = computed(() => this.estado()?.items ?? []);
  readonly cantidad = computed(() => this.estado()?.cantidad ?? 0);
  readonly subtotal = computed(() => this.estado()?.subtotal ?? 0);
  readonly costoEnvio = computed(() => this.estado()?.costoEnvio ?? null);
  readonly total = computed(() => this.estado()?.total ?? null);
  readonly panelAbierto = signal(false);

  abrirPanel(): void {
    this.panelAbierto.set(true);
  }

  cerrarPanel(): void {
    this.panelAbierto.set(false);
  }

  alternarPanel(): void {
    this.panelAbierto.update((abierto) => !abierto);
  }

  constructor() {
    effect(() => {
      const usuario = this.auth.usuario();
      const userId = usuario?.rol === 'CLIENTE' ? usuario.id : null;
      if (this.activeUserId === userId) return;

      if (typeof this.activeUserId === 'number') {
        localStorage.removeItem(this.clave(this.activeUserId));
      }
      this.activeUserId = userId;
      this.usuarioId.set(userId);
      this.estado.set(null);
      this.error.set(null);
      if (userId === null) return;

      this.restaurarCache(userId);
      this.cargar().subscribe({
        error: (error: unknown) => this.error.set(procesarErrorApi(error)),
      });
    });
  }

  cargar(): Observable<Carrito> {
    const userId = this.requerirUsuarioId();
    this.cargando.set(true);
    this.error.set(null);
    return this.http.get<Carrito>(this.baseUrl).pipe(
      tap((carrito) => this.actualizar(carrito, userId)),
      finalize(() => this.cargando.set(false)),
    );
  }

  agregar(request: AgregarItemCarritoRequest): Observable<Carrito> {
    const userId = this.requerirUsuarioId();
    return this.http.post<Carrito>(`${this.baseUrl}/items`, request).pipe(
      tap((carrito) => this.actualizar(carrito, userId)),
    );
  }

  cambiarCantidad(itemId: number, cantidad: number): Observable<Carrito> {
    const userId = this.requerirUsuarioId();
    return this.http.put<Carrito>(`${this.baseUrl}/items/${itemId}`, { cantidad }).pipe(
      tap((carrito) => this.actualizar(carrito, userId)),
    );
  }

  quitar(itemId: number): Observable<Carrito> {
    const userId = this.requerirUsuarioId();
    return this.http.delete<Carrito>(`${this.baseUrl}/items/${itemId}`).pipe(
      tap((carrito) => this.actualizar(carrito, userId)),
    );
  }

  vaciar(): Observable<Carrito> {
    const userId = this.requerirUsuarioId();
    return this.http.delete<Carrito>(this.baseUrl).pipe(
      tap((carrito) => this.actualizar(carrito, userId)),
    );
  }

  private restaurarCache(userId: number): void {
    const cache = localStorage.getItem(this.clave(userId));
    if (cache === null) return;

    try {
      const carrito: unknown = JSON.parse(cache);
      if (esCarrito(carrito)) {
        this.estado.set(carrito);
      } else {
        localStorage.removeItem(this.clave(userId));
      }
    } catch (error) {
      if (!(error instanceof SyntaxError)) throw error;
      localStorage.removeItem(this.clave(userId));
    }
  }

  private actualizar(carrito: Carrito, userId: number): void {
    if (this.auth.usuario()?.id !== userId || this.usuarioId() !== userId) return;
    this.estado.set(carrito);
    localStorage.setItem(this.clave(userId), JSON.stringify(carrito));
  }

  private requerirUsuarioId(): number {
    const userId = this.auth.usuario()?.id;
    if (userId === undefined || userId === null) {
      throw new Error('Se requiere una sesión autenticada para usar el carrito');
    }
    return userId;
  }

  private clave(userId: number): string {
    return `${CARRITO_CACHE_PREFIX}${userId}`;
  }
}

function esCarrito(valor: unknown): valor is Carrito {
  if (typeof valor !== 'object' || valor === null) return false;
  const carrito = valor as Partial<Carrito>;
  return Array.isArray(carrito.items)
    && typeof carrito.cantidad === 'number'
    && typeof carrito.subtotal === 'number'
    && (typeof carrito.costoEnvio === 'number' || carrito.costoEnvio === null)
    && (typeof carrito.total === 'number' || carrito.total === null)
    && carrito.items.every(
      (item) =>
        typeof item === 'object'
        && item !== null
        && typeof item.id === 'number'
        && typeof item.prendaId === 'number'
        && typeof item.nombre === 'string'
        && (typeof item.imagenUrl === 'string' || item.imagenUrl === null)
        && typeof item.talla === 'string'
        && typeof item.cantidad === 'number'
        && typeof item.precioUnitario === 'number'
        && typeof item.subtotal === 'number'
        && typeof item.disponible === 'boolean',
    );
}
