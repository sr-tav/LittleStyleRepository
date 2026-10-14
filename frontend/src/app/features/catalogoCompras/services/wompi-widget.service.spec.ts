import { TestBed } from '@angular/core/testing';

import { InicioPago } from '../models/pago.models';
import {
  CLASE_MODAL_WOMPI,
  ResultadoWidgetWompi,
  TIEMPO_MAXIMO_CARGA_MS,
  WOMPI_WIDGET_URL,
  WompiWidgetService,
} from './wompi-widget.service';

type VentanaPrueba = Window & { WidgetCheckout?: unknown };

describe('WompiWidgetService', () => {
  const inicio: InicioPago = {
    pedidoId: 55,
    referencia: 'LS-55-ABC',
    montoCentavos: 7280000,
    moneda: 'COP',
    llavePublica: 'pub_test_llave',
    firmaIntegridad: 'firma',
    urlRedireccion: 'http://localhost:4200/cliente/pago/resultado',
    emailCliente: 'ana@correo.com',
    nombreCliente: 'Ana Ruiz',
    telefonoCliente: '3001234567',
  };

  afterEach(() => {
    vi.useRealTimers();
    delete (window as VentanaPrueba).WidgetCheckout;
    document.querySelectorAll(`.${CLASE_MODAL_WOMPI}`).forEach((modal) => modal.remove());
    document.querySelectorAll(`script[src="${WOMPI_WIDGET_URL}"]`).forEach((script) => script.remove());
  });

  it('abre el widget con los datos firmados y emite el id de la transacción', async () => {
    const configuraciones: unknown[] = [];
    (window as VentanaPrueba).WidgetCheckout = class {
      constructor(config: unknown) {
        configuraciones.push(config);
      }
      open(callback: (resultado: { transaction: { id: string; status: string } }) => void): void {
        callback({ transaction: { id: 'wompi-1', status: 'APPROVED' } });
      }
    };
    const service = TestBed.inject(WompiWidgetService);
    const alAbrir = vi.fn();

    const resultados: ResultadoWidgetWompi[] = [];
    await new Promise<void>((resolve) => {
      service.abrir(inicio, alAbrir).subscribe({ next: (r) => resultados.push(r), complete: resolve });
    });

    expect(resultados).toEqual([{ tipo: 'transaccion', id: 'wompi-1' }]);
    expect(alAbrir).toHaveBeenCalled();
    expect(configuraciones[0]).toEqual({
      currency: 'COP',
      amountInCents: 7280000,
      reference: 'LS-55-ABC',
      publicKey: 'pub_test_llave',
      signature: { integrity: 'firma' },
      redirectUrl: 'http://localhost:4200/cliente/pago/resultado',
      customerData: {
        email: 'ana@correo.com',
        fullName: 'Ana Ruiz',
        phoneNumber: '3001234567',
        phoneNumberPrefix: '+57',
      },
    });
  });

  it('informa que el pago no se completó cuando el cliente cierra la ventana de Wompi', async () => {
    let modal: HTMLDivElement | null = null;
    (window as VentanaPrueba).WidgetCheckout = class {
      open(): void {
        // Como Wompi: crea su modal y, al cerrarlo sin pagar, lo oculta sin invocar el callback
        modal = document.createElement('div');
        modal.className = CLASE_MODAL_WOMPI;
        document.body.appendChild(modal);
      }
    };
    const service = TestBed.inject(WompiWidgetService);

    const resultado = new Promise<ResultadoWidgetWompi[]>((resolve) => {
      const recibidos: ResultadoWidgetWompi[] = [];
      service.abrir(inicio).subscribe({ next: (r) => recibidos.push(r), complete: () => resolve(recibidos) });
    });
    await vi.waitFor(() => expect(modal).not.toBeNull());
    modal!.setAttribute('hidden', '');

    expect(await resultado).toEqual([{ tipo: 'cerrado' }]);
  });

  it('trata como cerrado un callback de Wompi sin transacción', async () => {
    (window as VentanaPrueba).WidgetCheckout = class {
      open(callback: (resultado: object) => void): void {
        callback({});
      }
    };
    const service = TestBed.inject(WompiWidgetService);

    const resultados: ResultadoWidgetWompi[] = [];
    await new Promise<void>((resolve) => {
      service.abrir(inicio).subscribe({ next: (r) => resultados.push(r), complete: resolve });
    });

    expect(resultados).toEqual([{ tipo: 'cerrado' }]);
  });

  it('avisa que se intente más tarde si el script de Wompi no carga a tiempo', async () => {
    vi.useFakeTimers();
    const service = TestBed.inject(WompiWidgetService);
    const error = new Promise<unknown>((resolve) => {
      service.abrir(inicio).subscribe({ error: resolve });
    });

    vi.advanceTimersByTime(TIEMPO_MAXIMO_CARGA_MS);

    expect(((await error) as Error).message).toContain('Intenta más tarde');
    expect(document.querySelectorAll(`script[src="${WOMPI_WIDGET_URL}"]`)).toHaveLength(0);
  });

  it('carga el script de Wompi una sola vez y reporta si falla', async () => {
    const service = TestBed.inject(WompiWidgetService);
    const error = new Promise<unknown>((resolve) => {
      service.abrir(inicio).subscribe({ error: resolve });
    });
    service.abrir(inicio).subscribe({ error: () => undefined });

    const scripts = document.querySelectorAll<HTMLScriptElement>(`script[src="${WOMPI_WIDGET_URL}"]`);
    expect(scripts).toHaveLength(1);
    scripts[0].dispatchEvent(new Event('error'));

    expect(((await error) as Error).message).toContain('No fue posible cargar la pasarela de pagos');
  });
});
