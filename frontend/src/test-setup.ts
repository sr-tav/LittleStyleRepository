// Aislamiento entre specs: jsdom comparte `localStorage` entre archivos del
// mismo worker. Sin esta limpieza, una sesión guardada por los specs de auth o
// carrito "revive" un rol en specs que asumen anonimato: con rol CLIENTE el
// Navbar evalúa `carrito.panelAbierto()` y rompe los specs cuyo mock de
// carrito no lo incluye, según el orden de ejecución.
import { afterEach } from 'vitest';

afterEach(() => {
  localStorage.clear();
});
