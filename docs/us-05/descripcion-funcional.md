# US-05: Módulo de autenticación, registro y autorización de usuarios

## Objetivo y alcance

La US-05 implementa la base de seguridad de LittleStyle: el registro público de clientes y vendedores, el inicio de sesión con correo y contraseña, la emisión y validación de JSON Web Tokens (JWT) y el control de acceso por rol tanto en el backend como en el frontend. Es una historia transversal: el resto de módulos (por ejemplo, US-06 bajo `/api/cliente/**`) dependen de la identidad y el rol que esta historia establece.

La decisión de arquitectura que la sustenta es el **ADR-04**: seguridad sin estado (_stateless_) mediante JWT integrados con Spring Security, para que una SPA en Angular pueda autenticarse sin sesión en el servidor. La historia también aplica el patrón de cadena de filtros de Spring Security (ADR-17, citado en `SecurityConfig`) y, en el frontend, guards de ruta e interceptor HTTP según la consecuencia descrita en la decisión de usar una SPA.

El flujo comienza cuando un visitante se registra (UI-2) o inicia sesión (UI-1). El backend valida los datos, verifica la contraseña contra su hash BCrypt y devuelve un JWT firmado junto con los datos públicos del usuario. El frontend guarda la sesión, adjunta el token en cada petición a la API y redirige al usuario al inicio correspondiente a su rol.

Fuera del alcance implementado: recuperación de contraseña (el enlace de UI-1 solo muestra un aviso de “próximamente”), envío de correo de bienvenida, refresh tokens, cierre de sesión en servidor y la administración de cuentas (suspender, cambiar rol), que corresponde a US-13.

## Actores e historia de usuario

**Actores:**

- **Visitante:** persona sin sesión que puede registrarse o iniciar sesión.
- **Cliente** (`CLIENTE`): padre, madre o acudiente que gestiona perfiles infantiles y compra.
- **Vendedor** (`VENDEDOR`): tienda que publica prendas; se registra indicando el nombre de su tienda.
- **Administrador** (`ADMINISTRADOR`): no puede registrarse desde el formulario público; su cuenta inicial se crea por configuración.

**Historia:** como visitante, quiero crear una cuenta como cliente o vendedor e iniciar sesión de forma segura, para acceder únicamente a las funciones que corresponden a mi rol.

## Funcionalidades implementadas

### Registro (`POST /api/auth/register`)

- Permite registrar cuentas con rol `CLIENTE` o `VENDEDOR`. Un registro con rol `ADMINISTRADOR` se rechaza con HTTP 400 asociado al campo `rol`; un rol desconocido produce HTTP 400 por cuerpo no válido.
- El correo se normaliza (sin espacios laterales y en minúsculas, `Locale.ROOT`) antes de comprobar duplicados y guardarlo. Un correo ya registrado responde HTTP 409 con el error asociado al campo `email`.
- La contraseña se almacena únicamente como hash **BCrypt** (`BCryptPasswordEncoder`); nunca se guarda en texto plano.
- `confirmarPassword` debe coincidir con `password`; en caso contrario, HTTP 400 asociado a `confirmarPassword`.
- Para `VENDEDOR`, `nombreTienda` es obligatorio. Para `CLIENTE` se ignora y se guarda `null`.
- `aceptaTerminos` debe ser verdadero. Al registrar, el backend guarda la versión de los términos aceptada (`version_terminos`, actualmente `"1.0"`) y la fecha de aceptación (`fecha_aceptacion_terminos`) como evidencia de la autorización de tratamiento de datos (Ley 1581 de 2012, art. 9).
- La cuenta se crea con estado `ACTIVO` y fecha de registro. La respuesta es HTTP 201 con un JWT, de modo que el usuario queda autenticado de inmediato.

### Inicio de sesión (`POST /api/auth/login`)

- Normaliza el correo y delega la verificación en el `AuthenticationManager` (`DaoAuthenticationProvider` + `UsuarioDetailsService` + BCrypt).
- Credenciales incorrectas o correo inexistente responden HTTP 401 con el mensaje genérico “Correo o contraseña incorrectos”, sin revelar cuál de los dos falló.
- Una cuenta `SUSPENDIDO` se marca como deshabilitada en `UsuarioDetailsService` y el login responde HTTP 403 con “La cuenta se encuentra suspendida. Contacte al administrador”.
- Si las credenciales son válidas responde HTTP 200 con el JWT y los datos públicos del usuario.

