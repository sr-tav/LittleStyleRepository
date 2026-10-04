import { tallaProvisional } from './talla-provisional';

describe('tallaProvisional', () => {
  it('asigna tallas pares provisionales en rangos de dos años', () => {
    expect(tallaProvisional(0)).toBe(0);
    expect(tallaProvisional(1)).toBe(0);
    expect(tallaProvisional(2)).toBe(2);
    expect(tallaProvisional(7)).toBe(6);
    expect(tallaProvisional(17)).toBe(16);
  });

  it('limita edades inválidas o fuera del rango provisional', () => {
    expect(tallaProvisional(-1)).toBe(0);
    expect(tallaProvisional(Number.NaN)).toBe(0);
    expect(tallaProvisional(30)).toBe(16);
  });
});
