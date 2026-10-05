# Feature frontend: perfiles infantiles

Feature Angular de US-06 para consultar, crear, editar y eliminar perfiles infantiles, mantener el perfil activo y registrar mediciones de crecimiento.

## Estructura

```text
perfilesInfantiles/
├── models/
│   ├── perfil-infantil.models.ts
│   ├── perfil-opciones.ts
│   ├── talla-provisional.ts
│   └── talla-provisional.spec.ts
├── pages/
│   ├── perfiles-lista.ts
│   ├── perfiles-lista.html
│   ├── perfiles-lista.spec.ts
│   ├── perfil-detalle.ts
│   ├── perfil-detalle.html
│   └── perfil-detalle.spec.ts
├── services/
│   ├── perfiles.service.ts
│   ├── perfil-activo.service.ts
│   └── perfil-activo.service.spec.ts
└── perfiles.routes.ts
```

## Responsabilidades

| Archivo/clase | Responsabilidad |
|---|---|
| `perfiles.routes.ts` | Declara la ruta de lista y la ruta de detalle/formulario con carga diferida. |
| `models/perfil-infantil.models.ts` | Tipos TypeScript de perfiles, mediciones, solicitudes y conjuntos enum. |
| `models/perfil-opciones.ts` | Opciones disponibles y etiquetas en español para alergias, colores y estampados; contiene las muestras visibles de color. |
| `models/talla-provisional.ts` | Función pura `tallaProvisional(edadAnios)`; el comentario del código la identifica como provisional hasta US-07. |
| `services/perfiles.service.ts` | Cliente HTTP para los endpoints de perfiles y mediciones. |
| `services/perfil-activo.service.ts` | Signal del perfil activo y persistencia/validación del ID en `localStorage`. |
| `pages/perfiles-lista.ts` / `.html` | Lista, estados de carga/error/vacío, completitud, etiquetas, acciones de administración y selector tipo lista cuando se llega con `modo=seleccionar`. |
| `pages/perfil-detalle.ts` / `.html` | Detalle, formularios de alta/edición, validaciones, alergias/preferencias, mediciones, historial y confirmación de eliminación. |
| `*.spec.ts` | Pruebas unitarias de modelos/servicios y pruebas de componentes con infraestructura HTTP de prueba. |

La pantalla de inicio de cliente que presenta “Perfil activo” y el acceso “Cambiar perfil” reside en `frontend/src/app/features/home/pages/cliente-inicio`, fuera de este directorio feature.
La gestión de cuenta asociada a US-06 está en `frontend/src/app/features/cuenta/pages/` y se abre desde “Mi cuenta” en la barra de navegación.

## Flujos principales

- La lista normal expone un único acceso para agregar; cada tarjeta permite ver o editar.
- “Cambiar perfil” navega a la lista con `modo=seleccionar`. Ese modo presenta perfiles como botones de selección, sin acciones de administración. Al elegir, guarda el ID y regresa a `/cliente`.
- Al entrar a UI-5 se consultan el perfil y sus mediciones; la interfaz ordena la colección de historial por fecha descendente.
- El frontend recibe valores legibles desde la API autenticada; el cifrado en reposo y la migración de datos existentes se realizan en backend/JPA. No se persisten fichas infantiles en `localStorage`.
- El alta valida ambos formularios y envía perfil y primera medición en una sola solicitud transaccional. Si la medición no puede persistirse, backend revierte también el perfil.
- La edición envía `PUT`; las mediciones se pueden agregar desde la edición o desde el historial. Una alta nueva no admite una fecha anterior ni una estatura menor que la última; el peso sí puede variar. Las correcciones individuales se pueden hacer sin límite de tiempo desde el historial o sobre la última medición en edición, y usan `PUT` sin enviar el formulario del perfil. La eliminación del perfil requiere confirmación en diálogo.
- Los perfiles incompletos muestran los grupos pendientes; si falta una preferencia, la indicación es “Agrega al menos un color o estampado preferido”.
- Alergias se eligen con checkboxes; “Sin alergias” limpia y deshabilita las opciones. “Otra alergia” revela un campo obligatorio. Colores y estampados se eligen en chips con límite de diez por categoría.

La talla mostrada es una aproximación calculada por edad, no una recomendación de producto; el código la marca como provisional hasta US-07.

## Ejecución de pruebas

Desde `frontend/`:

```sh
ng test --watch=false
```

También puede usarse el comando configurado en los scripts del paquete del frontend. Las pruebas de esta feature están en `models/`, `services/` y `pages/`; una prueba de UI-3 relacionada con el acceso “Cambiar perfil” se encuentra en `features/home/pages/cliente-inicio/cliente-inicio.spec.ts`. La página de cuenta y sus pruebas están en `features/cuenta/pages/`.

## Decisiones de diseño

- Los modelos tipan alergias, colores y estampados como uniones de literales coincidentes con los enums backend.
- Las etiquetas de opción se centralizan y se reutilizan en lista y detalle para no mostrar códigos internos.
- El ID del perfil activo se persiste; no se guarda la ficha del menor en `localStorage`.
- La llamada a API usa el `environment.apiUrl` y los mecanismos HTTP comunes de la aplicación.
- El cliente Angular depende del backend configurado en `environment.apiUrl`; para la verificación local, el perfil backend `local` usa H2. No se debe iniciar el backend con perfiles `dev` o `prod` para esta comprobación local.
- El bloqueo de doble envío y los mensajes asociados a errores del servidor se implementan en el componente de detalle; los formularios no se reinician tras un error general.
- No hay cliente/servidor frontend de recomendación, catálogo, pedidos o carrito implementado por esta feature.
