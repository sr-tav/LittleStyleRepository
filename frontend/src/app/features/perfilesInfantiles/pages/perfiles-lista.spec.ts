import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Router } from '@angular/router';

import { PerfilInfantil } from '../models/perfil-infantil.models';
import { PerfilesLista } from './perfiles-lista';

const perfilEjemplo: PerfilInfantil = {
  id: 12,
  nombre: 'Sofía',
  fechaNacimiento: '2021-03-10',
  edadAnios: 5,
  mesesRestantes: 6,
  contextura: 'MEDIA',
  holgura: 'REGULAR',
  alergias: [],
  otraAlergia: null,
  sinAlergias: true,
  coloresPreferidos: ['VERDE'],
  estampadosPreferidos: [],
  otroColor: null,
  otroEstampado: null,
  fechaCreacion: '2025-01-01T12:00:00',
  fechaActualizacion: '2025-01-01T12:00:00',
  ultimaMedicion: { id: 4, fechaMedicion: '2025-05-01', estaturaCm: 110, pesoKg: 20 },
  porcentajeCompletitud: 100,
  camposPendientes: [],
};

describe('PerfilesLista (UI-4)', () => {
  let httpMock: HttpTestingController;
  let resultadoRuta: string | null;
  let modoRuta: string | null;

  beforeEach(async () => {
    resultadoRuta = null;
    modoRuta = null;
    localStorage.removeItem('littleStyle.perfilActivoId');
    await TestBed.configureTestingModule({
      imports: [PerfilesLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useFactory: () => ({
            snapshot: {
              queryParamMap: convertToParamMap({ resultado: resultadoRuta, modo: modoRuta }),
            },
          }),
        },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function crear() {
    const fixture = TestBed.createComponent(PerfilesLista);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('muestra el estado vacío cuando no hay perfiles', () => {
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([]);
    fixture.detectChanges();

    expect(el.textContent).toContain('Aún no tienes perfiles infantiles');
    expect(el.querySelectorAll('a[routerLink="/cliente/hijos/nuevo"]').length).toBe(1);
    expect(el.querySelectorAll('.profiles-grid a[routerLink="/cliente/hijos/nuevo"]').length).toBe(
      0,
    );
  });

  it('ofrece volver al inicio en modo normal y conserva el CTA de alta único', () => {
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([perfilEjemplo]);
    fixture.detectChanges();

    const volver = el.querySelector<HTMLAnchorElement>('a[routerLink="/cliente"]');
    expect(volver?.textContent?.trim()).toBe('← Volver al inicio');
    expect(volver?.getAttribute('aria-label')).toBe('Volver al inicio');
    expect(el.querySelectorAll('a[routerLink="/cliente/hijos/nuevo"]')).toHaveLength(1);
    expect(
      el
        .querySelector<HTMLAnchorElement>('a[routerLink="/cliente/hijos/nuevo"]')
        ?.getAttribute('aria-label'),
    ).toBe('Agregar un perfil infantil');
  });

  it('confirma la operación al regresar del formulario', () => {
    resultadoRuta = 'actualizado';
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([]);
    fixture.detectChanges();

    expect(el.querySelector('[role="status"]')?.textContent).toContain(
      'Los cambios del perfil se guardaron correctamente.',
    );
    expect(el.querySelectorAll('a[routerLink="/cliente/hijos/nuevo"]').length).toBe(1);
  });

  it('confirma la creación al regresar y mantiene una sola acción para agregar', () => {
    resultadoRuta = 'creado';
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([]);
    fixture.detectChanges();

    expect(el.querySelector('[role="status"]')?.textContent).toContain(
      'El perfil infantil se creó correctamente.',
    );
    expect(el.querySelectorAll('a[routerLink="/cliente/hijos/nuevo"]').length).toBe(1);
  });

  it('muestra la tarjeta con medidas y completitud del perfil', () => {
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([perfilEjemplo]);
    fixture.detectChanges();

    expect(el.textContent).toContain('Sofía');
    expect(el.textContent).toContain('110 cm');
    expect(el.textContent).toContain('20 kg');
    expect(el.textContent).not.toContain('Talla 4');
    expect(el.textContent).toContain('Ver talla sugerida');
    const actions = el.querySelectorAll<HTMLAnchorElement>('.profile-card-actions a');
    expect(actions).toHaveLength(2);
    expect(actions[0].textContent).toContain('Ver perfil');
    expect(actions[0].getAttribute('href')).toContain('/cliente/hijos/12');
    expect(actions[1].textContent).toContain('Editar perfil');
    expect(actions[1].getAttribute('href')).toContain('/cliente/hijos/12?editar=true');
    expect(el.querySelector('.completion-badge')?.textContent?.trim()).toBe('Completo');
    expect(el.querySelector('.profile-avatar svg')).not.toBeNull();
  });

  it('muestra acciones distintas para ver y editar, sin selector activo en la lista de gestión', () => {
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([perfilEjemplo]);
    fixture.detectChanges();

    expect(el.querySelector('.profile-activate')).toBeNull();
    expect(el.querySelectorAll('.profile-card-actions a')).toHaveLength(2);
    expect(el.querySelector('.profile-edit-link')?.textContent).toContain('Editar perfil');
  });

  it('en modo cambiar perfil muestra solo selectores y vuelve al inicio al elegir', () => {
    modoRuta = 'seleccionar';
    const segundoPerfil = { ...perfilEjemplo, id: 13, nombre: 'Mateo' };
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([perfilEjemplo, segundoPerfil]);
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const selectores = el.querySelectorAll<HTMLButtonElement>('.profile-picker');

    expect(el.textContent).toContain('¿Quién usará LittleStyle?');
    expect(el.querySelector('a[routerLink="/cliente"]')?.textContent?.trim()).toBe('Volver');
    expect(el.querySelector('a[routerLink="/cliente/hijos/nuevo"]')).toBeNull();
    expect(el.querySelectorAll('.profile-card-actions')).toHaveLength(0);
    expect(selectores).toHaveLength(2);

    selectores[1].click();
    fixture.detectChanges();

    expect(localStorage.getItem('littleStyle.perfilActivoId')).toBe('13');
    expect(navegar).toHaveBeenCalledWith('/cliente');
  });

  it('marca Incompleto cuando la completitud es menor al 90 por ciento', () => {
    const { fixture, el } = crear();
    httpMock
      .expectOne('/api/cliente/perfiles')
      .flush([{
        ...perfilEjemplo,
        porcentajeCompletitud: 89,
        camposPendientes: ['Agrega al menos un color o estampado preferido'],
      }]);
    fixture.detectChanges();

    expect(el.querySelector('.completion-badge')?.textContent?.trim()).toBe('Incompleto');
    expect(el.textContent).toContain('Agrega al menos un color o estampado preferido');
  });

  it('mantiene un único CTA de alta y no agrega tarjeta duplicada', () => {
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([perfilEjemplo]);
    fixture.detectChanges();

    expect(el.querySelectorAll('a[routerLink="/cliente/hijos/nuevo"]')).toHaveLength(1);
    expect(el.textContent).not.toContain('Agregar hijo</h2>');
  });

  it('renderiza alergias y preferencias con etiquetas en español', () => {
    const { fixture, el } = crear();
    httpMock.expectOne('/api/cliente/perfiles').flush([
      {
        ...perfilEjemplo,
        alergias: ['FORMALDEHIDO', 'OTRA'],
        otraAlergia: 'Piel sensible',
        coloresPreferidos: ['CAFE'],
        estampadosPreferidos: ['DIBUJOS_ANIMADOS'],
      },
    ]);
    fixture.detectChanges();

    expect(el.textContent).toContain('Formaldehído');
    expect(el.textContent).toContain('Otra: Piel sensible');
    expect(el.textContent).toContain('Café');
    expect(el.textContent).toContain('Dibujos animados');
    expect(el.textContent).not.toContain('DIBUJOS_ANIMADOS');
  });
});
