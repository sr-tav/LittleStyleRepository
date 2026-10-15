import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PedidosVendedorService } from '../services/pedidos-vendedor.service';
import { PedidoVendedorDetallePage } from './pedido-vendedor-detalle';

describe('PedidoVendedorDetallePage', () => {
  beforeEach(async () => {
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
          useValue: {
            detalle: () =>
              of({
                id: 7,
                estado: 'EN_PREPARACION',
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
                historial: [],
              }),
          },
        },
      ],
    }).compileComponents();
  });

  it('muestra dirección, productos propios e historial', () => {
    const fixture = TestBed.createComponent(PedidoVendedorDetallePage);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Pedido #7');
    expect(root.textContent).toContain('Calle 1 # 2-3');
    expect(root.textContent).toContain('Pantalón Jogger Azul');
  });
});
