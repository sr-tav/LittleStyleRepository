import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { TallaSugerida } from '../../recomendacion/components/talla-sugerida';
import { RecomendacionItem } from '../../recomendacion/models/recomendacion.models';
import { RecomendacionService } from '../../recomendacion/services/recomendacion.service';
import { formatearPrecio } from '../models/prenda-opciones';
import {PrendaMiniatura} from '../components/prenda-miniatura';

@Component({
  selector: 'app-prenda-detalle-cliente',
  imports: [Navbar, RouterLink, TallaSugerida, PrendaMiniatura],
  templateUrl: './prenda-detalle-cliente.html',
})
export class PrendaDetalleCliente implements OnInit {
  private route = inject(ActivatedRoute);
  private reco = inject(RecomendacionService);
  private activo = inject(PerfilActivoService);

  protected readonly item = signal<RecomendacionItem | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly cargando = signal(true);
  protected readonly perfilId = signal<number | null>(null);

  protected precio(): string | null {
    const valor = this.item()?.precio ?? null;
    return valor === null ? null : formatearPrecio(valor);
  }

  ngOnInit(): void {
    const prendaId = Number(this.route.snapshot.paramMap.get('id'));
    const pid = this.activo.perfil()?.id ?? null;
    this.perfilId.set(pid);
    if (!pid) {
      this.error.set('Seleccioná un perfil primero.');
      this.cargando.set(false);
      return;
    }
    this.reco.porPrenda(prendaId, pid).subscribe({
      next: (r) => { this.item.set(r); this.cargando.set(false); },
      error: (e) => { this.error.set(procesarErrorApi(e)); this.cargando.set(false); },
    });
  }
}
