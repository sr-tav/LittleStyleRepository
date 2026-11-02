export type TipoReporte = 'ventas' | 'inventario' | 'demanda' | 'busquedas';

export const OPCIONES_REPORTE: { valor: TipoReporte; etiqueta: string }[] = [
  { valor: 'ventas', etiqueta: 'Ventas' },
  { valor: 'inventario', etiqueta: 'Inventario' },
  { valor: 'demanda', etiqueta: 'Demanda' },
  { valor: 'busquedas', etiqueta: 'Búsquedas' },
];

/** Formatos interoperables que expone la fábrica del backend (RNF-24). */
export type FormatoReporte = 'PDF' | 'XLSX' | 'CSV';

export const OPCIONES_FORMATO: { valor: FormatoReporte; etiqueta: string; extension: string }[] = [
  { valor: 'PDF', etiqueta: 'PDF (.pdf)', extension: 'pdf' },
  { valor: 'XLSX', etiqueta: 'Excel (.xlsx)', extension: 'xlsx' },
  { valor: 'CSV', etiqueta: 'CSV (.csv)', extension: 'csv' },
];

export function extensionDe(formato: FormatoReporte): string {
  return OPCIONES_FORMATO.find((o) => o.valor === formato)?.extension ?? 'pdf';
}

export type FiltrosReporte = { startDate?: string; endDate?: string; categoria?: string };

export type ArchivoDescargado = { blob: Blob; nombre: string };

export type EstadoExportacion =
  | { fase: 'generando' }
  /** `progreso` es 0-100, o `null` si el servidor/proxy no envió Content-Length. */
  | { fase: 'descargando'; progreso: number | null }
  | { fase: 'listo'; archivo: ArchivoDescargado };

export type AccionReporte = 'descargar' | 'imprimir';

/** Resultado que el cliente reporta al backend para medir el RNF-24 de punta a punta. */
export type ResultadoTelemetria =
  | 'OK'
  | 'ERROR_RED'
  | 'BLOB_VACIO'
  | 'ERROR_CLIENTE'
  | 'CANCELADO';

/** Desenlace de la impresión: iframe, pestaña de respaldo o ninguna ventana disponible. */
export type ResultadoImpresion = 'impreso' | 'pestana' | 'sin-ventana';
