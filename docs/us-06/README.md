# Documentación de US-06

Esta carpeta reúne la descripción funcional, los casos de prueba y la evidencia automatizada de la gestión de cuenta y perfiles infantiles de LittleStyle.

## Qué se implementó

- **Cuenta autenticada:** consulta y edición de datos personales, cambio opcional de contraseña con verificación de la actual y eliminación de cuenta con confirmación. La baja elimina perfiles infantiles, mediciones y preferencias asociadas; anonimiza la cuenta y revoca sus sesiones.
- **Perfiles duplicados y gemelos:** se rechaza un perfil de la misma cuenta si coincide el nombre normalizado y la fecha de nacimiento; se permite compartir fecha cuando los nombres son distintos. La validación se serializa por cuenta para cubrir solicitudes simultáneas.
- **Perfiles infantiles:** alta, consulta, edición y eliminación de perfiles propios; selector del perfil activo; captura de alergias, contextura, holgura y preferencias.
- **Alta atómica:** el alta del perfil y la primera medición se realiza en una única operación transaccional. Si falla la medición, también se revierte el perfil.
- **Mediciones:** historial descendente; altas desde el historial o la edición; corrección de registros desde el historial y corrección de la medición actual desde la edición. Una medición nueva no puede tener fecha anterior ni estatura menor que la última registrada; el peso sí puede aumentar o disminuir. La corrección de un registro existente no tiene límite de tiempo. Las altas y correcciones actualizan la fecha de actualización del perfil.
- **Completitud:** se informa el porcentaje y los grupos pendientes. Si falta una preferencia, se muestra “Agrega al menos un color o estampado preferido”.

La recomendación de prendas, los pedidos y el catálogo no son parte de esta implementación. La eliminación individual de una medición tampoco está implementada.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| [`descripcion-funcional.md`](descripcion-funcional.md) | Objetivo, alcance, reglas funcionales y de validación, seguridad, persistencia/cifrado, endpoints, pantallas, trazabilidad y limitaciones. |
| [`casos-de-prueba.md`](casos-de-prueba.md) | Casos CP-US06, relación con pruebas automatizadas, resultados de suites y casos de recorrido manual aún pendientes. |
| [`../../frontend/src/app/features/perfilesInfantiles/README.md`](../../frontend/src/app/features/perfilesInfantiles/README.md) | Estructura y responsabilidades de la feature Angular, flujos de interfaz y ejecución de pruebas frontend. |

## Evidencia registrada

La ejecución automatizada anterior registrada para el 5 de octubre de 2026 quedó superada por las pruebas del 7 de octubre:

- Backend: `.\mvnw.cmd test` — 162 pruebas aprobadas; 0 fallidas, 0 errores y 0 omitidas. Incluye las pruebas de gemelos y de creación concurrente con baja.
- Frontend: desde `frontend/`, `.\node_modules\.bin\ng.cmd test --watch=false` — 95 pruebas aprobadas en 16 archivos.
- Prueba manual de integración local: crear cuenta y perfil y ejecutar `DELETE /api/auth/me` respondió HTTP 204 después de migrar la columna de estado de H2. En el ensayo anterior a la migración, la operación respondió 409 y la transacción revirtió la baja completa; por ello ese intento no eliminó el perfil.
- Las pruebas del frontend verifican método, URL y cuerpo enviado con HTTP simulado; las pruebas de integración backend verifican el contrato HTTP y la persistencia. No existe actualmente una prueba E2E que maneje un navegador contra ambos procesos reales.

Estas cifras son evidencia automatizada, no de una revisión manual en navegador. En `casos-de-prueba.md`, CP-US06-27 (accesibilidad/adaptación visual) sigue pendiente de ejecución manual y CP-US06-26 tiene pendiente completar el recorrido visual. No se afirma que esas pruebas manuales se hayan realizado.

## Actualización del 7 de octubre de 2026

Se documentaron la supresión atómica de los datos al eliminar una cuenta, el tratamiento de la columna de estado en bases H2 previas y las comprobaciones de concurrencia entre creación de perfiles y baja. La prueba local confirmó un HTTP 204 después de la migración. No se inspeccionaron datos personales de cuentas reales durante esa comprobación.
