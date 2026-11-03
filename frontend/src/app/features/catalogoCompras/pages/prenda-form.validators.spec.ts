import { FormControl } from '@angular/forms';

import { nombreConLetra } from './prenda-form';

describe('nombreConLetra', () => {
  it.each(['12345', '123 456', '---', '007'])('rechaza "%s"', (nombre) => {
    expect(nombreConLetra(new FormControl(nombre))).toEqual({ sinLetras: true });
  });

  it.each(['Camisa 2', 'Body 3M', 'Ñandú', 'Vestido'])('acepta "%s"', (nombre) => {
    expect(nombreConLetra(new FormControl(nombre))).toBeNull();
  });
});
