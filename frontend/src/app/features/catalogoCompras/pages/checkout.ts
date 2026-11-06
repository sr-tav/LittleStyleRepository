import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { ColorEtiquetaPipe } from '../../../shared/pipes/color-etiqueta.pipe';
import { EstampadoEtiquetaPipe } from '../../../shared/pipes/estampado-etiqueta.pipe';
import { procesarErrorApi } from '../../auth/auth.validators';
import { PerfilActivoService } from '../../perfilesInfantiles/services/perfil-activo.service';
import { PerfilesService } from '../../perfilesInfantiles/services/perfiles.service';
import { formatearPrecio } from '../models/prenda-opciones';
import { DireccionClienteGuardada, DireccionEnvio, ResumenCheckout } from '../models/checkout.models';
import { componerDireccion, parsearDireccion } from '../data/direccion-estructurada';
import { codigoPostalCoherente, codigosPostalesDe } from '../data/codigos-postales-colombia';
import { DEPARTAMENTOS_COLOMBIA, municipiosDe } from '../data/municipios-colombia';
import { CarritoService } from '../services/carrito.service';
import { CheckoutService } from '../services/checkout.service';
import { PagoWompi } from '../components/pago-wompi';
import {BotonVolver} from '../../../shared/components/boton-volver/boton-volver';

/**
 * Gestiona las direcciones de entrega, el perfil infantil y el resumen previo al pago.
 * @see US-10
 * @see UI-11
 * @see UI-12
 */
