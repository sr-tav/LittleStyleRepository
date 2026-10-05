import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { RolRegistro } from '../../../../core/models/auth.models';
import { AuthService } from '../../../../core/services/auth.service';
import { AuthBrand } from '../../components/auth-brand/auth-brand';
import { TerminosModal } from '../../components/terminos-modal/terminos-modal';
import {
  PATRON_CELULAR,
  PATRON_EMAIL,
  PATRON_NOMBRE,
  fortalezaPassword,
  passwordSegura,
  passwordsIguales,
  procesarErrorApi,
} from '../../auth.validators';

type Campo =
  | 'nombre'
  | 'apellido'
  | 'email'
  | 'telefono'
  | 'nombreTienda'
  | 'password'
  | 'confirmarPassword'
  | 'aceptaTerminos';

const NIVELES = ['Muy débil', 'Débil', 'Aceptable', 'Buena', 'Excelente'];

/** UI-2: Registro de usuarios (clientes y vendedores). */
@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink, AuthBrand, TerminosModal],
  templateUrl: './registro.html',
})
export class Registro {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  protected readonly mostrarPassword = signal(false);
  protected readonly mostrarTerminos = signal(false);

  protected readonly form = this.fb.nonNullable.group(
    {
      rol: this.fb.nonNullable.control<RolRegistro>('CLIENTE'),
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60), Validators.pattern(PATRON_NOMBRE)]],
      apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(60), Validators.pattern(PATRON_NOMBRE)]],
      email: ['', [Validators.required, Validators.maxLength(120), Validators.pattern(PATRON_EMAIL)]],
      telefono: ['', [Validators.required, Validators.pattern(PATRON_CELULAR)]],
      nombreTienda: ['', [Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(64), passwordSegura]],
      confirmarPassword: ['', [Validators.required]],
      aceptaTerminos: [false, [Validators.requiredTrue]],
    },
    { validators: passwordsIguales() },
  );

  protected readonly rol = toSignal(this.form.controls.rol.valueChanges, { initialValue: this.form.controls.rol.value });
  protected readonly esVendedor = computed(() => this.rol() === 'VENDEDOR');

  private readonly passwordValor = toSignal(this.form.controls.password.valueChanges, { initialValue: '' });
  protected readonly fortaleza = computed(() => fortalezaPassword(this.passwordValor()));
  protected readonly nivelTexto = computed(() => NIVELES[this.fortaleza()]);

  constructor() {
    // El nombre de la tienda solo es obligatorio para vendedores
    this.form.controls.rol.valueChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe((rol) => {
      const tienda = this.form.controls.nombreTienda;
      tienda.setValidators(
        rol === 'VENDEDOR' ? [Validators.required, Validators.maxLength(100)] : [Validators.maxLength(100)],
      );
      if (rol !== 'VENDEDOR') {
        tienda.reset('');
      }
      tienda.updateValueAndValidity();
    });
  }

  protected seleccionarRol(rol: RolRegistro): void {
    this.form.controls.rol.setValue(rol);
  }

  protected abrirTerminos(evento: Event): void {
    // El enlace está dentro del <label>: evita que el clic marque el checkbox sin haber leído
    evento.preventDefault();
    this.mostrarTerminos.set(true);
  }

  protected aceptarTerminos(): void {
    const control = this.form.controls.aceptaTerminos;
    control.setValue(true);
    control.markAsTouched();
    this.mostrarTerminos.set(false);
  }

  protected invalido(campo: Campo): boolean {
    const control = this.form.controls[campo];
    const errorGrupo = campo === 'confirmarPassword' && this.form.hasError('noCoincide');
    return (control.invalid || errorGrupo) && (control.touched || this.enviado());
  }

  protected error(campo: Campo): string {
    const c = this.form.controls[campo];
    const e = c.errors ?? {};
    if (e['servidor']) return e['servidor'];
    if (e['required']) return MENSAJES_REQUERIDO[campo];
    if (e['requiredTrue']) return 'Debes aceptar los términos y la política de tratamiento de datos';
    if (e['minlength']) return `Debe tener al menos ${e['minlength'].requiredLength} caracteres`;
    if (e['maxlength']) return `No puede superar ${e['maxlength'].requiredLength} caracteres`;
    if (e['pattern']) return MENSAJES_PATRON[campo] ?? 'Formato inválido';
    if (e['passwordDebil']) return 'Debe contener al menos una letra y un número';
    if (campo === 'confirmarPassword' && this.form.hasError('noCoincide')) return 'Las contraseñas no coinciden';
    return '';
  }

  protected onSubmit(): void {
    this.enviado.set(true);
    this.errorGeneral.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorGeneral.set('Revisa los campos marcados en rojo.');
      return;
    }

    this.enviando.set(true);
    const v = this.form.getRawValue();
    this.auth
      .registrar({
        nombre: v.nombre.trim(),
        apellido: v.apellido.trim(),
        email: v.email.trim(),
        telefono: v.telefono.trim(),
        password: v.password,
        confirmarPassword: v.confirmarPassword,
        rol: v.rol,
        nombreTienda: v.rol === 'VENDEDOR' ? v.nombreTienda.trim() : null,
        aceptaTerminos: v.aceptaTerminos,
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.router.navigateByUrl(this.auth.rutaInicio());
        },
        error: (err) => {
          this.enviando.set(false);
          this.errorGeneral.set(procesarErrorApi(err, this.form));
        },
      });
  }
}

const MENSAJES_REQUERIDO: Record<Campo, string> = {
  nombre: 'El nombre es obligatorio',
  apellido: 'El apellido es obligatorio',
  email: 'El correo es obligatorio',
  telefono: 'El teléfono es obligatorio',
  nombreTienda: 'El nombre de la tienda es obligatorio para vendedores',
  password: 'La contraseña es obligatoria',
  confirmarPassword: 'Confirma tu contraseña',
  aceptaTerminos: 'Debes aceptar los términos',
};

const MENSAJES_PATRON: Partial<Record<Campo, string>> = {
  nombre: 'Solo se permiten letras',
  apellido: 'Solo se permiten letras',
  email: 'Ingresa un correo válido',
  telefono: 'Ingresa un celular de 10 dígitos que empiece por 3',
};
