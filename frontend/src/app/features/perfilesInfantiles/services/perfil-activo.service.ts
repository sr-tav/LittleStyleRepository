import { Injectable, signal } from '@angular/core';

import { PerfilInfantil } from '../models/perfil-infantil.models';

const CLAVE_PERFIL_ACTIVO = 'littleStyle.perfilActivoId';

/**
 * Mantiene el perfil infantil activo en memoria y conserva su identificador localmente.
 * @see US-06
 * @see UI-4
 */
@Injectable({ providedIn: 'root' })
export class PerfilActivoService {
  private readonly activo = signal<PerfilInfantil | null>(null);
  private perfiles: PerfilInfantil[] = [];

  readonly perfil = this.activo.asReadonly();

  /**
   * Restaura el perfil guardado si sigue disponible; en caso contrario, activa el primero.
   * @param perfiles Perfiles infantiles disponibles para el usuario.
   * @returns No devuelve un valor.
   */
  sincronizar(perfiles: PerfilInfantil[]): void {
    this.perfiles = perfiles;
    const idGuardado = this.leerIdGuardado();
    const perfilGuardado =
      idGuardado === null ? undefined : perfiles.find(({ id }) => id === idGuardado);
    const seleccionado = perfilGuardado ?? perfiles[0] ?? null;
    this.activo.set(seleccionado);
    this.persistirId(seleccionado?.id ?? null);
  }

  /**
   * Activa y persiste el perfil identificado.
   * @param id Identificador del perfil que se desea activar.
   * @returns No devuelve un valor.
   * @throws RangeError si el identificador no corresponde a un perfil disponible.
   */
  seleccionar(id: number): void {
    const perfil = this.perfiles.find((actual) => actual.id === id);
    if (!perfil) throw new RangeError('No se puede activar un perfil inexistente.');
    this.activo.set(perfil);
    this.persistirId(perfil.id);
  }

  /**
   * Olvida el perfil activo (memoria y localStorage); se llama al cerrar sesión.
   * @returns No devuelve un valor.
   */
  limpiar(): void {
    this.perfiles = [];
    this.activo.set(null);
    this.persistirId(null);
  }

  /**
   * Lee y valida el identificador de perfil guardado en almacenamiento local.
   * @returns Identificador entero positivo o `null` si no es válido.
   */
  private leerIdGuardado(): number | null {
    const valor = globalThis.localStorage?.getItem(CLAVE_PERFIL_ACTIVO);
    if (!valor) return null;
    const id = Number(valor);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  }

  /**
   * Guarda el identificador seleccionado o elimina la clave si no hay perfil activo.
   * @param id Identificador que se persiste, o `null` para limpiar.
   * @returns No devuelve un valor.
   */
  private persistirId(id: number | null): void {
    if (id === null) {
      globalThis.localStorage?.removeItem(CLAVE_PERFIL_ACTIVO);
      return;
    }
    globalThis.localStorage?.setItem(CLAVE_PERFIL_ACTIVO, String(id));
  }
}
