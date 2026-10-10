import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { CarritoService } from '../services/carrito.service';
import { CarritoPage } from './carrito';

describe('CarritoPage', () => {
  const item = {
    id: 10,
    prendaId: 20,
    nombre: 'Pantalón Jogger',
    imagenUrl: null,
    talla: '4',
    cantidad: 2,
    precioUnitario: 32900,
    subtotal: 65800,
    disponible: true,
  };
  let carrito: {
    items: ReturnType<typeof signal<typeof item[]>>;
    cantidad: ReturnType<typeof signal<number>>;
    subtotal: ReturnType<typeof signal<number>>;
    costoEnvio: ReturnType<typeof signal<number | null>>;
    total: ReturnType<typeof signal<number | null>>;
    cargando: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    cargar: ReturnType<typeof vi.fn>;
    cambiarCantidad: ReturnType<typeof vi.fn>;
    quitar: ReturnType<typeof vi.fn>;
    vaciar: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    carrito = {
      items: signal([item]),
      cantidad: signal(2),
      subtotal: signal(65800),
      costoEnvio: signal<number | null>(null),
      total: signal<number | null>(null),
      cargando: signal(false),
      error: signal(null),
      cargar: vi.fn(() => of({})),
      cambiarCantidad: vi.fn(() => of({})),
      quitar: vi.fn(() => of({})),
      vaciar: vi.fn(() => of({})),
    };
    await TestBed.configureTestingModule({
      imports: [CarritoPage],
      providers: [
        provideRouter([]),
        { provide: CarritoService, useValue: carrito },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('muestra los avisos de disponibilidad en el resumen y permite continuar si hay stock físico', () => {
    const fixture = TestBed.createComponent(CarritoPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;

    const resumen = root.querySelector('.carrito-resumen');
    expect(resumen?.textContent).toContain('Los artículos no están reservados');
    expect(resumen?.textContent).toContain('Subtotal');
    expect(resumen?.textContent).toContain('Envío');
    expect(resumen?.textContent).toContain('Ingresa la dirección de envío');
    expect(resumen?.textContent).toContain('Por calcular');
    const checkout = root.querySelector<HTMLButtonElement>('.carrito-resumen .btn-primary');
    expect(checkout?.disabled).toBe(false);
  });

  it('pide confirmación antes de eliminar una línea del carrito', () => {
    const fixture = TestBed.createComponent(CarritoPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    Array.from(root.querySelectorAll('button'))
      .find((button) => button.textContent?.trim() === 'Eliminar')
      ?.click();

    expect(window.confirm).toHaveBeenCalledWith(
      '¿Deseas eliminar esta prenda del carrito?',
    );
    expect(carrito.quitar).toHaveBeenCalledWith(item.id);
  });

  it('no elimina el artículo si se cancela la confirmación', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture = TestBed.createComponent(CarritoPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    Array.from(root.querySelectorAll('button'))
      .find((button) => button.textContent?.trim() === 'Eliminar')
      ?.click();

    expect(carrito.quitar).not.toHaveBeenCalled();
  });

  it('solicita eliminar el artículo al disminuir desde cantidad uno', () => {
    carrito.items.set([{ ...item, cantidad: 1, subtotal: item.precioUnitario }]);
    const fixture = TestBed.createComponent(CarritoPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('[aria-label="Disminuir cantidad de Pantalón Jogger"]')?.click();

    expect(window.confirm).toHaveBeenCalled();
    expect(carrito.quitar).toHaveBeenCalledWith(item.id);
    expect(carrito.cambiarCantidad).not.toHaveBeenCalled();
  });

  it('vacía el carrito desde el resumen', () => {
    const fixture = TestBed.createComponent(CarritoPage);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('.carrito-vaciar')?.click();

    expect(carrito.vaciar).toHaveBeenCalledOnce();
  });
});
