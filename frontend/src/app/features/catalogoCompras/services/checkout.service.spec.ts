import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { CheckoutService } from './checkout.service';

describe('CheckoutService', () => {
  let http: HttpTestingController;
  let service: CheckoutService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(CheckoutService);
  });

  afterEach(() => http.verify());

  it('solicita al backend el resumen del carrito para el perfil seleccionado', () => {
    const requestBody = {
      perfilInfantilId: 3,
      direccion: {
        destinatario: 'Ana Ruiz',
        direccion: 'Calle 1 # 2-3',
        complemento: null,
        codigoPostal: '630001',
        departamento: 'Quindío',
        municipio: 'Armenia',
        telefono: '3001234567',
      },
    };
    let subtotal = 0;

    service.calcularResumen(requestBody).subscribe((response) => {
      subtotal = response.subtotal;
    });

    const request = http.expectOne(`${environment.apiUrl}/cliente/checkout/resumen`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(requestBody);
    request.flush({
      perfilInfantilId: 3,
      items: [],
      subtotal: 65800,
      costoEnvio: 7000,
      total: 72800,
    });
    expect(subtotal).toBe(65800);
  });

  it('consulta y guarda la dirección asociada a la cuenta del cliente', () => {
    const direccion = {
      destinatario: 'Ana Ruiz',
      direccion: 'Calle 1 # 2-3',
      complemento: 'Apto. 302',
      codigoPostal: '630001',
      departamento: 'Quindío',
      municipio: 'Armenia',
      telefono: '3001234567',
    };

    service.obtenerDireccion().subscribe((response) => {
      expect(response).toEqual({ guardada: true, direccion });
    });
    const getRequest = http.expectOne(`${environment.apiUrl}/cliente/checkout/direccion`);
    expect(getRequest.request.method).toBe('GET');
    getRequest.flush({ guardada: true, direccion });

    service.guardarDireccion(direccion).subscribe((response) => {
      expect(response).toEqual({ guardada: true, direccion });
    });
    const putRequest = http.expectOne(`${environment.apiUrl}/cliente/checkout/direccion`);
    expect(putRequest.request.method).toBe('PUT');
    expect(putRequest.request.body).toEqual(direccion);
    putRequest.flush({ guardada: true, direccion });
  });
});
