export type TipoReporte = 'ventas' | 'inventario' | 'demanda';

export const OPCIONES_REPORTE: { valor: TipoReporte; etiqueta: string }[] = [
  { valor: 'ventas', etiqueta: 'Ventas' },
  { valor: 'inventario', etiqueta: 'Inventario' },
  { valor: 'demanda', etiqueta: 'Demanda' },
];

export type FiltrosReporte = { startDate?: string; endDate?: string; categoria?: string };

export type ArchivoDescargado = { blob: Blob; nombre: string };

export type EstadoExportacion =
  | { fase: 'generando' }
  /** `progreso` es 0-100, o `null` si el servidor/proxy no envió Content-Length. */
  | { fase: 'descargando'; progreso: number | null }
  | { fase: 'listo'; archivo: ArchivoDescargado };

export type AccionReporte = 'descargar' | 'imprimir';
