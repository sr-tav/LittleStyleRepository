import { TestBed } from '@angular/core/testing';

import { InicioPago } from '../models/pago.models';
import { WOMPI_WIDGET_URL, WompiWidgetService } from './wompi-widget.service';

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
    delete (window as VentanaPrueba).WidgetCheckout;
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

    const ids: string[] = [];
    await new Promise<void>((resolve) => {
      service.abrir(inicio, alAbrir).subscribe({ next: (id) => ids.push(id), complete: resolve });
    });

    expect(ids).toEqual(['wompi-1']);
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
