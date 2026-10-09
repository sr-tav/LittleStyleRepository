import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import {
  AlergiaTextil,
  ColorPreferido,
  Estampado,
  PerfilInfantil,
} from '../models/perfil-infantil.models';
import { formatearEdadPerfil } from '../models/edad-perfil';
import {
  ETIQUETAS_ALERGIAS,
  ETIQUETAS_COLORES,
  ETIQUETAS_ESTAMPADOS,
} from '../models/perfil-opciones';
import { PerfilActivoService } from '../services/perfil-activo.service';
import { PerfilesService } from '../services/perfiles.service';

@Component({
  selector: 'app-perfiles-lista',
  imports: [Navbar, RouterLink],
  templateUrl: './perfiles-lista.html',
})
export class PerfilesLista implements OnInit {
  private readonly perfilesService = inject(PerfilesService);
  private readonly perfilActivoService = inject(PerfilActivoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly perfiles = signal<PerfilInfantil[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmacion = signal<string | null>(null);
  protected readonly modoSeleccion = signal(false);

  ngOnInit(): void {
    this.modoSeleccion.set(
      this.route.snapshot.queryParamMap.get('modo') === 'seleccionar',
    );
    const resultado = this.route.snapshot.queryParamMap.get('resultado');
    if (resultado === 'creado') {
      this.confirmacion.set('El perfil infantil se creó correctamente.');
    } else if (resultado === 'actualizado') {
      this.confirmacion.set('Los cambios del perfil se guardaron correctamente.');
    }

    this.perfilesService.listar().subscribe({
      next: (perfiles) => {
        this.perfiles.set(perfiles);
        this.perfilActivoService.sincronizar(perfiles);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.error.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }

  protected edad(perfil: PerfilInfantil): string {
    return formatearEdadPerfil(perfil);
  }

  protected esActivo(perfil: PerfilInfantil): boolean {
    return this.perfilActivoService.perfil()?.id === perfil.id;
  }

  protected seleccionar(perfil: PerfilInfantil): void {
    this.perfilActivoService.seleccionar(perfil.id);
  }

  protected seleccionarYVolver(perfil: PerfilInfantil): void {
    this.seleccionar(perfil);
    void this.router.navigateByUrl('/cliente');
  }

  protected contextura(perfil: PerfilInfantil): string {
    return { DELGADA: 'Delgada', MEDIA: 'Media', ROBUSTA: 'Robusta' }[perfil.contextura];
  }

  protected alergias(perfil: PerfilInfantil): string {
    const etiquetas = perfil.alergias.map((alergia: AlergiaTextil) =>
      alergia === 'OTRA' && perfil.otraAlergia
        ? `Otra: ${perfil.otraAlergia}`
        : ETIQUETAS_ALERGIAS[alergia],
    );
    return etiquetas.join(', ');
  }

  protected colores(perfil: PerfilInfantil): string {
    return [
      ...perfil.coloresPreferidos.map((color: ColorPreferido) => ETIQUETAS_COLORES[color]),
      perfil.otroColor,
    ]
      .filter((valor): valor is string => Boolean(valor))
      .join(', ');
  }

  protected estampados(perfil: PerfilInfantil): string {
    return [
      ...perfil.estampadosPreferidos.map((estampado: Estampado) => ETIQUETAS_ESTAMPADOS[estampado]),
      perfil.otroEstampado,
    ]
      .filter((valor): valor is string => Boolean(valor))
      .join(', ');
  }
}
