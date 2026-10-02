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

/** Formato de error que devuelve el backend (ErrorResponse). */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  mensaje: string;
  path: string;
  errores?: Record<string, string>;
}
