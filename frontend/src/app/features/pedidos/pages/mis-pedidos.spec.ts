import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PedidosService } from '../services/pedidos.service';
import { MisPedidos } from './mis-pedidos';

describe('MisPedidos (US-12 / UI-17)', () => {
  let listarRespuesta: unknown[];

  beforeEach(async () => {
    listarRespuesta = [
      {
        id: 7,
        estado: 'ENVIADO',
        total: 107000,
        fechaCreacion: '2026-10-01T10:00:00',
        cantidadItems: 1,
        numeroGuia: 'COL123',
      },
    ];
    await TestBed.configureTestingModule({
      imports: [MisPedidos],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: PedidosService, useFactory: () => ({ listar: () => of(listarRespuesta) }) },
      ],
    }).compileComponents();
  });

  it('muestra cada pedido con su estado y enlace al detalle', () => {
    const fixture = TestBed.createComponent(MisPedidos);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.textContent).toContain('Pedido #7');
    expect(root.textContent).toContain('Enviado');
    expect(root.textContent).toContain('107.000');
    expect(root.textContent).toContain('COL123');
    const enlace = root.querySelector<HTMLAnchorElement>('a[href="/cliente/pedidos/7"]');
    expect(enlace).not.toBeNull();
  });

  it('muestra estado vacío sin pedidos', () => {
    listarRespuesta = [];
    const fixture = TestBed.createComponent(MisPedidos);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Todavía no tienes pedidos');
  });
});
