import { AuthResponse, Rol } from '../models/auth.models';

/** Construye un JWT sin firma válida (solo para pruebas del frontend) con el claim exp indicado. */
export function crearJwt(expiraEnMs: number): string {
  const b64 = (obj: object) => btoa(JSON.stringify(obj)).replace(/=+$/, '');
  return `${b64({ alg: 'HS256' })}.${b64({ sub: 'test@correo.com', exp: Math.floor(expiraEnMs / 1000) })}.firma`;
}

export function crearAuthResponse(rol: Rol, expiraEn = Date.now() + 60 * 60 * 1000): AuthResponse {
  return {
    token: crearJwt(expiraEn),
    tipo: 'Bearer',
    expiraEn,
    usuario: {
      id: 1,
      nombre: 'Tomás',
      apellido: 'Aristizabal',
      email: 'test@correo.com',
      telefono: '3001234567',
      nombreTienda: rol === 'VENDEDOR' ? 'Mundo Pequeño' : null,
      rol,
    },
  };
}
