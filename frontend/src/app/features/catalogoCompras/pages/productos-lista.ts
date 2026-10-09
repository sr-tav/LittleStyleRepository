import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { distinctUntilChanged, Subject, switchMap, takeUntil } from 'rxjs';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { NivelStockBadge } from '../components/nivel-stock-badge';
import { PrendaMiniatura } from '../components/prenda-miniatura';
import {
  describirComposicion,
  ETIQUETAS_CATEGORIAS,
  formatearPrecio,
} from '../models/prenda-opciones';
import { Prenda } from '../models/prenda.models';
import { PrendasService } from '../services/prendas.service';

/** UI-19: Mis productos (catálogo del vendedor). */
@Component({
  selector: 'app-productos-lista',
  imports: [Navbar, RouterLink, NivelStockBadge, PrendaMiniatura],
  templateUrl: './productos-lista.html',
})
export class ProductosLista implements OnInit, OnDestroy {
  private readonly prendasService = inject(PrendasService);
  private readonly busquedaServidor$ = new Subject<string>();
  private readonly destruir$ = new Subject<void>();

  protected readonly prendas = signal<Prenda[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly busqueda = signal('');
  protected readonly cambiandoEstado = signal<number | null>(null);
  /** Mensaje enviado por el formulario al guardar (estado de navegación). */
  protected readonly mensaje = signal<string | null>((history.state?.mensaje as string | undefined) ?? null);

  protected readonly filtradas = computed(() => {
    const texto = normalizar(this.busqueda());
    if (!texto) return this.prendas();
    return this.prendas().filter(
      (p) =>
        normalizar(p.nombre).includes(texto) ||
        normalizar(ETIQUETAS_CATEGORIAS[p.categoria]).includes(texto),
    );
  });

  protected readonly categoria = (p: Prenda) => ETIQUETAS_CATEGORIAS[p.categoria];
  protected readonly composicion = (p: Prenda) => describirComposicion(p.composicion);
  protected readonly precio = (p: Prenda) => formatearPrecio(p.precio);
  protected readonly imagen = (p: Prenda) => p.imagenes[0]?.url ?? null;

  ngOnInit(): void {
    this.cargar();
    // Búsqueda en el servidor: cada texto cancela la petición anterior
    this.busquedaServidor$
      .pipe(
        distinctUntilChanged(),
        switchMap((texto) => this.prendasService.listar(texto)),
        takeUntil(this.destruir$),
      )
      .subscribe({
        next: (prendas) => this.prendas.set(prendas),
        error: (err) => this.error.set(procesarErrorApi(err)),
      });
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }

  protected alBuscar(valor: string): void {
    this.busqueda.set(valor);
    this.busquedaServidor$.next(valor);
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.prendasService.listar().subscribe({
      next: (prendas) => {
        this.prendas.set(prendas);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }

  /** Publica u oculta la prenda del catálogo de clientes y del motor de recomendaciones. */
  protected alternarEstado(prenda: Prenda): void {
    if (this.cambiandoEstado() !== null) return;
    this.cambiandoEstado.set(prenda.id);
    this.mensaje.set(null);
    const nuevo = prenda.estado === 'ACTIVA' ? 'INACTIVA' : 'ACTIVA';
    this.prendasService.cambiarEstado(prenda.id, nuevo).subscribe({
      next: (actualizada) => {
        this.prendas.update((lista) => lista.map((p) => (p.id === actualizada.id ? actualizada : p)));
        this.cambiandoEstado.set(null);
      },
      error: (err) => {
        this.error.set(procesarErrorApi(err));
        this.cambiandoEstado.set(null);
      },
    });
  }
}

function normalizar(valor: string): string {
  return valor
    .trim()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase();
}
