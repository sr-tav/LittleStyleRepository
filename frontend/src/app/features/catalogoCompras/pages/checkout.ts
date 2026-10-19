import { HttpErrorResponse } from '@angular/common/http';
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
import { DireccionEnvio, ResumenCheckout } from '../models/checkout.models';
import { DEPARTAMENTOS_COLOMBIA, municipiosDe } from '../data/municipios-colombia';
import { CarritoService } from '../services/carrito.service';
import { CheckoutService } from '../services/checkout.service';
import { PagoWompi } from '../components/pago-wompi';

/**
 * Gestiona la dirección de entrega, el perfil infantil y el resumen previo al pago.
 * @see US-10
 * @see UI-11
 * @see UI-12
 */
@Component({
  selector: 'app-checkout',
  imports: [Navbar, RouterLink, ReactiveFormsModule, PagoWompi],
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
  protected readonly claveIdempotencia = signal(crypto.randomUUID());
  protected readonly solicitudPago = computed(() => {
    const perfilInfantilId = this.perfilId();
    if (this.resumen() === null || perfilInfantilId === null) return null;
    return { perfilInfantilId, direccion: this.direccionFormulario() };
  });
  protected readonly cargando = signal(true);
  protected readonly procesando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly precio = formatearPrecio;

  /**
   * Valida los datos de entrega; requiere código postal de seis dígitos y celular colombiano
   * que inicia en 3 y contiene diez dígitos.
   */
  protected readonly form = this.fb.nonNullable.group({
    destinatario: ['', [Validators.required, Validators.maxLength(120)]],
    direccion: ['', [Validators.required, Validators.maxLength(200)]],
    complemento: ['', [Validators.maxLength(120)]],
    codigoPostal: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    departamento: ['', [Validators.required, Validators.maxLength(80)]],
    municipio: ['', [Validators.required, Validators.maxLength(80)]],
    telefono: ['', [Validators.required, Validators.pattern(/^3\d{9}$/)]],
  });

  /**
   * Carga en paralelo los perfiles infantiles y la dirección de entrega guardada.
   * @returns No devuelve un valor.
   */
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

  /**
   * Valida dirección, perfil y disponibilidad del carrito antes de solicitar el resumen.
   * Guarda la dirección editada antes de continuar.
   * @returns No devuelve un valor.
   */
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

  /**
   * Restaura la dirección guardada e intenta avanzar con ella al resumen.
   * @returns No devuelve un valor.
   */
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

  /**
   * Actualiza el departamento y reinicia el municipio para mantener la selección consistente.
   * @param event Evento del selector de departamento.
   * @returns No devuelve un valor.
   */
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

  /**
   * Solicita el cálculo del resumen y avanza al paso de pago al recibirlo.
   * @param perfilInfantilId Identificador del perfil infantil seleccionado.
   * @returns No devuelve un valor.
   */
  private solicitarResumen(perfilInfantilId: number): void {
    this.procesando.set(true);
    this.checkoutService.calcularResumen({
      perfilInfantilId,
      direccion: this.direccionFormulario(),
    }).subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.claveIdempotencia.set(crypto.randomUUID());
        this.paso.set('resumen');
        this.procesando.set(false);
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.procesando.set(false);
      },
    });
  }

  /**
   * Carga una dirección en el formulario y reconcilia departamento/municipio sin depender de tildes.
   * @param direccion Dirección que se copia al formulario.
   * @returns No devuelve un valor.
   */
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

  /**
   * Normaliza espacios y convierte campos opcionales vacíos a `null`.
   * @returns Dirección preparada para las operaciones de checkout.
   */
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

  /**
   * Verifica que el código postal y la combinación de departamento/municipio estén completos.
   * @param direccion Dirección que se comprueba.
   * @returns `true` si los datos geográficos coinciden con el catálogo local.
   */
  private direccionCompleta(direccion: DireccionEnvio): boolean {
    const departamento = this.departamentos.find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.departamento ?? ''),
    );
    const municipio = municipiosDe(departamento ?? '').find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.municipio ?? ''),
    );
    return Boolean(direccion.codigoPostal && departamento && municipio);
  }

  /**
   * Quita diacríticos y normaliza espacios y mayúsculas para comparar ubicaciones.
   * @param valor Texto de ubicación que se normaliza.
   * @returns Texto normalizado para comparación.
   */
  private normalizarUbicacion(valor: string): string {
    return valor.normalize('NFD').replace(/\p{M}/gu, '').trim().toLocaleUpperCase('es-CO');
  }

  protected volverAInformacion(): void {
    this.paso.set('informacion');
    this.resumen.set(null);
  }

  /**
   * Comprueba que el carrito no esté vacío y todas sus líneas estén disponibles.
   * @returns `true` cuando cada producto puede incluirse en la compra.
   */
  protected stockDisponible(): boolean {
    const items = this.carrito.items();
    return items.length > 0 && items.every((item) => item.disponible);
  }
}
