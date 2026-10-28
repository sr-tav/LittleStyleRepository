import { Rol } from '../../../core/models/auth.models';

/** Tipo genérico para las respuestas paginadas del backend. */
export type Pagina<T> = {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
};

/** Estados del ciclo de vida de una cuenta de usuario. */
export type EstadoUsuario = 'ACTIVO' | 'SUSPENDIDO' | 'BLOQUEADO' | 'ELIMINADO';

/** Estados de publicación de una prenda en el catálogo. */
export type EstadoPrenda = 'ACTIVA' | 'INACTIVA';

/** Estados del flujo de moderación posterior de prendas. */
export type EstadoModeracion = 'SIN_REVISAR' | 'APROBADA' | 'DESACTIVADA';

/** Tipos de incidencia que puede reportar un usuario. */
export type TipoIncidencia =
  | 'APELACION_SUSPENSION'
  | 'APELACION_PRODUCTO'
  | 'REPORTE_PRODUCTO'
  | 'SOPORTE_GENERAL';

/** Estados del ciclo de vida de una incidencia. */
export type EstadoIncidencia = 'ABIERTA' | 'EN_REVISION' | 'RESUELTA' | 'RECHAZADA';

/** Resultado de una acción registrada en la auditoría. */
export type ResultadoAuditoria = 'EXITO' | 'FALLO';

/** Cuenta de usuario presentada en la gestión administrativa. */
export interface UsuarioAdmin {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  nombreTienda: string | null;
  rol: Rol;
  estado: EstadoUsuario;
  fechaRegistro: string;
}

/** Cambio de estado solicitado por el administrador para una cuenta. */
export interface CambioEstadoUsuario {
  estado: 'SUSPENDIDO' | 'BLOQUEADO' | 'ACTIVO';
  motivo: string;
}

/** Filtros opcionales del listado de usuarios del panel. */
export interface FiltrosUsuarios {
  rol?: Rol;
  estado?: EstadoUsuario;
  texto?: string;
  pagina?: number;
  tamano?: number;
}

/** Prenda presentada en la cola de moderación. */
export interface PrendaAdmin {
  id: number;
  nombre: string;
  precio: number;
  estado: EstadoPrenda;
  estadoModeracion: EstadoModeracion;
  motivoModeracion: string | null;
  vendedorId: number;
  vendedorEmail: string;
  vendedorTienda: string | null;
  fechaActualizacion: string;
}

/** Composición de materiales declarada por el vendedor. */
export interface ComposicionMaterial {
  material: string;
  porcentaje: number;
}

/** Talla declarada por el vendedor con existencias y medidas. */
export interface TallaAdmin {
  talla: string;
  stock: number;
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  edadMinMeses: number | null;
  edadMaxMeses: number | null;
  stockMinimo: number | null;
}

/** Detalle completo de una prenda para evaluar su moderación. */
export interface PrendaAdminDetalle extends PrendaAdmin {
  categoria: string;
  genero: string | null;
  descripcion: string | null;
  instruccionesCuidado: string | null;
  marca: string | null;
  estampado: string | null;
  tintesSinteticos: boolean;
  brochesMetalicos: boolean;
  contieneNiquel: boolean;
  tratamientoFormaldehido: boolean;
  fechaCreacion: string;
  composicion: ComposicionMaterial[];
  tallas: TallaAdmin[];
  imagenes: string[];
}

/** Filtros opcionales de la cola de moderación. */
export interface FiltrosModeracion {
  vendedorEmail?: string;
  estadoPrenda?: EstadoPrenda;
  estadoModeracion?: EstadoModeracion;
  texto?: string;
  pagina?: number;
  tamano?: number;
}

/** Registro inmutable de auditoría (solo lectura). */
export interface RegistroAuditoria {
  id: number;
  fechaHora: string;
  adminId: number;
  adminEmail: string;
  rol: Rol;
  accion: string;
  tipoEntidad: string;
  entidadId: number | null;
  valorAnterior: string | null;
  valorNuevo: string | null;
  motivo: string | null;
  resultado: ResultadoAuditoria;
  hash: string;
  hashAnterior: string | null;
}

/** Filtros opcionales del visor de auditoría. */
export interface FiltrosAuditoria {
  adminId?: number;
  adminEmail?: string;
  accion?: string;
  tipoEntidad?: string;
  entidadId?: number;
  resultado?: string;
  desde?: string;
  hasta?: string;
  pagina?: number;
  tamano?: number;
}

/** Resultado de la verificación de integridad de la cadena de auditoría. */
export interface RespuestaIntegridad {
  totalRegistros: number;
  integra: boolean;
  primerInconsistenteId: number | null;
  detalle: string | null;
}

