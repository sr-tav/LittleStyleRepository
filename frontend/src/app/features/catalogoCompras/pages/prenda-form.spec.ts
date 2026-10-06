import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FormArray, FormControl, FormGroup } from '@angular/forms';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';

import { crearPrenda } from '../testing/catalogo-testing';
import { composicionValida, PrendaForm, tallasValidas } from './prenda-form';

describe('PrendaForm (UI-20)', () => {
  let http: HttpTestingController;
  let idRuta: string | null;

  beforeEach(async () => {
    idRuta = null;
    URL.createObjectURL = vi.fn(() => 'blob:vista');
    URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [PrendaForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useFactory: () => ({ snapshot: { paramMap: convertToParamMap(idRuta ? { id: idRuta } : {}) } }),
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    // Al guardar se vuelve al catálogo; en las pruebas no hay rutas registradas
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(PrendaForm);
    fixture.detectChanges();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = fixture.componentInstance as any;
    return { fixture, c, el: fixture.nativeElement as HTMLElement };
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  function completar(c: any) {
    c.form.patchValue({ nombre: 'Vestido Floral', categoria: 'VESTIDOS', precio: 45900, stockMinimo: 3 });
    c.tallas.at(0).patchValue({
      talla: ' xs ', estaturaMinCm: 90, estaturaMaxCm: 99, pesoMinKg: 12, pesoMaxKg: 15, stock: 6,
    });
  }

  function seleccionarFoto(el: HTMLElement, archivo: File) {
    const input = el.querySelector<HTMLInputElement>('input[type="file"]')!;
    Object.defineProperty(input, 'files', { value: [archivo], configurable: true });
    input.dispatchEvent(new Event('change'));
  }

  it('valida que la composición sume 100 y no repita materiales', () => {
    const fila = (material: string, porcentaje: number) =>
      new FormGroup({ material: new FormControl(material), porcentaje: new FormControl(porcentaje) });

    expect(composicionValida(new FormArray([fila('ALGODON', 100)]))).toBeNull();
    expect(composicionValida(new FormArray([fila('ALGODON', 70), fila('ELASTANO', 20)]))).toEqual({ suma: 90 });
    expect(composicionValida(new FormArray([fila('ALGODON', 50), fila('ALGODON', 50)]))).toEqual({
      materialRepetido: true,
    });
  });

  it('detecta tallas repetidas sin distinguir mayúsculas ni espacios', () => {
    const talla = (nombre: string) => new FormGroup({ talla: new FormControl(nombre) });
    expect(tallasValidas(new FormArray([talla('xs'), talla(' XS ')]))).toEqual({ tallaRepetida: 'XS' });
    expect(tallasValidas(new FormArray([talla('4'), talla('6')]))).toBeNull();
  });

  it('no envía la prenda si el formulario es inválido', () => {
    const { fixture, c, el } = crear();
    c.form.patchValue({ nombre: 'Vestido', precio: 45900 });
    c.composicion.at(0).patchValue({ porcentaje: 60 });

    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(el.textContent).toContain('Revisa los campos marcados');
    expect(el.textContent).toContain('La composición debe sumar 100%');
    http.expectNone('/api/vendedor/prendas');
  });

  it('crea la prenda, sube las fotos en una sola petición y vuelve al catálogo', () => {
    const navegar = vi.mocked(TestBed.inject(Router).navigate);
    const { fixture, c, el } = crear();
    completar(c);
    seleccionarFoto(el, new File(['x'], 'frente.png', { type: 'image/png' }));
    fixture.detectChanges();
    expect(el.querySelector('.photo-badge-pending')?.textContent).toContain('Por subir');

    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    const crearReq = http.expectOne('/api/vendedor/prendas');
    expect(crearReq.request.method).toBe('POST');
    expect(crearReq.request.body.tallas[0]).toEqual({
      talla: 'XS', estaturaMinCm: 90, estaturaMaxCm: 99, pesoMinKg: 12, pesoMaxKg: 15, stock: 6,
    });
    expect(crearReq.request.body.composicion).toEqual([{ material: 'ALGODON', porcentaje: 100 }]);
    crearReq.flush(crearPrenda({ id: 40 }));

    const fotos = http.expectOne('/api/vendedor/prendas/40/imagenes');
    expect((fotos.request.body as FormData).getAll('archivos').length).toBe(1);
    fotos.flush([{ id: 1, url: '/media/x.png', orden: 0 }]);

    expect(navegar).toHaveBeenCalledWith(['/vendedor/productos'], {
      state: { mensaje: 'Producto creado correctamente.' },
    });
  });

  it('si fallan las fotos conserva la prenda creada y pasa a edición para reintentar', () => {
    const location = TestBed.inject(Location);
    const reemplazar = vi.spyOn(location, 'replaceState');
    const { fixture, c, el } = crear();
    completar(c);
    seleccionarFoto(el, new File(['x'], 'frente.png', { type: 'image/png' }));
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    http.expectOne('/api/vendedor/prendas').flush(crearPrenda({ id: 40 }));
    http
      .expectOne('/api/vendedor/prendas/40/imagenes')
      .flush({ mensaje: 'No fue posible guardar las imágenes' }, { status: 502, statusText: 'Bad Gateway' });
    fixture.detectChanges();

    expect(reemplazar).toHaveBeenCalledWith('/vendedor/productos/40');
    expect(el.querySelector('h1')?.textContent).toContain('Editar producto');
    expect(el.textContent).toContain('La prenda se guardó, pero las fotos no se pudieron subir');
    expect(c.pendientes().length).toBe(1);

    el.querySelector<HTMLButtonElement>('.alert-error .link-button')!.click();
    http.expectOne('/api/vendedor/prendas/40').flush(crearPrenda({ id: 40 }));
    http.expectOne('/api/vendedor/prendas/40/imagenes').flush([]);
  });

  it('rechaza archivos que no son imágenes sin agregarlos', () => {
    const { fixture, c, el } = crear();
    seleccionarFoto(el, new File(['x'], 'doc.pdf', { type: 'application/pdf' }));
    fixture.detectChanges();

    expect(el.textContent).toContain('doc.pdf no es una imagen JPG, PNG o WEBP');
    expect(c.pendientes().length).toBe(0);
  });

  it('en edición muestra el stock existente sin editarlo y no lo envía', () => {
    idRuta = '7';
    const { fixture, c, el } = crear();
    http.expectOne('/api/vendedor/prendas/7').flush(crearPrenda());
    fixture.detectChanges();

    const stock = el.querySelector<HTMLInputElement>('#stock-0')!;
    expect(stock.disabled).toBe(true);
    expect(stock.value).toBe('2');
    expect(el.querySelector('.photo-badge')?.textContent).toContain('Principal');

    c.form.patchValue({ precio: 35000 });
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    const req = http.expectOne('/api/vendedor/prendas/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.precio).toBe(35000);
    expect(req.request.body.tallas.map((t: { stock: number | null }) => t.stock)).toEqual([null, null]);
    req.flush(crearPrenda());
  });
});
