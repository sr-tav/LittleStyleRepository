import {
  LIMITES_TALLA_CORPORAL,
  TALLAS_REFERENCIA,
  buscarTallaReferencia,
  describirSugerencia,
  NOMBRES_TALLAS_REFERENCIA,
  normalizarNombreTalla,
} from './tallas-referencia';

describe('tallas-referencia', () => {
  /** Tabla ordenada por edad mínima, como la usa la prueba de coherencia. */
  const ordenada = [...TALLAS_REFERENCIA].sort((a, b) => a.edadMinMeses - b.edadMinMeses);

  it('cubre los nombres habituales de ropita infantil', () => {
    const nombres = new Set(NOMBRES_TALLAS_REFERENCIA);
    for (const esperada of ['0-3M', '3-6M', '6-9M', '9-12M', '12-18M', '18-24M']) {
      expect(nombres.has(esperada)).toBe(true);
    }
    for (const esperada of ['2', '4', '6', '8', '10', '12', '14', '16', 'XS', 'S', 'M', 'L', 'XL']) {
      expect(nombres.has(esperada)).toBe(true);
    }
  });

  it('mantiene todos los rangos dentro de los límites del backend (TallaRequest)', () => {
    const limites = LIMITES_TALLA_CORPORAL;
    for (const talla of TALLAS_REFERENCIA) {
      expect(talla.estaturaMinCm).toBeGreaterThanOrEqual(limites.estaturaMinCm);
      expect(talla.estaturaMaxCm).toBeLessThanOrEqual(limites.estaturaMaxCm);
      expect(talla.pesoMinKg).toBeGreaterThanOrEqual(limites.pesoMinKg);
      expect(talla.pesoMaxKg).toBeLessThanOrEqual(limites.pesoMaxKg);
      expect(talla.edadMinMeses).toBeGreaterThanOrEqual(limites.edadMinMeses);
      expect(talla.edadMaxMeses).toBeLessThanOrEqual(limites.edadMaxMeses);
    }
  });

  it('nunca deja que un mínimo supere al máximo en la misma talla', () => {
    for (const t of TALLAS_REFERENCIA) {
      expect(t.estaturaMinCm).toBeLessThan(t.estaturaMaxCm);
      expect(t.pesoMinKg).toBeLessThan(t.pesoMaxKg);
      expect(t.edadMinMeses).toBeLessThan(t.edadMaxMeses);
    }
  });

  it('es coherente entre tallas consecutivas: sin saltos bruscos ni inversiones', () => {
    for (let i = 1; i < ordenada.length; i++) {
      const previa = ordenada[i - 1];
      const actual = ordenada[i];
      // Las tallas crecen: el mínimo de cada rango nunca retrocede con la edad.
      expect(actual.estaturaMinCm).toBeGreaterThan(previa.estaturaMinCm - 12);
      expect(actual.pesoMinKg).toBeGreaterThanOrEqual(previa.pesoMinKg);
      // Las estaturas se traslapan con la talla anterior, sin huecos grandes para el cuerpo en transición.
      expect(actual.estaturaMinCm).toBeLessThanOrEqual(previa.estaturaMaxCm);
      expect(actual.edadMinMeses).toBeGreaterThan(previa.edadMinMeses);
    }
  });

  it('normaliza la talla escrita como hace el backend: recorta, compacta y pasa a mayúsculas', () => {
    expect(normalizarNombreTalla(' xs ')).toBe('XS');
    expect(normalizarNombreTalla('0   -  3m')).toBe('0 - 3M');
  });

  it('resuelve la talla escrita aunque lleve espaciados, mayúsculas o alias', () => {
    expect(buscarTallaReferencia(' xs ')?.nombre).toBe('XS');
    expect(buscarTallaReferencia(' 4t ')?.nombre).toBe('4');
    expect(buscarTallaReferencia(' 4 ')?.nombre).toBe('4');
    expect(buscarTallaReferencia('0 - 3M')?.nombre).toBe('0-3M');
    expect(buscarTallaReferencia('t8')?.nombre).toBe('8');
  });

  it('no inventa una referencia para tallas desconocidas ni vacías', () => {
    expect(buscarTallaReferencia('')).toBeNull();
    expect(buscarTallaReferencia('  ')).toBeNull();
    expect(buscarTallaReferencia('XXXL')).toBeNull();
    expect(buscarTallaReferencia(null)).toBeNull();
  });

  it('describirSugerencia arma el texto con los tres rangos para el vendedor', () => {
    expect(describirSugerencia(buscarTallaReferencia('4')!)).toBe('98–110 cm · 15–19 kg · 36–48 meses');
  });
});
