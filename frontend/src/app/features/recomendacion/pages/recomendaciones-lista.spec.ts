import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { CarritoService } from '../../catalogoCompras/services/carrito.service';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { RecomendacionItem } from '../models/recomendacion.models';
import { RecomendacionService } from '../services/recomendacion.service';
import { RecomendacionesLista } from './recomendaciones-lista';

function item(): RecomendacionItem {
  return {
    prendaId: 7,
    nombre: 'Camiseta',
    precio: 25900,
    tallaSugerida: '4',
    puntaje: 92,
    afinidad: 8,
    riesgo: 'ADVERTENCIA',
    advertencias: ['Contiene lana'],
    tieneStock: true,
    imagenUrl: null,
  };
}

describe('RecomendacionesLista (US-07)', () => {
  const recomendaciones = { listar: vi.fn() };

  beforeEach(async () => {
    recomendaciones.listar.mockReset();
    await TestBed.configureTestingModule({
      imports: [RecomendacionesLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ perfilId: '3' }) } },
        },
        { provide: RecomendacionService, useValue: recomendaciones },
        { provide: PerfilesService, useValue: { listar: () => of([]) } },
        { provide: PerfilActivoService, useValue: { perfil: () => ({ id: 3 }), sincronizar: vi.fn() } },
        {
          provide: AuthService,
          useValue: { rol: () => null, usuario: () => null, rutaInicio: () => '/auth/login', logout: vi.fn() },
        },
        {
          provide: CarritoService,
          useValue: {
            panelAbierto: signal(false),
            items: signal([]),
            cantidad: signal(0),
            subtotal: signal(0),
            alternarPanel: vi.fn(),
            cerrarPanel: vi.fn(),
          },
        },
      ],
    }).compileComponents();
  });

  function crear() {
    const fixture = TestBed.createComponent(RecomendacionesLista);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('muestra las prendas con badge, advertencia y compatibilidad', () => {
    recomendaciones.listar.mockReturnValue(of([item()]));
    const { el } = crear();

    expect(recomendaciones.listar).toHaveBeenCalledWith(3);
    expect(el.textContent).toContain('Camiseta');
    expect(el.textContent).toContain('Talla sugerida: 4');
    expect(el.textContent).toContain('Contiene lana');
    expect(el.textContent).toContain('92%');
    expect(el.textContent).toContain('Afinidad de estilo: +8');
  });

  it('avisa cuando no hay prendas compatibles', () => {
    recomendaciones.listar.mockReturnValue(of([]));
    const { el } = crear();

    expect(el.textContent).toContain('No hay prendas compatibles');
  });

  it('ofrece agregar medición cuando el 422 es por medidas desactualizadas', () => {
    recomendaciones.listar.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 422, error: { mensaje: 'Medición desactualizada' } }),
      ),
    );
    const { el } = crear();

    expect(el.querySelector('a.btn-primary')?.textContent).toContain('Agregar medición');
  });
});
