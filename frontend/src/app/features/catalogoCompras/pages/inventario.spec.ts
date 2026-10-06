import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { crearAlerta, crearItemInventario } from '../testing/catalogo-testing';
import { AlertasStock } from './alertas';
import { Inventario } from './inventario';

describe('Inventario (UI-21)', () => {
  let http: HttpTestingController;
  let prendaRuta: string | null;

  beforeEach(async () => {
    prendaRuta = null;
    await TestBed.configureTestingModule({
      imports: [Inventario],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: { queryParamMap: convertToParamMap(prendaRuta ? { prenda: prendaRuta } : {}) },
          }),
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(Inventario);
    fixture.detectChanges();
    http.expectOne('/api/vendedor/inventario').flush([crearItemInventario()]);
    http.expectOne('/api/vendedor/inventario/alertas').flush([crearAlerta()]);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  function escribir(input: HTMLInputElement, valor: string) {
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  it('lista stock actual, mínimo, estado y el número de alertas activas', () => {
    const { el } = crear();

    const fila = el.querySelector('tbody tr')!;
    expect(fila.querySelector('.stock-number')?.textContent?.trim()).toBe('3');
    expect(fila.textContent).toContain('5');
    expect(fila.querySelector('.stock-badge')?.textContent).toContain('Stock bajo');
    expect(el.querySelector('.btn-alerts-count')?.textContent?.trim()).toBe('1');
  });

  it('actualiza el stock por talla y el mínimo, y refresca la fila y las alertas', () => {
    const { fixture, el } = crear();
    el.querySelector<HTMLButtonElement>('.btn-soft')!.click();
    fixture.detectChanges();

    const entradas = el.querySelectorAll<HTMLInputElement>('.stock-editor input');
    escribir(entradas[0], '10');
    escribir(entradas[2], '4');
    fixture.detectChanges();
    expect(el.querySelector('.stock-editor-footer')?.textContent).toContain('11');

    el.querySelector<HTMLButtonElement>('.stock-editor .btn-primary')!.click();
    const req = http.expectOne('/api/vendedor/inventario/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      tallas: [
        { talla: '4', stock: 10 },
        { talla: '6', stock: 1 },
      ],
      stockMinimo: 4,
    });
    req.flush(crearItemInventario({ stockTotal: 11, stockMinimo: 4, nivel: 'DISPONIBLE' }));
    http.expectOne('/api/vendedor/inventario/alertas').flush([]);
    fixture.detectChanges();

    expect(el.querySelector('.stock-editor')).toBeNull();
    expect(el.querySelector('tbody .stock-badge')?.textContent).toContain('Disponible');
    expect(el.querySelector('.btn-alerts-count')).toBeNull();
  });

  it('no envía valores inválidos', () => {
    const { fixture, el } = crear();
    el.querySelector<HTMLButtonElement>('.btn-soft')!.click();
    fixture.detectChanges();

    escribir(el.querySelectorAll<HTMLInputElement>('.stock-editor input')[0], '-3');
    el.querySelector<HTMLButtonElement>('.stock-editor .btn-primary')!.click();
    fixture.detectChanges();

    expect(el.querySelector('.stock-editor .field-error')?.textContent).toContain('números enteros');
    http.expectNone('/api/vendedor/inventario/7');
  });

  it('abre directamente la edición de la prenda indicada desde las alertas', () => {
    prendaRuta = '7';
    const { el } = crear();

    expect(el.querySelector('.stock-editor')).not.toBeNull();
  });
});

describe('AlertasStock (UI-22)', () => {
  it('muestra actual, mínimo, diferencia y el mensaje según el nivel', async () => {
    await TestBed.configureTestingModule({
      imports: [AlertasStock],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AlertasStock);
    fixture.detectChanges();
    http
      .expectOne('/api/vendedor/inventario/alertas')
      .flush([
        crearAlerta(),
        crearAlerta({ id: 2, prendaId: 9, nombre: 'Conjunto Verano', nivel: 'AGOTADO', stockActual: 0, stockMinimo: 3, diferencia: -3 }),
      ]);
    fixture.detectChanges();

    const tarjetas = (fixture.nativeElement as HTMLElement).querySelectorAll('.alert-card');
    expect(tarjetas.length).toBe(2);
    expect(tarjetas[0].textContent).toContain('-2');
    expect(tarjetas[0].textContent).toContain('Requiere reabastecimiento');
    expect(tarjetas[1].classList).toContain('alert-card-AGOTADO');
    expect(tarjetas[1].textContent).toContain('Sin stock para venta');
    http.verify();
  });
});
