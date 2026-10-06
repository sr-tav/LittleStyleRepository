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

@Injectable({ providedIn: 'root' })
export class PerfilesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cliente/perfiles`;

  listar(): Observable<PerfilInfantil[]> {
    return this.http.get<PerfilInfantil[]>(this.baseUrl);
  }

  obtener(id: number): Observable<PerfilInfantil> {
    return this.http.get<PerfilInfantil>(`${this.baseUrl}/${id}`);
  }

  crear(request: PerfilRequest): Observable<PerfilInfantil> {
    return this.http.post<PerfilInfantil>(this.baseUrl, request);
  }

  crearConMedicionInicial(request: CrearPerfilConMedicionRequest): Observable<PerfilInfantil> {
    return this.http.post<PerfilInfantil>(`${this.baseUrl}/con-medicion-inicial`, request);
  }

  actualizar(id: number, request: PerfilRequest): Observable<PerfilInfantil> {
    return this.http.put<PerfilInfantil>(`${this.baseUrl}/${id}`, request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listarMediciones(id: number): Observable<Medicion[]> {
    return this.http.get<Medicion[]>(`${this.baseUrl}/${id}/mediciones`);
  }

  crearMedicion(id: number, request: MedicionRequest): Observable<Medicion> {
    return this.http.post<Medicion>(`${this.baseUrl}/${id}/mediciones`, request);
  }

  actualizarMedicion(id: number, medicionId: number, request: MedicionRequest): Observable<Medicion> {
    return this.http.put<Medicion>(`${this.baseUrl}/${id}/mediciones/${medicionId}`, request);
  }
}
