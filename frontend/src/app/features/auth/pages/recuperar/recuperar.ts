import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { PATRON_EMAIL, procesarErrorApi } from '../../auth.validators';
import { AuthBrand } from '../../components/auth-brand/auth-brand';

@Component({
  selector: 'app-recuperar-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthBrand],
  template: `
    <main class="auth-page">
      <div class="auth-card">
        <app-auth-brand />
        <section class="auth-form-panel">
          <header class="auth-header">
            <h1>Recuperar contraseña</h1>
            <p>Te enviaremos un enlace para restablecer tu acceso.</p>
          </header>

          @if (mensaje()) {
            <div class="alert" [class.alert-success]="estado() === 'ok'" [class.alert-error]="estado() === 'error'" role="status">
              {{ mensaje() }}
            </div>
          }

          <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
            <div class="field" [class.has-error]="invalido('email')">
              <label for="email">Correo electrónico</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                placeholder="tucorreo@ejemplo.com"
                autocomplete="email"
                [attr.aria-invalid]="invalido('email')"
              />
              @if (invalido('email')) {
                <small class="field-error">
                  @if (form.controls.email.hasError('required')) {
                    El correo es obligatorio
                  } @else if (form.controls.email.hasError('pattern')) {
                    Ingresa un correo válido
                  }
                </small>
              }
            </div>

            <button type="submit" class="btn-primary" [disabled]="enviando()">
              @if (enviando()) {
                <span class="spinner" aria-hidden="true"></span> Enviando...
              } @else {
                Enviar enlace
              }
            </button>
          </form>

          <p class="auth-switch">
            <a routerLink="/auth/login">Volver al inicio de sesión</a>
          </p>
        </section>
      </div>
    </main>
  `,
  styles: `
    :host { display: block; }
    .auth-page { min-height: 100vh; display: grid; place-items: center; padding: 2rem; background: #f5f7f2; }
    .auth-card { width: min(100%, 960px); display: grid; grid-template-columns: minmax(220px, 320px) minmax(0, 1fr); background: #fff; border-radius: 30px; box-shadow: 0 18px 60px rgba(20, 83, 45, 0.12); overflow: hidden; }
    .auth-form-panel { padding: 2rem 2.2rem; }
    .auth-header { margin-bottom: 1.25rem; }
    .auth-header h1 { margin: 0; font-size: clamp(2rem, 2vw, 2.5rem); color: #14532d; }
    .auth-header p { margin: .5rem 0 0; color: #4b5563; }
    .field { display: flex; flex-direction: column; gap: .5rem; margin-bottom: 1rem; }
    .field label { font-weight: 600; color: #1f2937; }
    .field input { width: 100%; min-height: 52px; border: 1px solid #d1d5db; border-radius: 12px; padding: 0 .9rem; font: inherit; }
    .field.has-error input { border-color: #dc2626; }
    .field-error { color: #b91c1c; font-size: .82rem; }
    .alert { margin-bottom: 1rem; border-radius: 12px; padding: .85rem 1rem; font-weight: 600; }
    .alert-success { background: #dcfce7; color: #166534; }
    .alert-error { background: #fee2e2; color: #991b1b; }
    .btn-primary { width: 100%; border: none; border-radius: 12px; background: linear-gradient(135deg, #15803d, #22c55e); color: white; min-height: 52px; font-weight: 700; cursor: pointer; }
    .btn-primary:disabled { opacity: .7; cursor: wait; }
    .spinner { display: inline-block; width: 1rem; height: 1rem; border: 2px solid rgba(255,255,255,.5); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; margin-right: .55rem; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .auth-switch { margin-top: 1.2rem; text-align: center; }
    .auth-switch a { color: #15803d; text-decoration: none; font-weight: 600; }
    @media (max-width: 760px) { .auth-card { grid-template-columns: 1fr; } }
  `,
})
export class RecuperarPassword {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly enviando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly estado = signal<'ok' | 'error' | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.pattern(PATRON_EMAIL)]],
  });

  protected invalido(campo: 'email'): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || this.enviando());
  }

  protected onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    this.enviando.set(true);
    this.estado.set(null);
    this.mensaje.set(null);
    const email = this.form.getRawValue().email.trim();

    this.auth.solicitarRecuperacion(email).subscribe({
      next: () => {
        this.enviando.set(false);
        this.estado.set('ok');
        this.mensaje.set('Si existe una cuenta con ese correo, te enviamos un enlace para restablecer la contraseña.');
        this.form.reset();
      },
      error: (err) => {
        this.enviando.set(false);
        const detalle = procesarErrorApi(err, this.form);
        this.estado.set('error');
        this.mensaje.set(detalle || 'No se pudo enviar el enlace de recuperación.');
      },
    });
  }
}

