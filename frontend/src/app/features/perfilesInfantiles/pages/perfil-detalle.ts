import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, take } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import {
  AlergiaTextil,
  ColorPreferido,
  Contextura,
  Estampado,
  Holgura,
  Medicion,
  MedicionRequest,
  PerfilInfantil,
  PerfilRequest,
} from '../models/perfil-infantil.models';
import { formatearEdadPerfil } from '../models/edad-perfil';
import {
  ETIQUETAS_ALERGIAS,
  ETIQUETAS_COLORES,
  ETIQUETAS_ESTAMPADOS,
  OPCIONES_ALERGIAS,
  OPCIONES_COLORES,
  OPCIONES_ESTAMPADOS,
} from '../models/perfil-opciones';
import { PerfilesService } from '../services/perfiles.service';

const FECHA_LOCAL = new Date();
const HOY = `${FECHA_LOCAL.getFullYear()}-${String(FECHA_LOCAL.getMonth() + 1).padStart(2, '0')}-${String(FECHA_LOCAL.getDate()).padStart(2, '0')}`;
const AYER_LOCAL = new Date(FECHA_LOCAL.getFullYear(), FECHA_LOCAL.getMonth(), FECHA_LOCAL.getDate() - 1);
const MAX_FECHA_NACIMIENTO = `${AYER_LOCAL.getFullYear()}-${String(AYER_LOCAL.getMonth() + 1).padStart(2, '0')}-${String(AYER_LOCAL.getDate()).padStart(2, '0')}`;

/**
 * Calcula los años cumplidos entre una fecha de nacimiento y una fecha de referencia.
 * @param fechaNacimiento Fecha de nacimiento en formato ISO de fecha.
 * @param fechaReferencia Fecha de comparación en formato ISO de fecha.
 * @returns Años cumplidos, descontando el año si aún no se alcanzó el aniversario.
 */
function edadEnAnios(fechaNacimiento: string, fechaReferencia: string): number {
  const nacimiento = new Date(`${fechaNacimiento}T00:00:00`);
  const referencia = new Date(`${fechaReferencia}T00:00:00`);
  let anios = referencia.getFullYear() - nacimiento.getFullYear();
  if (
    referencia.getMonth() < nacimiento.getMonth() ||
    (referencia.getMonth() === nacimiento.getMonth() && referencia.getDate() < nacimiento.getDate())
  ) {
    anios--;
  }
  return anios;
}

/**
 * Permite crear, consultar y actualizar perfiles infantiles y sus mediciones corporales.
 * @see US-06
 * @see UI-5
 */
