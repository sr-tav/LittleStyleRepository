import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { EstadoPrenda, ImagenPrenda, Prenda, PrendaRequest } from '../models/prenda.models';

@Injectable({ providedIn: 'root' })
export class PrendasService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/vendedor/prendas`;

  listar(busqueda = ''): Observable<Prenda[]> {
    const texto = busqueda.trim();
    const params = texto ? new HttpParams().set('q', texto) : undefined;
    return this.http.get<Prenda[]>(this.baseUrl, { params });
  }

  obtener(id: number): Observable<Prenda> {
    return this.http.get<Prenda>(`${this.baseUrl}/${id}`);
  }

  crear(request: PrendaRequest): Observable<Prenda> {
    return this.http.post<Prenda>(this.baseUrl, request);
  }

  actualizar(id: number, request: PrendaRequest): Observable<Prenda> {
    return this.http.put<Prenda>(`${this.baseUrl}/${id}`, request);
  }

  cambiarEstado(id: number, estado: EstadoPrenda): Observable<Prenda> {
    return this.http.patch<Prenda>(`${this.baseUrl}/${id}/estado`, { estado });
  }

  /** Envía todas las fotos en una sola petición multipart (campo `archivos`). */
  subirImagenes(id: number, archivos: File[]): Observable<ImagenPrenda[]> {
    const datos = new FormData();
    archivos.forEach((archivo) => datos.append('archivos', archivo, archivo.name));
    return this.http.post<ImagenPrenda[]>(`${this.baseUrl}/${id}/imagenes`, datos);
  }

  marcarPrincipal(id: number, imagenId: number): Observable<ImagenPrenda[]> {
    return this.http.put<ImagenPrenda[]>(`${this.baseUrl}/${id}/imagenes/${imagenId}/principal`, {});
  }

  eliminarImagen(id: number, imagenId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/imagenes/${imagenId}`);
  }
}
