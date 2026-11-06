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

  it('muestra la matriz por variante con talla, color, stock y mínimo', () => {
    const { el } = crear();

    const filas = el.querySelectorAll<HTMLTableRowElement>(
      '.inventory-matrix tbody tr',
    );
    expect(filas).toHaveLength(2);

    const azul = filas[0];
    expect(azul.textContent).toContain('4');
    expect(azul.textContent).toContain('Azul');
    expect(azul.textContent).toContain('2');
    expect(azul.textContent).toContain('5');
    expect(azul.querySelector('.color-swatch')).not.toBeNull();

    const rosa = filas[1];
    expect(rosa.textContent).toContain('6');
    expect(rosa.textContent).toContain('Rosa');
    expect(rosa.textContent).toContain('1');
    expect(rosa.textContent).toContain('3');

    expect(el.querySelector('.stock-badge')?.textContent).toContain('Stock bajo');
    expect(el.querySelector('.btn-alerts-count')?.textContent?.trim()).toBe('1');
  });

  it('actualiza el stock por talla y el mínimo, y refresca la fila y las alertas', () => {
    const { fixture, el } = crear();
    el.querySelector<HTMLButtonElement>('.btn-soft')!.click();
    fixture.detectChanges();

    const entradas = el.querySelectorAll<HTMLInputElement>(
      '.inventory-matrix input',
    );    escribir(entradas[0], '10');
    escribir(entradas[1], '4');
    fixture.detectChanges();
    expect(el.querySelector('.stock-editor-footer')?.textContent).toContain('11');

    el.querySelector<HTMLButtonElement>(
      '.inventory-variants .btn-primary',
    )!.click();    const req = http.expectOne('/api/vendedor/inventario/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      tallas: [
        { talla: '4', color: 'AZUL', nuevoColor: null, stock: 10, stockMinimo: 4 },
        { talla: '6', color: 'ROSA', nuevoColor: null, stock: 1, stockMinimo: 3 },
      ],
    });
    req.flush(crearItemInventario({ stockTotal: 11, nivel: 'DISPONIBLE' }));
    http.expectOne('/api/vendedor/inventario/alertas').flush([]);
    fixture.detectChanges();

    expect(el.querySelector('.inventory-matrix input')).toBeNull();
    expect(el.querySelector('tbody .stock-badge')?.textContent).toContain('Disponible');
    expect(el.querySelector('.btn-alerts-count')).toBeNull();
  });

  it('avisa cuando el guardado genera una alerta de stock', () => {
    const { fixture, el } = crear();
    el.querySelector<HTMLButtonElement>('.btn-soft')!.click();
    fixture.detectChanges();

    const entradas = el.querySelectorAll<HTMLInputElement>(
      '.inventory-matrix input',
    );    escribir(entradas[0], '0');
    escribir(entradas[1], '0');
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>(
      '.inventory-variants .btn-primary',
    )!.click();    const req = http.expectOne('/api/vendedor/inventario/7');
    req.flush(crearItemInventario({ stockTotal: 0, nivel: 'AGOTADO' }));
    http.expectOne('/api/vendedor/inventario/alertas').flush([crearAlerta()]);
    fixture.detectChanges();

    expect(el.querySelector('[role="status"]')?.textContent).toContain('Agotado');
  });

  it('no envía valores inválidos', () => {
    const { fixture, el } = crear();

    el.querySelector<HTMLButtonElement>('.btn-soft')!.click();
    fixture.detectChanges();

    const entradas = el.querySelectorAll<HTMLInputElement>(
      '.inventory-matrix input',
    );
    expect(entradas.length).toBeGreaterThan(0);

    escribir(entradas[0], '-3');

    el.querySelector<HTMLButtonElement>(
      '.inventory-variants .btn-primary',
    )!.click();
    fixture.detectChanges();

    expect(
      el.querySelector('.inventory-variants .field-error')?.textContent,
    ).toContain('números enteros');

    http.expectNone('/api/vendedor/inventario/7');
  });

  it('abre directamente la edición de la prenda indicada desde las alertas', () => {
    prendaRuta = '7';
    const { el } = crear();

    expect(el.querySelector('.inventory-matrix input')).not.toBeNull();
  });

  it('abre directamente la edición de la prenda indicada desde las alertas', () => {
    prendaRuta = '7';
    const { el } = crear();

    expect(el.querySelector('.inventory-matrix input')).not.toBeNull();
  });

  it('asigna color a una variante sin color desde Inventario y conserva su stock', () => {
    const varianteSinColor = {
      talla: '4',
      color: null,
      stock: 2,
      stockMinimo: 5,
    };
    const fixture = TestBed.createComponent(Inventario);
    fixture.detectChanges();

    http.expectOne('/api/vendedor/inventario').flush([
      crearItemInventario({ tallas: [varianteSinColor], stockTotal: 2 }),
    ]);
    http.expectOne('/api/vendedor/inventario/alertas').flush([]);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    el.querySelector<HTMLButtonElement>('.btn-soft')!.click();
    fixture.detectChanges();

    const opcionAzul = [...el.querySelectorAll<HTMLButtonElement>(
      '.inventory-variant-color-picker .stock-color-option',
    )].find((opcion) => opcion.textContent?.includes('Azul'));

    expect(opcionAzul).toBeDefined();
    opcionAzul!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.inventory-variants .btn-primary')!.click();

    const req = http.expectOne('/api/vendedor/inventario/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      tallas: [{
        talla: '4',
        color: null,
        nuevoColor: 'AZUL',
        stock: 2,
        stockMinimo: 5,
      }],
    });

    req.flush(crearItemInventario({
      stockTotal: 2,
      tallas: [{ talla: '4', color: 'AZUL', stock: 2, stockMinimo: 5 }],
    }));
    http.expectOne('/api/vendedor/inventario/alertas').flush([]);
    fixture.detectChanges();

    expect(el.querySelector('.inventory-matrix tbody')?.textContent).toContain('Azul');
  });

});


describe('AlertasStock (UI-22)', () => {
  it('muestra talla, actual, mínimo, diferencia y el mensaje según el nivel', async () => {
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
        crearAlerta({ id: 2, prendaId: 9, nombre: 'Conjunto Verano', talla: '6', nivel: 'AGOTADO', stockActual: 0, stockMinimo: 3, diferencia: -3 }),
      ]);
    fixture.detectChanges();

    const tarjetas = (fixture.nativeElement as HTMLElement).querySelectorAll('.alert-card');
    expect(tarjetas.length).toBe(2);
    expect(tarjetas[0].textContent).toContain('Talla');
    expect(tarjetas[0].textContent).toContain('-3');
    expect(tarjetas[0].classList).toContain('alert-card-AGOTADO');
    expect(tarjetas[0].textContent).toContain('Sin stock para venta');
    expect(tarjetas[1].classList).toContain('alert-card-STOCK_BAJO');
    expect(tarjetas[1].textContent).toContain('Quedan 2 unidades; tu mínimo es 5');
    http.verify();
  });
});

