import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RecomendacionItem } from '../models/recomendacion.models';

@Injectable({ providedIn: 'root' })
export class RecomendacionService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/cliente/recomendaciones`;
  listar(perfilId: number): Observable<RecomendacionItem[]> {
    return this.http.get<RecomendacionItem[]>(this.base, { params: { perfilId } });
  }
  porPrenda(prendaId: number, perfilId: number): Observable<RecomendacionItem> {
    return this.http.get<RecomendacionItem>(`${this.base}/prenda/${prendaId}`, { params: { perfilId } });
  }
}
