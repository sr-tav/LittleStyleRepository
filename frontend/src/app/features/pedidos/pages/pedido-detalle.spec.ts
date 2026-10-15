import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

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
  beforeEach(async () => {
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
        { provide: PedidosService, useValue: { detalle: () => of(detalle()) } },
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
    expect(root.querySelectorAll('.pedido-linea-tiempo li')).toHaveLength(4);
    expect(root.querySelector('.pedido-linea-tiempo li.is-actual')?.textContent).toContain('Enviado');
  });
});
