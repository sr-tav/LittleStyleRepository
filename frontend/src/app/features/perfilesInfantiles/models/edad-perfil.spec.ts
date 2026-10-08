import { formatearEdadPerfil } from './edad-perfil';

describe('formatearEdadPerfil', () => {
  it('muestra la edad en días durante el primer mes', () => {
    expect(
      formatearEdadPerfil(
        { fechaNacimiento: '2026-10-06', edadAnios: 0, mesesRestantes: 0 },
        new Date(2026, 9, 7),
      ),
    ).toBe('1 día');
    expect(
      formatearEdadPerfil(
        { fechaNacimiento: '2026-09-30', edadAnios: 0, mesesRestantes: 0 },
        new Date(2026, 9, 7),
      ),
    ).toBe('7 días');
  });

  it('conserva el formato en meses y años para edades mayores', () => {
    expect(
      formatearEdadPerfil(
        { fechaNacimiento: '2026-09-07', edadAnios: 0, mesesRestantes: 1 },
        new Date(2026, 9, 7),
      ),
    ).toBe('1 mes');
    expect(
      formatearEdadPerfil(
        { fechaNacimiento: '2020-04-07', edadAnios: 6, mesesRestantes: 6 },
        new Date(2026, 9, 7),
      ),
    ).toBe('6 años, 6 meses');
  });
});
