import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { PedidoDetalle } from '../models/pedido.models';
import { PedidosService } from '../services/pedidos.service';
import { PedidoDetallePage } from './pedido-detalle';

function detalle(): PedidoDetalle {
  return {
    id: 7,
    estado: 'ENVIADO',
    subtotal: 100000,
    costoEnvio: 7000,
    total: 107000,
    fechaCreacion: '2026-10-01T10:00:00',
    numeroGuia: 'COL123',
    ciudad: 'Armenia',
    departamento: 'Quindío',
    items: [
      { prendaId: 10, nombre: 'Pantalón Jogger Azul', talla: '4', cantidad: 1, precioUnitario: 100000, subtotal: 100000 },
    ],
    historial: [
      { anterior: null, nuevo: 'PENDIENTE_PAGO', fecha: '2026-10-01T10:00:00' },
      { anterior: 'PAGADO', nuevo: 'EN_PREPARACION', fecha: '2026-10-02T10:00:00' },
      { anterior: 'EN_PREPARACION', nuevo: 'ENVIADO', fecha: '2026-10-03T10:00:00' },
    ],
  };
}

describe('PedidoDetallePage (US-12 / UI-16)', () => {
  const pedidos = { detalle: vi.fn(), cancelar: vi.fn() };

  beforeEach(async () => {
    pedidos.detalle.mockReset();
    pedidos.cancelar.mockReset();
    pedidos.detalle.mockReturnValue(of(detalle()));
    await TestBed.configureTestingModule({
      imports: [PedidoDetallePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } },
        },
        { provide: PedidosService, useValue: pedidos },
      ],
    }).compileComponents();
  });

  it('muestra guía, línea de tiempo, productos y totales', () => {
    const fixture = TestBed.createComponent(PedidoDetallePage);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('COL123');
    expect(root.textContent).toContain('Pantalón Jogger Azul');
    expect(root.textContent).toContain('107.000');
    expect(root.querySelectorAll('.pedido-linea-tiempo li')).toHaveLength(5);
    expect(root.querySelectorAll('.pedido-historial li')).toHaveLength(3);
    expect(root.querySelector('.pedido-linea-tiempo li.is-actual')?.textContent).toContain('Enviado');
  });

  it('muestra el historial con fechas por transición', () => {
    const fixture = TestBed.createComponent(PedidoDetallePage);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    const historial = root.querySelector('[aria-label="Historial del pedido"]');
    expect(historial?.textContent).toContain('Pendiente de pago');
    expect(historial?.textContent).toContain('En preparación');
    expect(historial?.textContent).toContain('2026');
  });

  it('muestra el aviso de cancelado sin línea de tiempo pero con historial', () => {
    pedidos.detalle.mockReturnValue(of({ ...detalle(), estado: 'CANCELADO' }));
    const fixture = TestBed.createComponent(PedidoDetallePage);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('fue cancelado');
    expect(root.querySelector('.pedido-linea-tiempo li.is-actual')).toBeNull();
    expect(root.querySelectorAll('.pedido-historial li')).toHaveLength(3);
  });

  it('muestra el error si la consulta falla', () => {
    pedidos.detalle.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: { mensaje: 'No existe' } })),
    );
    const fixture = TestBed.createComponent(PedidoDetallePage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No existe');
  });

  it('recarga el detalle al pulsar Actualizar', () => {
    const fixture = TestBed.createComponent(PedidoDetallePage);
    fixture.detectChanges();
    expect(pedidos.detalle).toHaveBeenCalledTimes(1);

    const boton = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Actualizar'),
    )!;
    boton.click();

    expect(pedidos.detalle).toHaveBeenCalledTimes(2);
  });

  it('cancela el pedido pendiente de pago tras confirmar', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    pedidos.detalle.mockReturnValue(of({ ...detalle(), estado: 'PENDIENTE_PAGO', numeroGuia: null }));
    pedidos.cancelar.mockReturnValue(of({ id: 7, estado: 'CANCELADO', numeroGuia: null }));
    const fixture = TestBed.createComponent(PedidoDetallePage);
    fixture.detectChanges();

    const boton = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Cancelar pedido'),
    )!;
    boton.click();

    expect(pedidos.cancelar).toHaveBeenCalledWith(7);
    vi.restoreAllMocks();
  });
});