@Component({
  selector: 'app-checkout',
  imports: [Navbar, RouterLink, ReactiveFormsModule, PagoWompi, ColorEtiquetaPipe, EstampadoEtiquetaPipe, BotonVolver],
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
  protected readonly direccionesGuardadas = signal<DireccionClienteGuardada[]>([]);
  protected readonly direccionSeleccionadaId = signal<number | null>(null);
  protected readonly editandoDireccion = signal(true);
  protected readonly editandoDireccionId = signal<number | null>(null);
  protected readonly eliminandoDireccionId = signal<number | null>(null);
  protected readonly eliminacionEnCursoId = signal<number | null>(null);
  protected readonly guardandoPredeterminadaId = signal<number | null>(null);
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
    direccion: ['', [
      Validators.required,
      Validators.maxLength(100),
      (control: AbstractControl) => parsearDireccion(control.value) ? null : { formatoDireccion: true },
    ]],
    complemento: ['', [Validators.maxLength(120)]],
    codigoPostal: ['', [Validators.required, Validators.pattern(/^\d{6}$/), codigoPostalCoherente]],
    departamento: ['', [Validators.required, Validators.maxLength(80)]],
    municipio: ['', [Validators.required, Validators.maxLength(80)]],
    telefono: ['', [Validators.required, Validators.pattern(/^3\d{9}$/)]],
  });

  /**
   * Carga en paralelo los perfiles infantiles y las direcciones de entrega guardadas.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    forkJoin({
      perfiles: this.perfilesService.listar(),
      direcciones: this.checkoutService.listarDirecciones(),
    }).subscribe({
      next: ({ perfiles, direcciones }) => {
        this.perfilActivoService.sincronizar(perfiles);
        const perfil = this.perfilActivoService.perfil();
        this.perfilId.set(perfil?.id ?? null);
        this.perfilNombre.set(perfil?.nombre ?? null);
        this.direccionesGuardadas.set(direcciones);
        this.direccionSeleccionadaId.set(
          direcciones.find((direccion) => direccion.predeterminada)?.id ?? null,
        );
        this.editandoDireccion.set(direcciones.length === 0);
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
  protected continuarAResumen(event?: Event): void {
    event?.preventDefault();
    this.error.set(null);
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
      if (this.form.invalid) {
        this.form.markAllAsTouched();
        return;
      }
      this.procesando.set(true);
      const direccion = this.direccionFormulario();
      const id = this.editandoDireccionId();
      const solicitud = id === null
        ? this.checkoutService.crearDireccion(direccion)
        : this.checkoutService.actualizarDireccion(id, direccion);
      solicitud.subscribe({
        next: (respuesta) => {
          this.guardarDireccionEnLista(respuesta);
          this.direccionSeleccionadaId.set(respuesta.id);
          this.cargarDireccionEnFormulario(respuesta.direccion);
          this.editandoDireccionId.set(null);
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

    const seleccionada = this.direccionesGuardadas().find(
      (direccion) => direccion.id === this.direccionSeleccionadaId(),
    );
    if (!seleccionada) {
      this.error.set('Selecciona una dirección de entrega para continuar.');
      return;
    }
    if (!this.direccionCompleta(seleccionada.direccion)) {
      this.error.set('Completa los datos de esta dirección antes de continuar.');
      return;
    }
    this.cargarDireccionEnFormulario(seleccionada.direccion);
    this.procesando.set(true);
    this.solicitarResumen(perfilInfantilId);
  }

  /**
   * Restaura la dirección guardada e intenta avanzar con ella al resumen.
   * @returns No devuelve un valor.
   */
  protected usarDireccionGuardada(): void {
    this.continuarAResumen();
  }

  protected seleccionarDireccion(id: number): void {
    this.direccionSeleccionadaId.set(id);
    this.error.set(null);
  }

  protected agregarDireccion(): void {
    this.limpiarDireccionFormulario();
    this.editandoDireccionId.set(null);
    this.editandoDireccion.set(true);
    this.error.set(null);
  }

  protected editarDireccion(id: number): void {
    const guardada = this.direccionesGuardadas().find((direccion) => direccion.id === id);
    if (!guardada) return;
    this.direccionSeleccionadaId.set(id);
    this.editandoDireccionId.set(id);
    this.cargarDireccionEnFormulario(guardada.direccion);
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
    this.form.controls.codigoPostal.setValue('');
    this.form.controls.codigoPostal.markAsUntouched();
  }

  protected codigosPostales(): readonly string[] | null {
    const { departamento, municipio } = this.form.getRawValue();
    return codigosPostalesDe(departamento, municipio);
  }

  protected cambioMunicipio(): void {
    this.form.controls.codigoPostal.setValue('');
    this.form.controls.codigoPostal.markAsUntouched();
  }

  protected cancelarEdicionDireccion(): void {
    if (this.direccionesGuardadas().length === 0) return;
    this.limpiarDireccionFormulario();
    this.editandoDireccionId.set(null);
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
      direccion: direccion.direccion
        || componerDireccion(direccion.tipoVia, direccion.numeroVia, direccion.numeroCruce, direccion.placa),
      complemento: direccion.complemento ?? '',
      codigoPostal: direccion.codigoPostal ?? '',
      departamento,
      municipio,
      telefono: direccion.telefono,
    });
    this.form.controls.codigoPostal.updateValueAndValidity();
  }

  /**
   * Normaliza espacios y convierte campos opcionales vacíos a `null`.
   * @returns Dirección preparada para las operaciones de checkout.
   */
  private direccionFormulario(): DireccionEnvio {
    const direccion = this.form.getRawValue();
    const partesDireccion = parsearDireccion(direccion.direccion);
    if (!partesDireccion) throw new Error('La dirección del checkout no tiene un formato válido.');
    return {
      ...direccion,
      destinatario: direccion.destinatario.trim(),
      direccion: direccion.direccion.trim(),
      ...partesDireccion,
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
  protected direccionCompleta(direccion: DireccionEnvio): boolean {
    const departamento = this.departamentos.find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.departamento ?? ''),
    );
    const municipio = municipiosDe(departamento ?? '').find(
      (opcion) => this.normalizarUbicacion(opcion) === this.normalizarUbicacion(direccion.municipio ?? ''),
    );
    return Boolean(
      parsearDireccion(direccion.direccion)
      && direccion.destinatario && direccion.codigoPostal && departamento && municipio && direccion.telefono,
    );
  }

  private guardarDireccionEnLista(direccion: DireccionClienteGuardada): void {
    const direcciones = this.direccionesGuardadas();
    const indice = direcciones.findIndex((guardada) => guardada.id === direccion.id);
    this.direccionesGuardadas.set(indice < 0
      ? [...direcciones, direccion]
      : direcciones.map((guardada) => guardada.id === direccion.id ? direccion : guardada));
  }

  private limpiarDireccionFormulario(): void {
    this.form.reset();
    this.departamentoSeleccionado.set('');
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

  protected confirmarEliminacionDireccion(id: number): void {
    if (this.eliminacionEnCursoId() !== null) return;
    this.eliminandoDireccionId.set(id);
    this.error.set(null);
  }

  protected cancelarEliminacionDireccion(): void {
    if (this.eliminacionEnCursoId() !== null) return;
    this.eliminandoDireccionId.set(null);
  }

  protected eliminarDireccion(id: number): void {
    if (this.eliminacionEnCursoId() !== null) return;

    const eliminada = this.direccionesGuardadas().find((direccion) => direccion.id === id);
    if (!eliminada) return;

    this.error.set(null);
    this.eliminacionEnCursoId.set(id);

    this.checkoutService.eliminarDireccion(id).subscribe({
      next: () => {
        const restantes = this.direccionesGuardadas().filter((direccion) => direccion.id !== id);
        const predeterminadaActual = restantes.find((direccion) => direccion.predeterminada);
        const nuevaPredeterminada = eliminada.predeterminada
          ? predeterminadaActual ?? restantes[0]
          : predeterminadaActual;

        this.direccionesGuardadas.set(restantes.map((direccion) => ({
          ...direccion,
          predeterminada: direccion.id === nuevaPredeterminada?.id,
        })));

        if (this.direccionSeleccionadaId() === id) {
          this.direccionSeleccionadaId.set(nuevaPredeterminada?.id ?? null);
        }
        if (this.editandoDireccionId() === id) {
          this.editandoDireccionId.set(null);
          this.editandoDireccion.set(false);
        }

        this.eliminandoDireccionId.set(null);
        this.eliminacionEnCursoId.set(null);
      },
      error: (error: unknown) => {
        this.eliminacionEnCursoId.set(null);
        this.error.set(procesarErrorApi(error));
      },
    });
  }

  protected marcarPredeterminada(id: number): void {
    if (this.guardandoPredeterminadaId() !== null) return;

    this.error.set(null);
    this.guardandoPredeterminadaId.set(id);

    this.checkoutService.marcarDireccionPredeterminada(id).subscribe({
      next: (actualizada) => {
        this.direccionesGuardadas.update((direcciones) =>
          direcciones.map((direccion) => ({
            ...direccion,
            predeterminada: direccion.id === actualizada.id,
          })),
        );
        this.guardandoPredeterminadaId.set(null);
      },
      error: (error: unknown) => {
        this.guardandoPredeterminadaId.set(null);
        this.error.set(procesarErrorApi(error));
      },
    });
  }
}