### Perfil autenticado (`GET /api/auth/me`)

Devuelve los datos públicos (`UsuarioResponse`) del usuario dueño del token. Requiere autenticación; sin token responde HTTP 401.

### Emisión y validación de JWT

`JwtService` emite tokens firmados con HMAC-SHA usando la clave Base64 configurada en `app.jwt.secret` (variable de entorno **`JWT_SECRET`**). El token contiene:

| Claim | Contenido |
|---|---|
| `sub` | Correo normalizado del usuario. |
| `uid` | Identificador del usuario. |
| `rol` | `CLIENTE`, `VENDEDOR` o `ADMINISTRADOR`. |
| `nombre` | Nombre del usuario. |
| `iat` / `exp` | Fecha de emisión y de expiración. |

La vigencia se configura con `app.jwt.expiration-ms` (variable `JWT_EXPIRATION_MS`); el valor predeterminado es 28 800 000 ms (**8 horas**). La respuesta de login/registro (`AuthResponse`) incluye `token`, `tipo` (`"Bearer"`), `expiraEn` (epoch en milisegundos) y `usuario`.

`JwtAuthenticationFilter` lee el encabezado `Authorization: Bearer <token>` en cada petición:

- Si falta o no usa el prefijo `Bearer `, la petición continúa sin autenticar.
- Si el token es válido (firma y expiración) **y la cuenta sigue activa**, coloca en el `SecurityContext` un `AuthenticatedUser(id, email, nombre, rol)` con la autoridad `ROLE_<ROL>`.
- La comprobación de estado (`EstadoCuentaService.estaActiva`) consulta solo la columna `estado` del usuario. Así, suspender una cuenta invalida de inmediato los tokens ya emitidos, en lugar de esperar a que expiren (mitiga la consecuencia de revocación señalada en el ADR-04).
- Si el token expiró, está alterado, mal formado o pertenece a una cuenta suspendida o inexistente, la petición queda sin autenticar y el punto de entrada responde HTTP 401 con la causa: “La sesión ha expirado, inicie sesión nuevamente”, “Token de autenticación inválido” o el mensaje de cuenta suspendida.

`SecurityUtils.usuarioActual()` expone el usuario autenticado a los servicios de cualquier módulo; US-06 lo usa para filtrar los perfiles por propietario.

### Autorización por rol (backend)

`SecurityConfig` define una cadena sin estado (`SessionCreationPolicy.STATELESS`), con CSRF, `formLogin` y `httpBasic` deshabilitados:

| Ruta | Acceso |
|---|---|
| `OPTIONS /**` | Público (preflight CORS). |
| `POST /api/auth/login`, `POST /api/auth/register` | Público. |
| `/error` | Público. |
| `/h2-console/**` | Público solo si `spring.h2.console.enabled=true` (perfil `local`). |
| `/api/admin/**` | Rol `ADMINISTRADOR`. |
| `/api/vendedor/**` | Rol `VENDEDOR`. |
| `/api/cliente/**` | Rol `CLIENTE`. |
| Cualquier otra ruta | Usuario autenticado. |

Las respuestas de seguridad se devuelven en JSON con el formato común `ErrorResponse`: `RestAuthenticationEntryPoint` produce HTTP 401 y `RestAccessDeniedHandler` produce HTTP 403 (“No tiene permisos para acceder a este recurso”). `@EnableMethodSecurity` queda habilitado para reglas por método en historias posteriores.

CORS se limita a `/api/**`, a los orígenes de `app.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`; por defecto `http://localhost:4200`), a los métodos `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` y a los encabezados `Authorization` y `Content-Type`.

### Administrador inicial

`AdminInitializer` crea la cuenta `ADMINISTRADOR` al arrancar solo si está definida la propiedad `app.admin.email` (y `app.admin.password`), y únicamente si el correo no existe. En el repositorio solo el perfil `local` la define, con credenciales de desarrollo. La cuenta inicial no registra aceptación de términos (`version_terminos` nulo), porque no se crea desde el registro público.

### Manejo de errores

