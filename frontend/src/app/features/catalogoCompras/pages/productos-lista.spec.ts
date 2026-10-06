import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { crearPrenda } from '../testing/catalogo-testing';
import { ProductosLista } from './productos-lista';

describe('ProductosLista (UI-19)', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductosLista],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(ProductosLista);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('muestra cada prenda con composición, precio, stock y nivel', () => {
    const { fixture, el } = crear();
    http.expectOne('/api/vendedor/prendas').flush([crearPrenda()]);
    fixture.detectChanges();

    const fila = el.querySelector('tbody tr')!;
    expect(fila.textContent).toContain('Pantalón Jogger Azul');
    expect(fila.textContent).toContain('80% Algodón, 20% Poliéster');
    expect(fila.textContent).toContain('Pantalones');
    expect(fila.textContent).toMatch(/\$\s?32\.900/);
    expect(fila.querySelector('.stock-badge')?.textContent).toContain('Stock bajo');
    expect(fila.querySelector('img')?.getAttribute('src')).toBe('/media/prendas/7/a.png');
  });

  it('filtra por nombre o categoría sin distinguir tildes', () => {
    const { fixture, el } = crear();
    http
      .expectOne('/api/vendedor/prendas')
      .flush([crearPrenda(), crearPrenda({ id: 8, nombre: 'Vestido Floral', categoria: 'VESTIDOS' })]);
    fixture.detectChanges();

    const buscador = el.querySelector<HTMLInputElement>('input[type="search"]')!;
    buscador.value = 'pantalon';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(el.querySelectorAll('tbody tr').length).toBe(1);

    buscador.value = 'vestidos';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(el.querySelector('tbody tr')?.textContent).toContain('Vestido Floral');
  });

  it('oculta una prenda del catálogo con el botón de visibilidad', () => {
    const { fixture, el } = crear();
    http.expectOne('/api/vendedor/prendas').flush([crearPrenda()]);
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('button[aria-label^="Ocultar"]')!.click();
    const req = http.expectOne('/api/vendedor/prendas/7/estado');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'INACTIVA' });
    req.flush(crearPrenda({ estado: 'INACTIVA' }));
    fixture.detectChanges();

    expect(el.querySelector('.state-chip')?.textContent).toContain('Inactivo');
  });

  it('invita a crear el primer producto cuando el catálogo está vacío', () => {
    const { fixture, el } = crear();
    http.expectOne('/api/vendedor/prendas').flush([]);
    fixture.detectChanges();

    expect(el.textContent).toContain('Aún no tienes productos');
  });
});