/** Incidencia reportada por un usuario y gestionada por el administrador. */
export interface Incidencia {
  id: number;
  reportanteEmail: string;
  tipo: TipoIncidencia;
  referenciaId: number | null;
  referenciaDetalle: string | null;
  asunto: string;
  descripcion: string;
  estado: EstadoIncidencia;
  respuesta: string | null;
  fechaCreacion: string;
  fechaResolucion: string | null;
}

/** Filtros opcionales de la bandeja de incidencias. */
export interface FiltrosIncidencias {
  estado?: EstadoIncidencia;
  tipo?: TipoIncidencia;
  rol?: Rol;
  texto?: string;
  pagina?: number;
  tamano?: number;
}

/** Respuesta escrita por el administrador al resolver o rechazar una incidencia. */
export interface RespuestaIncidencia {
  respuesta: string;
}

/** Datos enviados por un usuario al crear una incidencia. */
export interface CrearIncidencia {
  tipo: TipoIncidencia;
  asunto: string;
  descripcion: string;
  referenciaId?: number;
  referenciaDetalle?: string;
}

/** Indicadores consolidados del panel administrativo. */
export interface ResumenAdmin {
  usuariosActivos: number;
  usuariosSuspendidos: number;
  usuariosBloqueados: number;
  usuariosEliminados: number;
  prendasSinRevisar: number;
  prendasPublicadas: number;
  prendasRechazadas: number;
  incidenciasAbiertas: number;
  incidenciasEnRevision: number;
  tasaResolucionPorcentaje: number;
}

/** Etiquetas en español para los estados de usuario. */
export const ETIQUETAS_ESTADO_USUARIO: Record<EstadoUsuario, string> = {
  ACTIVO: 'Activo',
  SUSPENDIDO: 'Suspendido',
  BLOQUEADO: 'Bloqueado',
  ELIMINADO: 'Eliminado',
};

/** Etiquetas en español para los roles de usuario. */
export const ETIQUETAS_ROL: Record<Rol, string> = {
  CLIENTE: 'Cliente',
  VENDEDOR: 'Vendedor',
  ADMINISTRADOR: 'Administrador',
};

/** Etiquetas en español para los estados de prenda. */
export const ETIQUETAS_ESTADO_PRENDA: Record<EstadoPrenda, string> = {
  ACTIVA: 'Activa',
  INACTIVA: 'Inactiva',
};

/** Etiquetas en español para los estados de moderación. */
export const ETIQUETAS_MODERACION: Record<EstadoModeracion, string> = {
  SIN_REVISAR: 'Sin revisar',
  APROBADA: 'Aprobada',
  DESACTIVADA: 'Desactivada',
};

/** Etiquetas en español para los tipos de incidencia. */
export const ETIQUETAS_TIPO_INCIDENCIA: Record<TipoIncidencia, string> = {
  APELACION_SUSPENSION: 'Apelación de suspensión',
  APELACION_PRODUCTO: 'Apelación de producto',
  REPORTE_PRODUCTO: 'Reporte de producto',
  SOPORTE_GENERAL: 'Soporte general',
};

/** Etiquetas en español para los estados de incidencia. */
export const ETIQUETAS_ESTADO_INCIDENCIA: Record<EstadoIncidencia, string> = {
  ABIERTA: 'Abierta',
  EN_REVISION: 'En revisión',
  RESUELTA: 'Resuelta',
  RECHAZADA: 'Rechazada',
};

/** Etiquetas en español para los resultados de auditoría. */
export const ETIQUETAS_RESULTADO: Record<ResultadoAuditoria, string> = {
  EXITO: 'Éxito',
  FALLO: 'Fallo',
};

export const ETIQUETAS_ACCION: Record<string, string> = {
  HABILITAR_USUARIO: 'Habilitar usuario',
  SUSPENDER_USUARIO: 'Suspender usuario',
  BLOQUEAR_USUARIO: 'Bloquear usuario',
  APROBAR_PRODUCTO: 'Aprobar producto',
  DESACTIVAR_PRODUCTO: 'Desactivar producto',
  RESTAURAR_PRODUCTO: 'Restaurar producto',
  TOMAR_INCIDENCIA: 'Tomar incidencia',
  RESOLVER_INCIDENCIA: 'Resolver incidencia',
  RECHAZAR_INCIDENCIA: 'Rechazar incidencia',
  ACCION_FALLIDA: 'Acción fallida',
};

export const ETIQUETAS_TIPO_ENTIDAD: Record<string, string> = {
  USUARIO: 'Usuario',
  PRENDA: 'Prenda',
  INCIDENCIA: 'Incidencia',
};
