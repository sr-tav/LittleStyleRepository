import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import { PerfilInfantil } from '../models/perfil-infantil.models';
import { PerfilDetalle } from './perfil-detalle';

const perfilEjemplo: PerfilInfantil = {
  id: 7,
  nombre: 'Mateo',
  fechaNacimiento: '2020-05-10',
  edadAnios: 6,
  mesesRestantes: 4,
  contextura: 'MEDIA',
  holgura: 'REGULAR',
  alergias: [],
  otraAlergia: null,
  sinAlergias: true,
  coloresPreferidos: ['AZUL'],
  estampadosPreferidos: [],
  otroColor: null,
  otroEstampado: null,
  fechaCreacion: '2025-01-01T12:00:00',
  fechaActualizacion: '2025-01-01T12:00:00',
  ultimaMedicion: null,
  porcentajeCompletitud: 75,
};

describe('PerfilDetalle (UI-5)', () => {
  let httpMock: HttpTestingController;
  let router: Router;
  let idRuta = 'nuevo';
  let editarRuta = true;

  beforeEach(async () => {
    idRuta = 'nuevo';
    editarRuta = true;
    await TestBed.configureTestingModule({
      imports: [PerfilDetalle],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: {
              paramMap: { get: () => idRuta },
              queryParamMap: convertToParamMap({ editar: editarRuta ? 'true' : null }),
            },
          }),
        },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  function crear() {
    const fixture = TestBed.createComponent(PerfilDetalle);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const input = (id: string, value: string) => {
      const element = el.querySelector<HTMLInputElement>(`#${id}`)!;
      element.value = value;
      element.dispatchEvent(new Event('input'));
    };
    const toggleAlergia = (value: string, checked = true) => {
      const element = el.querySelector<HTMLInputElement>(`input[aria-label="${value}"]`)!;
      element.checked = checked;
      element.dispatchEvent(new Event('change'));
      fixture.detectChanges();
    };
    const select = (id: string, value: string) => {
      const element = el.querySelector<HTMLSelectElement>(`#${id}`)!;
      element.value = value;
      element.dispatchEvent(new Event('change'));
    };
    const clickButton = (text: string) => {
      const button = Array.from(el.querySelectorAll('button')).find((item) =>
        item.textContent?.includes(text),
      );
      button?.click();
    };
    const submitProfile = () => {
      el.querySelector<HTMLFormElement>('.profile-form')!.dispatchEvent(
        new Event('submit', { bubbles: true, cancelable: true }),
      );
    };
    const submitCurrentForm = () => {
      el.querySelector<HTMLFormElement>('form.profile-form, form.measurement-form')!.dispatchEvent(
        new Event('submit', { bubbles: true, cancelable: true }),
      );
    };
    return {
      fixture,
      el,
      input,
      select,
      toggleAlergia,
      clickButton,
      submitProfile,
      submitCurrentForm,
    };
  }

  function completarFormulario(creado: ReturnType<typeof crear>): void {
    creado.input('nombre', 'Lucía');
    creado.input('fechaNacimiento', '2022-04-12');
    creado.select('contextura', 'MEDIA');
    creado.select('holgura', 'REGULAR');
    const checkbox = creado.el.querySelector<HTMLInputElement>('#sinAlergias')!;
    checkbox.checked = true;
    checkbox.dispatchEvent(new Event('change'));
    creado.input('fechaMedicion', '2025-04-15');
    creado.input('estaturaCm', '110');
    creado.input('pesoKg', '20');
    creado.fixture.detectChanges();
  }

  function cargarPerfil(creado: ReturnType<typeof crear>): void {
    httpMock.expectOne('/api/cliente/perfiles/7').flush(perfilEjemplo);
    httpMock.expectOne('/api/cliente/perfiles/7/mediciones').flush([]);
    creado.fixture.detectChanges();
  }

  it('valida los datos obligatorios y alergias antes de enviar', () => {
    const { fixture, el, clickButton, input } = crear();
    input('nombre', '   ');
    clickButton('Crear perfil');
    fixture.detectChanges();

    expect(el.textContent).toContain('El nombre es obligatorio.');
    expect(el.textContent).toContain('La fecha de nacimiento es obligatoria.');
    expect(el.textContent).toContain('Declare las alergias o indique que no tiene alergias.');
    httpMock.expectNone('/api/cliente/perfiles');
  });

  it('limpia y desactiva las alergias al marcar Sin alergias', () => {
    const creado = crear();
    creado.toggleAlergia('Lana');
    const noAlergias = creado.el.querySelector<HTMLInputElement>('#sinAlergias')!;
    noAlergias.checked = true;
    noAlergias.dispatchEvent(new Event('change'));
    creado.fixture.detectChanges();

    expect(creado.el.querySelector<HTMLInputElement>('input[aria-label="Lana"]')?.checked).toBe(
      false,
    );
    expect(creado.el.querySelector<HTMLInputElement>('input[aria-label="Lana"]')?.disabled).toBe(
      true,
    );
  });

  it('exige el texto de Otra alergia y muestra el campo al seleccionarla', () => {
    const creado = crear();
    const noAlergias = creado.el.querySelector<HTMLInputElement>('#sinAlergias')!;
    noAlergias.checked = false;
    noAlergias.dispatchEvent(new Event('change'));
    creado.toggleAlergia('Otra alergia');
    creado.input('nombre', 'Lucía');
    creado.input('fechaNacimiento', '2022-04-12');
    creado.select('contextura', 'MEDIA');
    creado.select('holgura', 'REGULAR');
    creado.input('fechaMedicion', '2025-04-15');
    creado.input('estaturaCm', '110');
    creado.input('pesoKg', '20');
    creado.clickButton('Crear perfil');
    creado.fixture.detectChanges();

    expect(creado.el.querySelector<HTMLInputElement>('#otraAlergia')?.required).toBe(true);
    expect(creado.el.textContent).toContain('Describe la otra alergia');
    httpMock.expectNone('/api/cliente/perfiles');
  });

  it('selecciona chips y bloquea nuevas opciones al alcanzar diez', () => {
    const creado = crear();
    const botones = Array.from(creado.el.querySelectorAll<HTMLButtonElement>('.color-option'));
    for (const boton of botones.slice(0, 10)) boton.click();
    creado.fixture.detectChanges();

    expect(botones[0].getAttribute('aria-pressed')).toBe('true');
    expect(botones[10].disabled).toBe(true);
    expect(creado.el.textContent).toContain('Límite alcanzado: máximo 10 colores.');
  });

  it('crea el perfil una vez aunque se envíe varias veces mientras guarda', () => {
    const creado = crear();
    completarFormulario(creado);
    creado.clickButton('Crear perfil');
    creado.submitProfile();

    const request = httpMock.expectOne('/api/cliente/perfiles');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      nombre: 'Lucía',
      fechaNacimiento: '2022-04-12',
      contextura: 'MEDIA',
      holgura: 'REGULAR',
      alergias: [],
      otraAlergia: null,
      sinAlergias: true,
      coloresPreferidos: [],
      estampadosPreferidos: [],
      otroColor: null,
      otroEstampado: null,
    });
    creado.submitProfile();
    httpMock.expectNone('/api/cliente/perfiles');
    creado.fixture.detectChanges();
    request.flush({ ...perfilEjemplo, id: 31, nombre: 'Lucía' });
    creado.fixture.detectChanges();

    const medicion = httpMock.expectOne('/api/cliente/perfiles/31/mediciones');
    expect(medicion.request.method).toBe('POST');
    expect(medicion.request.body).toEqual({
      fechaMedicion: '2025-04-15',
      estaturaCm: 110,
      pesoKg: 20,
    });
    expect(creado.el.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(
      true,
    );
    creado.submitCurrentForm();
    httpMock.expectNone('/api/cliente/perfiles');
    httpMock.expectNone('/api/cliente/perfiles/31/mediciones');
    medicion.flush({
      id: 91,
      fechaMedicion: '2025-04-15',
      estaturaCm: 110,
      pesoKg: 20,
    });
    expect(router.navigate).toHaveBeenCalledWith(['/cliente/hijos'], {
      queryParams: { resultado: 'creado' },
    });
  });

  it('conserva el formulario y permite reintentar después de un error del backend', () => {
    const creado = crear();
    completarFormulario(creado);
    creado.clickButton('Crear perfil');

    const primerIntento = httpMock.expectOne('/api/cliente/perfiles');
    primerIntento.flush(
      { mensaje: 'Hay campos con errores de validación', errores: { nombre: 'Revisa el nombre' } },
      { status: 400, statusText: 'Bad Request' },
    );
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('Revisa el nombre');
    expect(creado.el.querySelector<HTMLInputElement>('#nombre')?.value).toBe('Lucía');
    creado.clickButton('Crear perfil');

    const segundoIntento = httpMock.expectOne('/api/cliente/perfiles');
    expect(segundoIntento.request.method).toBe('POST');
    expect(segundoIntento.request.body.nombre).toBe('Lucía');
    segundoIntento.flush({ ...perfilEjemplo, id: 31, nombre: 'Lucía' });
    const medicion = httpMock.expectOne('/api/cliente/perfiles/31/mediciones');
    medicion.flush({
      id: 91,
      fechaMedicion: '2025-04-15',
      estaturaCm: 110,
      pesoKg: 20,
    });
    expect(router.navigate).toHaveBeenCalledWith(['/cliente/hijos'], {
      queryParams: { resultado: 'creado' },
    });
  });

  it('si falla la medición conserva el perfil y reintenta solo esa petición', () => {
    const creado = crear();
    completarFormulario(creado);
    creado.clickButton('Crear perfil');

    const perfilRequest = httpMock.expectOne('/api/cliente/perfiles');
    perfilRequest.flush({ ...perfilEjemplo, id: 31, nombre: 'Lucía' });
    creado.fixture.detectChanges();

    const primeraMedicion = httpMock.expectOne('/api/cliente/perfiles/31/mediciones');
    primeraMedicion.flush(
      { mensaje: 'No se pudo guardar la medición.' },
      { status: 400, statusText: 'Bad Request' },
    );
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('El perfil ya se creó.');
    expect(creado.el.textContent).toContain('No se pudo guardar la medición.');
    expect(creado.el.querySelector<HTMLInputElement>('#nombre')).toBeNull();
    expect(creado.el.querySelector<HTMLInputElement>('#fechaMedicion')?.value).toBe('2025-04-15');
    expect(creado.el.querySelector<HTMLInputElement>('#estaturaCm')?.value).toBe('110');
    expect(creado.el.querySelector<HTMLInputElement>('#pesoKg')?.value).toBe('20');

    creado.clickButton('Reintentar medición');
    const reintento = httpMock.expectOne('/api/cliente/perfiles/31/mediciones');
    expect(reintento.request.method).toBe('POST');
    expect(reintento.request.body).toEqual({
      fechaMedicion: '2025-04-15',
      estaturaCm: 110,
      pesoKg: 20,
    });
    creado.submitCurrentForm();
    httpMock.expectNone('/api/cliente/perfiles');
    httpMock.expectNone('/api/cliente/perfiles/31/mediciones');
    reintento.flush({
      id: 92,
      fechaMedicion: '2025-04-15',
      estaturaCm: 110,
      pesoKg: 20,
    });

    expect(router.navigate).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['/cliente/hijos'], {
      queryParams: { resultado: 'creado' },
    });
  });

  it('actualiza el perfil existente y vuelve al listado sin crear otro', () => {
    idRuta = '7';
    const creado = crear();
    cargarPerfil(creado);
    creado.input('nombre', 'Mateo actualizado');
    creado.clickButton('Guardar cambios');

    const request = httpMock.expectOne({ method: 'PUT', url: '/api/cliente/perfiles/7' });
    expect(request.request.body.nombre).toBe('Mateo actualizado');
    httpMock.expectNone({ method: 'POST', url: '/api/cliente/perfiles' });
    request.flush({ ...perfilEjemplo, nombre: 'Mateo actualizado' });

    expect(router.navigate).toHaveBeenCalledWith(['/cliente/hijos'], {
      queryParams: { resultado: 'actualizado' },
    });
  });

  it('elimina solamente después de confirmar en el diálogo', () => {
    idRuta = '7';
    editarRuta = false;
    const creado = crear();
    cargarPerfil(creado);
    creado.clickButton('Eliminar perfil');
    creado.fixture.detectChanges();

    expect(creado.el.querySelector('[role="alertdialog"]')).not.toBeNull();
    creado.clickButton('Cancelar');
    creado.fixture.detectChanges();
    httpMock.expectNone('/api/cliente/perfiles/7');

    creado.clickButton('Eliminar perfil');
    creado.fixture.detectChanges();
    creado.clickButton('Sí, eliminar');
    httpMock.expectOne({ method: 'DELETE', url: '/api/cliente/perfiles/7' }).flush(null);

    expect(router.navigate).toHaveBeenCalledWith(['/cliente/hijos']);
  });

  it('presenta cuatro tarjetas de información en el modo detalle', () => {
    idRuta = '7';
    editarRuta = false;
    const creado = crear();
    cargarPerfil(creado);

    expect(creado.el.querySelectorAll('.detail-card')).toHaveLength(4);
    expect(creado.el.textContent).toContain('Restricciones textiles');
    expect(creado.el.textContent).toContain('Sin alergias');
    const recomendaciones = creado.el.querySelector<HTMLButtonElement>('.recommendation-button');
    expect(recomendaciones?.textContent?.trim()).toBe('Recomendaciones');
    expect(recomendaciones?.disabled).toBe(true);
  });
});
