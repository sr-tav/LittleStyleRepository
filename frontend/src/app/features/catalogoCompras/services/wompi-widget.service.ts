import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Observable, from, switchMap } from 'rxjs';

import { InicioPago } from '../models/pago.models';

export const WOMPI_WIDGET_URL = 'https://checkout.wompi.co/widget.js';
/** Si el script de Wompi no carga en este tiempo se avisa al cliente en lugar de esperar indefinidamente. */
export const TIEMPO_MAXIMO_CARGA_MS = 15000;
/** Clase del modal que crea el Widget; al cerrarse, Wompi le agrega el atributo `hidden`. */
export const CLASE_MODAL_WOMPI = 'waybox-modal';

/** Cómo terminó el widget: con una transacción creada o cerrado por el cliente sin pagar. */
export type ResultadoWidgetWompi = { tipo: 'transaccion'; id: string } | { tipo: 'cerrado' };

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

  /**
   * Abre el widget y emite una sola vez cómo terminó: el id de la transacción si el cliente pagó, o
   * `cerrado` si cerró la ventana sin pagar. Wompi no invoca el callback al cerrar el modal, por eso
   * se observa el atributo `hidden` que agrega a su modal.
   */
  abrir(inicio: InicioPago, alAbrir?: () => void): Observable<ResultadoWidgetWompi> {
    return from(this.cargar()).pipe(
      switchMap((WidgetCheckout) => new Observable<ResultadoWidgetWompi>((suscriptor) => {
        let terminado = false;
        const terminar = (resultado: ResultadoWidgetWompi) => {
          if (terminado) return;
          terminado = true;
          suscriptor.next(resultado);
          suscriptor.complete();
        };

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
        const observador = this.observarCierre(() => terminar({ tipo: 'cerrado' }));
        widget.open((resultado) => {
          const id = resultado?.transaction?.id;
          terminar(id ? { tipo: 'transaccion', id } : { tipo: 'cerrado' });
        });
        alAbrir?.();
        return () => observador?.disconnect();
      })),
    );
  }

  private observarCierre(alCerrar: () => void): MutationObserver | null {
    if (typeof MutationObserver === 'undefined') return null;
    const observador = new MutationObserver((cambios) => {
      const cerrado = cambios.some((cambio) => cambio.target instanceof Element
        && cambio.target.classList.contains(CLASE_MODAL_WOMPI)
        && cambio.target.hasAttribute('hidden'));
      if (cerrado) alCerrar();
    });
    observador.observe(this.document.body, { subtree: true, attributes: true, attributeFilter: ['hidden'] });
    return observador;
  }

  private cargar(): Promise<WidgetCheckoutConstructor> {
    const ventana = this.document.defaultView as VentanaConWompi | null;
    if (ventana?.WidgetCheckout) return Promise.resolve(ventana.WidgetCheckout);
    if (this.carga) return this.carga;

    this.carga = new Promise<WidgetCheckoutConstructor>((resolve, reject) => {
      const script = this.document.createElement('script');
      script.src = WOMPI_WIDGET_URL;
      script.async = true;
      const limite = setTimeout(() => {
        this.carga = null;
        script.remove();
        reject(new Error('La pasarela de pagos está tardando en cargar. Intenta más tarde.'));
      }, TIEMPO_MAXIMO_CARGA_MS);
      script.onload = () => {
        clearTimeout(limite);
        const constructor = (this.document.defaultView as VentanaConWompi | null)?.WidgetCheckout;
        if (constructor) {
          resolve(constructor);
          return;
        }
        this.carga = null;
        reject(new Error('La pasarela de pagos no está disponible. Intenta de nuevo.'));
      };
      script.onerror = () => {
        clearTimeout(limite);
        this.carga = null;
        script.remove();
        reject(new Error('No fue posible cargar la pasarela de pagos. Revisa tu conexión e intenta de nuevo.'));
      };
      this.document.body.appendChild(script);
    });
    return this.carga;
  }
}
