import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CrearPerfilConMedicionRequest,
  Medicion,
  MedicionRequest,
  PerfilInfantil,
  PerfilRequest,
} from '../models/perfil-infantil.models';

/** Consulta y administra perfiles infantiles y sus mediciones. */
@Injectable({ providedIn: 'root' })
export class PerfilesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cliente/perfiles`;

  /**
   * Consulta los perfiles infantiles del cliente.
   * @returns Flujo con los perfiles disponibles.
   */
  listar(): Observable<PerfilInfantil[]> {
    return this.http.get<PerfilInfantil[]>(this.baseUrl);
  }

  /**
   * Consulta un perfil infantil por identificador.
   * @param id Identificador del perfil.
   * @returns Flujo con el perfil solicitado.
   */
  obtener(id: number): Observable<PerfilInfantil> {
    return this.http.get<PerfilInfantil>(`${this.baseUrl}/${id}`);
  }

  /**
   * Crea un perfil infantil.
   * @param request Preferencias y datos de identificación del perfil.
   * @returns Flujo con el perfil creado.
   */
  crear(request: PerfilRequest): Observable<PerfilInfantil> {
    return this.http.post<PerfilInfantil>(this.baseUrl, request);
  }

  /**
   * Crea un perfil junto con su medición inicial.
   * @param request Datos del perfil y de la medición inicial.
   * @returns Flujo con el perfil creado.
   */
  crearConMedicionInicial(request: CrearPerfilConMedicionRequest): Observable<PerfilInfantil> {
    return this.http.post<PerfilInfantil>(`${this.baseUrl}/con-medicion-inicial`, request);
  }

  /**
   * Actualiza los datos de un perfil infantil.
   * @param id Identificador del perfil.
   * @param request Valores actualizados del perfil.
   * @returns Flujo con el perfil actualizado.
   */
  actualizar(id: number, request: PerfilRequest): Observable<PerfilInfantil> {
    return this.http.put<PerfilInfantil>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Elimina el perfil identificado.
   * @param id Identificador del perfil.
   * @returns Flujo que finaliza sin contenido al completarse la eliminación.
   */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Consulta las mediciones registradas para un perfil.
   * @param id Identificador del perfil.
   * @returns Flujo con el historial de mediciones.
   */
  listarMediciones(id: number): Observable<Medicion[]> {
    return this.http.get<Medicion[]>(`${this.baseUrl}/${id}/mediciones`);
  }

  /**
   * Registra una medición para un perfil.
   * @param id Identificador del perfil.
   * @param request Fecha, estatura y peso de la medición.
   * @returns Flujo con la medición creada.
   */
  crearMedicion(id: number, request: MedicionRequest): Observable<Medicion> {
    return this.http.post<Medicion>(`${this.baseUrl}/${id}/mediciones`, request);
  }

  /**
   * Actualiza una medición existente de un perfil.
   * @param id Identificador del perfil.
   * @param medicionId Identificador de la medición.
   * @param request Fecha, estatura y peso actualizados.
   * @returns Flujo con la medición actualizada.
   */
  actualizarMedicion(id: number, medicionId: number, request: MedicionRequest): Observable<Medicion> {
    return this.http.put<Medicion>(`${this.baseUrl}/${id}/mediciones/${medicionId}`, request);
  }
}
