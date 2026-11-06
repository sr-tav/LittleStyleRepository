import { AlergiaTextil } from '../../perfilesInfantiles/models/perfil-infantil.models';
import {
  ETIQUETAS_ALERGIAS,
  OPCIONES_ALERGIAS,
} from '../../perfilesInfantiles/models/perfil-opciones';
import {
  alergiasDetectadasDe,
  ALERGIAS_DECLARADAS,
  ALERGIAS_DETECTADAS,
  OPCIONES_ALERGIAS_DECLARABLES, opcionesAlergiasDeclaradas, alergiasDeclaradasDe,
} from './filtro-alergia-estandar';

/** Regla espejo: los valores excluidos por el backend (níquel y formaldehído) son solo declarables. */
const DECLARABLES: AlergiaTextil[] = [
  'TINTES',
  'BROCHES_METALICOS',
  'NIQUEL',
  'FORMALDEHIDO',
];

/** Los otros seis se detectan exclusivamente a partir de la composición textil. */
const DETECTABLES: AlergiaTextil[] = [
  'FIBRAS_SINTETICAS',
  'LANA',
  'ALGODON',
  'LATEX_ELASTICO',
  'CUERO',
  'OTRA',
];

describe('filtro-alergia-estandar — contrato de alergias del perfil', () => {
  it('cubre cada alergia exactamente en uno de los dos grupos y usa las etiquetas compartidas', () => {
    const valoresPerfil = OPCIONES_ALERGIAS.map((opcion) => opcion.valor);
    const grupos = [...ALERGIAS_DECLARADAS, ...ALERGIAS_DETECTADAS];

    expect(Object.keys(ETIQUETAS_ALERGIAS)).toEqual(valoresPerfil);
    expect(new Set(grupos).size).toBe(valoresPerfil.length);
    expect([...grupos].sort()).toEqual([...valoresPerfil].sort());

    for (const opcion of OPCIONES_ALERGIAS) {
      const enDeclaradas = ALERGIAS_DECLARADAS.filter((valor) => valor === opcion.valor).length;
      const enDetectadas = ALERGIAS_DETECTADAS.filter((valor) => valor === opcion.valor).length;

      expect(enDeclaradas + enDetectadas).toBe(1);
      expect(ETIQUETAS_ALERGIAS[opcion.valor]).toBe(opcion.etiqueta);
    }

    expect(OPCIONES_ALERGIAS_DECLARABLES.map(({ valor, etiqueta }) => ({ valor, etiqueta })))
      .toEqual(
        OPCIONES_ALERGIAS
          .filter((opcion) => ALERGIAS_DECLARADAS.includes(opcion.valor))
          .map(({ valor, etiqueta }) => ({ valor, etiqueta })),
      );
  });

  it('sigue detectando sintéticos justo en 30 %, pero no por debajo', () => {
    const justo = alergiasDetectadasDe({
      composicion: [{ material: 'POLIESTER', porcentaje: 30 }],
    });
    const bajo = alergiasDetectadasDe({
      composicion: [
        { material: 'POLIESTER', porcentaje: 29 },
        { material: 'ALGODON', porcentaje: 71 },
      ],
    });

    expect(justo.map((alergia) => alergia.valor)).toContain('FIBRAS_SINTETICAS');
    expect(bajo.map((alergia) => alergia.valor)).not.toContain('FIBRAS_SINTETICAS');
  });

  it('detecta Otra alergia ante composición vacía o material OTRO', () => {
    expect(alergiasDetectadasDe({}).map((alergia) => alergia.valor)).toContain('OTRA');
    expect(
      alergiasDetectadasDe({
        composicion: [{ material: 'OTRO', porcentaje: 100 }],
      }).map((alergia) => alergia.valor),
    ).toContain('OTRA');
  });
});

  it('solo usa las etiquetas compartidas del perfil infantil, en su mismo orden', () => {expect(opcionesAlergiasDeclaradas()).toEqual(
      DECLARABLES.map((valor) => ({ valor, etiqueta: ETIQUETAS_ALERGIAS[valor], detalle: '' })),
    );
    const detectadas = alergiasDetectadasDe({
      composicion: [
        { material: 'POLIESTER', porcentaje: 30 },
        { material: 'LANA', porcentaje: 15 },
        { material: 'ALGODON', porcentaje: 10 },
        { material: 'LATEX', porcentaje: 10 },
        { material: 'CUERO', porcentaje: 10 },
        { material: 'OTRO', porcentaje: 25 },
      ],
    });
    detectadas.forEach((a) => expect(a.etiqueta).toBe(ETIQUETAS_ALERGIAS[a.valor]));
  });

