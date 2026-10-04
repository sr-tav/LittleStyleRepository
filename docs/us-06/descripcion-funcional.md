# US-06: Gestión integral del perfil infantil

## Objetivo y alcance

La US-06 implementa, dentro del Proceso 1 de LittleStyle, la gestión por parte del cliente de perfiles infantiles asociados a su propia cuenta. El alcance observable en el código comprende el registro, consulta, actualización y eliminación de perfiles; el registro de mediciones y consulta del historial; la captura de alergias y preferencias; y la selección local de un perfil activo en la interfaz.

El propósito del Proceso 1, según la Definición del Proyecto, es mantener información estructurada de varios menores —incluidos datos físicos, historial de crecimiento, preferencias y restricciones textiles— y dejarla disponible para funcionalidades posteriores. La recomendación personalizada de prendas no forma parte de la implementación funcional de esta historia: UI-5 muestra el control “Recomendaciones” deshabilitado como elemento próximo.

El flujo empieza cuando un cliente autenticado consulta, crea, selecciona o edita uno de sus perfiles. Los datos se validan antes de persistirse. La historia no implementa edición ni eliminación individual de mediciones: las mediciones se agregan y se consultan como historial; eliminar un perfil elimina sus mediciones en cascada.

## Actor e historia de usuario

**Actor principal:** padre, madre o acudiente con rol `CLIENTE` y sesión autenticada.

**Historia:** como cliente, quiero crear y mantener perfiles infantiles con sus medidas, alergias y preferencias, para consultar su información e historial de crecimiento y tenerla preparada para futuras funciones de recomendación.

Los roles `VENDEDOR` y `ADMINISTRADOR` no tienen autorización para las rutas `/api/cliente/**`.

## Funcionalidades implementadas

### Perfiles y mediciones

- Crear, listar, consultar por identificador, actualizar y eliminar perfiles infantiles.
- Limitar el número de perfiles por cliente mediante `app.perfiles.max`; el valor predeterminado en código es 10. El servicio rechaza una nueva creación si el cliente ya alcanzó ese límite.
- Registrar mediciones con fecha, estatura y peso.
- Consultar el historial de mediciones en orden descendente por fecha.
- Mostrar en la respuesta del perfil la medición más reciente, calculada por fecha y, en caso de empate, por identificador.
- Eliminar las mediciones asociadas al eliminar un perfil mediante la relación JPA con cascada y `orphanRemoval`.

No existe endpoint para actualizar o eliminar una medición individual.

### Alergias

El campo `alergias` acepta un conjunto de opciones cerradas:

`FIBRAS_SINTETICAS`, `LANA`, `TINTES`, `BROCHES_METALICOS`, `ALGODON`, `LATEX_ELASTICO`, `NIQUEL`, `CUERO`, `FORMALDEHIDO` y `OTRA`.

`otraAlergia` admite hasta 60 caracteres y debe tener contenido no vacío cuando se selecciona `OTRA`. Debe ser `null` cuando `OTRA` no está seleccionada. Si `sinAlergias` es verdadero, `alergias` debe estar vacío y `otraAlergia` debe ser `null`; en caso contrario debe declararse al menos una alergia.

### Preferencias

- `coloresPreferidos` es un conjunto de opciones enumeradas: `ROSA`, `LILA`, `AZUL`, `CELESTE`, `VERDE`, `AMARILLO`, `ROJO`, `NARANJA`, `MORADO`, `BLANCO`, `NEGRO`, `GRIS`, `BEIGE` y `CAFE`.
- `estampadosPreferidos` es un conjunto de opciones enumeradas: `LISO`, `FLORES`, `ESTRELLAS`, `RAYAS`, `CUADROS`, `LUNARES`, `ANIMALES`, `DIBUJOS_ANIMADOS`, `GEOMETRICO` y `MARINERO`.
- La interfaz permite seleccionar hasta diez opciones de cada categoría y deshabilita opciones nuevas al alcanzar el límite. El backend valida también el máximo de diez.
- `otroColor` y `otroEstampado` son opcionales. Cada campo admite de 2 a 30 letras y espacios. El servidor normaliza el texto y, si coincide con una opción enumerada ignorando mayúsculas, espacios, guiones bajos y diacríticos, lo incorpora al conjunto enumerado y deja el texto libre en `null`.
- La holgura es uno de `AJUSTADA`, `REGULAR` o `HOLGADA`; la contextura es `DELGADA`, `MEDIA` o `ROBUSTA`.

Los DTO utilizan conjuntos (`Set`), por lo que su representación no conserva duplicados. Los valores enum desconocidos en el JSON producen HTTP 400 con un error asociado al campo reconocido por el manejador del módulo.

### Completitud y perfil activo

