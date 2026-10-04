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
import {
  ETIQUETAS_ALERGIAS,
  ETIQUETAS_COLORES,
  ETIQUETAS_ESTAMPADOS,
  OPCIONES_ALERGIAS,
  OPCIONES_COLORES,
  OPCIONES_ESTAMPADOS,
} from '../models/perfil-opciones';
import { tallaProvisional } from '../models/talla-provisional';
import { PerfilesService } from '../services/perfiles.service';

const FECHA_LOCAL = new Date();
const HOY = `${FECHA_LOCAL.getFullYear()}-${String(FECHA_LOCAL.getMonth() + 1).padStart(2, '0')}-${String(FECHA_LOCAL.getDate()).padStart(2, '0')}`;

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
  protected readonly eliminando = signal(false);
  protected readonly confirmarEliminacion = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  protected readonly errorMedicion = signal<string | null>(null);

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

  protected esInvalido(control: AbstractControl): boolean {
    return control.invalid && (control.touched || this.enviado());
  }

  protected editarPerfil(): void {
    this.esEditando.set(true);
  }

  protected edad(perfil: PerfilInfantil): string {
    if (perfil.edadAnios === 0) {
      return `${perfil.mesesRestantes} ${perfil.mesesRestantes === 1 ? 'mes' : 'meses'}`;
    }
    const anios = `${perfil.edadAnios} ${perfil.edadAnios === 1 ? 'año' : 'años'}`;
    return perfil.mesesRestantes > 0 ? `${anios} · ${perfil.mesesRestantes} meses` : anios;
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

  protected talla(perfil: PerfilInfantil): number {
    return tallaProvisional(perfil.edadAnios);
  }

  protected alternarAlergia(alergia: AlergiaTextil, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const alergias = this.form.controls.alergias.value.filter((actual) => actual !== alergia);
    if (checked) alergias.push(alergia);
    this.form.controls.alergias.setValue(alergias);
    this.form.controls.alergias.markAsTouched();
    if (!checked && alergia === 'OTRA') this.form.controls.otraAlergia.setValue('');
    this.form.controls.otraAlergia.updateValueAndValidity();
  }

  protected cambiarSinAlergias(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.form.controls.alergias.setValue([]);
      this.form.controls.otraAlergia.setValue('');
    }
    this.form.controls.alergias.updateValueAndValidity();
    this.form.controls.otraAlergia.updateValueAndValidity();
  }

  protected guardarPerfil(): void {
    if (this.guardando()) return;
    if (this.esNuevo() && this.perfil()) {
      this.reintentarMedicionInicial();
      return;
    }
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
      ? this.perfilesService.crear(request)
      : this.perfilesService.actualizar(this.perfil()!.id, request);
    solicitud.pipe(take(1)).subscribe({
      next: (perfil) => {
        if (this.esNuevo()) {
          this.perfil.set(perfil);
          this.guardandoMedicion.set(true);
          this.perfilesService
            .crearMedicion(perfil.id, this.requestMedicion())
            .pipe(take(1))
            .subscribe({
              next: () => {
                this.guardandoMedicion.set(false);
                this.volverAlListado('creado');
              },
              error: (err: unknown) => {
                this.guardandoMedicion.set(false);
                this.guardando.set(false);
                this.errorMedicion.set(procesarErrorApi(err, this.medicionForm));
              },
            });
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

  protected reintentarMedicionInicial(): void {
    const perfil = this.perfil();
    if (!this.esNuevo() || !perfil || this.guardandoMedicion()) return;
    this.limpiarErroresServidor(this.medicionForm);
    this.errorMedicion.set(null);
    this.medicionForm.markAllAsTouched();
    this.medicionForm.updateValueAndValidity();
    if (this.medicionForm.invalid) return;

    this.guardandoMedicion.set(true);
    this.perfilesService
      .crearMedicion(perfil.id, this.requestMedicion())
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.guardandoMedicion.set(false);
          this.volverAlListado('creado');
        },
        error: (err: unknown) => {
          this.guardandoMedicion.set(false);
          this.errorMedicion.set(procesarErrorApi(err, this.medicionForm));
        },
      });
  }

  protected guardarMedicion(): void {
    if (this.guardandoMedicion()) return;
    this.limpiarErroresServidor(this.medicionForm);
    this.errorMedicion.set(null);
    this.medicionForm.markAllAsTouched();
    this.medicionForm.updateValueAndValidity();
    if (this.medicionForm.invalid) return;

    this.guardandoMedicion.set(true);
    this.perfilesService
      .crearMedicion(this.perfil()!.id, this.medicionForm.getRawValue())
      .subscribe({
        next: () => {
          forkJoin({
            perfil: this.perfilesService.obtener(this.perfil()!.id),
            mediciones: this.perfilesService.listarMediciones(this.perfil()!.id),
          }).subscribe({
            next: ({ perfil, mediciones }) => {
              this.perfil.set(perfil);
              this.mediciones.set(this.ordenarMediciones(mediciones));
              this.medicionForm.reset({ fechaMedicion: HOY, estaturaCm: 0, pesoKg: 0 });
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
    if (errors['mayorEdad']) return 'El perfil debe corresponder a una persona menor de 18 años.';
    if (errors['fechaAntesNacimiento'])
      return 'La medición no puede ser anterior a la fecha de nacimiento.';
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

  private valorOpcional(valor: string): string | null {
    const limpio = valor.trim();
    return limpio ? limpio : null;
  }

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

  private clavePreferencia(valor: string): string {
    return valor.normalize('NFD').replace(/\p{M}/gu, '').replace(/[_ ]/g, '').toUpperCase();
  }

  private ordenarMediciones(mediciones: Medicion[]): Medicion[] {
    return [...mediciones].sort((a, b) => b.fechaMedicion.localeCompare(a.fechaMedicion));
  }

  private requestMedicion(): MedicionRequest {
    return this.medicionForm.getRawValue();
  }

  private volverAlListado(resultado: 'creado' | 'actualizado'): void {
    void this.router.navigate(['/cliente/hijos'], { queryParams: { resultado } });
  }

  private limpiarErroresServidor(form: FormGroup): void {
    for (const control of Object.values(form.controls)) {
      if (!control.errors?.['servidor']) continue;
      const errores = { ...control.errors };
      delete errores['servidor'];
      control.setErrors(Object.keys(errores).length > 0 ? errores : null);
      control.updateValueAndValidity();
    }
  }

  private nombreNoVacio(): ValidatorFn {
    return (control) => (String(control.value ?? '').trim() ? null : { nombreVacio: true });
  }

  private fechaNacimientoValida(): ValidatorFn {
    return (control) => {
      const fecha = String(control.value ?? '');
      if (!fecha) return null;
      if (fecha > HOY) return { fechaFutura: true };
      return edadEnAnios(fecha, HOY) >= 18 ? { mayorEdad: true } : null;
    };
  }

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

  private validarOtraAlergia(): ValidatorFn {
    return (control) => {
      const alergias = control.parent?.get('alergias')?.value as AlergiaTextil[] | undefined;
      if (!alergias?.includes('OTRA')) return null;
      const valor = String(control.value ?? '').trim();
      if (!valor) return { otraAlergia: true };
      return valor.length > 60 ? { maxlength: { requiredLength: 60 } } : null;
    };
  }

  private validarPreferenciaLibre(): ValidatorFn {
    return (control) => {
      const valor = String(control.value ?? '').trim();
      if (!valor) return null;
      return valor.length < 2 || valor.length > 30 || !/^[\p{L} ]+$/u.test(valor)
        ? { otroPreferenciaFormato: true }
        : null;
    };
  }

  private fechaMedicionValida(): ValidatorFn {
    return (control) => {
      const fecha = String(control.value ?? '');
      if (!fecha) return null;
      if (fecha > HOY) return { fechaFutura: true };
      const nacimiento = this.form.controls.fechaNacimiento.value;
      return nacimiento && fecha < nacimiento ? { fechaAntesNacimiento: true } : null;
    };
  }

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
      return valor < rango[0] || valor > rango[1] ? { rangoEdad: true } : null;
    };
  }
}
