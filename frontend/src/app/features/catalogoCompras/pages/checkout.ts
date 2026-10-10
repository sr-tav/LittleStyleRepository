import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { formatearPrecio } from '../models/prenda-opciones';
import { DireccionEnvio, PedidoCreado, ResumenCheckout } from '../models/checkout.models';
import { DEPARTAMENTOS_COLOMBIA, municipiosDe } from '../data/municipios-colombia';
import { CarritoService } from '../services/carrito.service';
import { CheckoutService } from '../services/checkout.service';

@Component({
  selector: 'app-checkout',
  imports: [Navbar, RouterLink, ReactiveFormsModule],
  templateUrl: './checkout.html',
})
export class CheckoutPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly perfilesService = inject(PerfilesService);
  private readonly perfilActivoService = inject(PerfilActivoService);
  private readonly checkoutService = inject(CheckoutService);

  protected readonly carrito = inject(CarritoService);
  protected readonly departamentos = DEPARTAMENTOS_COLOMBIA;
  protected readonly departamentoSeleccionado = signal('');
  protected readonly municipios = computed(() => municipiosDe(this.departamentoSeleccionado()));
  protected readonly perfilNombre = signal<string | null>(null);
  protected readonly direccionGuardada = signal<DireccionEnvio | null>(null);
  protected readonly editandoDireccion = signal(true);
  protected readonly perfilId = signal<number | null>(null);
  protected readonly paso = signal<'informacion' | 'resumen'>('informacion');
  protected readonly resumen = signal<ResumenCheckout | null>(null);
  protected readonly pedido = signal<PedidoCreado | null>(null);
  protected readonly cargando = signal(true);
  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly precio = formatearPrecio;

  protected readonly form = this.fb.nonNullable.group({
    destinatario: ['', [Validators.required, Validators.maxLength(120)]],
    direccion: ['', [Validators.required, Validators.maxLength(200)]],
    complemento: ['', [Validators.maxLength(120)]],
    codigoPostal: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    departamento: ['', [Validators.required, Validators.maxLength(80)]],
    municipio: ['', [Validators.required, Validators.maxLength(80)]],
    telefono: ['', [Validators.required, Validators.pattern(/^3\d{9}$/)]],
  });

  ngOnInit(): void {
    forkJoin({
      perfiles: this.perfilesService.listar(),
      direccion: this.checkoutService.obtenerDireccion(),
    }).subscribe({
      next: ({ perfiles, direccion }) => {
        this.perfilActivoService.sincronizar(perfiles);
        const perfil = this.perfilActivoService.perfil();
        this.perfilId.set(perfil?.id ?? null);
        this.perfilNombre.set(perfil?.nombre ?? null);
        const guardada = direccion.guardada ? direccion.direccion : null;
        this.direccionGuardada.set(guardada);
        this.editandoDireccion.set(guardada === null || !this.direccionCompleta(guardada));
        if (guardada) this.cargarDireccionEnFormulario(guardada);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cargando.set(false);
      },
    });
  }

  protected continuarAResumen(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const perfilInfantilId = this.perfilId();
    if (perfilInfantilId === null) {
      this.error.set('Crea un perfil infantil antes de continuar con la compra.');
      return;
    }
    if (this.carrito.items().length === 0) {
      this.error.set('El carrito está vacío. Agrega productos antes de continuar.');
      return;
    }

    if (this.editandoDireccion()) {
      this.procesando.set(true);
      this.checkoutService.guardarDireccion(this.direccionFormulario()).subscribe({
        next: (respuesta) => {
          if (!respuesta.guardada || !respuesta.direccion) {
            this.error.set('No fue posible guardar la dirección de entrega.');
            this.procesando.set(false);
            return;
          }
          this.direccionGuardada.set(respuesta.direccion);
          this.cargarDireccionEnFormulario(respuesta.direccion);
          this.editandoDireccion.set(false);
          this.solicitarResumen(perfilInfantilId);
        },
        error: (error: unknown) => {
          this.error.set(procesarErrorApi(error));
          this.procesando.set(false);
        },
      });
      return;
    }

    this.procesando.set(true);
    this.solicitarResumen(perfilInfantilId);
  }

  protected usarDireccionGuardada(): void {
    const direccion = this.direccionGuardada();
    if (!direccion) return;
    this.cargarDireccionEnFormulario(direccion);
    this.editandoDireccion.set(false);
    this.continuarAResumen();
  }

  protected editarDireccion(): void {
    this.error.set(null);
    this.editandoDireccion.set(true);
  }

  protected cambioDepartamento(event: Event): void {
    const departamento = (event.currentTarget as HTMLSelectElement).value;
    this.form.controls.departamento.setValue(departamento);
    this.departamentoSeleccionado.set(departamento);
    this.form.controls.municipio.setValue('');
    this.form.controls.municipio.markAsUntouched();
  }

  protected cancelarEdicionDireccion(): void {
    const direccion = this.direccionGuardada();
    if (!direccion) return;
    this.cargarDireccionEnFormulario(direccion);
    this.editandoDireccion.set(false);
    this.error.set(null);
  }

  private solicitarResumen(perfilInfantilId: number): void {
    this.procesando.set(true);
    this.checkoutService.calcularResumen({
      perfilInfantilId,
      direccion: this.direccionFormulario(),
    }).subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.paso.set('resumen');
        this.procesando.set(false);
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.procesando.set(false);
      },
    });
  }

  private cargarDireccionEnFormulario(direccion: DireccionEnvio): void {
    const departamento = this.departamentos.find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.departamento ?? ''),
    ) ?? '';
    const municipio = municipiosDe(departamento).find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.municipio ?? ''),
    ) ?? '';
    this.departamentoSeleccionado.set(departamento);
    this.form.patchValue({
      destinatario: direccion.destinatario,
      direccion: direccion.direccion,
      complemento: direccion.complemento ?? '',
      codigoPostal: direccion.codigoPostal ?? '',
      departamento,
      municipio,
      telefono: direccion.telefono,
    });
  }

  private direccionFormulario(): DireccionEnvio {
    const direccion = this.form.getRawValue();
    return {
      ...direccion,
      destinatario: direccion.destinatario.trim(),
      direccion: direccion.direccion.trim(),
      complemento: direccion.complemento.trim() || null,
      codigoPostal: direccion.codigoPostal.trim(),
      departamento: direccion.departamento.trim(),
      municipio: direccion.municipio.trim(),
      telefono: direccion.telefono.trim(),
    };
  }

  private direccionCompleta(direccion: DireccionEnvio): boolean {
    const departamento = this.departamentos.find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.departamento ?? ''),
    );
    const municipio = municipiosDe(departamento ?? '').find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.municipio ?? ''),
    );
    return Boolean(direccion.codigoPostal && departamento && municipio);
  }

  private normalizarUbicacion(valor: string): string {
    return valor.normalize('NFD').replace(/\p{M}/gu, '').trim().toLocaleUpperCase('es-CO');
  }

  protected volverAInformacion(): void {
    if (this.pedido()) return;
    this.paso.set('informacion');
    this.resumen.set(null);
  }

  protected confirmarPedido(): void {
    if (this.procesando() || this.pedido() || this.resumen() === null) return;
    const perfilInfantilId = this.perfilId();
    if (perfilInfantilId === null) {
      this.error.set('Selecciona un perfil infantil para confirmar el pedido.');
      return;
    }
    this.procesando.set(true);
    this.error.set(null);
    this.checkoutService.crearPedido({
      perfilInfantilId,
      direccion: this.direccionFormulario(),
    }).subscribe({
      next: (pedido) => {
        this.pedido.set(pedido);
        this.procesando.set(false);
        this.carrito.cargar().subscribe({
          error: (error: unknown) => this.error.set(procesarErrorApi(error)),
        });
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.procesando.set(false);
      },
    });
  }

  protected stockDisponible(): boolean {
    const items = this.carrito.items();
    return items.length > 0 && items.every((item) => item.disponible);
  }
}
