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

// === Punto 12 (UI-20): sugerencias de medidas por talla ===

function clickPorTexto(el: HTMLElement, texto: string): HTMLButtonElement {
  const boton = [...el.querySelectorAll<HTMLButtonElement>('button')]
    .find((b) => b.textContent?.includes(texto));
  expect(boton, `falta el botón «${texto}»`).toBeTruthy();
  boton!.click();
  return boton!;
}

describe('PrendaForm — sugerencias de medidas (Punto 12)', () => {
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
          useFactory: () => ({
            snapshot: { paramMap: convertToParamMap(idRuta ? { id: idRuta } : {}) },
          }),
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(PrendaForm);
    fixture.detectChanges();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = fixture.componentInstance as any;
    return { fixture, c, el: fixture.nativeElement as HTMLElement };
  }

  it('muestra la sugerencia visible con la talla normalizada y la aplica con el botón', () => {
    const { fixture, c, el } = crear();
    c.tallas.at(0).patchValue({ talla: ' 4 ', color: 'AZUL' });
    fixture.detectChanges();

    const ayuda = el.querySelector('.talla-ayuda')!;
    expect(ayuda.textContent).toContain('Medidas habituales para la talla 4');
    expect(ayuda.textContent).toContain('98–110 cm · 15–19 kg · 36–48 meses');

    // Autocompletado accesible con la tabla de referencia.
    expect(el.querySelector('#tallas-referencia-0')).toBeTruthy();
    expect([...el.querySelectorAll('#tallas-referencia-0 option')].map((o) => o.getAttribute('value'))).toContain('16');

    clickPorTexto(el, 'Aplicar medidas sugeridas');
    const fila = c.tallas.at(0).getRawValue();
    expect(fila.estaturaMinCm).toBe(98);
    expect(fila.estaturaMaxCm).toBe(110);
    expect(fila.pesoMinKg).toBe(15);
    expect(fila.pesoMaxKg).toBe(19);
    expect(fila.edadMinMeses).toBe(36);
    expect(fila.edadMaxMeses).toBe(48);
  });

  it('no sobrescribe sin acción: escribir la talla deja intactas las medidas ya digitadas', () => {
    const { fixture, c, el } = crear();
    c.tallas.at(0).patchValue({
      talla: 'XS',
      color: 'AZUL',
      estaturaMinCm: 96,
      estaturaMaxCm: 108,
    });
    fixture.detectChanges();

    expect(el.textContent).toContain('Medidas habituales para la talla XS');
    let fila = c.tallas.at(0).getRawValue();
    expect(fila.estaturaMinCm).toBe(96);
    expect(fila.estaturaMaxCm).toBe(108);

    // Solo el botón explícito rellena, y las medidas siguen editables después.
    clickPorTexto(el, 'Aplicar medidas sugeridas');
    fila = c.tallas.at(0).getRawValue();
    expect(fila.estaturaMinCm).toBe(100);
    expect(fila.estaturaMaxCm).toBe(112);
    c.tallas.at(0).patchValue({ estaturaMaxCm: 118 });
    expect(c.tallas.at(0).getRawValue().estaturaMaxCm).toBe(118);
  });

  it('reutiliza las medidas definidas en otro color para respetar los mismos rangos', () => {
    const { fixture, c, el } = crear();
    c.tallas.at(0).patchValue({
      talla: '6',
      color: 'AZUL',
      estaturaMinCm: 108,
      estaturaMaxCm: 124,
      pesoMinKg: 19,
      pesoMaxKg: 25,
      edadMinMeses: 48,
      edadMaxMeses: 72,
    });
    c.agregarTalla();
    c.tallas.at(1).patchValue({ talla: ' 6 ', color: 'ROSA' });
    fixture.detectChanges();

    const ayuda = el.querySelector('#talla-ayuda-1')!;
    expect(ayuda.textContent).toContain('Mismas medidas que la talla 6 en Azul');
    expect(ayuda.textContent).not.toContain('Aplicar medidas sugeridas');

    ayuda.querySelector<HTMLButtonElement>('.talla-ayuda-boton')!.click();
    const copiada = c.tallas.at(1).getRawValue();

    expect(copiada.estaturaMinCm).toBe(108);
    expect(copiada.estaturaMaxCm).toBe(124);
    expect(copiada.pesoMaxKg).toBe(25);
    expect(copiada.edadMinMeses).toBe(48);
    expect(copiada.edadMaxMeses).toBe(72);
  });

  it('muestra la advertencia de medidas atípicas comparada con la referencia', () => {
    const { fixture, c, el } = crear();
    c.composicion.at(0).patchValue({ porcentaje: 100 });
    c.tallas.at(0).patchValue({
      talla: '4',
      color: 'AZUL',
      estaturaMinCm: 40,
      estaturaMaxCm: 200,
      pesoMinKg: 15,
      pesoMaxKg: 19,
      edadMinMeses: 36,
      edadMaxMeses: 48,
    });
    fixture.detectChanges();

    const advertencia = el.querySelector('.talla-ayuda-advertencia')!;
    expect(advertencia.textContent).toContain('se alejan de lo habitual para la talla 4');
    expect(advertencia.textContent).toContain('98–110 cm');
    expect(advertencia.closest('[role="status"]')?.getAttribute('aria-live')).toBe('polite');
  });

  it('no ofrece sugerencias sobre variantes ya guardadas en edición', () => {
    idRuta = '7';
    const { fixture, el } = crear();
    http.expectOne('/api/vendedor/prendas/7').flush(crearPrenda());
    fixture.detectChanges();

    expect(el.querySelector('.talla-ayuda')).toBeNull();
    expect(el.querySelector('.talla-ayuda-advertencia')).toBeNull();
  });
});