`GlobalExceptionHandler` convierte las excepciones en `ErrorResponse` (`timestamp`, `status`, `error`, `mensaje`, `path`, `errores`):

| Situación | HTTP | Detalle |
|---|---|---|
| Validación de Bean Validation | 400 | `errores` por campo; si un campo incumple varias reglas, el mensaje de obligatorio tiene prioridad. |
| JSON ilegible o enum desconocido | 400 | “El cuerpo de la petición no es válido”. |
| `BusinessException` (reglas de negocio) | Según la excepción | Asociado al campo indicado (`rol`, `confirmarPassword`, `nombreTienda`, `email`). |
| Credenciales incorrectas | 401 | “Correo o contraseña incorrectos”. |
| Cuenta deshabilitada en login | 403 | Mensaje de cuenta suspendida. |
| Conflicto de integridad en base de datos | 409 | Mensaje genérico de conflicto. |
| Error no controlado | 500 | Se registra en el log con la URI; al cliente se devuelve un mensaje genérico. |

## Reglas de validación

Las reglas del backend (`RegistroRequest`, `LoginRequest`, `AuthService`) se replican en el frontend (`auth.validators.ts`) para dar retroalimentación inmediata; el backend vuelve a validar siempre.

### Registro

| Campo | Regla |
|---|---|
| `nombre`, `apellido` | Obligatorios, de 2 a 60 caracteres, solo letras (incluidas tildes y ñ) y espacios. |
| `email` | Obligatorio, formato de correo válido, máximo 120 caracteres, único tras normalizar. |
| `telefono` | Obligatorio; celular colombiano de 10 dígitos que empieza por 3 (`^3\d{9}$`). |
| `password` | Obligatoria, de 8 a 64 caracteres, con al menos una letra y un número. |
| `confirmarPassword` | Obligatoria e igual a `password`. |
| `rol` | Obligatorio; solo `CLIENTE` o `VENDEDOR` desde el registro público. |
| `nombreTienda` | Máximo 100 caracteres; obligatorio solo para `VENDEDOR`. |
| `aceptaTerminos` | Debe ser `true`. |

### Inicio de sesión

| Campo | Regla |
|---|---|
| `email` | Obligatorio y con formato de correo. |
| `password` | Obligatoria. |

## Persistencia

La entidad `Usuario` se almacena en la tabla `usuarios`:

| Columna | Descripción |
|---|---|
| `id` | Identificador autogenerado. |
| `nombre`, `apellido` | Hasta 60 caracteres. |
| `email` | Único, hasta 120 caracteres, normalizado en minúsculas. |
| `password` | Hash BCrypt; nunca texto plano. |
| `telefono` | Hasta 15 caracteres. |
| `nombre_tienda` | Solo vendedores; hasta 100 caracteres. |
| `rol` | `CLIENTE`, `VENDEDOR` o `ADMINISTRADOR` (texto). |
| `estado` | `ACTIVO` o `SUSPENDIDO`; por defecto `ACTIVO`. |
| `fecha_registro` | Asignada al crear; no actualizable. |
| `version_terminos`, `fecha_aceptacion_terminos` | Evidencia de la aceptación de términos y política de datos; nulos para el administrador inicial. |

El repositorio expone `findByEmail`, `existsByEmail` y `findEstadoById`, esta última limitada a la columna de estado porque se ejecuta en cada petición autenticada.

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Registra un cliente o vendedor; responde 201 con JWT y usuario. |
| `POST` | `/api/auth/login` | Público | Verifica credenciales; responde 200 con JWT y usuario. |
| `GET` | `/api/auth/me` | Autenticado (cualquier rol) | Devuelve los datos públicos del usuario del token. |

## Frontend

### Sesión (`AuthService`)

- Guarda la sesión en `localStorage` con las claves `ls_token` (JWT) y `ls_session` (token, expiración y usuario) y la expone mediante signals (`usuario`, `rol`, `autenticado`).
- Al iniciar la aplicación restaura la sesión solo si el token coincide con el de la sesión guardada y no ha expirado; la expiración se toma del claim `exp` del JWT (decodificado sin verificar firma, que corresponde al backend).
- `getToken()` cierra la sesión local si el token ya expiró.
- `logout()` elimina ambas claves, limpia el perfil infantil activo (`PerfilActivoService.limpiar()`, memoria y `littleStyle.perfilActivoId`) y redirige al login. Lo mismo ocurre al descartar una sesión expirada. Al ser JWT sin estado, no hay llamada de cierre al servidor.
- `rutaInicio()` lleva a cada rol a su inicio: `CLIENTE → /cliente`, `VENDEDOR → /vendedor`, `ADMINISTRADOR → /admin`; sin sesión, a `/auth/login`.

