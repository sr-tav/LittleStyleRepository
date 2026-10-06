import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';

import { Medicion, PerfilInfantil } from '../models/perfil-infantil.models';
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
  camposPendientes: ['Estatura', 'Peso'],
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
              get queryParamMap() {
                return convertToParamMap({ editar: editarRuta ? 'true' : null });
              },
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

  function cargarPerfil(
    creado: ReturnType<typeof crear>,
    mediciones: Medicion[] = [],
    perfil = perfilEjemplo,
  ): void {
    httpMock.expectOne('/api/cliente/perfiles/7').flush(perfil);
    httpMock.expectOne('/api/cliente/perfiles/7/mediciones').flush(mediciones);
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

  it('crea perfil y medición inicial en una sola solicitud aunque se envíe varias veces', () => {
    const creado = crear();
    completarFormulario(creado);
    creado.clickButton('Crear perfil');
    creado.submitProfile();

    const request = httpMock.expectOne('/api/cliente/perfiles/con-medicion-inicial');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      perfil: {
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
      },
      medicionInicial: {
        fechaMedicion: '2025-04-15',
        estaturaCm: 110,
        pesoKg: 20,
      },
    });
    creado.submitProfile();
    httpMock.expectNone('/api/cliente/perfiles/con-medicion-inicial');
    creado.fixture.detectChanges();
    request.flush({ ...perfilEjemplo, id: 31, nombre: 'Lucía' });
    creado.fixture.detectChanges();

    expect(creado.el.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(
      true,
    );
    creado.submitCurrentForm();
    httpMock.expectNone('/api/cliente/perfiles/con-medicion-inicial');
    expect(router.navigate).toHaveBeenCalledWith(['/cliente/hijos'], {
      queryParams: { resultado: 'creado' },
    });
  });

  it('conserva el formulario y permite reintentar después de un error del backend', () => {
    const creado = crear();
    completarFormulario(creado);
    creado.clickButton('Crear perfil');

    const primerIntento = httpMock.expectOne('/api/cliente/perfiles/con-medicion-inicial');
    primerIntento.flush(
      { mensaje: 'Hay campos con errores de validación', errores: { nombre: 'Revisa el nombre' } },
      { status: 400, statusText: 'Bad Request' },
    );
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('Revisa el nombre');
    expect(creado.el.querySelector<HTMLInputElement>('#nombre')?.value).toBe('Lucía');
    creado.clickButton('Crear perfil');

    const segundoIntento = httpMock.expectOne('/api/cliente/perfiles/con-medicion-inicial');
    expect(segundoIntento.request.method).toBe('POST');
    expect(segundoIntento.request.body.perfil.nombre).toBe('Lucía');
    segundoIntento.flush({ ...perfilEjemplo, id: 31, nombre: 'Lucía' });
    expect(router.navigate).toHaveBeenCalledWith(['/cliente/hijos'], {
      queryParams: { resultado: 'creado' },
    });
  });

  it('si falla el alta atómica conserva el formulario para reintentar ambos datos', () => {
    const creado = crear();
    completarFormulario(creado);
    creado.clickButton('Crear perfil');

    const solicitud = httpMock.expectOne('/api/cliente/perfiles/con-medicion-inicial');
    solicitud.flush(
      { mensaje: 'No se pudo guardar la medición.' },
      { status: 400, statusText: 'Bad Request' },
    );
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('No se pudo guardar la medición.');
    expect(creado.el.querySelector<HTMLInputElement>('#nombre')?.value).toBe('Lucía');
    expect(creado.el.querySelector<HTMLInputElement>('#fechaMedicion')?.value).toBe('2025-04-15');
    expect(creado.el.querySelector<HTMLInputElement>('#estaturaCm')?.value).toBe('110');
    expect(creado.el.querySelector<HTMLInputElement>('#pesoKg')?.value).toBe('20');

    creado.clickButton('Crear perfil');
    const reintento = httpMock.expectOne('/api/cliente/perfiles/con-medicion-inicial');
    expect(reintento.request.body.perfil.nombre).toBe('Lucía');
    expect(reintento.request.body.medicionInicial.estaturaCm).toBe(110);
    reintento.flush({ ...perfilEjemplo, id: 32, nombre: 'Lucía' });
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

  it('muestra el historial y solo el botón de nueva medición hasta solicitarla', () => {
    idRuta = '7';
    editarRuta = false;
    const visto = crear();
    cargarPerfil(visto);

    expect(visto.el.textContent).toContain('Historial de crecimiento');
    expect(visto.el.querySelector('#fechaMedicion')).toBeNull();
    expect(visto.el.textContent).toContain('Nueva medición');
    visto.clickButton('Nueva medición');
    visto.fixture.detectChanges();
    expect(visto.el.querySelector('#fechaMedicion')).not.toBeNull();

    editarRuta = true;
    const editado = crear();
    cargarPerfil(editado);

    expect(editado.el.textContent).toContain('Editar perfil de Mateo');
    expect(editado.el.textContent).not.toContain('Nueva medición');
    expect(editado.el.textContent).toContain('Agregar medición');
    expect(editado.el.querySelector('#fechaMedicion')).not.toBeNull();
    expect(editado.el.textContent).not.toContain('Historial de crecimiento');
  });

  it('agrega una medición desde el historial y actualiza el resumen sin guardar cambios de perfil', () => {
    idRuta = '7';
    editarRuta = false;
    const creado = crear();
    cargarPerfil(creado);
    creado.clickButton('Nueva medición');
    creado.fixture.detectChanges();
    creado.input('fechaMedicion', '2026-10-05');
    creado.input('estaturaCm', '115');
    creado.input('pesoKg', '22');
    creado.clickButton('Guardar medición');

    const request = httpMock.expectOne({
      method: 'POST',
      url: '/api/cliente/perfiles/7/mediciones',
    });
    expect(request.request.body).toEqual({
      fechaMedicion: '2026-10-05',
      estaturaCm: 115,
      pesoKg: 22,
    });
    request.flush({ id: 8, fechaMedicion: '2026-10-05', estaturaCm: 115, pesoKg: 22 });

    httpMock.expectOne('/api/cliente/perfiles/7').flush({
      ...perfilEjemplo,
      fechaActualizacion: '2026-10-05T12:00:00',
      ultimaMedicion: { id: 8, fechaMedicion: '2026-10-05', estaturaCm: 115, pesoKg: 22 },
    });
    httpMock.expectOne('/api/cliente/perfiles/7/mediciones').flush([
      { id: 8, fechaMedicion: '2026-10-05', estaturaCm: 115, pesoKg: 22 },
    ]);
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('115 cm');
    expect(creado.el.textContent).toContain('22 kg');
    expect(creado.el.querySelector<HTMLInputElement>('#estaturaCm')).toBeNull();
    httpMock.expectNone({ method: 'PUT', url: '/api/cliente/perfiles/7' });
  });

  it('también permite agregar una medición mientras se edita el perfil, sin guardar el formulario del perfil', () => {
    idRuta = '7';
    const creado = crear();
    cargarPerfil(creado);
    creado.input('fechaMedicion', '2026-10-05');
    creado.input('estaturaCm', '115');
    creado.input('pesoKg', '22');
    creado.clickButton('Agregar medición');

    const request = httpMock.expectOne({
      method: 'POST',
      url: '/api/cliente/perfiles/7/mediciones',
    });
    expect(request.request.body).toEqual({
      fechaMedicion: '2026-10-05',
      estaturaCm: 115,
      pesoKg: 22,
    });
    request.flush({ id: 9, fechaMedicion: '2026-10-05', estaturaCm: 115, pesoKg: 22 });
    httpMock.expectOne('/api/cliente/perfiles/7').flush({
      ...perfilEjemplo,
      fechaActualizacion: '2026-10-05T12:00:00',
      ultimaMedicion: { id: 9, fechaMedicion: '2026-10-05', estaturaCm: 115, pesoKg: 22 },
    });
    httpMock.expectOne('/api/cliente/perfiles/7/mediciones').flush([
      { id: 9, fechaMedicion: '2026-10-05', estaturaCm: 115, pesoKg: 22 },
    ]);
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('115 cm');
    httpMock.expectNone({ method: 'PUT', url: '/api/cliente/perfiles/7' });
  });

  it('permite corregir la medida actual directamente desde la edición del perfil', () => {
    idRuta = '7';
    const ultima: Medicion = {
      id: 8,
      fechaMedicion: '2026-10-04',
      estaturaCm: 115,
      pesoKg: 22,
    };
    const perfilConMedicion = { ...perfilEjemplo, ultimaMedicion: ultima };
    const creado = crear();
    cargarPerfil(creado, [ultima], perfilConMedicion);
    creado.clickButton('Corregir última medición');
    creado.fixture.detectChanges();

    expect(creado.el.querySelector<HTMLInputElement>('#estaturaCm')?.value).toBe('115');
    creado.input('estaturaCm', '114');
    creado.clickButton('Actualizar medición');
    const request = httpMock.expectOne({
      method: 'PUT',
      url: '/api/cliente/perfiles/7/mediciones/8',
    });
    expect(request.request.body).toEqual({
      fechaMedicion: '2026-10-04',
      estaturaCm: 114,
      pesoKg: 22,
    });
    request.flush({ id: 8, fechaMedicion: '2026-10-04', estaturaCm: 114, pesoKg: 22 });
    httpMock.expectOne('/api/cliente/perfiles/7').flush({
      ...perfilConMedicion,
      fechaActualizacion: '2026-10-05T12:00:00',
      ultimaMedicion: { id: 8, fechaMedicion: '2026-10-04', estaturaCm: 114, pesoKg: 22 },
    });
    httpMock.expectOne('/api/cliente/perfiles/7/mediciones').flush([
      { id: 8, fechaMedicion: '2026-10-04', estaturaCm: 114, pesoKg: 22 },
    ]);
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('114 cm');
    httpMock.expectNone({ method: 'PUT', url: '/api/cliente/perfiles/7' });
  });

  it('evita nuevas mediciones anteriores o con menor estatura, pero permite que baje el peso', () => {
    idRuta = '7';
    editarRuta = false;
    const ultima: Medicion = {
      id: 8,
      fechaMedicion: '2026-10-05',
      estaturaCm: 115,
      pesoKg: 22,
    };
    const perfilConMedicion = {
      ...perfilEjemplo,
      ultimaMedicion: ultima,
    };
    const creado = crear();
    cargarPerfil(creado, [ultima], perfilConMedicion);
    creado.clickButton('Nueva medición');
    creado.fixture.detectChanges();

    const fecha = creado.el.querySelector<HTMLInputElement>('#fechaMedicion')!;
    expect(fecha.min).toBe('2026-10-05');
    creado.input('fechaMedicion', '2026-10-02');
    creado.input('estaturaCm', '120');
    creado.input('pesoKg', '20');
    creado.clickButton('Guardar medición');
    creado.fixture.detectChanges();
    expect(creado.el.textContent).toContain('La nueva medición no puede ser anterior');
    httpMock.expectNone({ method: 'POST', url: '/api/cliente/perfiles/7/mediciones' });

    creado.input('fechaMedicion', '2026-10-05');
    creado.input('estaturaCm', '114');
    creado.clickButton('Guardar medición');
    creado.fixture.detectChanges();
    expect(creado.el.textContent).toContain('La estatura no puede ser menor');
    httpMock.expectNone({ method: 'POST', url: '/api/cliente/perfiles/7/mediciones' });

    creado.input('estaturaCm', '116');
    creado.input('pesoKg', '19');
    creado.fixture.detectChanges();
    expect(creado.el.querySelector<HTMLInputElement>('#fechaMedicion')?.value).toBe('2026-10-05');
    creado.clickButton('Guardar medición');
    const request = httpMock.expectOne({
      method: 'POST',
      url: '/api/cliente/perfiles/7/mediciones',
    });
    expect(request.request.body).toEqual({
      fechaMedicion: '2026-10-05',
      estaturaCm: 116,
      pesoKg: 19,
    });
    request.flush({ id: 9, fechaMedicion: '2026-10-05', estaturaCm: 116, pesoKg: 19 });
    httpMock.expectOne('/api/cliente/perfiles/7').flush({
      ...perfilConMedicion,
      fechaActualizacion: '2026-10-05T12:00:00',
      ultimaMedicion: { id: 9, fechaMedicion: '2026-10-05', estaturaCm: 116, pesoKg: 19 },
    });
    httpMock.expectOne('/api/cliente/perfiles/7/mediciones').flush([
      { id: 9, fechaMedicion: '2026-10-05', estaturaCm: 116, pesoKg: 19 },
      ultima,
    ]);
  });

  it('permite corregir una medición existente desde el historial', () => {
    idRuta = '7';
    editarRuta = false;
    const creado = crear();
    cargarPerfil(creado, [
      { id: 8, fechaMedicion: '2026-10-05', estaturaCm: 115, pesoKg: 22 },
    ]);
    creado.fixture.detectChanges();
    creado.clickButton('Corregir');
    creado.fixture.detectChanges();

    expect(creado.el.querySelector<HTMLInputElement>('#estaturaCm')?.value).toBe('115');
    expect(creado.el.querySelector<HTMLInputElement>('#pesoKg')?.value).toBe('22');
    creado.input('estaturaCm', '114');
    creado.input('pesoKg', '21');
    creado.clickButton('Actualizar medición');

    const request = httpMock.expectOne({
      method: 'PUT',
      url: '/api/cliente/perfiles/7/mediciones/8',
    });
    expect(request.request.body).toEqual({
      fechaMedicion: '2026-10-05',
      estaturaCm: 114,
      pesoKg: 21,
    });
    request.flush({ id: 8, fechaMedicion: '2026-10-05', estaturaCm: 114, pesoKg: 21 });
    httpMock.expectOne('/api/cliente/perfiles/7').flush({
      ...perfilEjemplo,
      fechaActualizacion: '2026-10-05T12:00:00',
      ultimaMedicion: { id: 8, fechaMedicion: '2026-10-05', estaturaCm: 114, pesoKg: 21 },
    });
    httpMock.expectOne('/api/cliente/perfiles/7/mediciones').flush([
      { id: 8, fechaMedicion: '2026-10-05', estaturaCm: 114, pesoKg: 21 },
    ]);
    creado.fixture.detectChanges();

    expect(creado.el.textContent).toContain('114 cm');
    expect(creado.el.textContent).toContain('21 kg');
    httpMock.expectNone({ method: 'POST', url: '/api/cliente/perfiles/7/mediciones' });
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
    const recomendaciones = creado.el.querySelector<HTMLAnchorElement>('.recommendation-button');
    expect(recomendaciones?.textContent?.trim()).toBe('Recomendaciones');
    expect(recomendaciones?.getAttribute('href')).toContain('/cliente/recomendaciones');
  });
});
