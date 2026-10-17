export type Rol = 'CLIENTE' | 'VENDEDOR' | 'ADMINISTRADOR';

/** Roles que pueden crearse desde el formulario público de registro. */
export type RolRegistro = Exclude<Rol, 'ADMINISTRADOR'>;

export interface Usuario {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  nombreTienda: string | null;
  rol: Rol;
}

export interface AuthResponse {
  token: string;
  tipo: 'Bearer';
  /** Epoch en milisegundos. */
  expiraEn: number;
  usuario: Usuario;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegistroRequest {
  nombre: string;
  apellido: string;
  email: string;
  telefono: string;
  password: string;
  confirmarPassword: string;
  rol: RolRegistro;
  nombreTienda: string | null;
  aceptaTerminos: boolean;
}

export interface ActualizarCuentaRequest {
  nombre?: string;
  apellido?: string;
  email?: string;
  telefono?: string | null;
  nombreTienda?: string | null;
  passwordActual?: string | null;
  nuevaPassword?: string | null;
  confirmarNuevaPassword?: string | null;
}

/** Formato de error que devuelve el backend (ErrorResponse). */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  mensaje: string;
  path: string;
  /** Código estable de error (p. ej. PRENDA_EXCLUIDA_ALERGIA): el frontend decide por código, no por texto. */
  codigo?: string | null;
  errores?: Record<string, string>;
}

/** Extrae el código de error estable del backend, si existe. */
export function codigoErrorApi(error: unknown): string | null {
  if (error instanceof Object && 'error' in (error as Record<string, unknown>)) {
    const body = (error as { error: Partial<ApiError> | null }).error;
    return body?.codigo ?? null;
  }
  return null;
}
