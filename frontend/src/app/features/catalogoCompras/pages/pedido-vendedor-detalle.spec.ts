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
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } },
        },
        {
          provide: PedidosVendedorService,
          useValue: servicio,
        },
      ],
    }).compileComponents();
  });

  function detalleBase(estado: string) {
    return {
      id: 7,
      estado,
      fechaCreacion: '2026-10-01T10:00:00',
      numeroGuia: null,
      destinatario: 'Ana Ruiz',
      direccion: 'Calle 1 # 2-3',
      complemento: null,
      codigoPostal: '630001',
      ciudad: 'Armenia',
      departamento: 'Quindío',
      telefono: '3001234567',
      items: [
        {
          prendaId: 10,
          nombre: 'Pantalón Jogger Azul',
          talla: '4',
          cantidad: 2,
          precioUnitario: 50000,
          subtotal: 100000,
        },
      ],
      historial: [
        { anterior: null, nuevo: 'REGISTRADO', fecha: '2026-10-01T10:00:00' },
        { anterior: 'REGISTRADO', nuevo: 'EN_PREPARACION', fecha: '2026-10-02T10:00:00' },
      ],
    };
  }

  it('muestra dirección, productos propios e historial', () => {
    servicio.detalle.mockReturnValue(of(detalleBase('EN_PREPARACION')));
    const fixture = TestBed.createComponent(PedidoVendedorDetallePage);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Pedido #7');
    expect(root.textContent).toContain('Calle 1 # 2-3');
    expect(root.textContent).toContain('Pantalón Jogger Azul');
    expect(root.textContent).toContain('Registrado');
    expect(root.textContent).toContain('2026');
  });

  it('avanza a preparación sin guía', () => {
    servicio.detalle.mockReturnValue(of(detalleBase('REGISTRADO')));
    const actualizado = { ...detalleBase('EN_PREPARACION') };
    servicio.avanzarEstado.mockReturnValue(of(actualizado));
    const fixture = TestBed.createComponent(PedidoVendedorDetallePage);
    fixture.detectChanges();

    const boton = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Marcar en preparación'),
    )!;
    boton.click();

    expect(servicio.avanzarEstado).toHaveBeenCalledWith(7, 'EN_PREPARACION', null);
  });

  it('exige la guía para marcar como enviado', () => {
    servicio.detalle.mockReturnValue(of(detalleBase('EN_PREPARACION')));
    servicio.avanzarEstado.mockReturnValue(of({ ...detalleBase('ENVIADO'), numeroGuia: 'COL123' }));
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

    expect(servicio.avanzarEstado).toHaveBeenCalledWith(7, 'ENVIADO', 'COL123');
  });
});
