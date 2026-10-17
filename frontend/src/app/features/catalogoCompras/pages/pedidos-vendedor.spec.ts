import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PedidosVendedorService } from '../services/pedidos-vendedor.service';
import { PedidosVendedor } from './pedidos-vendedor';

describe('PedidosVendedor', () => {
  let listarRespuesta: unknown[];

  beforeEach(async () => {
    listarRespuesta = [
      {
        despachoId: 5,
        pedidoId: 7,
        vendedorId: 31,
        tienda: 'Tienda Sol',
        estado: 'EN_PREPARACION',
        numeroGuia: null,
        fechaActualizacion: '2026-10-01T10:00:00',
        items: [{ prendaId: 10, nombre: 'Pantalón', talla: '4', cantidad: 2, precioUnitario: 50000, subtotal: 100000 }],
      },
    ];
    await TestBed.configureTestingModule({
      imports: [PedidosVendedor],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: PedidosVendedorService, useFactory: () => ({ listar: () => of(listarRespuesta) }) },
      ],
    }).compileComponents();
  });

  it('muestra los despachos con estado y enlace al detalle', () => {
    const fixture = TestBed.createComponent(PedidosVendedor);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Pedido #7');
    expect(root.textContent).toContain('Tu paquete');
    expect(root.textContent).toContain('En preparación');
    expect(root.textContent).toContain('100.000');
    expect(root.textContent).toContain('2026');
    expect(root.querySelector('a[href="/vendedor/pedidos/5"]')).not.toBeNull();
  });

  it('muestra estado vacío sin despachos', () => {
    listarRespuesta = [];
    const fixture = TestBed.createComponent(PedidosVendedor);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ningún pedido incluye tus productos');
  });
});