`CalculadoraCompletitud` evalúa ocho grupos de información: nombre, fecha de nacimiento, contextura, holgura, estatura de la última medición, peso de la última medición, declaración de alergias (incluida la declaración explícita de ausencia) y preferencias (al menos una opción enumerada o libre). La respuesta incluye `porcentajeCompletitud`, calculado como campos diligenciados sobre ocho y redondeado al entero más cercano. La lista muestra “Completo” desde 90 % y “Incompleto” por debajo de ese umbral.

En frontend, `PerfilActivoService` mantiene el perfil seleccionado en un signal y persiste su identificador en `localStorage` con la clave `littleStyle.perfilActivoId`. Al sincronizar la lista valida que el identificador guardado aún exista; si no existe, selecciona el primer perfil y actualiza la persistencia. Sin perfiles, deja el estado activo en `null`. El selector abierto desde “Cambiar perfil” permite elegir un menor y volver al inicio.

### Talla provisional

`tallaProvisional(edadAnios)` calcula una talla orientativa en intervalos pares de dos años, con tope 16. Para edad no finita o negativa retorna 0. El propio código la describe como **provisional hasta US-07**. No es el resultado de un motor de recomendación ni debe presentarse como recomendación de talla.

## Reglas de validación

### Perfil

- Nombre obligatorio, no vacío tras quitar espacios y máximo 60 caracteres.
- Fecha de nacimiento obligatoria, no futura y correspondiente a una persona menor de 18 años.
- Contextura y holgura obligatorias y limitadas a sus enumeraciones.
- Se debe declarar al menos una alergia o marcar `sinAlergias`.
- Hasta diez alergias; `OTRA` requiere texto de hasta 60 caracteres. El texto no aplica sin `OTRA`.
- Colores y estampados son conjuntos enumerados, obligatorios como colecciones (pueden estar vacíos) y limitados a diez por categoría.
- Las preferencias libres opcionales deben contener de 2 a 30 caracteres compuestos por letras y espacios.

### Mediciones

La fecha de medición es obligatoria, no puede ser futura ni anterior a la fecha de nacimiento. Los límites generales son estatura de 40 a 200 cm y peso de 2 a 120 kg. Además, se aplican los siguientes intervalos inclusivos según la edad en años cumplidos a la fecha de medición:

| Edad | Estatura | Peso |
|---|---:|---:|
| Menor de 2 años | 40–100 cm | 2–20 kg |
| De 2 a 5 años | 60–130 cm | 5–40 kg |
| De 6 a 9 años | 80–160 cm | 10–80 kg |
| De 10 a 13 años | 100–190 cm | 15–120 kg |
| 14 años o más (el perfil siempre es menor de 18) | 120–200 cm | 25–120 kg |

El validador de backend comprueba los límites generales y los del tramo etario. La interfaz reproduce las comprobaciones y rangos en el formulario; el backend vuelve a validar antes de guardar.

## Seguridad, propiedad y persistencia

### Autenticación y autorización

Las rutas del controlador están bajo `/api/cliente/perfiles`; `SecurityConfig` exige rol `CLIENTE` para `/api/cliente/**`. El servicio obtiene la identidad del usuario autenticado mediante `SecurityUtils.usuarioActual()`; el cliente no suministra `clienteId` en el cuerpo de perfil. Las consultas de perfiles utilizan `findByIdAndClienteId`, y las consultas del historial filtran por el identificador del perfil y el cliente propietario.

Un identificador que no pertenece al cliente autenticado se trata como no encontrado y devuelve HTTP 404, tanto para las operaciones sobre el perfil como para sus mediciones. Una solicitud sin autenticación devuelve HTTP 401 y los roles `VENDEDOR` y `ADMINISTRADOR` reciben HTTP 403.

### Cifrado AES-GCM

`CifradoPerfil` utiliza `AES/GCM/NoPadding`, un IV aleatorio de 12 bytes y una etiqueta GCM de 128 bits. La clave se configura desde la propiedad `app.crypto.perfil-key`, asociada a la variable de entorno **`PERFIL_ENC_KEY`**. El código exige una clave Base64 que decodifique a 32 bytes. Este documento no incluye valores de claves.

