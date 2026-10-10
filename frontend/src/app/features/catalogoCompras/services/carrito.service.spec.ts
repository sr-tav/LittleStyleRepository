import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';

import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/services/auth.service';
import { Carrito } from '../models/carrito.models';
import { CarritoService } from './carrito.service';

const BASE_URL = `${environment.apiUrl}/cliente/carrito`;

const CARRITO: Carrito = {
  items: [{
    id: 10,
    prendaId: 20,
    nombre: 'Pantalón Jogger',
    imagenUrl: null,
    talla: '4',
    cantidad: 2,
    precioUnitario: 32900,
    subtotal: 65800,
    disponible: true,
  }],
  cantidad: 2,
  subtotal: 65800,
  costoEnvio: null,
  total: null,
};

describe('CarritoService', () => {
  let http: HttpTestingController;
  let usuario: ReturnType<typeof signal<{ id: number; rol: string } | null>>;
  let service: CarritoService;

  beforeEach(() => {
    localStorage.clear();
    usuario = signal({ id: 11, rol: 'CLIENTE' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { usuario } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(CarritoService);
    TestBed.flushEffects();
    http.expectOne(BASE_URL).flush({ items: [], cantidad: 0, subtotal: 0, costoEnvio: 0, total: 0 });
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('sincroniza operaciones con el backend y deriva cantidad, subtotal y total', () => {
    service.agregar({ prendaId: 20, talla: '4', cantidad: 2 }).subscribe();
    const request = http.expectOne(`${BASE_URL}/items`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ prendaId: 20, talla: '4', cantidad: 2 });
    request.flush(CARRITO);

    expect(service.items()).toEqual(CARRITO.items);
    expect(service.cantidad()).toBe(2);
    expect(service.subtotal()).toBe(65800);
    expect(service.total()).toBeNull();
    expect(service.costoEnvio()).toBeNull();
    expect(JSON.parse(localStorage.getItem('ls_carrito_usuario_11') ?? 'null')).toEqual(CARRITO);

    service.cambiarCantidad(10, 3).subscribe();
    const update = http.expectOne(`${BASE_URL}/items/10`);
    expect(update.request.method).toBe('PUT');
    update.flush(CARRITO);

    service.quitar(10).subscribe();
    const remove = http.expectOne(`${BASE_URL}/items/10`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush({ items: [], cantidad: 0, subtotal: 0, costoEnvio: 0, total: 0 });
    service.vaciar().subscribe();
    const clear = http.expectOne(BASE_URL);
    expect(clear.request.method).toBe('DELETE');
    clear.flush({ items: [], cantidad: 0, subtotal: 0, costoEnvio: 0, total: 0 });
  });

  it('aísla y limpia la caché al cambiar de usuario o cerrar sesión', () => {
    localStorage.setItem('ls_carrito_usuario_11', JSON.stringify(CARRITO));
    usuario.set({ id: 22, rol: 'CLIENTE' });
    TestBed.flushEffects();
    expect(localStorage.getItem('ls_carrito_usuario_11')).toBeNull();
    expect(service.items()).toEqual([]);
    http.expectOne(BASE_URL).flush({ items: [], cantidad: 0, subtotal: 0, costoEnvio: 0, total: 0 });

    service.agregar({ prendaId: 20, talla: '4', cantidad: 1 }).subscribe();
    http.expectOne(`${BASE_URL}/items`).flush(CARRITO);
    expect(localStorage.getItem('ls_carrito_usuario_22')).not.toBeNull();

    usuario.set(null);
    TestBed.flushEffects();
    expect(localStorage.getItem('ls_carrito_usuario_22')).toBeNull();
    expect(service.items()).toEqual([]);
  });
});
