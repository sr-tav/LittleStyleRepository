import { FormControl, FormGroup } from '@angular/forms';

import {
  CODIGOS_POSTALES_POR_MUNICIPIO_DANE,
  codigoPostalCoherente,
  codigosPostalesDe,
  prefijoPostalDe,
  validarCodigoPostal,
} from './codigos-postales-colombia';
import { MUNICIPIOS_COLOMBIA } from './municipios-colombia';

describe('codigos-postales-colombia', () => {
  it('lista los códigos existentes de Armenia y de los demás municipios de Quindío', () => {
    expect(codigosPostalesDe('Quindío', 'Armenia')).toEqual(['630001', '630002', '630003', '630004', '630007', '630008']);
    const quindio = MUNICIPIOS_COLOMBIA.filter((m) => m.departamento === 'Quindío');
    expect(quindio).toHaveLength(12);
    for (const m of quindio) expect(codigosPostalesDe('Quindío', m.municipio), m.municipio).not.toBeNull();
  });

  it('incluye una lista postal para cada municipio del catálogo DANE', () => {
    expect(Object.keys(CODIGOS_POSTALES_POR_MUNICIPIO_DANE)).toHaveLength(MUNICIPIOS_COLOMBIA.length);
    for (const municipio of MUNICIPIOS_COLOMBIA) {
      expect(codigosPostalesDe(municipio.departamento, municipio.municipio), municipio.municipio).not.toBeNull();
    }
  });

  it('incluye códigos oficiales para capitales, municipios principales y Clemencia', () => {
    const ciudades: [string, string, string][] = [
      ['Bogotá D.C.', 'Bogotá D.C.', '110111'],
      ['Antioquia', 'Medellín', '050001'],
      ['Antioquia', 'Bello', '051050'],
      ['Antioquia', 'Envigado', '055420'],
      ['Antioquia', 'Itagui', '055410'],
      ['Valle del Cauca', 'Cali', '760001'],
      ['Valle del Cauca', 'Palmira', '763531'],
      ['Atlántico', 'Barranquilla', '080001'],
      ['Bolívar', 'Cartagena', '130001'],
      ['Bolívar', 'Clemencia', '130510'],
      ['Santander', 'Bucaramanga', '680001'],
      ['Risaralda', 'Pereira', '660001'],
      ['Caldas', 'Manizales', '170001'],
      ['Tolima', 'Ibagué', '730001'],
      ['Huila', 'Neiva', '410001'],
      ['Nariño', 'Pasto', '520001'],
      ['Boyacá', 'Tunja', '150001'],
      ['Cauca', 'Popayán', '190001'],
      ['Magdalena', 'Santa Marta', '470001'],
      ['Norte de Santander', 'Cúcuta', '540001'],
      ['Meta', 'Villavicencio', '500001'],
      ['Cesar', 'Valledupar', '200001'],
      ['Córdoba', 'Montería', '230001'],
      ['Sucre', 'Sincelejo', '700001'],
      ['Chocó', 'Belén de Bajira', '277017'],
    ];
    for (const [departamento, municipio, codigo] of ciudades) {
      expect(codigosPostalesDe(departamento, municipio), municipio).toContain(codigo);
    }
  });

  it('todos los códigos catalogados empiezan por el prefijo de su departamento', () => {
    for (const [dane, codigos] of Object.entries(CODIGOS_POSTALES_POR_MUNICIPIO_DANE)) {
      const m = MUNICIPIOS_COLOMBIA.find((x) => x.municipioDane === dane)!;
      for (const c of codigos) expect(c.startsWith(m.departamentoDane), `${m.municipio} ${c}`).toBe(true);
    }
  });

  it('devuelve null para municipios desconocidos y el prefijo del departamento', () => {
    expect(codigosPostalesDe('Valle del Cauca', 'Municipio inexistente')).toBeNull();
    expect(prefijoPostalDe('Valle del Cauca')).toBe('76');
    expect(prefijoPostalDe('Inexistente')).toBeNull();
  });

  it('valida departamento y municipio', () => {
    expect(validarCodigoPostal('Quindío', 'Armenia', '630001')).toBe('valido');
    expect(validarCodigoPostal('Quindío', 'Armenia', '630005')).toBe('municipio');
    expect(validarCodigoPostal('Quindío', 'Armenia', '631001')).toBe('municipio');
    expect(validarCodigoPostal('Quindío', 'Armenia', '050001')).toBe('departamento');
    expect(validarCodigoPostal('Valle del Cauca', 'Cali', '760001')).toBe('valido');
    expect(validarCodigoPostal('Valle del Cauca', 'Cali', '760047')).toBe('municipio');
    expect(validarCodigoPostal('Valle del Cauca', 'Cali', '110111')).toBe('departamento');
  });

  it('el validador de control lee departamento y municipio del formulario', () => {
    const form = new FormGroup({
      departamento: new FormControl('Quindío'),
      municipio: new FormControl('Armenia'),
      codigoPostal: new FormControl('630005', [codigoPostalCoherente]),
    });
    form.controls.codigoPostal.updateValueAndValidity();
    expect(form.controls.codigoPostal.errors).toEqual({ codigoPostalIncoherente: 'municipio' });
    form.controls.codigoPostal.setValue('630004');
    expect(form.controls.codigoPostal.errors).toBeNull();
  });
});
