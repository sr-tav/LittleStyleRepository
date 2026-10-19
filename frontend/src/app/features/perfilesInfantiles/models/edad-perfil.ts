import { PerfilInfantil } from './perfil-infantil.models';

type DatosEdadPerfil = Pick<PerfilInfantil, 'fechaNacimiento' | 'edadAnios' | 'mesesRestantes'>;

/**
 * Presenta la edad usando días, meses o años según los datos calculados del perfil.
 * @param perfil Datos de nacimiento y edad del perfil infantil.
 * @param fechaReferencia Fecha local usada para calcular los días cuando la edad es menor de un mes.
 * @returns Edad con unidades y singular/plural en español.
 */
export function formatearEdadPerfil(
  perfil: DatosEdadPerfil,
  fechaReferencia = new Date(),
): string {
  if (perfil.edadAnios === 0) {
    if (perfil.mesesRestantes === 0) {
      const [anio, mes, dia] = perfil.fechaNacimiento.split('-').map(Number);
      const nacimiento = Date.UTC(anio, mes - 1, dia);
      const hoy = Date.UTC(
        fechaReferencia.getFullYear(),
        fechaReferencia.getMonth(),
        fechaReferencia.getDate(),
      );
      const dias = Math.floor((hoy - nacimiento) / 86_400_000);
      return `${dias} ${dias === 1 ? 'día' : 'días'}`;
    }
    return `${perfil.mesesRestantes} ${perfil.mesesRestantes === 1 ? 'mes' : 'meses'}`;
  }

  const anios = `${perfil.edadAnios} ${perfil.edadAnios === 1 ? 'año' : 'años'}`;
  return perfil.mesesRestantes > 0 ? `${anios}, ${perfil.mesesRestantes} meses` : anios;
}
