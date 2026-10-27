import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';
import {
  CrearIncidencia,
  FiltrosIncidencias,
  Incidencia,
  Pagina,
  RespuestaIncidencia,
} from '../models/admin.models';

/**
 * Gestión de incidencias (US-14): bandeja del administrador para apelaciones y
 * soporte, y bandeja propia del cliente o vendedor que reporta.
 * @see US-14
 */
@Injectable({ providedIn: 'root' })
export class IncidenciasService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseAdmin = `${environment.apiUrl}/admin/incidencias`;

  private get baseUsuario(): string {
    return `${environment.apiUrl}/${this.auth.rol() === 'VENDEDOR' ? 'vendedor' : 'cliente'}/incidencias`;
  }

  /**
   * Lista la bandeja de incidencias aplicando solo los filtros definidos.
   * @param filtros Estado, tipo, rol del reportante, texto y paginación.
   * @returns Flujo con la página de incidencias que coincide con los filtros.
   * @see US-14
   */
  listar(filtros: FiltrosIncidencias): Observable<Pagina<Incidencia>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<Incidencia>>(this.baseAdmin, { params });
  }

  /**
   * Consulta el detalle de una incidencia.
   * @param id Identificador de la incidencia.
   * @returns Flujo con la incidencia completa.
   * @see US-14
   */
  detalle(id: number): Observable<Incidencia> {
    return this.http.get<Incidencia>(`${this.baseAdmin}/${id}`);
  }

  /**
   * Toma una incidencia abierta para su atención (pasa a EN_REVISION).
   * @param id Identificador de la incidencia que se toma.
   * @returns Flujo con la incidencia actualizada.
   * @see US-14
   */
  tomar(id: number): Observable<Incidencia> {
    return this.http.post<Incidencia>(`${this.baseAdmin}/${id}/tomar`, {});
  }

  /**
   * Resuelve una incidencia en revisión con la respuesta obligatoria.
   * @param id Identificador de la incidencia que se resuelve.
   * @param cuerpo Respuesta para el reportante (10 a 4000 caracteres).
   * @returns Flujo con la incidencia actualizada.
   * @see US-14
   */
  resolver(id: number, cuerpo: RespuestaIncidencia): Observable<Incidencia> {
    return this.http.post<Incidencia>(`${this.baseAdmin}/${id}/resolver`, cuerpo);
  }

  /**
   * Rechaza una incidencia en revisión con la respuesta obligatoria.
   * @param id Identificador de la incidencia que se rechaza.
   * @param cuerpo Respuesta que explica el rechazo (10 a 4000 caracteres).
   * @returns Flujo con la incidencia actualizada.
   * @see US-14
   */
  rechazar(id: number, cuerpo: RespuestaIncidencia): Observable<Incidencia> {
    return this.http.post<Incidencia>(`${this.baseAdmin}/${id}/rechazar`, cuerpo);
  }

  /**
   * Lista las incidencias reportadas por el usuario autenticado.
   * @param pagina Índice de la página solicitada.
   * @param tamano Cantidad de registros por página.
   * @returns Flujo con la página de incidencias propias.
   * @see US-14
   */
  listarPropias(pagina = 0, tamano = 10): Observable<Pagina<Incidencia>> {
    const params = new HttpParams().set('pagina', String(pagina)).set('tamano', String(tamano));
    return this.http.get<Pagina<Incidencia>>(this.baseUsuario, { params });
  }

  /**
   * Crea una incidencia como usuario autenticado (cliente o vendedor).
   * @param cuerpo Datos de la incidencia: tipo, asunto, descripción y referencia opcional.
   * @returns Flujo con la incidencia creada.
   * @see US-14
   */
  crear(cuerpo: CrearIncidencia): Observable<Incidencia> {
    return this.http.post<Incidencia>(this.baseUsuario, cuerpo);
  }
}
