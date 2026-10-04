import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { PerfilInfantil } from '../../../perfilesInfantiles/models/perfil-infantil.models';
import { PerfilesService } from '../../../perfilesInfantiles/services/perfiles.service';
import { ClienteInicio } from './cliente-inicio';

describe('ClienteInicio', () => {
  let perfiles: PerfilInfantil[];

  beforeEach(async () => {
    perfiles = [];
    await TestBed.configureTestingModule({
      imports: [ClienteInicio],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            usuario: () => ({ nombre: 'Laura', apellido: 'Gómez', rol: 'CLIENTE' }),
            rol: () => 'CLIENTE',
            rutaInicio: () => '/cliente',
            logout: vi.fn(),
          },
        },
        { provide: PerfilesService, useValue: { listar: () => of(perfiles) } },
      ],
    }).compileComponents();
  });

  it('presenta Mis hijos como un único acceso directo al listado', () => {
    const fixture = TestBed.createComponent(ClienteInicio);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    const link = root.querySelector<HTMLAnchorElement>(
      'a.home-tile[routerLink="/cliente/hijos"]',
    );
    expect(link).not.toBeNull();
    expect(link?.getAttribute('href')).toBe('/cliente/hijos');
    expect(link?.textContent).toContain('Mis hijos');
    expect(fixture.nativeElement.textContent).toContain('Bienvenido de vuelta a LittleStyle');
  });

  it('abre el selector de perfiles al pulsar Cambiar perfil', () => {
    perfiles = [
      {
        id: 1,
        nombre: 'Sofía',
        fechaNacimiento: '2021-03-10',
        edadAnios: 5,
        mesesRestantes: 6,
        contextura: 'MEDIA',
        holgura: 'REGULAR',
        alergias: [],
        otraAlergia: null,
        sinAlergias: true,
        coloresPreferidos: [],
        estampadosPreferidos: [],
        otroColor: null,
        otroEstampado: null,
        fechaCreacion: '2025-01-01T12:00:00',
        fechaActualizacion: '2025-01-01T12:00:00',
        ultimaMedicion: null,
        porcentajeCompletitud: 100,
      },
    ];
    const fixture = TestBed.createComponent(ClienteInicio);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const link = root.querySelector<HTMLAnchorElement>(
      '.active-profile a[routerLink="/cliente/hijos"]',
    );
    expect(link?.textContent).toContain('Cambiar perfil');
    expect(link?.getAttribute('href')).toBe('/cliente/hijos?modo=seleccionar');
  });

  it('mantiene los demás accesos sin enlace, deshabilitados y con tooltip', () => {
    const fixture = TestBed.createComponent(ClienteInicio);
    fixture.detectChanges();

    const root: HTMLElement = fixture.nativeElement;
    const disabledTiles = Array.from(
      root.querySelectorAll<HTMLElement>('.home-tile-disabled'),
    );
    expect(disabledTiles).toHaveLength(3);
    for (const tile of disabledTiles) {
      expect(tile.tagName).not.toBe('A');
      expect(tile.getAttribute('aria-disabled')).toBe('true');
      expect(tile.getAttribute('title')).toBe('Próximamente');
    }
    expect(root.textContent).not.toContain('Próximamente');
  });

  it('muestra cuatro iconos SVG decorativos', () => {
    const fixture = TestBed.createComponent(ClienteInicio);
    fixture.detectChanges();

    const icons: NodeListOf<SVGElement> =
      fixture.nativeElement.querySelectorAll('.home-tile svg');
    expect(icons).toHaveLength(4);
    for (const icon of Array.from(icons)) {
      expect(icon.getAttribute('viewBox')).toBe('0 0 24 24');
      expect(icon.getAttribute('fill')).toBe('none');
      expect(icon.getAttribute('stroke')).toBe('currentColor');
      expect(icon.getAttribute('stroke-width')).toBe('1.8');
      expect(icon.getAttribute('stroke-linecap')).toBe('round');
      expect(icon.getAttribute('stroke-linejoin')).toBe('round');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    }
  });
});
