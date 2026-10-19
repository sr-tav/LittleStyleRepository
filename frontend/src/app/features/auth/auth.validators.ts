import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

import { ApiError } from '../../core/models/auth.models';

/**
 * Mismas reglas que el backend (RegistroRequest) para dar feedback inmediato.
 * @see US-05
 * @see UI-2
 */
export const PATRON_NOMBRE = /^[\p{L} ]+$/u;
/** Expresión regular para validar celulares de diez dígitos iniciados en 3. */
export const PATRON_CELULAR = /^3\d{9}$/;
/** Expresión regular para un correo con dominio y sufijo de al menos dos caracteres. */
export const PATRON_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/**
 * Valida que la contraseña contenga al menos una letra y un número.
 * @param control Control cuyo valor se valida.
 * @returns Error `passwordDebil` si no cumple o `null` si es válido o está vacío.
 * @see US-05
 * @see UI-2
 */
export const passwordSegura: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor: string = control.value ?? '';
  if (!valor) {
    return null;
  }
  return /[A-Za-z]/.test(valor) && /\d/.test(valor) ? null : { passwordDebil: true };
};

/**
 * Validador de grupo: marca `confirmarPassword` con `noCoincide` si difiere de `password`.
 * @param campo Nombre del control que contiene la contraseña.
 * @param confirmacion Nombre del control que confirma la contraseña.
 * @returns Validador de grupo que compara ambos controles.
 */
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

/**
 * Calcula el nivel de fortaleza de 0 a 4 para la barra visual del registro.
 * @param valor Contraseña cuyo nivel se calcula.
 * @returns Puntaje de 0 a 4 según longitud, mayúsculas/minúsculas, dígitos y símbolos.
 */
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
 * @param error Error recibido al procesar una solicitud.
 * @param form Formulario cuyos controles reciben los errores de campo, si se proporciona.
 * @returns Mensaje general para mostrar al usuario.
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