### Interceptor (`authInterceptor`)

- Adjunta `Authorization: Bearer <token>` solo a peticiones dirigidas a la API (`environment.apiUrl`, `/api`) y nunca a login, registro o URLs externas.
- Ante un **401** en un endpoint protegido cierra la sesión y navega a `/auth/login?sesion=expirada&returnUrl=...`. Un 401 en el login no redirige, porque significa credenciales incorrectas.
- Ante un **403** navega a `/acceso-denegado` sin cerrar la sesión.

### Guards

| Guard | Comportamiento |
|---|---|
| `roleGuard` | Exige sesión y que el rol esté en `data.roles`. Sin sesión → login con `returnUrl`; rol no permitido → `/acceso-denegado`. Sin `data.roles` solo exige autenticación. |
| `authGuard` | Exige sesión vigente; si no, redirige al login con `returnUrl`. |
| `guestGuard` | Impide a un usuario autenticado volver a login/registro y lo envía a su inicio. |

La raíz (`/`) redirige al inicio del rol o al login. Las rutas `/cliente` y `/cliente/hijos` exigen `CLIENTE`; `/vendedor`, `VENDEDOR`; y `/admin`, `ADMINISTRADOR`. Existen además `/acceso-denegado` y una página 404 para rutas desconocidas.

### Pantallas

| Interfaz | Comportamiento que existe |
|---|---|
| UI-1, inicio de sesión | Formulario de correo y contraseña con validación en cliente, opción de mostrar contraseña, botón deshabilitado con indicador mientras se envía y mensaje del backend ante error. Muestra “Tu sesión expiró” cuando llega con `sesion=expirada`. Tras autenticar navega a `returnUrl` (solo rutas internas que empiezan por `/`) o al inicio del rol. “¿Olvidaste tu contraseña?” solo informa que la recuperación estará disponible próximamente. |
| UI-2, registro | Selector Cliente/Vendedor; el campo “Nombre de la tienda” aparece y se vuelve obligatorio solo para vendedor. Validación en cliente por campo, barra de fortaleza de contraseña (cinco niveles, de “Muy débil” a “Excelente”), confirmación de contraseña y errores por campo devueltos por el backend. El enlace de términos abre un modal accesible (`TerminosModal`) con los términos y la política de tratamiento de datos (versión `1.0`); abrirlo no marca la casilla, “He leído y acepto” sí la marca y “Cerrar” no la acepta. Tras registrar navega al inicio del rol. |
| Inicio por rol | UI-3 (cliente, ampliada en US-06), UI-18 (panel del vendedor) y UI-25 (panel del administrador) con barra de navegación que muestra nombre, rol y “Cerrar sesión”. Los paneles de vendedor y administrador son marcadores con mosaicos sin funcionalidad, pendientes de sus historias. |
| Acceso denegado / 404 | Páginas informativas para rol no permitido y rutas inexistentes. |

## Trazabilidad de requisitos no funcionales

La columna de prueba identifica pruebas existentes que respaldan el mecanismo. No afirma que se hayan medido los porcentajes poblacionales especificados en el Plan de Calidad.

