import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PedidosVendedorService } from '../services/pedidos-vendedor.service';
import { PedidoVendedorDetallePage } from './pedido-vendedor-detalle';

describe('PedidoVendedorDetallePage', () => {
  const servicio = { detalle: vi.fn(), avanzarEstado: vi.fn() };

  beforeEach(async () => {
    servicio.detalle.mockReset();
    servicio.avanzarEstado.mockReset();
    await TestBed.configureTestingModule({
      imports: [PedidoVendedorDetallePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '5' }) } },
        },
        {
          provide: PedidosVendedorService,
          useValue: servicio,
        },
      ],
    }).compileComponents();
  });

  function despachoBase(estado: string) {
    return {
      despachoId: 5,
      pedidoId: 7,
      vendedorId: 31,
      tienda: 'Tienda Sol',
      estado,
      numeroGuia: null,
      fechaActualizacion: '2026-10-01T10:00:00',
      items: [
        {
          prendaId: 10,
          nombre: 'Pantalón Jogger Azul',
          talla: '4',
          cantidad: 2,
          precioUnitario: 50000,
          subtotal: 100000,
          imagenUrl: '/media/a.png',
        },
      ],
    };
  }

  it('muestra tu paquete, productos propios y estado', () => {
    servicio.detalle.mockReturnValue(of(despachoBase('EN_PREPARACION')));
    const fixture = TestBed.createComponent(PedidoVendedorDetallePage);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Pedido #7');
    expect(root.textContent).toContain('Tu paquete');
    expect(root.textContent).toContain('Pantalón Jogger Azul');
    expect(root.textContent).toContain('En preparación');
    const img = root.querySelector('img.pedido-miniatura') as HTMLImageElement;
    expect(img?.getAttribute('src')).toBe('/media/a.png');
    expect(root.textContent).toContain('c/u');
  });

  it('avanza a preparación sin guía', () => {
    servicio.detalle.mockReturnValue(of(despachoBase('REGISTRADO')));
    const actualizado = { ...despachoBase('EN_PREPARACION') };
    servicio.avanzarEstado.mockReturnValue(of(actualizado));
    const fixture = TestBed.createComponent(PedidoVendedorDetallePage);
    fixture.detectChanges();

    const boton = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Marcar en preparación'),
    )!;
    boton.click();

    expect(servicio.avanzarEstado).toHaveBeenCalledWith(5, 'EN_PREPARACION', null);
  });

  it('exige la guía para marcar como enviado', () => {
    servicio.detalle.mockReturnValue(of(despachoBase('EN_PREPARACION')));
    servicio.avanzarEstado.mockReturnValue(of({ ...despachoBase('ENVIADO'), numeroGuia: 'COL123' }));
    const fixture = TestBed.createComponent(PedidoVendedorDetallePage);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const boton = [...root.querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Marcar como enviado'),
    )!;
    boton.click();
    fixture.detectChanges();

    expect(servicio.avanzarEstado).not.toHaveBeenCalled();
    expect(root.textContent).toContain('número de guía');

    const guia = root.querySelector<HTMLInputElement>('input[type="text"]')!;
    guia.value = 'COL123';
    guia.dispatchEvent(new Event('input'));
    boton.click();

    expect(servicio.avanzarEstado).toHaveBeenCalledWith(5, 'ENVIADO', 'COL123');
  });
});
