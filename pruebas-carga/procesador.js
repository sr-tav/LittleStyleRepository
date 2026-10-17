// Funciones de apoyo para Artillery (pruebas-carga/catalogo-recomendaciones.yml).

/** Datos únicos para la cuenta de la prueba: cada ejecución crea su propio cliente. */
function datosDeCuenta(context, events, done) {
  const marca = Date.now();
  context.vars.email = `carga.${marca}@littlestyle.test`;
  context.vars.telefono = '3' + String(marca).slice(-9);
  context.vars.password = `Carga${marca}`;
  context.vars.hoy = new Date().toISOString().slice(0, 10);
  done();
}

/**
 * Registra el tiempo que el backend reporta en sus cabeceras (sin la red):
 * X-Catalogo-Ms y X-Recomendacion-Ms.
 */
function medirServidor(requestParams, response, context, events, done) {
  const catalogo = response.headers['x-catalogo-ms'];
  const recomendacion = response.headers['x-recomendacion-ms'];
  if (catalogo !== undefined) events.emit('histogram', 'servidor.catalogo_ms', Number(catalogo));
  if (recomendacion !== undefined) events.emit('histogram', 'servidor.recomendacion_ms', Number(recomendacion));
  done();
}

module.exports = { datosDeCuenta, medirServidor };
