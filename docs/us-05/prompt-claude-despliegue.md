# Prompt para Claude: despliegue de US-05

Usa este prompt cuando se autorice preparar el despliegue de US-05. No despliegues ni
modifiques infraestructura hasta confirmar expresamente el entorno, la disponibilidad de
`JWT_SECRET`, los orígenes CORS permitidos, el respaldo de la base y cómo se creará el
administrador inicial.

```text
Contexto: LittleStyle, US-05 módulo de autenticación, registro y autorización de usuarios
(JWT + Spring Security + guards/interceptor en Angular). Revisa primero el estado actual del
repositorio, rama y cambios locales; no sobrescribas trabajo ajeno ni hagas commit, push o
cambios remotos. No modifiques pom.xml.

Prepara un plan seguro para desplegar el backend y frontend de US-05. Verifica el contrato
actual de /api/auth/register, /api/auth/login y /api/auth/me, las reglas de SecurityConfig y
la configuración de JWT en código; no asumas que la documentación sigue vigente sin
compararla con el código.

Antes de desplegar, exige:
1. Generar o confirmar un JWT_SECRET de producción en Base64 de al menos 256 bits, distinto
   de la clave predeterminada de application.properties (pública en el repositorio). Nunca
   imprimirlo ni registrarlo. Advertir que rotarlo invalida todas las sesiones activas.
2. Confirmar JWT_EXPIRATION_MS (por defecto 8 horas) y CORS_ALLOWED_ORIGINS con el dominio
   exacto del frontend (sin comodines). En prod ambos JWT_SECRET y CORS_ALLOWED_ORIGINS son
   obligatorios y no tienen valor por defecto.
3. Verificar que el perfil activo sea `prod` y no `dev`/`local` (que usan clave JWT por
   defecto, H2 y, en local, credenciales de administrador de desarrollo). La consola H2 debe
   quedar deshabilitada.
4. Identificar motor/versión y esquema de la base. La tabla `usuarios` requiere las columnas
   `version_terminos` y `fecha_aceptacion_terminos`, añadidas después del primer commit de la
   historia. Como `prod` usa ddl-auto=validate, confirmar si existen; si no, actualizar el
   esquema con JPA (perfil con ddl-auto=update en una única instancia y ventana controlada,
   p. ej. `perfiles-migracion` junto con `prod` si se despliega con US-06), con respaldo
   restaurable previo. No escribir SQL manual sin autorización.
5. Definir cómo se crea el administrador inicial: APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD como
   variables de entorno/secretos solo durante el primer arranque, con contraseña robusta que
   no sea la del perfil local. Retirarlas después; AdminInitializer no recrea la cuenta si ya
   existe y registra en el log únicamente el correo.
6. Confirmar que el tráfico entre navegador, frontend y backend use HTTPS/TLS (el módulo no
   lo configura) y que el proxy reenvíe el encabezado Authorization.
7. Configurar `environment.apiUrl` del build de producción del frontend (o el proxy inverso)
   para que las peticiones a /api lleguen al backend.

Tras desplegar, valida con cuentas y datos sintéticos (nunca datos reales):
- Registro de cliente y de vendedor (201), correo duplicado (409), registro como
  ADMINISTRADOR rechazado (400).
- Login correcto (200), contraseña incorrecta (401 con mensaje genérico), cuenta suspendida
  (403).
- /api/auth/me sin token y con token alterado (401); acceso cruzado entre roles a
  /api/admin/**, /api/vendedor/** y /api/cliente/** (403).
- Redirecciones por rol, guards y cierre de sesión en el frontend.
- Logs sin contraseñas, tokens ni hashes; health checks correctos.

Si cualquier dato no se puede confirmar (clave, orígenes, esquema, respaldo o credenciales del
administrador), detén la ejecución y pide decisión al responsable. Devuelve un checklist por
fases, comandos exactos específicos del entorno solo después de confirmar sus detalles,
riesgos, validaciones/criterios de éxito y rollback. No despliegues por tu cuenta ni modifiques
la base productiva sin autorización explícita.
```

Los JWT son sin estado: un rollback del binario no invalida los tokens ya emitidos mientras
se conserve el mismo `JWT_SECRET`. Para forzar el cierre de todas las sesiones es necesario
rotar la clave.
