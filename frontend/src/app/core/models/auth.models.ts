/** Roles admitidos por la aplicación para las cuentas de usuario. */
export type Rol = 'CLIENTE' | 'VENDEDOR' | 'ADMINISTRADOR';

/**
 * Roles que pueden crearse desde el formulario público de registro.
 * @see US-05
 * @see UI-2
 */
export type RolRegistro = Exclude<Rol, 'ADMINISTRADOR'>;

/** Datos de identidad y contacto de la cuenta autenticada. */
export interface Usuario {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  nombreTienda: string | null;
  rol: Rol;
}

/** Resultado de autenticación con token, vencimiento y usuario asociado. */
export interface AuthResponse {
  token: string;
  tipo: 'Bearer';
  /** Epoch en milisegundos. */
  expiraEn: number;
  usuario: Usuario;
}

/** Credenciales enviadas para iniciar sesión. */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Datos y aceptación de términos enviados para crear una cuenta.
 * @see US-05
 * @see UI-2
 * @see RNF-06
 */
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

/** Campos opcionales para actualizar los datos de una cuenta. */
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

/**
 * Extrae el código de error estable del backend, si existe.
 * @param error Valor recibido cuyo cuerpo puede contener el error de API.
 * @returns Código estable del error o `null` si no está presente.
 */
export function codigoErrorApi(error: unknown): string | null {
  if (error instanceof Object && 'error' in (error as Record<string, unknown>)) {
    const body = (error as { error: Partial<ApiError> | null }).error;
    return body?.codigo ?? null;
  }
  return null;
}

export interface PasswordResetRequest {
  email: string;
}

export interface RestablecerPasswordRequest {
  token: string;
  password: string;
  confirmarPassword: string;
}
