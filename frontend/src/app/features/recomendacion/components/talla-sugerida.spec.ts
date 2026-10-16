import { TestBed } from '@angular/core/testing';

import { TallaSugerida } from './talla-sugerida';
import { RecomendacionItem } from '../models/recomendacion.models';

function item(cambios: Partial<RecomendacionItem> = {}): RecomendacionItem {
  return {
    prendaId: 1,
    nombre: 'Pantalón Jogger',
    precio: 32900,
    tallaSugerida: '4',
    puntaje: 95,
    afinidad: 0,
    riesgo: 'APTA',
    advertencias: [],
    tieneStock: true,
    imagenUrl: null,
    ...cambios,
  };
}

describe('TallaSugerida (US-07)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TallaSugerida] }).compileComponents();
  });

  function crear(r: RecomendacionItem): HTMLElement {
    const fixture = TestBed.createComponent(TallaSugerida);
    fixture.componentRef.setInput('item', r);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('muestra el badge de la talla sin alertas cuando es apta con stock', () => {
    const el = crear(item());

    expect(el.textContent).toContain('Talla sugerida: 4');
    expect(el.querySelector('[role="alert"]')).toBeNull();
    expect(el.textContent).not.toContain('Sin stock');
  });

  it('muestra la advertencia amarilla con los motivos', () => {
    const el = crear(item({ riesgo: 'ADVERTENCIA', advertencias: ['Contiene lana'] }));

    expect(el.textContent).toContain('Talla sugerida: 4');
    expect(el.querySelector('.alert-warn')?.textContent).toContain('Contiene lana');
  });

  it('muestra la exclusión con motivo aunque no haya talla sugerida', () => {
    const el = crear(
      item({ tallaSugerida: '', puntaje: 0, riesgo: 'EXCLUIDA', advertencias: ['Prenda excluida por alergia: Contiene níquel'], tieneStock: false }),
    );

    expect(el.textContent).not.toContain('Talla sugerida:');
    expect(el.querySelector('.alert-danger')?.textContent).toContain('No compatible');
    expect(el.querySelector('.alert-danger')?.textContent).toContain('Contiene níquel');
    expect(el.textContent).not.toContain('Sin stock en esa talla');
  });

  it('avisa cuando la talla sugerida no tiene stock', () => {
    const el = crear(item({ tieneStock: false }));

    expect(el.textContent).toContain('Sin stock en esa talla');
  });
});