describe('filtro-alergia-estandar — detección por composición', () => {
  it('advierte sintéticos solo al llegar o superar el 30 % (límite incluido)', () => {
    const justo = alergiasDetectadasDe({
      composicion: [{ material: 'POLIESTER', porcentaje: 30 }],
    });
    const bajo = alergiasDetectadasDe({
      composicion: [
        { material: 'POLIESTER', porcentaje: 29 },
        { material: 'ALGODON', porcentaje: 71 },
      ],
    });
    expect(justo.map((a) => a.valor)).toEqual(['FIBRAS_SINTETICAS']);
    expect(justo[0].detalle).toBe('Contiene 30% fibras sintéticas');
    expect(bajo.map((a) => a.valor)).toEqual(['ALGODON']);
  });

  it('acumula porcentajes de materiales sintéticos distintos', () => {
    const r = alergiasDetectadasDe({
      composicion: [
        { material: 'NYLON', porcentaje: 15 },
        { material: 'ELASTANO', porcentaje: 15 },
      ],
    });
    expect(r.map((a) => a.valor)).toContain('FIBRAS_SINTETICAS');
    expect(r.find((a) => a.valor === 'FIBRAS_SINTETICAS')?.detalle).toBe(
      'Contiene 30% fibras sintéticas',
    );
  });

  it('detecta látex con cualquiera de los dos materiales (LATEX o ELASTANO)', () => {
    const conLatex = alergiasDetectadasDe({ composicion: [{ material: 'LATEX', porcentaje: 5 }] });
    const conElastano = alergiasDetectadasDe({
      composicion: [{ material: 'ELASTANO', porcentaje: 5 }],
    });
    expect(conLatex.map((a) => a.valor)).toContain('LATEX_ELASTICO');
    expect(conElastano.map((a) => a.valor)).toContain('LATEX_ELASTICO');
  });

  it('la composición vacía activa «Otra alergia» y no detecta material alguno', () => {
    const r = alergiasDetectadasDe({});
    expect(r.map((a) => a.valor)).toEqual(['OTRA']);
    expect(r[0].detalle).toBe('Composición no declarada, verificar otra alergia');
  });

  it('el material OTRO activa «Otra alergia» con su propio motivo', () => {
    const r = alergiasDetectadasDe({
      composicion: [{ material: 'OTRO', porcentaje: 100 }],
    });
    expect(r.map((a) => a.valor)).toEqual(['OTRA']);
    expect(r[0].detalle).toBe('Contiene un material no especificado, verificar otra alergia');
  });

  it('no repite alergias y prevalece el orden del perfil infantil', () => {
    const r = alergiasDetectadasDe({
      composicion: [{ material: 'ALGODON', porcentaje: 100 }],
    });
    expect(r).toEqual([
      { valor: 'ALGODON', etiqueta: ETIQUETAS_ALERGIAS.ALGODON, detalle: 'Contiene algodón' },
    ]);
  });
});

describe('filtro-alergia-estandar — alergias declaradas', () => {
  it('devuelve las 4 marcadas con etiqueta del perfil y sin motivo', () => {
    const r = alergiasDeclaradasDe({
      tintesSinteticos: true,
      brochesMetalicos: true,
      contieneNiquel: true,
      tratamientoFormaldehido: true,
    });
    expect(r.map((a) => a.valor)).toEqual(DECLARABLES);
    r.forEach((a) => expect(a.detalle).toBe(''));
  });

  it('no marca lo que el vendedor dejó en falso', () => {
    const r = alergiasDeclaradasDe({ tintesSinteticos: true });
    expect(r.map((a) => a.valor)).toEqual(['TINTES']);
  });
});