// === Punto 3 (UI-20): alergias del formulario coherentes con el perfil infantil ===

describe('PrendaForm — alergias declaradas y detectadas (Punto 3)', () => {
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
          useFactory: () => ({
            snapshot: { paramMap: convertToParamMap(idRuta ? { id: idRuta } : {}) },
          }),
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(PrendaForm);
    fixture.detectChanges();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = fixture.componentInstance as any;
    return { fixture, c, el: fixture.nativeElement as HTMLElement };
  }

  function bloqueoAlergias(el: HTMLElement): HTMLElement {
    const bloque = [...el.querySelectorAll('fieldset')].find((f) =>
      f.textContent?.includes('Información para alergias'),
    );
    expect(bloque, 'falta el fieldset «Información para alergias»').toBeTruthy();
    return bloque!;
  }

  it('muestra las 4 alergias declarables con etiqueta del perfil y el grupo detectado sin controles', () => {
    const { el } = crear();
    const bloque = bloqueoAlergias(el);
    const texto = bloque.textContent ?? '';

    ['Tintes', 'Broches metálicos', 'Níquel', 'Formaldehído'].forEach((etiqueta) => {
      expect(texto).toContain(etiqueta);
    });

    // Los del grupo declarado son checkboxes; el grupo de composición nunca los ofrece.
    const declarada = bloque.querySelector('.catalog-checks label');
    expect(declarada?.textContent).toContain('Tintes');
    expect(texto).toContain('Declara tú');
    expect(texto).toContain('Se detecta por la composición');
    const inputsEnChips = bloque.querySelectorAll('.alergia-chip input');
    expect(inputsEnChips.length).toBe(0);
  });

  it('chips en vivo: la composición por defecto detecta algodón y la edición lo reemplaza por sintéticos', () => {
    const { fixture, c, el } = crear();
    // Composición inicial del formulario: ALGODON 100
    let bloque = bloqueoAlergias(el);
    expect(bloque.textContent).toContain('Algodón');
    expect(bloque.textContent).toContain('Contiene algodón');
    expect(bloque.textContent).not.toContain('Fibras sintéticas');

    // El vendedor edita la composición: 40% poliéster ya supera el umbral del 30%
    c.composicion.at(0).patchValue({ material: 'POLIESTER', porcentaje: 40 });
    fixture.detectChanges();

    bloque = bloqueoAlergias(el);
    const texto = bloque.textContent ?? '';
    expect(texto).toContain('Fibras sintéticas');
    expect((new RegExp('Contiene 40% fibras sintéticas')).test(texto)).toBe(true);
    expect(texto).not.toContain('Contiene algodón');
  });

  it('no ofrece checkboxes para las alergias detectadas: están a cargo de la composición', () => {
    const { el } = crear();
    const bloque = bloqueoAlergias(el);
    const etiquetasDelGrupo = bloque.querySelectorAll('.catalog-checks label');
    expect(etiquetasDelGrupo.length).toBe(4);
  });
});