| Dato persistido | Estado según las conversiones JPA |
|---|---|
| Nombre (`nombre`) | Cifrado con `NombreCifradoConverter`. |
| Fecha de nacimiento (`fecha_nacimiento`) | Cifrada con `FechaNacimientoConverter`. |
| Contextura (`contextura`), holgura (`holgura`) y declaración `sin_alergias` | Serializados como texto y cifrados con `TextoLegadoCifradoConverter`, que admite leer sus valores previos en claro para migrarlos. |
| Conjunto de alergias (`alergias_cifradas`) | Serializado y cifrado con `AlergiasConverter`. |
| Descripción de otra alergia (`otra_alergia_cifrada`) | Cifrada con `StringCifradoConverter`. |
| Conjuntos de colores y estampados preferidos | Cada opción de las colecciones JPA se cifra con `TextoLegadoCifradoConverter`, que admite leer los códigos enum previos en claro para migrarlos. |
| Otro color (`otro_color_cifrado`) | Cifrado con `StringCifradoConverter`. |
| Otro estampado (`otro_estampado_cifrado`) | Cifrado con `StringCifradoConverter`. |
| Fecha de medición (`fecha_medicion`) | Cifrada con `FechaMedicionConverter`; el historial se ordena por fecha descifrada en el servicio. |
| Estatura (`estatura_cifrada`) y peso (`peso_cifrado`) | Cifrados como texto decimal con `BigDecimalCifradoConverter`. |
| Identificadores, relación con cliente y fechas de creación/actualización | Permanecen sin cifrar como metadatos operativos de relación y auditoría. |

Los valores personales y de salud se cifran mediante converters JPA con AES-GCM. Los IDs, la relación de propiedad y las fechas operativas de creación/actualización se conservan como metadatos relacionales; no contienen los campos de perfil que se muestran al cliente. Las fechas cifradas se ordenan luego de descifrarse en el servicio; no se usa el orden SQL para el historial. Con `app.perfiles.cifrado.migrar-datos=true`, `PerfilDatosCifradoMigracion` actualiza de forma transaccional, una sola vez, filas legadas en claro o con el formato cifrado anterior; `PerfilDatosCifradoVersion` conserva la marca JPA para evitar repetirla. La ejecución de producción se limita a una instancia durante una ventana de mantenimiento.

El módulo de perfiles y las páginas frontend examinadas no contienen llamadas propias a `logger`, `System.out` o `console` para emitir datos del menor. Esto no equivale a una garantía frente a cualquier excepción: el manejador global puede registrar excepciones no controladas y URI de solicitudes. El transporte HTTPS/TLS no lo configura este módulo; su cumplimiento depende de la configuración del despliegue y no se certifica aquí.

## Endpoints

Todas las rutas requieren rol `CLIENTE` y autenticación conforme a `SecurityConfig`. `{id}` identifica un perfil propiedad del usuario autenticado.

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| `GET` | `/api/cliente/perfiles` | `CLIENTE` | Lista perfiles propios, ordenados por fecha de actualización descendente. |
| `GET` | `/api/cliente/perfiles/{id}` | `CLIENTE` | Consulta un perfil propio y su medición más reciente. |
| `POST` | `/api/cliente/perfiles` | `CLIENTE` | Crea un perfil, sujeto al límite de perfiles y validaciones. Responde 201. |
| `PUT` | `/api/cliente/perfiles/{id}` | `CLIENTE` | Actualiza el perfil propio; responde 200. |
| `DELETE` | `/api/cliente/perfiles/{id}` | `CLIENTE` | Elimina el perfil propio y sus mediciones en cascada; responde 204. |
| `POST` | `/api/cliente/perfiles/{id}/mediciones` | `CLIENTE` | Agrega una medición validada al perfil; responde 201. |
| `GET` | `/api/cliente/perfiles/{id}/mediciones` | `CLIENTE` | Lista el historial propio en orden descendente de fecha. |

## Pantallas

| Interfaz | Comportamiento que existe |
|---|---|
| UI-3, inicio de cliente | Saludo y acceso a Mis hijos; muestra el perfil activo con edad, última estatura y talla provisional, o invitación si no existen perfiles. “Cambiar perfil” abre una selección de perfiles. Catálogo, pedidos y carrito son accesos deshabilitados sin ruta funcional. |
| UI-4, lista de perfiles | Presenta perfiles con avatar SVG, edad/talla, completitud, medidas, contextura, alergias y preferencias disponibles. Tiene acciones para ver, editar y un único CTA para agregar. En modo selección presenta opciones y retorna al inicio tras elegir. |
| UI-5, detalle/formulario | Muestra información básica, medidas actuales, preferencias, restricciones e historial descendente. Permite agregar mediciones, crear/editar el perfil y eliminarlo tras confirmación. El control “Recomendaciones” está deshabilitado. El alta envía primero el perfil y luego la medición inicial; si esta falla, permite reintentarla sin volver a crear el perfil. |

## Trazabilidad de requisitos no funcionales

La columna de prueba identifica una prueba existente que respalda parte del mecanismo. No afirma que se hayan medido los porcentajes poblacionales especificados en el Plan de Calidad.

