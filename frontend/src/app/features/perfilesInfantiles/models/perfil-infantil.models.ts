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
export type Contextura = 'DELGADA' | 'MEDIA' | 'ROBUSTA';
export type Holgura = 'AJUSTADA' | 'REGULAR' | 'HOLGADA';

export interface Medicion {
  id: number;
  fechaMedicion: string;
  estaturaCm: number;
  pesoKg: number;
}

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
}

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

export interface MedicionRequest {
  fechaMedicion: string;
  estaturaCm: number;
  pesoKg: number;
}
