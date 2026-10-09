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
  tallas: { talla: string; stock: number | null }[];
  stockMinimo: number | null;
}

/** UI-21: Inventario del vendedor con actualización de stock por talla en línea. */
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

  ngOnInit(): void {
    this.cargar();
  }

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

  private contarAlertas(): void {
    this.inventarioService.alertas().subscribe({
      next: (alertas) => this.alertasActivas.set(alertas.length),
      error: () => {
        this.alertasActivas.set(0);
        this.mensaje.set({ tipo: 'error', texto: 'No se pudo actualizar el contador de alertas.' });
      },
    });
  }

  protected editar(item: InventarioItem): void {
    this.errorEdicion.set(null);
    this.edicion.set({
      prendaId: item.prendaId,
      tallas: item.tallas.map((t) => ({ ...t })),
      stockMinimo: item.stockMinimo,
    });
  }

  protected cancelar(): void {
    this.edicion.set(null);
    this.errorEdicion.set(null);
  }

  protected cambiarStock(indice: number, valor: string): void {
    this.edicion.update((e) =>
      e ? { ...e, tallas: e.tallas.map((t, i) => (i === indice ? { ...t, stock: entero(valor) } : t)) } : e,
    );
  }

  protected cambiarMinimo(valor: string): void {
    this.edicion.update((e) => (e ? { ...e, stockMinimo: entero(valor) } : e));
  }

  protected totalEdicion(): number {
    return this.edicion()?.tallas.reduce((total, t) => total + (t.stock ?? 0), 0) ?? 0;
  }

  protected guardar(): void {
    const e = this.edicion();
    if (!e || this.guardando()) return;
    const invalido = (v: number | null, max: number) => v === null || v < 0 || v > max;
    if (e.tallas.some((t) => invalido(t.stock, 100_000)) || invalido(e.stockMinimo, 10_000)) {
      this.errorEdicion.set('Usa números enteros: stock entre 0 y 100000 y mínimo entre 0 y 10000.');
      return;
    }
    const anterior = this.items().find((i) => i.prendaId === e.prendaId);
    this.guardando.set(true);
    this.errorEdicion.set(null);
    this.mensaje.set(null);
    this.inventarioService
      .actualizarStock(e.prendaId, {
        tallas: e.tallas.map((t) => ({ talla: t.talla, stock: t.stock as number })),
        stockMinimo: e.stockMinimo,
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

  /** Mensaje según lo que pasó con el nivel de stock tras guardar. */
  private avisoAlertas(
    nivelAnterior: InventarioItem['nivel'] | null,
    actualizado: InventarioItem,
  ): { tipo: 'exito' | 'alerta' | 'error'; texto: string } {
    if (actualizado.nivel !== 'DISPONIBLE' && actualizado.nivel !== nivelAnterior) {
      return {
        tipo: 'alerta',
        texto: `${actualizado.nombre} entró en ${ETIQUETAS_NIVEL[actualizado.nivel]}: actual ${actualizado.stockTotal}, mínimo ${actualizado.stockMinimo}.`,
      };
    }
    if (actualizado.nivel === 'DISPONIBLE' && nivelAnterior !== null && nivelAnterior !== 'DISPONIBLE') {
      return { tipo: 'exito', texto: `${actualizado.nombre} volvió a estar disponible.` };
    }
    return { tipo: 'exito', texto: `Stock de ${actualizado.nombre} actualizado.` };
  }
}

function entero(valor: string): number | null {
  return /^\d+$/.test(valor.trim()) ? Number(valor) : null;
}
