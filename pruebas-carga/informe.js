// Convierte el resultado de Artillery (--output informe.json) en informe.html con las métricas del
// Plan de Calidad y si cumplen su meta. Uso: node informe.js informe.json
const fs = require('node:fs');
const path = require('node:path');

const entrada = process.argv[2] || 'informe.json';
const { aggregate: a } = JSON.parse(fs.readFileSync(entrada, 'utf8'));
const c = a.counters;
const s = a.summaries;
const prefijo = 'plugins.metrics-by-endpoint.response_time.';

const peticiones = c['http.requests'] || 0;
const exitosas = Object.entries(c)
  .filter(([k]) => /^http\.codes\.2\d\d$/.test(k))
  .reduce((t, [, v]) => t + v, 0);
const errores = Object.entries(c).filter(([k]) => k.startsWith('errors.'));
const sesion = s['vusers.session_length'];
const pico = Math.max(...Object.values(a.rates || {}).map(Number), 0);
const tasaExito = peticiones ? (100 * exitosas) / peticiones : 0;

const endpoints = Object.keys(s)
  .filter((k) => k.startsWith(prefijo))
  .map((k) => ({ nombre: k.slice(prefijo.length), ...s[k] }));
const filtros = endpoints.filter((e) => e.nombre.startsWith('Catálogo') || e.nombre.startsWith('Filtro')
  || e.nombre.startsWith('Búsqueda'));
const recomendaciones = endpoints.find((e) => e.nombre === 'Recomendaciones');
const peorFiltroP95 = Math.max(...filtros.map((e) => e.p95), 0);

const ms = (v) => (v === undefined ? '—' : `${Math.round(v)} ms`);
const fila = (metrica, meta, valor, cumple) =>
  `<tr><td>${metrica}</td><td>${meta}</td><td>${valor}</td>` +
  `<td class="${cumple ? 'ok' : 'no'}">${cumple ? 'Cumple' : 'No cumple'}</td></tr>`;

const metricas = [
  fila('Tasa de procesamiento de carga simultánea', '≥ 99 % sin error con 100 usuarios',
    `${tasaExito.toFixed(2)} % (${exitosas} de ${peticiones})`, tasaExito >= 99),
  fila('Efectividad de filtros en catálogo', '< 1.5 s', `p95 del filtro más lento: ${ms(peorFiltroP95)}`,
    filtros.length > 0 && peorFiltroP95 < 1500),
  fila('Tiempo de carga de recomendaciones', '< 2 s (promedio)', ms(recomendaciones?.mean),
    recomendaciones !== undefined && recomendaciones.mean < 2000),
  fila('Consultas de recomendación sin fallos', '≥ 99 %',
    `${c['plugins.metrics-by-endpoint.Recomendaciones.codes.200'] || 0} de ${recomendaciones?.count || 0} con 200`,
    recomendaciones !== undefined
      && (c['plugins.metrics-by-endpoint.Recomendaciones.codes.200'] || 0) >= 0.99 * recomendaciones.count),
].join('\n');

const tablaEndpoints = endpoints.map((e) =>
  `<tr><td>${e.nombre}</td><td>${e.count}</td><td>${ms(e.mean)}</td><td>${ms(e.median)}</td>` +
  `<td>${ms(e.p95)}</td><td>${ms(e.p99)}</td><td>${ms(e.max)}</td></tr>`).join('\n');

const servidor = ['servidor.catalogo_ms', 'servidor.recomendacion_ms'].filter((k) => s[k]).map((k) =>
  `<tr><td>${k === 'servidor.catalogo_ms' ? 'Catálogo (X-Catalogo-Ms)' : 'Motor de recomendación (X-Recomendacion-Ms)'}</td>` +
  `<td>${s[k].count}</td><td>${ms(s[k].mean)}</td><td>${ms(s[k].p95)}</td><td>${ms(s[k].max)}</td></tr>`).join('\n');

const html = `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Informe de carga</title>
<style>
  body { font-family: system-ui, sans-serif; margin: 24px; color: #1f2937; background: #fff; }
  h1 { font-size: 22px; margin-bottom: 4px; } h2 { font-size: 17px; margin-top: 28px; }
  p { color: #4b5563; margin-top: 0; }
  table { border-collapse: collapse; width: 100%; font-size: 14px; }
  th, td { border: 1px solid #d1d5db; padding: 6px 10px; text-align: left; }
  th { background: #f3f4f6; }
  .ok { color: #047857; font-weight: 600; } .no { color: #b91c1c; font-weight: 600; }
</style></head><body>
<h1>LittleStyle: prueba de carga del catálogo y las recomendaciones</h1>
<p>Generado desde ${path.basename(entrada)} el ${new Date().toLocaleString('es-CO')}. Duración de la prueba: 3.5 minutos
(calentamiento, rampa y pico con un máximo de 100 usuarios simultáneos).</p>

<h2>Métricas del Plan de Calidad</h2>
<table><tr><th>Métrica</th><th>Meta</th><th>Resultado</th><th>Estado</th></tr>
${metricas}
</table>

<h2>Carga generada</h2>
<table>
<tr><th>Usuarios virtuales creados</th><td>${c['vusers.created'] || 0}</td></tr>
<tr><th>Usuarios completados / fallidos</th><td>${c['vusers.completed'] || 0} / ${c['vusers.failed'] || 0}</td></tr>
<tr><th>Duración media de cada usuario</th><td>${ms(sesion?.mean)}</td></tr>
<tr><th>Peticiones totales / tasa de peticiones por segundo</th><td>${peticiones} / ${pico}</td></tr>
<tr><th>Errores de red</th><td>${errores.length ? errores.map(([k, v]) => `${k.slice(7)}: ${v}`).join(', ') : 'Ninguno'}</td></tr>
</table>

<h2>Tiempo de respuesta por endpoint (visto por el usuario, incluye la red)</h2>
<table><tr><th>Endpoint</th><th>Peticiones</th><th>Promedio</th><th>Mediana</th><th>p95</th><th>p99</th><th>Máximo</th></tr>
${tablaEndpoints}
</table>

<h2>Tiempo de procesamiento en el servidor (sin la red)</h2>
<table><tr><th>Componente</th><th>Mediciones</th><th>Promedio</th><th>p95</th><th>Máximo</th></tr>
${servidor || '<tr><td colspan="5">Sin datos</td></tr>'}
</table>
</body></html>
`;

const salida = entrada.replace(/\.json$/, '') + '.html';
fs.writeFileSync(salida, html);
console.log(`Informe generado: ${salida}`);
