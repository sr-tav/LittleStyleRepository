import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ResumenAdmin } from '../models/admin.models';

/**
 * Indicadores consolidados del panel administrativo (US-14).
 * @see US-14
 */
@Injectable({ providedIn: 'root' })
export class ResumenAdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin/resumen`;

  /**
   * Consulta los KPIs del panel: usuarios por estado, prendas por moderación,
   * incidencias abiertas y tasa de resolución.
   * @returns Flujo con los indicadores del resumen.
   * @see US-14
   */
  consultar(): Observable<ResumenAdmin> {
    return this.http.get<ResumenAdmin>(this.base);
  }
}
