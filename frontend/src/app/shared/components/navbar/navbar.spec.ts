import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../../features/catalogoCompras/services/carrito.service';
import { Navbar } from './navbar';

describe('Navbar', () => {
  afterEach(() => vi.restoreAllMocks());

  it('abre el panel lateral con aviso, controles de producto y enlace al carrito completo', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const panelAbierto = signal(false);
    const item = {
      id: 1,
      prendaId: 7,
      nombre: 'Camiseta infantil',
      imagenUrl: null,
      talla: '4',
      cantidad: 2,
      precioUnitario: 25900,
      subtotal: 51800,
      disponible: true,
    };
    const carrito = {
      panelAbierto,
      items: signal([item]),
      cantidad: signal(2),
      subtotal: signal(51800),
      alternarPanel: vi.fn(() => panelAbierto.update((abierto) => !abierto)),
      cerrarPanel: vi.fn(() => panelAbierto.set(false)),
      cambiarCantidad: vi.fn(() => of({})),
      quitar: vi.fn(() => of({})),
    };
    await TestBed.configureTestingModule({
      imports: [Navbar],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { rol: () => 'CLIENTE', usuario: () => null, rutaInicio: () => '/cliente' } },
        { provide: CarritoService, useValue: carrito },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(Navbar);
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.navbar-carrito-trigger')
      ?.click();
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('[role="dialog"]')).not.toBeNull();
    const resumen = root.querySelector('.carrito-panel-footer');
    expect(resumen?.textContent).toContain('Subtotal');
    expect(resumen?.textContent).not.toContain('Envío');
    expect(resumen?.textContent).toContain('Los artículos no están reservados');
    expect(root.textContent).toContain('Camiseta infantil');
    expect(root.querySelector<HTMLButtonElement>('[aria-label="Aumentar cantidad de Camiseta infantil"]'))
      .not.toBeNull();
    expect(root.querySelector<HTMLButtonElement>('[aria-label="Eliminar Camiseta infantil del carrito"]'))
      .not.toBeNull();
    expect(root.querySelector<HTMLAnchorElement>('.carrito-panel-ver')?.getAttribute('href'))
      .toBe('/cliente/carrito');
    expect(carrito.alternarPanel).toHaveBeenCalledOnce();

    root.querySelector<HTMLButtonElement>('[aria-label="Aumentar cantidad de Camiseta infantil"]')?.click();
    expect(carrito.cambiarCantidad).toHaveBeenCalledWith(1, 3);
    root.querySelector<HTMLButtonElement>('[aria-label="Eliminar Camiseta infantil del carrito"]')?.click();
    expect(window.confirm).toHaveBeenCalledWith('¿Deseas eliminar esta prenda del carrito?');
    expect(carrito.quitar).toHaveBeenCalledWith(1);
  });
});
