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
        id: 7,
        estado: 'EN_PREPARACION',
        fechaCreacion: '2026-10-01T10:00:00',
        cantidadItemsPropios: 2,
        subtotalPropio: 100000,
        numeroGuia: null,
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

  it('muestra los pedidos con estado y enlace al detalle', () => {
    const fixture = TestBed.createComponent(PedidosVendedor);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Pedido #7');
    expect(root.textContent).toContain('En preparación');
    expect(root.querySelector('a[href="/vendedor/pedidos/7"]')).not.toBeNull();
  });

  it('muestra estado vacío sin pedidos', () => {
    listarRespuesta = [];
    const fixture = TestBed.createComponent(PedidosVendedor);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ningún pedido incluye tus productos');
  });
});
