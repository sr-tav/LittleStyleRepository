import { NavigationError } from '@angular/router';

import { recargarSiCambioLaVersion } from './app.config';

function fallo(mensaje: string, url = '/vendedor/inventario'): NavigationError {
  return new NavigationError(1, url, new TypeError(mensaje));
}

describe('recargarSiCambioLaVersion', () => {
  beforeEach(() => sessionStorage.clear());

  it('recarga hacia la URL destino cuando falta un chunk de una versión anterior', () => {
    const recargar = vi.fn();

    recargarSiCambioLaVersion(fallo('Failed to fetch dynamically imported module: /chunk-VIEJO.js'), recargar, 1000);

    expect(recargar).toHaveBeenCalledWith('/vendedor/inventario');
  });

  it('no vuelve a recargar la misma URL enseguida, para no entrar en un ciclo', () => {
    const recargar = vi.fn();
    const evento = fallo('Failed to fetch dynamically imported module: /chunk-VIEJO.js');

    recargarSiCambioLaVersion(evento, recargar, 1000);
    recargarSiCambioLaVersion(evento, recargar, 5000);
    recargarSiCambioLaVersion(evento, recargar, 60_000);

    expect(recargar).toHaveBeenCalledTimes(2);
  });

  it('ignora otros errores de navegación', () => {
    const recargar = vi.fn();

    recargarSiCambioLaVersion(fallo('Cannot match any routes'), recargar, 1000);

    expect(recargar).not.toHaveBeenCalled();
  });
});
