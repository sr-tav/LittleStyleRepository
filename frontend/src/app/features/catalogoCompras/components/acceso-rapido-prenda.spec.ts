import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { CarritoService } from '../services/carrito.service';
import { VitrinaService } from '../../vitrina/services/vitrina.service';
import { AccesoRapidoPrenda } from './acceso-rapido-prenda';

describe('AccesoRapidoPrenda', () => {
  it('exige elegir el color y agrega solo una talla con existencias en esa combinación', async () => {
    const vitrina = {
      detalle: vi.fn(() => of({
        tallas: [
          { talla: '4', color: 'ROSA', stockDisponible: 2, disponible: true },
          { talla: '4', color: 'AZUL', stockDisponible: 0, disponible: false },
          { talla: '6', color: 'AZUL', stockDisponible: 1, disponible: true },
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
    fixture.detectChanges();
    expect(vitrina.detalle).not.toHaveBeenCalled();

    (fixture.nativeElement as HTMLElement).dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Agregar al carrito');
    expect(vitrina.detalle).toHaveBeenCalledOnce();
    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.producto-acceso-trigger')
      ?.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Selecciona un color.');
    expect(carrito.agregar).not.toHaveBeenCalled();

    const color = (fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>(
      '.producto-acceso-color select',
    )!;
    color.value = 'ROSA';
    color.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    const tallas = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      '.producto-acceso-tallas button',
    );
    expect(Array.from(tallas).map((boton) => boton.textContent?.trim())).toEqual(['4']);
    tallas[0].click();

    expect(carrito.agregar).toHaveBeenCalledWith({
      prendaId: 10,
      talla: '4',
      cantidad: 1,
      color: 'ROSA',
    });
    expect(carrito.abrirPanel).toHaveBeenCalledOnce();
  });
});
