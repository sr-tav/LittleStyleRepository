import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Observable, from, switchMap } from 'rxjs';

import { InicioPago } from '../models/pago.models';

export const WOMPI_WIDGET_URL = 'https://checkout.wompi.co/widget.js';

interface ConfiguracionWidget {
  currency: string;
  amountInCents: number;
  reference: string;
  publicKey: string;
  signature: { integrity: string };
  redirectUrl?: string;
  customerData?: {
    email?: string;
    fullName?: string;
    phoneNumber?: string;
    phoneNumberPrefix?: string;
  };
}

interface ResultadoWidget {
  transaction?: { id: string; status: string };
}

export type WidgetCheckoutConstructor = new (config: ConfiguracionWidget) => {
  open(callback: (resultado: ResultadoWidget) => void): void;
};

type VentanaConWompi = Window & { WidgetCheckout?: WidgetCheckoutConstructor };

/** Carga bajo demanda el Widget de Wompi y lo abre con los datos firmados por el backend. */
@Injectable({ providedIn: 'root' })
export class WompiWidgetService {
  private readonly document = inject(DOCUMENT);
  private carga: Promise<WidgetCheckoutConstructor> | null = null;

  /** Emite el id de la transacción de Wompi cuando el cliente termina el pago en el widget. */
  abrir(inicio: InicioPago, alAbrir?: () => void): Observable<string> {
    return from(this.cargar()).pipe(
      switchMap((WidgetCheckout) => new Observable<string>((suscriptor) => {
        const widget = new WidgetCheckout({
          currency: inicio.moneda,
          amountInCents: inicio.montoCentavos,
          reference: inicio.referencia,
          publicKey: inicio.llavePublica,
          signature: { integrity: inicio.firmaIntegridad },
          ...(inicio.urlRedireccion ? { redirectUrl: inicio.urlRedireccion } : {}),
          customerData: {
            ...(inicio.emailCliente ? { email: inicio.emailCliente } : {}),
            ...(inicio.nombreCliente ? { fullName: inicio.nombreCliente } : {}),
            ...(inicio.telefonoCliente
              ? { phoneNumber: inicio.telefonoCliente, phoneNumberPrefix: '+57' }
              : {}),
          },
        });
        widget.open((resultado) => {
          const id = resultado?.transaction?.id;
          if (id) suscriptor.next(id);
          suscriptor.complete();
        });
        alAbrir?.();
      })),
    );
  }

  private cargar(): Promise<WidgetCheckoutConstructor> {
    const ventana = this.document.defaultView as VentanaConWompi | null;
    if (ventana?.WidgetCheckout) return Promise.resolve(ventana.WidgetCheckout);
    if (this.carga) return this.carga;

    this.carga = new Promise<WidgetCheckoutConstructor>((resolve, reject) => {
      const script = this.document.createElement('script');
      script.src = WOMPI_WIDGET_URL;
      script.async = true;
      script.onload = () => {
        const constructor = (this.document.defaultView as VentanaConWompi | null)?.WidgetCheckout;
        if (constructor) {
          resolve(constructor);
          return;
        }
        this.carga = null;
        reject(new Error('La pasarela de pagos no está disponible. Intenta de nuevo.'));
      };
      script.onerror = () => {
        this.carga = null;
        script.remove();
        reject(new Error('No fue posible cargar la pasarela de pagos. Revisa tu conexión e intenta de nuevo.'));
      };
      this.document.body.appendChild(script);
    });
    return this.carga;
  }
}
