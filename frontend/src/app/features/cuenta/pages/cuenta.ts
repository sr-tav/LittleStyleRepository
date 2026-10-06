import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';

import { Usuario } from '../../../core/models/auth.models';
import { AuthService } from '../../../core/services/auth.service';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';

@Component({
  selector: 'app-cuenta',
  imports: [Navbar, ReactiveFormsModule],
  templateUrl: './cuenta.html',
})
export class Cuenta implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly usuario = signal<Usuario | null>(null);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly guardandoPassword = signal(false);
  protected readonly desactivando = signal(false);
  protected readonly confirmarDesactivacion = signal(false);
  protected readonly editandoDatos = signal(false);
  protected readonly cambiandoPassword = signal(false);
  protected readonly enviado = signal(false);
  protected readonly passwordEnviado = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60), Validators.pattern(/^[\p{L} ]+$/u)]],
    apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60), Validators.pattern(/^[\p{L} ]+$/u)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    telefono: ['', [Validators.pattern(/^$|^3\d{9}$/)]],
    nombreTienda: ['', [Validators.maxLength(100)]],
  });

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
