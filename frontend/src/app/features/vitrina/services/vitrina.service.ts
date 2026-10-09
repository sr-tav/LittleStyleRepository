import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { FiltrosVitrina, Pagina, VitrinaDetalle, VitrinaItem } from '../models/vitrina.models';

/** Catálogo público del cliente (US-09): exploración con filtros y ficha. */
@Injectable({ providedIn: 'root' })
export class VitrinaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/cliente/catalogo`;

  explorar(filtros: FiltrosVitrina): Observable<Pagina<VitrinaItem>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<VitrinaItem>>(this.base, { params });
  }

  detalle(id: number): Observable<VitrinaDetalle> {
    return this.http.get<VitrinaDetalle>(`${this.base}/${id}`);
  }
}
