import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';

import { Usuario } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { DireccionEnvio } from '../../catalogoCompras/models/checkout.models';
import { DEPARTAMENTOS_COLOMBIA, municipiosDe } from '../../catalogoCompras/data/municipios-colombia';
import { CheckoutService } from '../../catalogoCompras/services/checkout.service';

@Component({
  selector: 'app-cuenta',
  imports: [Navbar, ReactiveFormsModule],
  templateUrl: './cuenta.html',
})
export class Cuenta implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  private readonly checkoutService = inject(CheckoutService);

  protected readonly usuario = signal<Usuario | null>(null);
  protected readonly direccionGuardada = signal<DireccionEnvio | null>(null);
  protected readonly cargando = signal(true);
  protected readonly cargandoDireccion = signal(true);
  protected readonly guardando = signal(false);
  protected readonly guardandoDireccion = signal(false);
  protected readonly guardandoPassword = signal(false);
  protected readonly desactivando = signal(false);
  protected readonly confirmarDesactivacion = signal(false);
  protected readonly editandoDatos = signal(false);
  protected readonly editandoDireccion = signal(false);
  protected readonly cambiandoPassword = signal(false);
  protected readonly enviado = signal(false);
  protected readonly direccionEnviada = signal(false);
  protected readonly passwordEnviado = signal(false);
  protected readonly departamentos = DEPARTAMENTOS_COLOMBIA;
  protected readonly departamentoSeleccionado = signal('');
  protected readonly mensaje = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60), Validators.pattern(/^[\p{L} ]+$/u)]],
    apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60), Validators.pattern(/^[\p{L} ]+$/u)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    telefono: ['', [Validators.pattern(/^$|^3\d{9}$/)]],
    nombreTienda: ['', [Validators.maxLength(100)]],
  });

  protected readonly direccionForm = this.fb.nonNullable.group({
    destinatario: ['', [Validators.required, Validators.maxLength(120)]],
    direccion: ['', [Validators.required, Validators.maxLength(200)]],
    complemento: ['', [Validators.maxLength(120)]],
    codigoPostal: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    departamento: ['', [Validators.required]],
    municipio: ['', [Validators.required]],
    telefono: ['', [Validators.required, Validators.pattern(/^3\d{9}$/)]],
  });

  protected readonly municipios = () => municipiosDe(this.departamentoSeleccionado());

  protected readonly passwordForm = this.fb.nonNullable.group({
    passwordActual: [''],
    nuevaPassword: ['', [Validators.minLength(8), Validators.maxLength(64), Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)]],
    confirmarNuevaPassword: [''],
  });

  ngOnInit(): void {
    this.auth.perfil().pipe(take(1)).subscribe({
      next: (usuario) => {
        this.usuario.set(usuario);
        if (usuario.rol === 'VENDEDOR') {
          this.form.controls.nombreTienda.setValidators([Validators.required, Validators.maxLength(100)]);
        }
        this.form.patchValue({
          nombre: usuario.nombre,
          apellido: usuario.apellido,
          email: usuario.email,
          telefono: usuario.telefono ?? '',
          nombreTienda: usuario.nombreTienda ?? '',
        });
        this.form.controls.nombreTienda.updateValueAndValidity();
        this.cargando.set(false);
        if (usuario.rol === 'CLIENTE') {
          this.cargarDireccion();
        } else {
          this.cargandoDireccion.set(false);
        }
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cargando.set(false);
      },
    });
  }

  protected editar(): void {
    this.mensaje.set(null);
    this.editandoDatos.set(true);
  }

  protected cancelarEdicion(): void {
    const usuario = this.usuario();
    if (usuario) this.cargarDatosFormulario(usuario);
    this.editandoDatos.set(false);
    this.enviado.set(false);
    this.error.set(null);
  }

  protected editarDireccion(): void {
    const direccion = this.direccionGuardada();
    if (direccion) this.cargarDireccionEnFormulario(direccion);
    else this.limpiarDireccionFormulario();
    this.error.set(null);
    this.mensaje.set(null);
    this.editandoDireccion.set(true);
  }

  protected cancelarEdicionDireccion(): void {
    const direccion = this.direccionGuardada();
    if (direccion) this.cargarDireccionEnFormulario(direccion);
    else this.limpiarDireccionFormulario();
    this.direccionForm.markAsPristine();
    this.editandoDireccion.set(false);
    this.direccionEnviada.set(false);
    this.error.set(null);
  }

  protected cambioDepartamento(event: Event): void {
    const departamento = (event.currentTarget as HTMLSelectElement).value;
    this.direccionForm.controls.departamento.setValue(departamento);
    this.departamentoSeleccionado.set(departamento);
    this.direccionForm.controls.municipio.setValue('');
    this.direccionForm.controls.municipio.markAsUntouched();
  }

  protected guardarDireccion(): void {
    if (this.guardandoDireccion()) return;
    this.error.set(null);
    this.mensaje.set(null);
    this.direccionEnviada.set(true);
    if (this.direccionForm.invalid) {
      this.direccionForm.markAllAsTouched();
      return;
    }

    const values = this.direccionForm.getRawValue();
    const direccion: DireccionEnvio = {
      ...values,
      destinatario: values.destinatario.trim(),
      direccion: values.direccion.trim(),
      complemento: values.complemento.trim() || null,
      codigoPostal: values.codigoPostal.trim(),
      departamento: values.departamento,
      municipio: values.municipio,
      telefono: values.telefono.trim(),
    };
    this.guardandoDireccion.set(true);
    this.checkoutService.guardarDireccion(direccion).subscribe({
      next: (respuesta) => {
        this.guardandoDireccion.set(false);
        if (!respuesta.guardada || !respuesta.direccion) {
          this.error.set('No fue posible guardar la dirección de envío.');
          return;
        }
        this.direccionGuardada.set(respuesta.direccion);
        this.cargarDireccionEnFormulario(respuesta.direccion);
        this.direccionForm.markAsPristine();
        this.direccionEnviada.set(false);
        this.editandoDireccion.set(false);
        this.mensaje.set('La dirección de envío se actualizó.');
      },
      error: (error: unknown) => {
        this.guardandoDireccion.set(false);
        this.error.set(procesarErrorApi(error, this.direccionForm));
      },
    });
  }

  protected guardar(): void {
    if (this.guardando()) return;
    this.error.set(null);
    this.mensaje.set(null);
    this.enviado.set(true);
    const valores = this.form.getRawValue();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.auth.actualizarCuenta({
      nombre: valores.nombre.trim(),
      apellido: valores.apellido.trim(),
      email: valores.email.trim(),
      telefono: valores.telefono.trim() || null,
      nombreTienda: this.usuario()?.rol === 'VENDEDOR' ? valores.nombreTienda.trim() : null,
      passwordActual: null,
      nuevaPassword: null,
      confirmarNuevaPassword: null,
    }).pipe(take(1)).subscribe({
      next: ({ usuario }) => {
        this.usuario.set(usuario);
        this.cargarDatosFormulario(usuario);
        this.form.markAsPristine();
        this.guardando.set(false);
        this.editandoDatos.set(false);
        this.mensaje.set('Los datos de tu cuenta se actualizaron.');
      },
      error: (error: unknown) => {
        this.guardando.set(false);
        this.error.set(procesarErrorApi(error, this.form));
      },
    });
  }

  protected guardarPassword(): void {
    if (this.guardandoPassword()) return;
    this.error.set(null);
    this.mensaje.set(null);
    this.passwordEnviado.set(true);
    const valores = this.passwordForm.getRawValue();
    if (
      this.passwordForm.controls.nuevaPassword.invalid ||
      !valores.passwordActual ||
      !valores.nuevaPassword ||
      valores.nuevaPassword !== valores.confirmarNuevaPassword
    ) {
      this.passwordForm.markAllAsTouched();
      if (!valores.passwordActual) {
        this.passwordForm.controls.passwordActual.setErrors({ required: true });
      }
      if (valores.nuevaPassword !== valores.confirmarNuevaPassword) {
        this.passwordForm.controls.confirmarNuevaPassword.setErrors({ noCoincide: true });
      }
      return;
    }
    const usuario = this.usuario();
    if (!usuario) return;

    this.guardandoPassword.set(true);
    this.auth.actualizarCuenta({
      nombre: usuario.nombre,
      apellido: usuario.apellido,
      email: usuario.email,
      telefono: usuario.telefono,
      nombreTienda: usuario.nombreTienda,
      passwordActual: valores.passwordActual,
      nuevaPassword: valores.nuevaPassword,
      confirmarNuevaPassword: valores.confirmarNuevaPassword,
    }).pipe(take(1)).subscribe({
      next: ({ usuario: actualizado }) => {
        this.usuario.set(actualizado);
        this.passwordForm.reset();
        this.passwordEnviado.set(false);
        this.cambiandoPassword.set(false);
        this.guardandoPassword.set(false);
        this.mensaje.set('La contraseña se actualizó correctamente.');
      },
      error: (error: unknown) => {
        this.guardandoPassword.set(false);
        this.error.set(procesarErrorApi(error, this.passwordForm));
      },
    });
  }

  protected desactivar(): void {
    if (this.desactivando()) return;
    this.error.set(null);
    this.desactivando.set(true);
    this.auth.desactivarCuenta().pipe(take(1)).subscribe({
      next: () => this.auth.logout(),
      error: (error: unknown) => {
        this.desactivando.set(false);
        this.confirmarDesactivacion.set(false);
        this.error.set(procesarErrorApi(error));
      },
    });
  }

  protected invalido(control: AbstractControl, esPassword = false): boolean {
    return control.invalid && (control.touched || (esPassword ? this.passwordEnviado() : this.enviado()));
  }

  protected invalidoDireccion(control: AbstractControl): boolean {
    return control.invalid && (control.touched || this.direccionEnviada());
  }

  private cargarDireccion(): void {
    this.checkoutService.obtenerDireccion().subscribe({
      next: (respuesta) => {
        const direccion = respuesta.guardada ? respuesta.direccion : null;
        this.direccionGuardada.set(direccion);
        if (direccion) this.cargarDireccionEnFormulario(direccion);
        this.cargandoDireccion.set(false);
      },
      error: (error: unknown) => {
        this.error.set(procesarErrorApi(error));
        this.cargandoDireccion.set(false);
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
    this.direccionForm.patchValue({
      destinatario: direccion.destinatario,
      direccion: direccion.direccion,
      complemento: direccion.complemento ?? '',
      codigoPostal: direccion.codigoPostal ?? '',
      departamento,
      municipio,
      telefono: direccion.telefono,
    });
  }

  private limpiarDireccionFormulario(): void {
    this.direccionForm.reset();
    this.departamentoSeleccionado.set('');
  }

  private normalizarUbicacion(valor: string): string {
    return valor.normalize('NFD').replace(/\p{M}/gu, '').trim().toLocaleUpperCase('es-CO');
  }

  private cargarDatosFormulario(usuario: Usuario): void {
    this.form.patchValue({
      nombre: usuario.nombre,
      apellido: usuario.apellido,
      email: usuario.email,
      telefono: usuario.telefono ?? '',
      nombreTienda: usuario.nombreTienda ?? '',
    });
  }
}
