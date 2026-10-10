import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { CarritoService } from '../services/carrito.service';
import { VitrinaService } from '../../vitrina/services/vitrina.service';
import { AccesoRapidoPrenda } from './acceso-rapido-prenda';

describe('AccesoRapidoPrenda', () => {
  it('muestra las tallas al pasar el cursor y exige seleccionar una antes de agregar', async () => {
    const vitrina = {
      detalle: vi.fn(() => of({
        tallas: [
          { talla: '4', disponible: true },
          { talla: '6', disponible: false },
          { talla: '8', disponible: true },
        ],
      })),
    };
    const carrito = {
      agregar: vi.fn(() => of({})),
      abrirPanel: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [AccesoRapidoPrenda],
      providers: [
        { provide: VitrinaService, useValue: vitrina },
        { provide: CarritoService, useValue: carrito },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AccesoRapidoPrenda);
    fixture.componentRef.setInput('prendaId', 10);
    fixture.componentRef.setInput('nombre', 'Vestido infantil');
    fixture.componentRef.setInput('tallasDisponibles', ['4', '8']);
    fixture.detectChanges();
    expect(vitrina.detalle).not.toHaveBeenCalled();

    (fixture.nativeElement as HTMLElement).dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();

    const tallas = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      '.producto-acceso-tallas button',
    );
    expect(fixture.nativeElement.textContent).toContain('Agregar al carrito');
    expect(Array.from(tallas).map((boton) => boton.textContent?.trim())).toEqual(['4', '8']);
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.producto-acceso-trigger')
      ?.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Selecciona una talla.');
    expect(carrito.agregar).not.toHaveBeenCalled();

    tallas[0].click();

    expect(carrito.agregar).toHaveBeenCalledWith({ prendaId: 10, talla: '4', cantidad: 1 });
    expect(carrito.abrirPanel).toHaveBeenCalledOnce();
  });
});
