# Documentación de US-06

Esta carpeta reúne la descripción funcional, los casos de prueba y la evidencia automatizada de la gestión de cuenta y perfiles infantiles de LittleStyle.

## Qué se implementó

- **Cuenta autenticada:** consulta y edición de datos personales, cambio opcional de contraseña con verificación de la actual y desactivación lógica con confirmación. La barra de navegación abre “Mi cuenta” o “Cerrar sesión” desde el menú del usuario.
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

La ejecución automatizada registrada para el 5 de octubre de 2026 es:

- Backend: `.\mvnw.cmd test` — 76 pruebas aprobadas, 0 fallidas, 0 errores y 0 omitidas.
- Frontend: desde `frontend/`, `npm.cmd test -- --watch=false` — 74 pruebas aprobadas en 12 archivos, 0 fallidas.
- Frontend: desde `frontend/`, `npm.cmd run build` — build Angular completado.

Estas cifras son evidencia automatizada, no de una revisión manual en navegador. En `casos-de-prueba.md`, CP-US06-27 (accesibilidad/adaptación visual) sigue pendiente de ejecución manual y CP-US06-26 tiene pendiente completar el recorrido visual. No se afirma que esas pruebas manuales se hayan realizado.

## Resumen de documentación completada el 5 de octubre de 2026

Se dejó registrado el total backend de 76 pruebas automatizadas aprobadas y se añadió este índice para localizar el alcance, los resultados y las limitaciones de evidencia. También se aclaró en la descripción funcional que las mediciones existentes sí se pueden corregir; lo que no está disponible es eliminarlas individualmente.
