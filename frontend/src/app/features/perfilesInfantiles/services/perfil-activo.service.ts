import { Injectable, signal } from '@angular/core';

import { PerfilInfantil } from '../models/perfil-infantil.models';

const CLAVE_PERFIL_ACTIVO = 'littleStyle.perfilActivoId';

@Injectable({ providedIn: 'root' })
export class PerfilActivoService {
  private readonly activo = signal<PerfilInfantil | null>(null);
  private perfiles: PerfilInfantil[] = [];

  readonly perfil = this.activo.asReadonly();

  sincronizar(perfiles: PerfilInfantil[]): void {
    this.perfiles = perfiles;
    const idGuardado = this.leerIdGuardado();
    const perfilGuardado =
      idGuardado === null ? undefined : perfiles.find(({ id }) => id === idGuardado);
    const seleccionado = perfilGuardado ?? perfiles[0] ?? null;
    this.activo.set(seleccionado);
    this.persistirId(seleccionado?.id ?? null);
  }

  seleccionar(id: number): void {
    const perfil = this.perfiles.find((actual) => actual.id === id);
    if (!perfil) throw new RangeError('No se puede activar un perfil inexistente.');
    this.activo.set(perfil);
    this.persistirId(perfil.id);
  }

  private leerIdGuardado(): number | null {
    const valor = globalThis.localStorage?.getItem(CLAVE_PERFIL_ACTIVO);
    if (!valor) return null;
    const id = Number(valor);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  }

  private persistirId(id: number | null): void {
    if (id === null) {
      globalThis.localStorage?.removeItem(CLAVE_PERFIL_ACTIVO);
      return;
    }
    globalThis.localStorage?.setItem(CLAVE_PERFIL_ACTIVO, String(id));
  }
}