@Component({
  selector: 'app-perfil-detalle',
  imports: [Navbar, ReactiveFormsModule, RouterLink],
  templateUrl: './perfil-detalle.html',
})
export class PerfilDetalle implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly perfilesService = inject(PerfilesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly hoy = HOY;
  protected readonly maxFechaNacimiento = MAX_FECHA_NACIMIENTO;
  protected readonly alergiasDisponibles = OPCIONES_ALERGIAS;
  protected readonly coloresDisponibles = OPCIONES_COLORES;
  protected readonly estampadosDisponibles = OPCIONES_ESTAMPADOS;
  protected readonly contexturas: { valor: Contextura; etiqueta: string }[] = [
    { valor: 'DELGADA', etiqueta: 'Delgada' },
    { valor: 'MEDIA', etiqueta: 'Media' },
    { valor: 'ROBUSTA', etiqueta: 'Robusta' },
  ];
  protected readonly holguras: { valor: Holgura; etiqueta: string }[] = [
    { valor: 'AJUSTADA', etiqueta: 'Ajustada' },
    { valor: 'REGULAR', etiqueta: 'Regular' },
    { valor: 'HOLGADA', etiqueta: 'Holgada' },
  ];

  protected readonly perfil = signal<PerfilInfantil | null>(null);
  protected readonly mediciones = signal<Medicion[]>([]);
  protected readonly esNuevo = signal(false);
  protected readonly esEditando = signal(false);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly guardandoMedicion = signal(false);
  protected readonly agregandoMedicion = signal(false);
  protected readonly medicionEditandoId = signal<number | null>(null);
  protected readonly eliminando = signal(false);
  protected readonly confirmarEliminacion = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  protected readonly errorMedicion = signal<string | null>(null);

  /**
   * Valida nombre, nacimiento, preferencias y alergias; la fecha debe ser anterior a hoy y
   * el perfil debe corresponder a una persona menor de 18 años.
   * @see US-06
   * @see UI-5
   */
  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(60), this.nombreNoVacio()]],
    fechaNacimiento: ['', [Validators.required, this.fechaNacimientoValida()]],
    contextura: this.fb.nonNullable.control<Contextura | ''>('', Validators.required),
    holgura: this.fb.nonNullable.control<Holgura | ''>('', Validators.required),
    sinAlergias: [false],
    alergias: this.fb.nonNullable.control<AlergiaTextil[]>([], [this.validarAlergias()]),
    otraAlergia: ['', [this.validarOtraAlergia()]],
    coloresPreferidos: this.fb.nonNullable.control<ColorPreferido[]>([]),
    estampadosPreferidos: this.fb.nonNullable.control<Estampado[]>([]),
    otroColor: ['', [this.validarPreferenciaLibre()]],
    otroEstampado: ['', [this.validarPreferenciaLibre()]],
  });

  /**
   * Valida mediciones dentro de los límites generales de 40–200 cm y 2–120 kg, además de
   * la fecha y el rango biométrico aplicable a la edad del perfil.
   * @see US-06
   * @see RNF-16
   */
  protected readonly medicionForm = this.fb.nonNullable.group({
    fechaMedicion: [HOY, [Validators.required, this.fechaMedicionValida()]],
    estaturaCm: [
      0,
      [
        Validators.required,
        Validators.min(40),
        Validators.max(200),
        this.rangoEdadMedida('estatura'),
      ],
    ],
    pesoKg: [
      0,
      [Validators.required, Validators.min(2), Validators.max(120), this.rangoEdadMedida('peso')],
    ],
  });

  /**
   * Configura la revalidación dependiente de fechas y carga el perfil y su historial de mediciones.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    this.form.controls.fechaNacimiento.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.medicionForm.controls.fechaMedicion.updateValueAndValidity();
        this.medicionForm.controls.estaturaCm.updateValueAndValidity();
        this.medicionForm.controls.pesoKg.updateValueAndValidity();
      });
    this.medicionForm.controls.fechaMedicion.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.medicionForm.controls.estaturaCm.updateValueAndValidity();
        this.medicionForm.controls.pesoKg.updateValueAndValidity();
      });
    const id = this.route.snapshot.paramMap.get('id');
    if (id === 'nuevo') {
      this.esNuevo.set(true);
      this.esEditando.set(true);
      this.cargando.set(false);
      return;
    }
    this.esEditando.set(this.route.snapshot.queryParamMap.get('editar') === 'true');
    const numericId = Number(id);
    if (!id || !Number.isSafeInteger(numericId) || numericId <= 0) {
      this.errorGeneral.set('El identificador del perfil no es válido.');
      this.cargando.set(false);
      return;
    }
    forkJoin({
      perfil: this.perfilesService.obtener(numericId),
      mediciones: this.perfilesService.listarMediciones(numericId),
    }).subscribe({
      next: ({ perfil, mediciones }) => {
        this.perfil.set(perfil);
        this.mediciones.set(this.ordenarMediciones(mediciones));
        this.medicionForm.controls.fechaMedicion.updateValueAndValidity();
        this.medicionForm.controls.estaturaCm.updateValueAndValidity();
        this.form.patchValue({
          nombre: perfil.nombre,
          fechaNacimiento: perfil.fechaNacimiento,
          contextura: perfil.contextura,
          holgura: perfil.holgura,
          sinAlergias: perfil.sinAlergias,
          alergias: perfil.alergias,
          otraAlergia: perfil.otraAlergia ?? '',
          coloresPreferidos: perfil.coloresPreferidos,
          estampadosPreferidos: perfil.estampadosPreferidos,
          otroColor: perfil.otroColor ?? '',
          otroEstampado: perfil.otroEstampado ?? '',
        });
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        this.errorGeneral.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }

  /**
   * Determina si se debe mostrar el error de un control del perfil.
   * @param control Control que se evalúa.
   * @returns `true` si es inválido y fue tocado o ya se intentó guardar.
   */
  protected esInvalido(control: AbstractControl): boolean {
    return control.invalid && (control.touched || this.enviado());
  }

  protected editarPerfil(): void {
    this.esEditando.set(true);
  }

  protected edad(perfil: PerfilInfantil): string {
    return formatearEdadPerfil(perfil);
  }

  /**
   * Calcula la fecha mínima permitida para una medición nueva.
   * @returns Fecha de nacimiento o la fecha de la última medición, la más reciente.
   */
  protected fechaMinimaMedicion(): string {
    const nacimiento = this.form.controls.fechaNacimiento.value;
    if (this.medicionEditandoId() !== null) return nacimiento;
    const ultimaFecha = this.perfil()?.ultimaMedicion?.fechaMedicion;
    return ultimaFecha && ultimaFecha > nacimiento ? ultimaFecha : nacimiento;
  }

  protected etiquetaContextura(contextura: Contextura): string {
    return { DELGADA: 'Delgada', MEDIA: 'Media', ROBUSTA: 'Robusta' }[contextura];
  }

  protected etiquetaHolgura(holgura: Holgura): string {
    return { AJUSTADA: 'Ajustada', REGULAR: 'Regular', HOLGADA: 'Holgada' }[holgura];
  }

  protected etiquetaAlergia(alergia: AlergiaTextil): string {
    return ETIQUETAS_ALERGIAS[alergia];
  }

  protected etiquetaColor(color: ColorPreferido): string {
    return ETIQUETAS_COLORES[color];
  }

  protected etiquetaEstampado(estampado: Estampado): string {
    return ETIQUETAS_ESTAMPADOS[estampado];
  }

  /**
   * Alterna un color preferido y limita el total junto con la opción libre a diez.
   * @param color Color que se agrega o quita.
   * @returns No devuelve un valor.
   */
  protected alternarColor(color: ColorPreferido): void {
    const seleccionados = this.form.controls.coloresPreferidos.value;
    const nuevos = seleccionados.includes(color)
      ? seleccionados.filter((actual) => actual !== color)
      : this.cantidadColores() < 10
        ? [...seleccionados, color]
        : seleccionados;
    this.form.controls.coloresPreferidos.setValue(nuevos);
    this.form.controls.coloresPreferidos.markAsTouched();
    this.form.controls.otroColor.updateValueAndValidity();
  }

  /**
   * Alterna un estampado preferido y limita el total junto con la opción libre a diez.
   * @param estampado Estampado que se agrega o quita.
   * @returns No devuelve un valor.
   */
  protected alternarEstampado(estampado: Estampado): void {
    const seleccionados = this.form.controls.estampadosPreferidos.value;
    const nuevos = seleccionados.includes(estampado)
      ? seleccionados.filter((actual) => actual !== estampado)
      : this.cantidadEstampados() < 10
        ? [...seleccionados, estampado]
        : seleccionados;
    this.form.controls.estampadosPreferidos.setValue(nuevos);
    this.form.controls.estampadosPreferidos.markAsTouched();
    this.form.controls.otroEstampado.updateValueAndValidity();
  }

  protected esOtraAlergia(): boolean {
    return (
      this.form.controls.alergias.value.includes('OTRA') && !this.form.controls.sinAlergias.value
    );
  }

  protected mostrarLimiteColores(): boolean {
    return this.cantidadColores() >= 10;
  }

  protected mostrarLimiteEstampados(): boolean {
    return this.cantidadEstampados() >= 10;
  }

  protected bloquearOtroColor(): boolean {
    return !this.form.controls.otroColor.value.trim() && this.mostrarLimiteColores();
  }

  protected bloquearOtroEstampado(): boolean {
    return !this.form.controls.otroEstampado.value.trim() && this.mostrarLimiteEstampados();
  }

  /**
   * Actualiza la lista de alergias y la descripción cuando se selecciona «Otra».
   * @param alergia Alergia cuya selección cambia.
   * @param event Evento que proporciona el estado del control.
   * @returns No devuelve un valor.
   */
  protected alternarAlergia(alergia: AlergiaTextil, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const alergias = this.form.controls.alergias.value.filter((actual) => actual !== alergia);
    if (checked) alergias.push(alergia);
    this.form.controls.alergias.setValue(alergias);
    this.form.controls.alergias.markAsTouched();
    if (!checked && alergia === 'OTRA') this.form.controls.otraAlergia.setValue('');
    this.form.controls.otraAlergia.updateValueAndValidity();
  }

  /**
   * Al activar «sin alergias», limpia las alergias declaradas y su descripción libre.
   * @param event Evento que proporciona el estado del control.
   * @returns No devuelve un valor.
   */
  protected cambiarSinAlergias(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.form.controls.alergias.setValue([]);
      this.form.controls.otraAlergia.setValue('');
    }
    this.form.controls.alergias.updateValueAndValidity();
    this.form.controls.otraAlergia.updateValueAndValidity();
  }

  /**
   * Valida y guarda el perfil; al crear uno nuevo también exige la medición inicial.
   * @returns No devuelve un valor.
   * @see US-06
   * @see UI-5
   */
  protected guardarPerfil(): void {
    if (this.guardando()) return;
    this.limpiarErroresServidor(this.form);
    this.enviado.set(true);
    this.errorGeneral.set(null);
    this.form.markAllAsTouched();
    this.form.updateValueAndValidity();
    if (this.esNuevo()) {
      this.medicionForm.markAllAsTouched();
      this.medicionForm.updateValueAndValidity();
    }
    if (this.form.invalid || (this.esNuevo() && this.medicionForm.invalid)) return;

    const valores = this.form.getRawValue();
    const request: PerfilRequest = {
      nombre: valores.nombre.trim(),
      fechaNacimiento: valores.fechaNacimiento,
      contextura: valores.contextura as Contextura,
      holgura: valores.holgura as Holgura,
      alergias: valores.alergias,
      otraAlergia: valores.alergias.includes('OTRA') ? valores.otraAlergia.trim() : null,
      sinAlergias: valores.sinAlergias,
      coloresPreferidos: valores.coloresPreferidos,
      estampadosPreferidos: valores.estampadosPreferidos,
      otroColor: this.valorOpcional(valores.otroColor),
      otroEstampado: this.valorOpcional(valores.otroEstampado),
    };
    this.guardando.set(true);
    const solicitud = this.esNuevo()
      ? this.perfilesService.crearConMedicionInicial({
          perfil: request,
          medicionInicial: this.requestMedicion(),
        })
      : this.perfilesService.actualizar(this.perfil()!.id, request);
    solicitud.pipe(take(1)).subscribe({
      next: (perfil) => {
        if (this.esNuevo()) {
          this.perfil.set(perfil);
          this.volverAlListado('creado');
          return;
        }
        this.volverAlListado('actualizado');
      },
      error: (err: unknown) => {
        this.guardando.set(false);
        this.errorGeneral.set(procesarErrorApi(err, this.form));
      },
    });
  }

  /**
   * Crea o corrige una medición y recarga el perfil junto con su historial.
   * @returns No devuelve un valor.
   */
  protected guardarMedicion(): void {
    if (this.guardandoMedicion()) return;
    this.limpiarErroresServidor(this.medicionForm);
    this.errorMedicion.set(null);
    this.medicionForm.markAllAsTouched();
    this.medicionForm.updateValueAndValidity();
    if (this.medicionForm.invalid) return;

    this.guardandoMedicion.set(true);
    const perfilId = this.perfil()!.id;
    const medicionId = this.medicionEditandoId();
    const solicitud = medicionId === null
      ? this.perfilesService.crearMedicion(perfilId, this.medicionForm.getRawValue())
      : this.perfilesService.actualizarMedicion(perfilId, medicionId, this.medicionForm.getRawValue());
    solicitud.subscribe({
        next: () => {
          forkJoin({
            perfil: this.perfilesService.obtener(perfilId),
            mediciones: this.perfilesService.listarMediciones(perfilId),
          }).subscribe({
            next: ({ perfil, mediciones }) => {
              this.perfil.set(perfil);
              this.mediciones.set(this.ordenarMediciones(mediciones));
              this.restablecerFormularioMedicion();
              this.agregandoMedicion.set(false);
              this.errorMedicion.set(null);
              this.guardandoMedicion.set(false);
            },
            error: (err: unknown) => {
              this.guardandoMedicion.set(false);
              this.errorMedicion.set(procesarErrorApi(err));
            },
          });
        },
        error: (err: unknown) => {
          this.guardandoMedicion.set(false);
          this.errorMedicion.set(procesarErrorApi(err, this.medicionForm));
        },
      });
  }

  /**
   * Prepara el formulario para una medición nueva con la fecha actual.
   * @returns No devuelve un valor.
   */
  protected iniciarNuevaMedicion(): void {
      this.errorMedicion.set(null);
      this.medicionForm.reset({ fechaMedicion: HOY, estaturaCm: 0, pesoKg: 0 });
      this.medicionEditandoId.set(null);
      this.agregandoMedicion.set(true);
  }

  /**
   * Restablece el formulario y cancela la operación de medición en curso.
   * @returns No devuelve un valor.
   */
  protected cancelarNuevaMedicion(): void {
      this.restablecerFormularioMedicion();
      this.errorMedicion.set(null);
      this.agregandoMedicion.set(false);
  }

  /**
   * Carga una medición existente para corregirla.
   * @param medicion Medición que se edita.
   * @returns No devuelve un valor.
   */
  protected editarMedicion(medicion: Medicion): void {
    this.medicionEditandoId.set(medicion.id);
    this.medicionForm.reset({
      fechaMedicion: medicion.fechaMedicion,
      estaturaCm: medicion.estaturaCm,
      pesoKg: medicion.pesoKg,
    });
    this.errorMedicion.set(null);
    this.agregandoMedicion.set(true);
  }

  private restablecerFormularioMedicion(): void {
    this.medicionForm.reset({ fechaMedicion: HOY, estaturaCm: 0, pesoKg: 0 });
    this.medicionEditandoId.set(null);
  }

  /**
   * Elimina el perfil actual y vuelve a la lista al completarse la operación.
   * @returns No devuelve un valor.
   */
  protected eliminarPerfil(): void {
    const perfil = this.perfil();
    if (!perfil) return;
    this.eliminando.set(true);
    this.errorGeneral.set(null);
    this.perfilesService.eliminar(perfil.id).subscribe({
      next: () => void this.router.navigate(['/cliente/hijos']),
      error: (err: unknown) => {
        this.eliminando.set(false);
        this.confirmarEliminacion.set(false);
        this.errorGeneral.set(procesarErrorApi(err));
      },
    });
  }

  /**
   * Traduce el primer error de validación del control a un mensaje para la interfaz.
   * @param control Control cuyos errores se presentan.
   * @returns Mensaje de validación o `null` si no hay errores.
   */
  protected mensaje(control: AbstractControl): string | null {
    const errors = control.errors;
    if (!errors) return null;
    if (errors['servidor']) return String(errors['servidor']);
    if (errors['required']) {
      if (control === this.form.controls.nombre) return 'El nombre es obligatorio.';
      if (control === this.form.controls.fechaNacimiento)
        return 'La fecha de nacimiento es obligatoria.';
      if (control === this.form.controls.contextura) return 'La contextura es obligatoria.';
      if (control === this.form.controls.holgura) return 'La holgura es obligatoria.';
      if (control === this.medicionForm.controls.fechaMedicion)
        return 'La fecha de medición es obligatoria.';
      if (control === this.medicionForm.controls.estaturaCm) return 'La estatura es obligatoria.';
      if (control === this.medicionForm.controls.pesoKg) return 'El peso es obligatorio.';
      return 'Este campo es obligatorio.';
    }
    if (errors['maxlength']) {
      return control === this.form.controls.nombre
        ? 'El nombre no puede superar 60 caracteres.'
        : 'La descripción no puede superar 60 caracteres.';
    }
    if (errors['nombreVacio']) return 'El nombre es obligatorio.';
    if (errors['fechaFutura']) {
      return control === this.medicionForm.controls.fechaMedicion
        ? 'La fecha de medición no puede ser futura.'
        : 'La fecha de nacimiento no puede ser futura.';
    }
    if (errors['fechaNacimientoHoy']) return 'La fecha de nacimiento debe ser anterior a hoy.';
    if (errors['mayorEdad']) return 'El perfil debe corresponder a una persona menor de 18 años.';
    if (errors['fechaAntesNacimiento'])
      return 'La medición no puede ser anterior a la fecha de nacimiento.';
    if (errors['fechaAnteriorUltimaMedicion'])
      return 'La nueva medición no puede ser anterior a la última registrada. Usa “Corregir” para ajustar una medición existente.';
    if (errors['estaturaMenorUltimaMedicion'])
      return 'La estatura no puede ser menor que la última registrada. Usa “Corregir” para ajustar una medición existente.';
    if (errors['min']) {
      return control === this.medicionForm.controls.estaturaCm
        ? 'La estatura debe ser mínimo 40 cm.'
        : 'El peso debe ser mínimo 2 kg.';
    }
    if (errors['max']) {
      return control === this.medicionForm.controls.estaturaCm
        ? 'La estatura debe ser máximo 200 cm.'
        : 'El peso debe ser máximo 120 kg.';
    }
    if (errors['rangoEdad']) {
      return control === this.medicionForm.controls.estaturaCm
        ? 'La estatura no es coherente con la edad del perfil.'
        : 'El peso no es coherente con la edad del perfil.';
    }
    if (errors['rangoAlergias']) return 'Declare las alergias o indique que no tiene alergias.';
    if (errors['maxAlergias']) return 'Solo puede registrar hasta 10 alergias.';
    if (errors['otraAlergia']) return 'Describe la otra alergia (máximo 60 caracteres).';
    if (errors['otroPreferenciaFormato']) return 'Usa de 2 a 30 letras y espacios.';
    return 'Revisa este campo.';
  }

  /**
   * Convierte un texto opcional vacío en `null` y recorta los espacios externos.
   * @param valor Texto introducido en el formulario.
   * @returns Texto recortado o `null` si queda vacío.
   */
  private valorOpcional(valor: string): string | null {
    const limpio = valor.trim();
    return limpio ? limpio : null;
  }

  /**
   * Cuenta colores predefinidos y evita duplicar una preferencia libre coincidente.
   * @returns Cantidad efectiva de colores, incluida la preferencia libre si es distinta.
   */
  private cantidadColores(): number {
    const colores = new Set(this.form.controls.coloresPreferidos.value);
    const otro = this.form.controls.otroColor.value.trim();
    if (!otro) return colores.size;
    const clave = this.clavePreferencia(otro);
    const coincidente = this.coloresDisponibles.find(
      (opcion) =>
        this.clavePreferencia(opcion.valor) === clave ||
        this.clavePreferencia(opcion.etiqueta) === clave,
    );
    if (coincidente) colores.add(coincidente.valor);
    else return colores.size + 1;
    return colores.size;
  }

  /**
   * Cuenta estampados predefinidos y evita duplicar una preferencia libre coincidente.
   * @returns Cantidad efectiva de estampados, incluida la preferencia libre si es distinta.
   */
  private cantidadEstampados(): number {
    const estampados = new Set(this.form.controls.estampadosPreferidos.value);
    const otro = this.form.controls.otroEstampado.value.trim();
    if (!otro) return estampados.size;
    const clave = this.clavePreferencia(otro);
    const coincidente = this.estampadosDisponibles.find(
      (opcion) =>
        this.clavePreferencia(opcion.valor) === clave ||
        this.clavePreferencia(opcion.etiqueta) === clave,
    );
    if (coincidente) estampados.add(coincidente.valor);
    else return estampados.size + 1;
    return estampados.size;
  }

  /**
   * Normaliza una preferencia para compararla con las opciones predefinidas.
   * @param valor Texto de preferencia.
   * @returns Clave en mayúsculas sin diacríticos, espacios ni guiones bajos.
   */
  private clavePreferencia(valor: string): string {
    return valor.normalize('NFD').replace(/\p{M}/gu, '').replace(/[_ ]/g, '').toUpperCase();
  }

  /**
   * Ordena las mediciones de la más reciente a la más antigua.
   * @param mediciones Mediciones que se ordenan.
   * @returns Nueva lista ordenada por fecha descendente.
   */
  private ordenarMediciones(mediciones: Medicion[]): Medicion[] {
    return [...mediciones].sort((a, b) => b.fechaMedicion.localeCompare(a.fechaMedicion));
  }

  /**
   * Obtiene los valores actuales para enviarlos como solicitud de medición.
   * @returns Fecha, estatura y peso del formulario.
   */
  private requestMedicion(): MedicionRequest {
    return this.medicionForm.getRawValue();
  }

  /**
   * Vuelve a la lista y comunica si el perfil se creó o actualizó.
   * @param resultado Resultado de la operación de perfil.
   * @returns No devuelve un valor.
   */
  private volverAlListado(resultado: 'creado' | 'actualizado'): void {
    void this.router.navigate(['/cliente/hijos'], { queryParams: { resultado } });
  }

  /**
   * Retira errores de servidor de los controles sin borrar otros errores de validación.
   * @param form Formulario cuyos controles se limpian.
   * @returns No devuelve un valor.
   */
  private limpiarErroresServidor(form: FormGroup): void {
    for (const control of Object.values(form.controls)) {
      if (!control.errors?.['servidor']) continue;
      const errores = { ...control.errors };
      delete errores['servidor'];
      control.setErrors(Object.keys(errores).length > 0 ? errores : null);
      control.updateValueAndValidity();
    }
  }

  /**
   * Crea un validador que rechaza nombres vacíos o compuestos solo por espacios.
   * @returns Validador de nombre con el error `nombreVacio`.
   */
  private nombreNoVacio(): ValidatorFn {
    return (control) => (String(control.value ?? '').trim() ? null : { nombreVacio: true });
  }

  /**
   * Crea un validador que rechaza fechas futuras, el día actual y edades de 18 años o más.
   * @returns Validador de fecha de nacimiento.
   * @see US-06
   */
  private fechaNacimientoValida(): ValidatorFn {
    return (control) => {
      const fecha = String(control.value ?? '');
      if (!fecha) return null;
      if (fecha > HOY) return { fechaFutura: true };
      if (fecha === HOY) return { fechaNacimientoHoy: true };
      return edadEnAnios(fecha, HOY) >= 18 ? { mayorEdad: true } : null;
    };
  }

  /**
   * Exige una lista coherente con «sin alergias» y limita la selección a diez.
   * @returns Validador con errores `maxAlergias` o `rangoAlergias`.
   */
  private validarAlergias(): ValidatorFn {
    return (control) => {
      const padre = control.parent;
      if (!padre) return null;
      const alergias = control.value as AlergiaTextil[];
      const sinAlergias = padre.get('sinAlergias')?.value === true;
      if (alergias.length > 10) return { maxAlergias: true };
      return (sinAlergias && alergias.length > 0) || (!sinAlergias && alergias.length === 0)
        ? { rangoAlergias: true }
        : null;
    };
  }

  /**
   * Exige una descripción de hasta 60 caracteres cuando se selecciona «Otra».
   * @returns Validador del campo de alergia libre.
   */
  private validarOtraAlergia(): ValidatorFn {
    return (control) => {
      const alergias = control.parent?.get('alergias')?.value as AlergiaTextil[] | undefined;
      if (!alergias?.includes('OTRA')) return null;
      const valor = String(control.value ?? '').trim();
      if (!valor) return { otraAlergia: true };
      return valor.length > 60 ? { maxlength: { requiredLength: 60 } } : null;
    };
  }

  /**
   * Valida una preferencia libre de 2 a 30 letras y espacios.
   * @returns Validador con error `otroPreferenciaFormato` si no cumple la regla.
   */
  private validarPreferenciaLibre(): ValidatorFn {
    return (control) => {
      const valor = String(control.value ?? '').trim();
      if (!valor) return null;
      return valor.length < 2 || valor.length > 30 || !/^[\p{L} ]+$/u.test(valor)
        ? { otroPreferenciaFormato: true }
        : null;
    };
  }

  /**
   * Rechaza fechas futuras, anteriores al nacimiento o anteriores a la última medición nueva.
   * @returns Validador de fecha de medición.
   */
  private fechaMedicionValida(): ValidatorFn {
    return (control) => {
      const fecha = String(control.value ?? '');
      if (!fecha) return null;
      if (fecha > HOY) return { fechaFutura: true };
      const nacimiento = this.form.controls.fechaNacimiento.value;
      if (nacimiento && fecha < nacimiento) return { fechaAntesNacimiento: true };
      if (this.medicionEditandoId() === null) {
        const ultimaFecha = this.perfil()?.ultimaMedicion?.fechaMedicion;
        if (ultimaFecha && fecha < ultimaFecha) return { fechaAnteriorUltimaMedicion: true };
      }
      return null;
    };
  }

  /**
   * Crea un validador con rangos de estatura o peso dependientes de la edad en la fecha medida.
   * Al registrar una medición nueva, impide que la estatura sea menor que la última registrada.
   * @param tipo Medida que se valida: estatura o peso.
   * @returns Validador que produce `rangoEdad` o `estaturaMenorUltimaMedicion`.
   * @see US-06
   * @see RNF-16
   */
  private rangoEdadMedida(tipo: 'estatura' | 'peso'): ValidatorFn {
    return (control) => {
      const nacimiento = this.form.controls.fechaNacimiento.value;
      const fechaMedicion = control.parent?.get('fechaMedicion')?.value;
      if (!nacimiento || !fechaMedicion || !control.value) return null;
      const edad = edadEnAnios(String(nacimiento), String(fechaMedicion));
      const rango =
        edad < 2
          ? tipo === 'estatura'
            ? [40, 100]
            : [2, 20]
          : edad <= 5
            ? tipo === 'estatura'
              ? [60, 130]
              : [5, 40]
            : edad <= 9
              ? tipo === 'estatura'
                ? [80, 160]
                : [10, 80]
              : edad <= 13
                ? tipo === 'estatura'
                  ? [100, 190]
                  : [15, 120]
                : tipo === 'estatura'
                  ? [120, 200]
                  : [25, 120];
      const valor = Number(control.value);
      if (tipo === 'estatura' && this.medicionEditandoId() === null) {
        const ultima = this.perfil()?.ultimaMedicion;
        if (
          ultima &&
          String(fechaMedicion) >= ultima.fechaMedicion &&
          valor < ultima.estaturaCm
        ) {
          return { estaturaMenorUltimaMedicion: true };
        }
      }
      return valor < rango[0] || valor > rango[1] ? { rangoEdad: true } : null;
    };
  }
}
