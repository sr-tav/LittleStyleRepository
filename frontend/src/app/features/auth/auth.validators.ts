import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

import { ApiError } from '../../core/models/auth.models';

/** Mismas reglas que el backend (RegistroRequest) para dar feedback inmediato. */
export const PATRON_NOMBRE = /^[\p{L} ]+$/u;
export const PATRON_CELULAR = /^3\d{9}$/;
export const PATRON_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Al menos una letra y un número. */
export const passwordSegura: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor: string = control.value ?? '';
  if (!valor) {
    return null;
  }
  return /[A-Za-z]/.test(valor) && /\d/.test(valor) ? null : { passwordDebil: true };
};

/** Validador de grupo: marca `confirmarPassword` con `noCoincide` si difiere de `password`. */
export function passwordsIguales(campo = 'password', confirmacion = 'confirmarPassword'): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const password = grupo.get(campo)?.value;
    const confirmar = grupo.get(confirmacion);
    if (!confirmar || !confirmar.value) {
      return null;
    }
    return password === confirmar.value ? null : { noCoincide: true };
  };
}

/** Nivel de fortaleza 0-4 para la barra visual del registro. */
export function fortalezaPassword(valor: string): number {
  if (!valor) {
    return 0;
  }
  let puntos = 0;
  if (valor.length >= 8) puntos++;
  if (/[a-z]/.test(valor) && /[A-Z]/.test(valor)) puntos++;
  if (/\d/.test(valor)) puntos++;
  if (/[^A-Za-z0-9]/.test(valor) || valor.length >= 12) puntos++;
  return puntos;
}

/**
 * Traduce un error HTTP del backend en un mensaje general y, si trae errores por campo,
 * los asigna a los controles del formulario como error `servidor`.
 */
export function procesarErrorApi(error: unknown, form?: FormGroup): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado. Intenta de nuevo.';
  }
  if (error.status === 0) {
    return 'No fue posible conectar con el servidor. Verifica que el backend esté en ejecución.';
  }
  const body = error.error as Partial<ApiError> | null;
  if (form && body?.errores) {
    for (const [campo, mensaje] of Object.entries(body.errores)) {
      const control = form.get(campo);
      if (control) {
        control.setErrors({ ...(control.errors ?? {}), servidor: mensaje });
        control.markAsTouched();
      }
    }
  }
  return body?.mensaje ?? 'Ocurrió un error inesperado. Intenta de nuevo.';
}
