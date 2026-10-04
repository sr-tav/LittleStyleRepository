import { AlergiaTextil, ColorPreferido, Estampado } from './perfil-infantil.models';

export const ETIQUETAS_ALERGIAS: Record<AlergiaTextil, string> = {
  FIBRAS_SINTETICAS: 'Fibras sintéticas',
  LANA: 'Lana',
  TINTES: 'Tintes',
  BROCHES_METALICOS: 'Broches metálicos',
  ALGODON: 'Algodón',
  LATEX_ELASTICO: 'Látex elástico',
  NIQUEL: 'Níquel',
  CUERO: 'Cuero',
  FORMALDEHIDO: 'Formaldehído',
  OTRA: 'Otra alergia',
};

export const OPCIONES_ALERGIAS = Object.entries(ETIQUETAS_ALERGIAS).map(([valor, etiqueta]) => ({
  valor: valor as AlergiaTextil,
  etiqueta,
}));

export const ETIQUETAS_COLORES: Record<ColorPreferido, string> = {
  ROSA: 'Rosa',
  LILA: 'Lila',
  AZUL: 'Azul',
  CELESTE: 'Celeste',
  VERDE: 'Verde',
  AMARILLO: 'Amarillo',
  ROJO: 'Rojo',
  NARANJA: 'Naranja',
  MORADO: 'Morado',
  BLANCO: 'Blanco',
  NEGRO: 'Negro',
  GRIS: 'Gris',
  BEIGE: 'Beige',
  CAFE: 'Café',
};

export const OPCIONES_COLORES: { valor: ColorPreferido; etiqueta: string; muestra: string }[] = [
  { valor: 'ROSA', etiqueta: ETIQUETAS_COLORES.ROSA, muestra: '#e85d9e' },
  { valor: 'LILA', etiqueta: ETIQUETAS_COLORES.LILA, muestra: '#9b72cf' },
  { valor: 'AZUL', etiqueta: ETIQUETAS_COLORES.AZUL, muestra: '#2672c9' },
  { valor: 'CELESTE', etiqueta: ETIQUETAS_COLORES.CELESTE, muestra: '#4aaed1' },
  { valor: 'VERDE', etiqueta: ETIQUETAS_COLORES.VERDE, muestra: '#36884a' },
  { valor: 'AMARILLO', etiqueta: ETIQUETAS_COLORES.AMARILLO, muestra: '#d2a500' },
  { valor: 'ROJO', etiqueta: ETIQUETAS_COLORES.ROJO, muestra: '#c83d3d' },
  { valor: 'NARANJA', etiqueta: ETIQUETAS_COLORES.NARANJA, muestra: '#d96c20' },
  { valor: 'MORADO', etiqueta: ETIQUETAS_COLORES.MORADO, muestra: '#7044a3' },
  { valor: 'BLANCO', etiqueta: ETIQUETAS_COLORES.BLANCO, muestra: '#ffffff' },
  { valor: 'NEGRO', etiqueta: ETIQUETAS_COLORES.NEGRO, muestra: '#242424' },
  { valor: 'GRIS', etiqueta: ETIQUETAS_COLORES.GRIS, muestra: '#757575' },
  { valor: 'BEIGE', etiqueta: ETIQUETAS_COLORES.BEIGE, muestra: '#c7b18a' },
  { valor: 'CAFE', etiqueta: ETIQUETAS_COLORES.CAFE, muestra: '#80553b' },
];

export const ETIQUETAS_ESTAMPADOS: Record<Estampado, string> = {
  LISO: 'Liso',
  FLORES: 'Flores',
  ESTRELLAS: 'Estrellas',
  RAYAS: 'Rayas',
  CUADROS: 'Cuadros',
  LUNARES: 'Lunares',
  ANIMALES: 'Animales',
  DIBUJOS_ANIMADOS: 'Dibujos animados',
  GEOMETRICO: 'Geométrico',
  MARINERO: 'Marinero',
};

export const OPCIONES_ESTAMPADOS = Object.entries(ETIQUETAS_ESTAMPADOS).map(
  ([valor, etiqueta]) => ({ valor: valor as Estampado, etiqueta }),
);
