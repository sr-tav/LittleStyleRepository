import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { EstadoPrenda, ImagenPrenda, Prenda, PrendaRequest } from '../models/prenda.models';

/** Consulta y administra las prendas y sus imágenes para el vendedor. */
@Injectable({ providedIn: 'root' })
export class PrendasService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/vendedor/prendas`;

  /**
   * Consulta prendas y aplica una búsqueda textual cuando se proporciona.
   * @param busqueda Texto de búsqueda; se eliminan espacios de los extremos.
   * @returns Flujo con las prendas coincidentes.
   */
  listar(busqueda = ''): Observable<Prenda[]> {
    const texto = busqueda.trim();
    const params = texto ? new HttpParams().set('q', texto) : undefined;
    return this.http.get<Prenda[]>(this.baseUrl, { params });
  }

  /**
   * Consulta una prenda por identificador.
   * @param id Identificador de la prenda.
   * @returns Flujo con la ficha de producto.
   */
  obtener(id: number): Observable<Prenda> {
    return this.http.get<Prenda>(`${this.baseUrl}/${id}`);
  }

  /**
   * Crea una prenda con los datos proporcionados.
   * @param request Datos del producto.
   * @returns Flujo con la prenda creada.
   */
  crear(request: PrendaRequest): Observable<Prenda> {
    return this.http.post<Prenda>(this.baseUrl, request);
  }

  /**
   * Actualiza los datos de una prenda existente.
   * @param id Identificador de la prenda.
   * @param request Datos actualizados del producto.
   * @returns Flujo con la prenda actualizada.
   */
  actualizar(id: number, request: PrendaRequest): Observable<Prenda> {
    return this.http.put<Prenda>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Cambia el estado de publicación de una prenda.
   * @param id Identificador de la prenda.
   * @param estado Estado que se asignará.
   * @returns Flujo con la prenda actualizada.
   */
  cambiarEstado(id: number, estado: EstadoPrenda): Observable<Prenda> {
    return this.http.patch<Prenda>(`${this.baseUrl}/${id}/estado`, { estado });
  }

  /**
   * Envía todas las fotos en una sola petición multipart (campo `archivos`).
   * @param id Identificador de la prenda a la que pertenecen las imágenes.
   * @param archivos Archivos de imagen que se envían en la petición.
   * @returns Flujo con las imágenes registradas.
   */
  subirImagenes(id: number, archivos: File[]): Observable<ImagenPrenda[]> {
    const datos = new FormData();
    archivos.forEach((archivo) => datos.append('archivos', archivo, archivo.name));
    return this.http.post<ImagenPrenda[]>(`${this.baseUrl}/${id}/imagenes`, datos);
  }

  /**
   * Marca una imagen como principal para la prenda.
   * @param id Identificador de la prenda.
   * @param imagenId Identificador de la imagen.
   * @returns Flujo con la lista de imágenes actualizada.
   */
  marcarPrincipal(id: number, imagenId: number): Observable<ImagenPrenda[]> {
    return this.http.put<ImagenPrenda[]>(`${this.baseUrl}/${id}/imagenes/${imagenId}/principal`, {});
  }

  /**
   * Elimina una imagen de la prenda.
   * @param id Identificador de la prenda.
   * @param imagenId Identificador de la imagen.
   * @returns Flujo que finaliza sin contenido al completarse la operación.
   */
  eliminarImagen(id: number, imagenId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/imagenes/${imagenId}`);
  }
}
