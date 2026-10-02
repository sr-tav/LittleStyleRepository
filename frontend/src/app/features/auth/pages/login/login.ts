import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { AuthBrand } from '../../components/auth-brand/auth-brand';
import { PATRON_EMAIL, procesarErrorApi } from '../../auth.validators';

/** UI-1: Inicio de sesión. */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, AuthBrand],
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly enviando = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  protected readonly mostrarPassword = signal(false);
  protected readonly avisoRecuperar = signal(false);
  protected readonly enviado = signal(false);
  protected readonly sesionExpirada = this.route.snapshot.queryParamMap.get('sesion') === 'expirada';

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.pattern(PATRON_EMAIL)]],
    password: ['', [Validators.required]],
  });

  protected invalido(campo: 'email' | 'password'): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || this.enviado());
  }

  protected onSubmit(): void {
    this.enviado.set(true);
    this.errorGeneral.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    const { email, password } = this.form.getRawValue();
    this.auth.login({ email: email.trim(), password }).subscribe({
      next: () => {
        this.enviando.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : this.auth.rutaInicio());
      },
      error: (err) => {
        this.enviando.set(false);
        this.errorGeneral.set(procesarErrorApi(err, this.form));
      },
    });
  }
}
