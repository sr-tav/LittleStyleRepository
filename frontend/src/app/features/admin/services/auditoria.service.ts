import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  FiltrosAuditoria,
  Pagina,
  RegistroAuditoria,
  RespuestaIntegridad,
} from '../models/admin.models';

/**
 * Visor de auditoría del administrador (US-14): consulta en solo lectura de los
 * registros de acciones y verificación de la integridad de la cadena de hashes.
 * @see US-14
 * @see RNF-17
 */
@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin/auditoria`;

  /**
   * Lista los registros de auditoría aplicando solo los filtros definidos.
   * @param filtros Administrador, acción, entidad y paginación.
   * @returns Flujo con la página de registros que coincide con los filtros.
   * @see US-14
   */
  listar(filtros: FiltrosAuditoria): Observable<Pagina<RegistroAuditoria>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<RegistroAuditoria>>(this.base, { params });
  }

  /**
   * Verifica la integridad de la cadena de hashes de la auditoría.
   * @returns Flujo con el resultado de la verificación (válido o id inconsistente).
   * @see US-14
   */
  verificarIntegridad(): Observable<RespuestaIntegridad> {
    return this.http.get<RespuestaIntegridad>(`${this.base}/integridad`);
  }
}
