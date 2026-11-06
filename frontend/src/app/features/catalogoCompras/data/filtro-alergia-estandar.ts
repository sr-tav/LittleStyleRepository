import { AlergiaTextil } from '../../perfilesInfantiles/models/perfil-infantil.models';
import { OPCIONES_ALERGIAS } from '../../perfilesInfantiles/models/perfil-opciones';
import { MaterialTextil } from '../models/prenda.models';

/**
 * Espejo frontend de las reglas del backend.
 *
 * Sincronización: replicar
 * `FiltroAlergiaEstandar.java` si cambian los materiales, el umbral o las reglas.
 */

/** Campos que el vendedor declara manualmente; las etiquetas y el orden vienen del perfil. */
export interface DatosAlergiaPrenda {
  tintesSinteticos?: boolean;
  brochesMetalicos?: boolean;
  contieneNiquel?: boolean;
  tratamientoFormaldehido?: boolean;
  /** Una composición vacía o ausente activa OTRA, igual que el backend. */
  composicion?: ReadonlyArray<{ material: string; porcentaje: number }> | null;
}

type CampoAlergiaDeclarable = keyof Pick<
  DatosAlergiaPrenda,
  'tintesSinteticos' | 'brochesMetalicos' | 'contieneNiquel' | 'tratamientoFormaldehido'
>;

const CAMPOS_ALERGIAS_MANUALES: Partial<Record<AlergiaTextil, CampoAlergiaDeclarable>> = {
  TINTES: 'tintesSinteticos',
  BROCHES_METALICOS: 'brochesMetalicos',
  NIQUEL: 'contieneNiquel',
  FORMALDEHIDO: 'tratamientoFormaldehido',
};

/** Las opciones declarables conservan etiqueta y orden del perfil infantil. */
export const OPCIONES_ALERGIAS_DECLARABLES = OPCIONES_ALERGIAS.flatMap((opcion) => {
  const control = CAMPOS_ALERGIAS_MANUALES[opcion.valor];
  return control ? [{ ...opcion, control }] : [];
});

/** Grupo editable, derivado del mismo catálogo que presenta el perfil. */
export const ALERGIAS_DECLARADAS: readonly AlergiaTextil[] =
  OPCIONES_ALERGIAS_DECLARABLES.map((opcion) => opcion.valor);

/** Grupo que se calcula a partir de la composición, también en orden del perfil. */
export const ALERGIAS_DETECTADAS: readonly AlergiaTextil[] =
  OPCIONES_ALERGIAS
    .filter((opcion) => CAMPOS_ALERGIAS_MANUALES[opcion.valor] === undefined)
    .map((opcion) => opcion.valor);

/** Materiales sintéticos y umbral del backend FiltroAlergiaEstandar. */
const SINTETICOS: readonly MaterialTextil[] = [
  'POLIESTER',
  'NYLON',
  'ACRILICO',
  'ELASTANO',
  'VISCOSA',
];
const UMBRAL_SINTETICO = 30;

/** Alergia detectada con la etiqueta común y su motivo explicativo. */
export interface AlergiaDetectada {
  valor: AlergiaTextil;
  etiqueta: string;
  detalle: string;
}

/** Opciones editables para mostrar junto a los controles del vendedor. */
export function opcionesAlergiasDeclaradas(): AlergiaDetectada[] {
  return OPCIONES_ALERGIAS_DECLARABLES.map(({ valor, etiqueta }) => ({
    valor,
    etiqueta,
    detalle: '',
  }));
}

/** Alergias marcadas manualmente, con etiquetas del catálogo del perfil. */
export function alergiasDeclaradasDe(prenda: DatosAlergiaPrenda): AlergiaDetectada[] {
  return OPCIONES_ALERGIAS_DECLARABLES
    .filter((opcion) => Boolean(prenda[opcion.control]))
    .map(({ valor, etiqueta }) => ({ valor, etiqueta, detalle: '' }));
}

/**
 * Detecta alergias por composición con las mismas reglas que FiltroAlergiaEstandar:
 * sintéticos >= 30 %, lana, látex/elastano, cuero, algodón y OTRA.
 */
export function alergiasDetectadasDe(prenda: DatosAlergiaPrenda): AlergiaDetectada[] {
  const composicion = prenda.composicion ?? [];
  const tiene = (material: MaterialTextil): boolean =>
    composicion.some((componente) => componente.material === material);

  const razones = new Map<AlergiaTextil, string>();

  if (tiene('LANA')) razones.set('LANA', 'Contiene lana');
  if (tiene('LATEX') || tiene('ELASTANO')) {
    razones.set('LATEX_ELASTICO', 'Contiene látex/elastano');
  }
  if (tiene('CUERO')) razones.set('CUERO', 'Contiene cuero');
  if (tiene('ALGODON')) razones.set('ALGODON', 'Contiene algodón');

  const porcentajeSintetico = composicion
    .filter((componente) =>
      SINTETICOS.some((material) => material === componente.material),
    )
    .reduce((total, componente) => total + componente.porcentaje, 0);

  if (porcentajeSintetico >= UMBRAL_SINTETICO) {
    razones.set(
      'FIBRAS_SINTETICAS',
      `Contiene ${porcentajeSintetico}% fibras sintéticas`,
    );
  }

  if (composicion.length === 0 || tiene('OTRO')) {
    razones.set(
      'OTRA',
      composicion.length === 0
        ? 'Composición no declarada, verificar otra alergia'
        : 'Contiene un material no especificado, verificar otra alergia',
    );
  }

  return OPCIONES_ALERGIAS
    .filter((opcion) => razones.has(opcion.valor))
    .map(({ valor, etiqueta }) => ({
      valor,
      etiqueta,
      detalle: razones.get(valor) ?? '',
    }));
}
