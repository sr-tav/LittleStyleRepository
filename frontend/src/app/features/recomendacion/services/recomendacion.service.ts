import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { RecomendacionItem } from '../models/recomendacion.models';

/**
 * Consulta recomendaciones y sus identificadores para un perfil infantil.
 * @see US-07
 * @see UI-6
 * @see RNF-11
 */
@Injectable({ providedIn: 'root' })
export class RecomendacionService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/cliente/recomendaciones`;
  /**
   * Solicita la lista de productos recomendados para el perfil indicado.
   * @param perfilId Identificador del perfil infantil.
   * @returns Flujo con los productos y resultados de compatibilidad.
   */
  listar(perfilId: number): Observable<RecomendacionItem[]> {
    return this.http.get<RecomendacionItem[]>(this.base, { params: { perfilId } });
  }
  /**
   * Solo identificadores recomendados, sin tope: filtro de servidor de la vitrina.
   * @param perfilId Identificador del perfil infantil.
   * @returns Flujo con los identificadores de productos recomendados.
   */
  ids(perfilId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.base}/ids`, { params: { perfilId } });
  }
  /**
   * Consulta la recomendación de una prenda para el perfil indicado.
   * @param prendaId Identificador de la prenda.
   * @param perfilId Identificador del perfil infantil.
   * @returns Flujo con la talla sugerida, compatibilidad y advertencias.
   */
  porPrenda(prendaId: number, perfilId: number): Observable<RecomendacionItem> {
    return this.http.get<RecomendacionItem>(`${this.base}/prenda/${prendaId}`, { params: { perfilId } });
  }
}
