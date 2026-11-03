import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PrendaRequest } from '../models/prenda.models';
import { PrendasService } from './prendas.service';

describe('PrendasService.crear', () => {
  it('envía multipart con la parte JSON `prenda` y las fotos en `archivos`', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const servicio = TestBed.inject(PrendasService);
    const http = TestBed.inject(HttpTestingController);
    const request = { nombre: 'Body 3M', descripcion: 'Suave' } as unknown as PrendaRequest;
    const foto = new File([new Uint8Array([1, 2, 3])], 'foto.png', { type: 'image/png' });

    servicio.crear(request, [foto]).subscribe();

    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/vendedor/prendas'));
    const cuerpo = req.request.body as FormData;
    expect(cuerpo.get('prenda')).toBeInstanceOf(Blob);
    expect((cuerpo.get('prenda') as Blob).type).toBe('application/json');
    expect(cuerpo.getAll('archivos')).toHaveLength(1);
    req.flush({});
    http.verify();
  });
});