| RNF / atributo | Mecanismo implementado | Prueba automatizada asociada | Alcance no demostrado |
|---|---|---|---|
| RNF-10, control de accesos por roles | `SecurityConfig` con reglas por prefijo (`/api/admin/**`, `/api/vendedor/**`, `/api/cliente/**`) y `anyRequest().authenticated()`; `JwtAuthenticationFilter` valida firma, expiración y estado de la cuenta; respuestas 401/403 en JSON; guards e interceptor en el frontend. | `AuthSecurityIntegrationTest.endpointProtegidoSinTokenDevuelve401`, `tokenInvalidoDevuelve401ConMensaje`, `rolSinPermisoDevuelve403`, `tokenDeCuentaSuspendidaDejaDeSerValidoDeInmediato`; `JwtServiceTest.rechazaTokenExpirado`, `rechazaTokenFirmadoConOtraClave`, `rechazaTokenAlterado`, `rechazaTokenMalformado`; `JwtAuthenticationFilterTest` (6 casos); `role.guard.spec.ts`; `auth.interceptor.spec.ts`. | Los rechazos (401/403, login fallido, bloqueado o de cuenta suspendida, auto-registro como administrador) se registran con nivel `WARN` en el logger `littlestyle.seguridad`, con correo enmascarado y sin contraseñas ni tokens. No se calcula automáticamente la tasa ≥ 98,5 %. |
| Seguridad – autenticidad (Plan de Calidad, RBAC con JWT firmados) | JWT firmados con HMAC-SHA y clave configurable; contraseñas con BCrypt; mensaje genérico ante credenciales incorrectas; administradores no auto-registrables. | `JwtServiceTest.generaTokenConClaimsDelUsuario`, `fallaSiNoHaySecretConfigurado`; `AuthServiceTest` (registro con hash y correo normalizado, rechazo de administrador); `AuthSecurityIntegrationTest.registroLoginYAccesoConToken`, `loginConPasswordIncorrectaDevuelve401`. | El límite de intentos de login (5 fallos en 15 minutos por correo + IP → bloqueo de 15 minutos con HTTP 429, configurable con `app.security.login.*`) se guarda en memoria: se pierde al reiniciar, no se comparte entre instancias y, detrás de un proxy, usa la IP del proxy. La fortaleza de la clave en producción depende de la configuración del despliegue. |
| RNF-06 (soporte legal: Ley 1581 de 2012) | Aceptación obligatoria de términos y política de datos, con versión y fecha guardadas. Base de identidad y propiedad que US-06 usa para restringir los datos del menor a su acudiente. | `AuthSecurityIntegrationTest.registroGuardaLaAceptacionDeTerminos`; `AuthControllerTest.registroConInputsInvalidosDevuelve400ConErroresPorCampo` (`aceptaTerminos`); `registro.spec.ts` (modal de términos). | El cifrado de los datos del menor corresponde a US-06. El transporte HTTPS/TLS depende del despliegue y no se certifica aquí. |

## Supuestos y limitaciones

- El backend es sin estado: no existe cierre de sesión en servidor ni lista negra de tokens. Un token de una cuenta **activa** sigue siendo válido hasta su expiración (8 horas por defecto) aunque el usuario cierre sesión en el navegador. Solo la suspensión de la cuenta lo invalida de inmediato.
- Los claims `rol` y `nombre` se toman del token. Si en el futuro se cambia el rol de un usuario (US-13), el cambio no se refleja hasta que se emita un nuevo token.
- El token se guarda en `localStorage`, por lo que su protección depende de evitar vulnerabilidades XSS en el frontend.
- No hay recuperación de contraseña, verificación de correo, correo de bienvenida (RNF-23) ni refresh token.
- En el perfil `dev` (activo por defecto) y `local` se usa una clave JWT predeterminada pública; en `prod`, `JWT_SECRET` y `CORS_ALLOWED_ORIGINS` son obligatorios y no tienen valor por defecto.
- Las credenciales del administrador inicial del perfil `local` son exclusivamente de desarrollo. En otros entornos el administrador inicial solo se crea si se configuran `app.admin.email` y `app.admin.password` por variables de entorno; `AdminInitializer` registra en el log el correo creado (nunca la contraseña).
- Las columnas `version_terminos` y `fecha_aceptacion_terminos` se añadieron después del primer commit de la historia. En `prod` (`ddl-auto=validate`) el esquema debe actualizarse antes de arrancar; ver `prompt-claude-despliegue.md`.
- La versión de los términos debe mantenerse sincronizada entre `AuthService.VERSION_TERMINOS_VIGENTE` (backend) y `VERSION_TERMINOS` en `terminos-modal.ts` (frontend). Un cambio de versión no obliga a los usuarios existentes a aceptar de nuevo.
- Los paneles de vendedor y administrador son marcadores visuales; su funcionalidad corresponde a las historias de los procesos 2 y 3.
