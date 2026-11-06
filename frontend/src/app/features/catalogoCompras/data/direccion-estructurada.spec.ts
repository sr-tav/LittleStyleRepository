import { PATRON_NUMERO_VIA, PATRON_PLACA, TIPOS_VIA, componerDireccion, parsearDireccion } from './direccion-estructurada';

describe('direccion-estructurada', () => {
  it('compone la dirección con la nomenclatura colombiana', () => {
    expect(componerDireccion('Calle', '45A', '12', '34')).toBe('Calle 45A # 12 - 34');
    expect(componerDireccion(' Carrera ', ' 13 ', '7', '8')).toBe('Carrera 13 # 7 - 8');
  });

  it('devuelve vacío si falta alguna parte', () => {
    expect(componerDireccion('Calle', '45', '', '34')).toBe('');
    expect(componerDireccion(null, '45', '12', '34')).toBe('');
  });

  it('valida números de vía, cruce y placa', () => {
    for (const ok of ['45', '45A', '45 bis', '45 Sur', '45A bis Sur']) expect(PATRON_NUMERO_VIA.test(ok)).toBe(true);
    for (const mal of ['', 'abc', '1234', '45 Norte Sur', '4 5']) expect(PATRON_NUMERO_VIA.test(mal)).toBe(false);
    expect(PATRON_PLACA.test('34')).toBe(true);
    expect(PATRON_PLACA.test('3a')).toBe(false);
    expect(PATRON_PLACA.test('1234')).toBe(false);
  });

  it('ofrece los tipos de vía principales', () => {
    expect(TIPOS_VIA).toContain('Calle');
    expect(TIPOS_VIA).toContain('Carrera');
  });

  it('separa una dirección completa escrita en un solo campo', () => {
    expect(parsearDireccion('Calle 45A # 12 - 34')).toEqual({
      tipoVia: 'Calle',
      numeroVia: '45A',
      numeroCruce: '12',
      placa: '34',
    });
    expect(parsearDireccion('avenida carrera 13 # 7 - 8')).toEqual({
      tipoVia: 'Avenida Carrera',
      numeroVia: '13',
      numeroCruce: '7',
      placa: '8',
    });
  });

  it('rechaza una dirección incompleta o con nomenclatura inválida', () => {
    expect(parsearDireccion('')).toBeNull();
    expect(parsearDireccion('Calle 45 # 12')).toBeNull();
    expect(parsearDireccion('Calle ABC # 12 - 34')).toBeNull();
    expect(parsearDireccion('Cl 45 # 12 - 34')).toBeNull();
  });
});
