import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../services/carrito.service';
import { crearAlerta } from '../testing/catalogo-testing';
import { AlertasStock } from './alertas';

describe('AlertasStock (UI-22)', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertasStock],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            rol: () => 'VENDEDOR',
            usuario: () => null,
            rutaInicio: () => '/vendedor',
            logout: vi.fn(),
          },
        },
        {
          provide: CarritoService,
          useValue: {
            panelAbierto: signal(false),
            items: signal([]),
            cantidad: signal(0),
            subtotal: signal(0),
            alternarPanel: vi.fn(),
            cerrarPanel: vi.fn(),
            cambiarCantidad: vi.fn(),
            quitar: vi.fn(),
          },
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear(alertas: ReturnType<typeof crearAlerta>[]) {
    const fixture = TestBed.createComponent(AlertasStock);
    fixture.detectChanges();
    http.expectOne('/api/vendedor/inventario/alertas').flush(alertas);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('resume el conteo de alertas agotadas y con stock bajo', () => {
    const { el } = crear([
      crearAlerta({ id: 1, nivel: 'AGOTADO', stockActual: 0 }),
      crearAlerta({ id: 2 }),
      crearAlerta({ id: 3, diferencia: -1 }),
    ]);

    const filtros = el.querySelector('.alert-filters')?.textContent?.replace(/\s+/g, ' ').trim();
    expect(filtros).toContain('Agotadas 1');
    expect(filtros).toContain('Stock bajo 2');
    expect(filtros).toContain('Todas 3');
  });

  it('ordena primero las agotadas y luego por diferencia ascendente', () => {
    const { el } = crear([
      crearAlerta({ id: 1, nombre: 'Menos urgente', nivel: 'STOCK_BAJO', diferencia: -2 }),
      crearAlerta({ id: 2, nombre: 'Agotado', nivel: 'AGOTADO', diferencia: -1 }),
      crearAlerta({ id: 3, nombre: 'Más urgente', nivel: 'STOCK_BAJO', diferencia: -5 }),
    ]);
    const tarjetas = el.querySelectorAll<HTMLElement>('.alert-card');

    expect(tarjetas[0].classList).toContain('alert-card-AGOTADO');
    expect(tarjetas[0].textContent).toContain('Sin stock para venta');
    expect(tarjetas[1].textContent).toContain('Más urgente');
    expect(tarjetas[1].textContent).toContain('Quedan 2 unidades; tu mínimo es 5');
    expect(tarjetas[2].textContent).toContain('Menos urgente');
  });

  it('muestra etiquetas claras para el stock actual y el mínimo', () => {
    const { el } = crear([crearAlerta()]);

    expect(el.querySelector('.alert-card-stats')?.textContent).toContain('Stock actual');
    expect(el.querySelector('.alert-card-stats')?.textContent).toContain('Stock mínimo');
  });

  it('presenta el botón para actualizar la prenda correspondiente', () => {
    const { el } = crear([crearAlerta({ prendaId: 42 })]);
    const boton = el.querySelector<HTMLAnchorElement>('.alert-card a');

    expect(boton?.textContent?.trim()).toBe('Actualizar stock');
    expect(boton?.classList).toContain('btn-primary');
    expect(boton?.getAttribute('href')).toBe('/vendedor/inventario?prenda=42');
  });

  it('muestra que todo está en orden cuando no hay alertas', () => {
    const { el } = crear([]);

    expect(el.textContent).toContain('Todo en orden');
    expect(el.querySelector('.catalog-subtitle[role="status"]')).toBeNull();
  });

  it('filtra las alertas por nivel y por las categorías disponibles', () => {
    const { fixture, el } = crear([
      crearAlerta({ id: 1, categoria: 'VESTIDOS', nivel: 'AGOTADO', stockActual: 0 }),
      crearAlerta({ id: 2, categoria: 'PANTALONES' }),
    ]);

    const agotadas = [...el.querySelectorAll<HTMLButtonElement>('.alert-filters button')][0];
    agotadas.click();
    fixture.detectChanges();
    expect(el.querySelectorAll('.alert-card')).toHaveLength(1);
    expect(el.querySelector('.alert-card')?.classList).toContain('alert-card-AGOTADO');

    const categoria = el.querySelector<HTMLSelectElement>('[aria-label="Filtrar por categoría"]')!;
    categoria.value = 'PANTALONES';
    categoria.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(el.querySelector('.catalog-state')?.textContent).toContain(
      'No hay alertas para los filtros seleccionados',
    );

    el.querySelectorAll<HTMLButtonElement>('.alert-filters button')[2].click();
    fixture.detectChanges();
    expect(el.querySelector('.alert-card')?.textContent).toContain('Pantalón Jogger Azul');
  });
});
