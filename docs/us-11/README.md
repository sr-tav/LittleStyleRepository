# Documentación de US-11

Esta carpeta reúne la descripción funcional, los casos de prueba y la configuración de la integración con la pasarela de pagos Wompi en LittleStyle.

## Qué se implementó

- **Generación del pago en el backend:** `POST /api/cliente/pagos/pedidos/{pedidoId}` valida que el pedido sea del cliente, siga `PENDIENTE_PAGO` y no haya vencido; registra una `TransaccionPago` con referencia única y devuelve monto en centavos, moneda, llave pública y la **firma de integridad SHA-256** calculada en el servidor.
- **Widget de Wompi en Angular:** el botón “Pagar” de UI-12 (que US-10 dejó deshabilitado) ahora es el componente `<app-pago-wompi>`, que carga `checkout.wompi.co/widget.js` bajo demanda y abre la pasarela (UI-13/UI-14) con los datos firmados.
- **Webhook firmado:** `POST /api/pagos/wompi/eventos` recibe los eventos `transaction.updated` (Aprobado, Rechazado, Pendiente, Anulado, Error). Es público para Wompi, pero rechaza con 401 cualquier evento cuyo **checksum SHA-256** no coincida con el secreto de eventos.
- **Confirmación sin URL pública:** al volver del widget, `POST /api/cliente/pagos/verificar` consulta la transacción directamente en la API de Wompi (`GET /v1/transactions/{id}`), así que el flujo también funciona en local sin túnel.
- **Conexión con el inventario de US-10:** un pago aprobado invoca `PedidoPagoService.confirmarPago`, que descuenta stock atómicamente y pasa el pedido a `PAGADO`. Si ya no hay stock, el pedido vence o ya no estaba pendiente, la transacción queda en `REEMBOLSO_PENDIENTE`.
- **Resultado del pago (UI-15):** `/cliente/pago/resultado` muestra aprobado, en proceso (consulta de nuevo cada 4 s), rechazado (con “Reintentar pago”) o prendas agotadas con aviso de reembolso y las opciones “Continuar sin las prendas agotadas” o “Volver al catálogo”.

### Ajustes de robustez (14 de octubre de 2026)

- **Avisos con vencimiento (RNF-07):** un evento del webhook con firma válida se rechaza si su fecha tiene más de 10 minutos, así un aviso capturado no se puede reutilizar.
- **Fecha del aviso flexible (RNF-21):** se acepta en segundos, milisegundos o ISO-8601.
- **Tiempo límite con Wompi (RNF-28):** 3 s para conectar y 8 s para responder; si se agotan, el cliente ve “Intenta más tarde” y su pedido queda intacto. En el navegador, 15 s para las peticiones de pago y la carga del Widget.
- **Avisos simultáneos sin fila (RNF-05):** el webhook responde tras validar la firma y aplica el evento en un grupo de hilos; la consulta a Wompi ya no retiene la transacción de base de datos.
- **Límite de intentos (RNF-28):** máximo 5 intentos de pago por pedido (429 al superarlo).
- **Ventana de pago cerrada (RNF-18):** aviso “No se completó el pago” y botón “Reintentar pago”.
- **Trazabilidad corregida:** la firma del webhook apuntaba a RNF-10 (control de acceso por roles); ahora apunta a RNF-07, y los casos de prueba se reparten entre RNF-03, RNF-05, RNF-07, RNF-10, RNF-18, RNF-21 y RNF-28.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| [`descripcion-funcional.md`](descripcion-funcional.md) | Flujo, reglas, firmas, estados, endpoints, configuración y límites de US-11. |
| [`casos-de-prueba.md`](casos-de-prueba.md) | Casos CP-US11, pruebas JUnit/Mockito/Vitest asociadas y resultados ejecutados. |

## Configuración para ejecutar

Las llaves se obtienen en el panel de comercios de Wompi (sección *Desarrolladores*, ambiente Sandbox) y **nunca se versionan**. Se definen como variables de entorno antes de iniciar el backend:

| Variable | Valor de ejemplo (sandbox) |
|---|---|
| `WOMPI_PUBLIC_KEY` | `pub_test_...` |
| `WOMPI_INTEGRITY_SECRET` | `test_integrity_...` |
| `WOMPI_EVENTS_SECRET` | `test_events_...` |
| `WOMPI_API_URL` | `https://sandbox.wompi.co/v1` (por defecto) |
| `WOMPI_REDIRECT_URL` | Opcional. Solo con dominio público (`https://.../cliente/pago/resultado`); vacío en local |
| `PAGOS_VIGENCIA_EVENTO`, `PAGOS_TIMEOUT_CONEXION`, `PAGOS_TIMEOUT_LECTURA`, `PAGOS_MAX_INTENTOS` | Opcionales. Por defecto `PT10M`, `PT3S`, `PT8S` y `5` |

La llave privada (`prv_test_...`) no es necesaria para este flujo: el widget crea la transacción con la llave pública y la firma de integridad, y la consulta de estado es pública.

Para recibir el webhook en local hay que exponer el backend (por ejemplo `ngrok http 8080`) y registrar en Wompi la URL de eventos `https://<subdominio>/api/pagos/wompi/eventos`. Sin túnel, el pago se confirma igualmente con la consulta directa al volver del widget.

En sandbox se pueden usar las tarjetas de prueba de Wompi: `4242 4242 4242 4242` (aprobada) y `4111 1111 1111 1111` (rechazada), con cualquier fecha futura y CVC.
