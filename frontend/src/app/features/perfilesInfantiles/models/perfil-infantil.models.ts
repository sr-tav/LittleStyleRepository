/** Tipos de alergia textil registrados en las preferencias del perfil infantil. */
export type AlergiaTextil =
  | 'FIBRAS_SINTETICAS'
  | 'LANA'
  | 'TINTES'
  | 'BROCHES_METALICOS'
  | 'ALGODON'
  | 'LATEX_ELASTICO'
  | 'NIQUEL'
  | 'CUERO'
  | 'FORMALDEHIDO'
  | 'OTRA';
/** Colores que pueden seleccionarse como preferidos. */
export type ColorPreferido =
  | 'ROSA'
  | 'LILA'
  | 'AZUL'
  | 'CELESTE'
  | 'VERDE'
  | 'AMARILLO'
  | 'ROJO'
  | 'NARANJA'
  | 'MORADO'
  | 'BLANCO'
  | 'NEGRO'
  | 'GRIS'
  | 'BEIGE'
  | 'CAFE';
/** Estampados que pueden seleccionarse como preferidos. */
export type Estampado =
  | 'LISO'
  | 'FLORES'
  | 'ESTRELLAS'
  | 'RAYAS'
  | 'CUADROS'
  | 'LUNARES'
  | 'ANIMALES'
  | 'DIBUJOS_ANIMADOS'
  | 'GEOMETRICO'
  | 'MARINERO';
/** Opciones de contextura utilizadas en el perfil infantil. */
export type Contextura = 'DELGADA' | 'MEDIA' | 'ROBUSTA';
/** Opciones de ajuste de la prenda utilizadas en las recomendaciones. */
export type Holgura = 'AJUSTADA' | 'REGULAR' | 'HOLGADA';

/** Medición corporal registrada para un perfil infantil. */
export interface Medicion {
  id: number;
  fechaMedicion: string;
  estaturaCm: number;
  pesoKg: number;
}

/** Datos personales, preferencias y mediciones asociados a un perfil infantil. */
export interface PerfilInfantil {
  id: number;
  nombre: string;
  fechaNacimiento: string;
  edadAnios: number;
  mesesRestantes: number;
  contextura: Contextura;
  holgura: Holgura;
  alergias: AlergiaTextil[];
  otraAlergia: string | null;
  sinAlergias: boolean;
  coloresPreferidos: ColorPreferido[];
  estampadosPreferidos: Estampado[];
  otroColor: string | null;
  otroEstampado: string | null;
  fechaCreacion: string;
  fechaActualizacion: string;
  ultimaMedicion: Medicion | null;
  porcentajeCompletitud: number;
  camposPendientes: string[];
}

/** Datos editables enviados al crear o actualizar un perfil infantil. */
export interface PerfilRequest {
  nombre: string;
  fechaNacimiento: string;
  contextura: Contextura;
  holgura: Holgura;
  alergias: AlergiaTextil[];
  otraAlergia: string | null;
  sinAlergias: boolean;
  coloresPreferidos: ColorPreferido[];
  estampadosPreferidos: Estampado[];
  otroColor: string | null;
  otroEstampado: string | null;
}

/** Valores de estatura y peso asociados a una fecha de medición. */
export interface MedicionRequest {
  fechaMedicion: string;
  estaturaCm: number;
  pesoKg: number;
}

/** Solicitud que crea un perfil junto con su medición inicial. */
export interface CrearPerfilConMedicionRequest {
  perfil: PerfilRequest;
  medicionInicial: MedicionRequest;
}