| RNF | Mecanismo implementado | Prueba automatizada asociada | Alcance no demostrado |
|---|---|---|---|
| RNF-06, confidencialidad | Converters AES-GCM para datos personales, físicos, alergias y preferencias, migración de filas legadas, identidad y propiedad filtradas por usuario; sin logs explícitos de datos del menor en el módulo. | `CifradoPerfilConverterTest.cifraYDescifraFechaNacimientoSinGuardarTextoPlano`, `cifraYDescifraFechaMedicionSinGuardarTextoPlano`, `cifraYDescifraValoresDecimales`, `cifraYDescifraTextoLibreSinGuardarElValorEnClaro`, `cifraYDescifraAlergias`, `rechazaCifradoAlteradoEnLugarDeTratarloComoTextoPlano`; `PerfilInfantilIntegrationTest.crudMedicionesYEliminacionEnCascada`, `migraFilasLegadasYConservaLaLecturaDelPerfilYElHistorial`, `seguridadRequiereTokenYRechazaRolesDistintosDeCliente`; `PerfilInfantilServiceTest.ocultaPerfilesDeOtrosClientesComoNoEncontrados`. | La prueba no mide el umbral >96% del plan. TLS depende del despliegue y del esquema actualizado al desplegar. |
| RNF-12, completitud | El DTO recoge datos básicos, declaración de alergias, preferencias y perfil; las mediciones se representan por separado. `CalculadoraCompletitud` computa ocho grupos y UI-4 muestra la insignia desde 90%. | `CalculadoraCompletitudTest.consideraElPerfilCompletoCuandoTieneDatosYMedicion`, `marcaComoDiligenciadaLaDeclaracionExplicitaDeNoAlergias`, `consideraLaPreferenciaLibreComoPreferenciaDiligenciada`; `perfiles-lista.spec.ts` (“marca Incompleto cuando la completitud es menor al 90 por ciento”). | No se mide qué porcentaje de todos los perfiles reales supera el 90% solicitado por el plan. |
| RNF-16, prevención de errores en medidas | Validaciones de formulario Angular y `ValidadorMedidas` para fechas, rangos generales y coherencia por edad; el servidor repite la validación. | `ValidadorMedidasTest.aceptaMedidasCoherentesConLaEdad`, `rechazaMedicionAnteriorAlNacimientoEnElCampoCorrespondiente`, `rechazaEstaturaFueraDelRangoDelTramoEtario`; `PerfilInfantilIntegrationTest.validaPerfilYMedicionYNoExponePerfilAjeno`; `perfil-detalle.spec.ts` (“valida los datos obligatorios y alergias antes de enviar”). | No se ha medido la tasa de errores de captura (<5%) ni cada límite de cada tramo tiene una prueba unitaria dedicada. |
| RNF-19, facilidad de actualización | Consulta y edición en UI-5, registro de nuevas medidas y vista del historial ordenado; el backend valida las mediciones y mantiene la más reciente. | `PerfilInfantilIntegrationTest.crudMedicionesYEliminacionEnCascada`; `perfil-detalle.spec.ts` (“actualiza el perfil existente y vuelve al listado sin crear otro”); `perfil-detalle.spec.ts` (“presenta cuatro tarjetas de información en el modo detalle”). | No se mide que más del 60% de perfiles se actualice en menos de seis meses. No existe edición/eliminación individual de mediciones. |

## Supuestos y limitaciones

- El rol y el identificador propietario se obtienen de la sesión autenticada; la aplicación frontend envía el token mediante la infraestructura común de autenticación.
- El máximo se configura con `app.perfiles.max` y su valor predeterminado en configuración/código es 10; el despliegue puede sobrescribirlo.
- Las colecciones de colores y estampados admiten cero opciones; las preferencias libres también son opcionales.
- La talla presentada es únicamente una aproximación provisional hasta US-07.
- El módulo todavía no implementa recomendaciones, catálogo, carrito ni pedidos.
- Las fechas de nacimiento y medición se devuelven como valores de fecha en respuestas API; ambas se almacenan mediante converters AES-GCM. Los atributos personales y de salud del perfil también se cifran; los IDs/relaciones y timestamps de auditoría permanecen operativos.
- La codificación de la clave y la protección de tráfico en despliegue son responsabilidades de configuración externa; este documento no incluye ni evalúa valores de credenciales.
- Los cambios de tipo/longitud de columnas deben aplicarse mediante el perfil JPA de migración `perfiles-migracion` junto con `prod`, deteniendo previamente todas las instancias y respaldando la base. Tras una ejecución y migración de datos en una única instancia, se reinicia únicamente con `prod` (modo `validate`); no se agregan scripts DDL manuales.
- RNF-06, RNF-12, RNF-16 y RNF-19 contienen métricas cuantitativas de sistema/uso. Las pruebas de código citadas verifican mecanismos y casos, no esas métricas agregadas.
