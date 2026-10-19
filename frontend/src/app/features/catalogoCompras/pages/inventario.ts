import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { NivelStockBadge } from '../components/nivel-stock-badge';
import { PrendaMiniatura } from '../components/prenda-miniatura';
import { ETIQUETAS_CATEGORIAS, ETIQUETAS_NIVEL } from '../models/prenda-opciones';
import { InventarioItem } from '../models/prenda.models';
import { InventarioService } from '../services/inventario.service';

interface Edicion {
  prendaId: number;
  tallas: { talla: string; stock: number | null; stockMinimo: number | null }[];
}

/**
 * UI-21: Inventario del vendedor con actualización de stock por talla en línea.
 * @see UI-21
 */
@Component({
  selector: 'app-inventario',
  imports: [Navbar, RouterLink, NivelStockBadge, PrendaMiniatura],
  templateUrl: './inventario.html',
})
export class Inventario implements OnInit {
  private readonly inventarioService = inject(InventarioService);
  private readonly route = inject(ActivatedRoute);

  protected readonly items = signal<InventarioItem[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly alertasActivas = signal(0);
  protected readonly edicion = signal<Edicion | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorEdicion = signal<string | null>(null);
  /** Aviso de lo que pasó con las alertas tras guardar (generada, resuelta o sin cambios). */
  protected readonly mensaje = signal<{ tipo: 'exito' | 'alerta' | 'error'; texto: string } | null>(null);

  protected readonly categoria = (item: InventarioItem) => ETIQUETAS_CATEGORIAS[item.categoria];
  protected readonly minimosDe = (item: InventarioItem) => {
    const valores = item.tallas.map((t) => t.stockMinimo).filter((v): v is number => v != null);
    return valores.length === 0 ? '—' : valores.join(' · ');
  };

  /**
   * Carga el inventario inicial.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Recarga inventario y alertas; atiende el parámetro `prenda` para abrir una edición directa.
   * @returns No devuelve un valor.
   * @see UI-21
   * @see UI-22
   */
  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.inventarioService.listar().subscribe({
      next: (items) => {
        this.items.set(items);
        this.cargando.set(false);
        // Desde UI-22 se llega con ?prenda=ID para abrir directamente su edición
        const prendaId = Number(this.route.snapshot.queryParamMap.get('prenda'));
        const destino = items.find((i) => i.prendaId === prendaId);
        if (destino) this.editar(destino);
      },
      error: (err) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
    this.contarAlertas();
  }

  /**
   * Actualiza el contador de alertas y muestra un aviso si la consulta falla.
   * @returns No devuelve un valor.
   */
  private contarAlertas(): void {
    this.inventarioService.alertas().subscribe({
      next: (alertas) => this.alertasActivas.set(alertas.length),
      error: () => {
        this.alertasActivas.set(0);
        this.mensaje.set({ tipo: 'error', texto: 'No se pudo actualizar el contador de alertas.' });
      },
    });
  }

  /**
   * Copia las tallas del producto al estado temporal de edición.
   * @param item Elemento de inventario que se editará.
   * @returns No devuelve un valor.
   */
  protected editar(item: InventarioItem): void {
    this.errorEdicion.set(null);
    this.edicion.set({
      prendaId: item.prendaId,
      tallas: item.tallas.map((t) => ({ ...t })),
    });
  }

  protected cancelar(): void {
    this.edicion.set(null);
    this.errorEdicion.set(null);
  }

  /**
   * Convierte la entrada de stock a entero o `null` y actualiza la talla indicada.
   * @param indice Índice de la talla editada.
   * @param valor Texto introducido en el control.
   * @returns No devuelve un valor.
   */
  protected cambiarStock(indice: number, valor: string): void {
    this.edicion.update((e) =>
      e ? { ...e, tallas: e.tallas.map((t, i) => (i === indice ? { ...t, stock: entero(valor) } : t)) } : e,
    );
  }

  /**
   * Actualiza el mínimo de alerta; una entrada vacía representa ausencia de mínimo.
   * @param indice Índice de la talla editada.
   * @param valor Texto introducido en el control.
   * @returns No devuelve un valor.
   */
  protected cambiarMinimo(indice: number, valor: string): void {
    const minimo = valor.trim() === '' ? null : entero(valor);
    this.edicion.update((e) =>
      e ? { ...e, tallas: e.tallas.map((t, i) => (i === indice ? { ...t, stockMinimo: minimo } : t)) } : e,
    );
  }

  /**
   * Suma las existencias de todas las tallas en edición.
   * @returns Total de unidades, o cero si no hay edición activa.
   */
  protected totalEdicion(): number {
    return this.edicion()?.tallas.reduce((total, t) => total + (t.stock ?? 0), 0) ?? 0;
  }

  /**
   * Valida los rangos de stock y mínimos antes de persistir la edición.
   * El stock admite de 0 a 100000; el mínimo, de 0 a 10000 o vacío.
   * @returns No devuelve un valor.
   * @see UI-21
   */
  protected guardar(): void {
    const e = this.edicion();
    if (!e || this.guardando()) return;
    const stockInvalido = (v: number | null) => v === null || v < 0 || v > 100_000;
    const minimoInvalido = (v: number | null) => v !== null && (v < 0 || v > 10_000);
    if (e.tallas.some((t) => stockInvalido(t.stock) || minimoInvalido(t.stockMinimo))) {
      this.errorEdicion.set('Usa números enteros: stock entre 0 y 100000 y mínimo entre 0 y 10000 (vacío = sin alerta).');
      return;
    }
    const anterior = this.items().find((i) => i.prendaId === e.prendaId);
    this.guardando.set(true);
    this.errorEdicion.set(null);
    this.mensaje.set(null);
    this.inventarioService
      .actualizarStock(e.prendaId, {
        tallas: e.tallas.map((t) => ({ talla: t.talla, stock: t.stock as number, stockMinimo: t.stockMinimo })),
      })
      .subscribe({
        next: (actualizado) => {
          this.items.update((items) => items.map((i) => (i.prendaId === actualizado.prendaId ? actualizado : i)));
          this.guardando.set(false);
          this.edicion.set(null);
          this.mensaje.set(this.avisoAlertas(anterior?.nivel ?? null, actualizado));
          this.contarAlertas();
        },
        error: (err) => {
          this.guardando.set(false);
          this.errorEdicion.set(procesarErrorApi(err));
        },
      });
  }

  /**
   * Mensaje según lo que pasó con el nivel de stock tras guardar.
   * @param nivelAnterior Nivel de stock antes de guardar, si se conoce.
   * @param actualizado Elemento de inventario recibido tras guardar.
   * @returns Aviso que comunica un cambio de nivel o la actualización realizada.
   */
  private avisoAlertas(
    nivelAnterior: InventarioItem['nivel'] | null,
    actualizado: InventarioItem,
  ): { tipo: 'exito' | 'alerta' | 'error'; texto: string } {
    if (actualizado.nivel !== 'DISPONIBLE' && actualizado.nivel !== nivelAnterior) {
      return {
        tipo: 'alerta',
        texto: `${actualizado.nombre} entró en ${ETIQUETAS_NIVEL[actualizado.nivel]}: mínimos por talla ${this.minimosDe(actualizado)}.`,
      };
    }
    if (actualizado.nivel === 'DISPONIBLE' && nivelAnterior !== null && nivelAnterior !== 'DISPONIBLE') {
      return { tipo: 'exito', texto: `${actualizado.nombre} volvió a estar disponible.` };
    }
    return { tipo: 'exito', texto: `Stock de ${actualizado.nombre} actualizado.` };
  }
}

/**
 * Convierte una cadena de dígitos a número entero.
 * @param valor Texto que se valida y convierte.
 * @returns Número convertido o `null` si contiene caracteres no numéricos.
 */
function entero(valor: string): number | null {
  return /^\d+$/.test(valor.trim()) ? Number(valor) : null;
}
