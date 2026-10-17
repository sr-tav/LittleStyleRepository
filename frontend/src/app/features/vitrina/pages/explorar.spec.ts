import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { CarritoService } from '../../catalogoCompras/services/carrito.service';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { Pagina, VitrinaItem } from '../models/vitrina.models';
import { Explorar } from './explorar';

function item(cambios: Partial<VitrinaItem> = {}): VitrinaItem {
  return {
    id: 10,
    nombre: 'Pantalón Jogger Azul',
    categoria: 'PANTALONES',
    genero: 'UNISEX',
    marca: 'Tienda Sol',
    precio: 32900,
    imagenUrl: '/media/a.png',
    disponible: true,
    tallasDisponibles: ['4', '6'],
    ...cambios,
  };
}

// El Navbar usa el carrito: con el servicio real, una sesión CLIENTE dejada por otro spec
// dispara GET /api/cliente/carrito y rompe http.verify() según el orden de ejecución.
const carritoMock = {
  agregar: vi.fn(),
  abrirPanel: vi.fn(),
  panelAbierto: signal(false),
  items: signal([]),
  cantidad: signal(0),
  subtotal: signal(0),
  alternarPanel: vi.fn(),
  cerrarPanel: vi.fn(),
};

function pagina(items: VitrinaItem[]): Pagina<VitrinaItem> {
  return { contenido: items, pagina: 0, tamano: 12, totalElementos: items.length, totalPaginas: 1 };
}

describe('Explorar (US-09)', () => {
  let http: HttpTestingController;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let perfilActual: { id: number } | null;

  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({}));
    perfilActual = null;
    await TestBed.configureTestingModule({
      imports: [Explorar],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: params.asObservable() } },
        { provide: PerfilActivoService, useValue: { perfil: () => perfilActual } },
        { provide: CarritoService, useValue: carritoMock },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const fixture = TestBed.createComponent(Explorar);
    fixture.detectChanges();
    http.expectOne('/api/cliente/catalogo?pagina=0&tamano=12&orden=novedades').flush(pagina([item()]));
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('muestra las prendas con precio y enlace al detalle', () => {
    const { el } = crear();

    expect(el.textContent).toContain('Pantalón Jogger Azul');
    expect(el.textContent).toContain('32.900');
    const enlace = el.querySelector<HTMLAnchorElement>('a[href*="/cliente/catalogo/10"]');
    expect(enlace).not.toBeNull();
  });

  it('cambiar un filtro vuelve a la primera página con ese parámetro', () => {
    const { fixture, el } = crear();

    const categoria = el.querySelector<HTMLSelectElement>('select')!;
    categoria.value = 'PANTALONES';
    categoria.dispatchEvent(new Event('change'));
    http
      .expectOne((p) => p.url === '/api/cliente/catalogo' && p.params.get('categoria') === 'PANTALONES')
      .flush(pagina([item()]));
    fixture.detectChanges();

    expect(el.textContent).toContain('Pantalón Jogger Azul');
  });

  it('muestra estado vacío cuando no hay coincidencias', () => {
    const { fixture, el } = crear();

    const categoria = el.querySelector<HTMLSelectElement>('select')!;
    categoria.value = 'VESTIDOS';
    categoria.dispatchEvent(new Event('change'));
    http
      .expectOne((p) => p.url === '/api/cliente/catalogo' && p.params.get('categoria') === 'VESTIDOS')
      .flush(pagina([]));
    fixture.detectChanges();

    expect(el.textContent).toContain('Ninguna prenda coincide');
  });

  it('reacciona a los query params del menú sin recargar la página', () => {
    const { fixture, el } = crear();

    // Como al elegir Niñas en el menú hamburguesa
    params.next(convertToParamMap({ genero: 'NINA' }));
    http
      .expectOne((p) => p.url === '/api/cliente/catalogo' && p.params.get('genero') === 'NINA')
      .flush(pagina([item()]));
    fixture.detectChanges();

    expect(el.textContent).toContain('Pantalón Jogger Azul');
  });

  it('filtra por mi talla en el servidor con perfilId sin ids en la URL (US-07)', () => {
    perfilActual = { id: 3 };
    const { fixture, el } = crear();

    const casillas = el.querySelectorAll<HTMLInputElement>('input[type="checkbox"]');
    casillas[1].click();
    fixture.detectChanges();

    // Solo viaja el perfilId: el servidor calcula los ids recomendados dentro.
    const conFiltro = http.expectOne(
      (p) =>
        p.url === '/api/cliente/catalogo' &&
        p.params.get('perfilId') === '3' &&
        p.params.get('soloMiTalla') === 'true' &&
        !p.params.has('ids'),
    );
    expect(conFiltro.request.params.get('pagina')).toBe('0');
    conFiltro.flush(pagina([item()]));
    http.expectOne('/api/cliente/recomendaciones?perfilId=3').flush([
      { prendaId: 10, nombre: 'Pantalón Jogger Azul', precio: 32900, tallaSugerida: '4', puntaje: 95, afinidad: 5, riesgo: 'ADVERTENCIA', advertencias: ['Contiene lana'], tieneStock: true, imagenUrl: null },
    ]);
    fixture.detectChanges();

    expect(el.textContent).toContain('Pantalón Jogger Azul');
    expect(el.textContent).toContain('Talla sugerida: 4');
  });

  it('envía la edad en meses como filtro de servidor (US-09)', () => {
    const { fixture, el } = crear();

    const edad = el.querySelectorAll<HTMLInputElement>('input[type="number"]')[0]!;
    edad.value = '4';
    edad.dispatchEvent(new Event('change'));
    http
      .expectOne((p) => p.url === '/api/cliente/catalogo' && p.params.get('edadMeses') === '48')
      .flush(pagina([item()]));
    fixture.detectChanges();

    expect(el.textContent).toContain('Pantalón Jogger Azul');
  });
});
