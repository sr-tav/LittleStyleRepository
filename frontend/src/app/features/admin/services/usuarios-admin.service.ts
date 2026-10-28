import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CambioEstadoUsuario, FiltrosUsuarios, Pagina, UsuarioAdmin } from '../models/admin.models';

/**
 * Gestión administrativa de cuentas de usuario (US-14): listado con filtros,
 * detalle y cambio de estado (suspender / bloquear / habilitar) con motivo.
 * @see US-14
 */
@Injectable({ providedIn: 'root' })
export class UsuariosAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin/usuarios`;

  /**
   * Lista las cuentas de la plataforma aplicando solo los filtros definidos.
   * @param filtros Rol, estado, texto libre y paginación.
   * @returns Flujo con la página de usuarios que coincide con los filtros.
   * @see US-14
   */
  listar(filtros: FiltrosUsuarios): Observable<Pagina<UsuarioAdmin>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<UsuarioAdmin>>(this.base, { params });
  }

  /**
   * Consulta el detalle de una cuenta de usuario.
   * @param id Identificador de la cuenta.
   * @returns Flujo con los datos completos del usuario.
   * @see US-14
   */
  detalle(id: number): Observable<UsuarioAdmin> {
    return this.http.get<UsuarioAdmin>(`${this.base}/${id}`);
  }

  /**
   * Cambia el estado de una cuenta con el motivo obligatorio que queda en auditoría.
   * @param id Identificador del usuario cuyo estado cambia.
   * @param cuerpo Estado destino (SUSPENDIDO, BLOQUEADO o ACTIVO) y motivo (10 a 500 caracteres).
   * @returns Flujo con el usuario actualizado.
   * @see US-14
   */
  cambiarEstado(id: number, cuerpo: CambioEstadoUsuario): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`${this.base}/${id}/estado`, cuerpo);
  }
}
