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
    c.form.patchValue({
      estampados: ['LISO'],
      nombre: 'Vestido Floral', categoria: 'VESTIDOS', descripcion: 'Vestido cómodo',
      precio: 45900,
      instruccionesCuidado: '  Lavar a mano en agua fría ',
    });
    c.tallas.at(0).patchValue({
      talla: ' xs ',
      color: 'AZUL',
      estaturaMinCm: 90,
      estaturaMaxCm: 99,
      pesoMinKg: 12,
      pesoMaxKg: 15,
      edadMinMeses: 3,
      edadMaxMeses: 18,
      stock: 6,
      stockMinimo: 2,
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
    const talla = (nombre: string, color: string | null = null) =>
      new FormGroup({ talla: new FormControl(nombre), color: new FormControl(color) });
    expect(tallasValidas(new FormArray([talla('xs'), talla(' XS ')]))).toEqual({ tallaRepetida: 'XS' });
    expect(tallasValidas(new FormArray([talla('4'), talla('6')]))).toBeNull();
    expect(tallasValidas(new FormArray([talla('4', 'AZUL'), talla('4', 'ROSA')]))).toBeNull();
  });

  it('muestra el inventario por talla con sus casillas en la sección Inventario', () => {
    const { fixture, c, el } = crear();
    completar(c);
    fixture.detectChanges();

    const inventario = el.querySelector('[aria-label="Inventario por variante de talla y color"]');
    expect(inventario?.textContent).toContain('xs');
    const stock = inventario?.querySelector<HTMLInputElement>('#stock-0');
    const minimo = inventario?.querySelector<HTMLInputElement>('#minimo-0');
    expect(stock?.value).toBe('6');
    expect(minimo?.value).toBe('2');
  });

  it('no envía la prenda si el formulario es inválido', () => {
    const { fixture, c, el } = crear();

    c.form.patchValue({ nombre: 'Vestido', precio: 45900 });
    c.tallas.at(0).patchValue({ color: 'AZUL' });
    c.composicion.at(0).patchValue({ porcentaje: 60 });

    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(el.textContent).toContain('Revisa los campos marcados');
    expect(el.textContent).toContain('La composición debe sumar 100%');
    expect(el.textContent).toContain('Indica cómo lavar y cuidar la prenda');
    http.expectNone('/api/vendedor/prendas');
  });

  it('avisa cuando la edad mínima en meses supera a la máxima', () => {
    const { fixture, c, el } = crear();
    completar(c);
    c.tallas.at(0).patchValue({ edadMinMeses: 24, edadMaxMeses: 6 });
    c.tallas.at(0).markAllAsTouched();
    fixture.detectChanges();

    expect(el.textContent).toContain('La edad mínima supera a la máxima');
    http.expectNone('/api/vendedor/prendas');
  });

  it('crea la prenda con sus fotos en una petición multipart y vuelve al catálogo', async () => {
    const navegar = vi.mocked(TestBed.inject(Router).navigate);
    const { fixture, c, el } = crear();
    completar(c);
    seleccionarFoto(el, new File(['x'], 'frente.png', { type: 'image/png' }));
    fixture.detectChanges();
    expect(el.querySelector('.photo-badge-pending')?.textContent).toContain('Por subir');

    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    const crearReq = http.expectOne('/api/vendedor/prendas');
    expect(crearReq.request.method).toBe('POST');
    expect(crearReq.request.body).toBeInstanceOf(FormData);
    const datos = crearReq.request.body as FormData;
    const partePrenda = datos.get('prenda') as Blob;
    expect(partePrenda.type).toBe('application/json');
    const request = JSON.parse(await partePrenda.text());
    expect(request.tallas[0]).toEqual({
      talla: 'XS', estaturaMinCm: 90, estaturaMaxCm: 99, pesoMinKg: 12, pesoMaxKg: 15,
      edadMinMeses: 3, edadMaxMeses: 18, stock: 6,
      stockMinimo: 2,
      color: 'AZUL',
    });
    expect(request.composicion).toEqual([{ material: 'ALGODON', porcentaje: 100 }]);
    expect(request.instruccionesCuidado).toBe('Lavar a mano en agua fría');
    expect(datos.getAll('archivos')).toHaveLength(1);
    crearReq.flush(crearPrenda({ id: 40 }));
    http.expectNone('/api/vendedor/prendas/40/imagenes');

    expect(navegar).toHaveBeenCalledWith(['/vendedor/productos'], {
      state: { mensaje: 'Producto creado correctamente.' },
    });
  });

  it('si falla el alta multipart conserva la foto pendiente para reintentar', () => {
    const { fixture, c, el } = crear();
    completar(c);
    seleccionarFoto(el, new File(['x'], 'frente.png', { type: 'image/png' }));
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    http.expectOne('/api/vendedor/prendas').flush(
      { mensaje: 'No fue posible guardar la prenda y sus imágenes' },
      { status: 502, statusText: 'Bad Gateway' },
    );
    fixture.detectChanges();

    expect(el.querySelector('h1')?.textContent).toContain('Crear producto');
    expect(el.textContent).toContain('No fue posible guardar la prenda y sus imágenes');
    expect(c.pendientes().length).toBe(1);
  });

  it('rechaza archivos que no son imágenes sin agregarlos', () => {
    const { fixture, c, el } = crear();
    seleccionarFoto(el, new File(['x'], 'doc.pdf', { type: 'application/pdf' }));
    fixture.detectChanges();

    expect(el.textContent).toContain('doc.pdf no es una imagen JPG, PNG o WEBP');
    expect(c.pendientes().length).toBe(0);
  });

  it('en edición oculta el inventario inicial y conserva el stock fuera del request de producto', () => {
    idRuta = '7';
    const { fixture, c, el } = crear();
    http.expectOne('/api/vendedor/prendas/7').flush(crearPrenda({ descripcion: 'Prenda cómoda' }));
    fixture.detectChanges();

    expect(
      el.querySelector('[aria-label="Inventario por variante de talla y color"]'),
    ).toBeNull();
    expect(el.textContent).toContain('El stock y el mínimo de alerta se administran en Inventario.');

    c.form.patchValue({ precio: 35000 });
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));

    const req = http.expectOne('/api/vendedor/prendas/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.precio).toBe(35000);
    expect(req.request.body.tallas.map((t: { stock: number | null }) => t.stock)).toEqual([null, null]);
    req.flush(crearPrenda());
  });
});
