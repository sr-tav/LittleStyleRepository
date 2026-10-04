# Prompt para Claude: despliegue de US-06

Usa este prompt cuando se autorice preparar el despliegue de US-06. No despliegues ni
modifiques infraestructura hasta confirmar expresamente el entorno, la ventana de
mantenimiento, el respaldo y la disponibilidad de `PERFIL_ENC_KEY`.

```text
Contexto: LittleStyle, US-06 gestión integral del perfil infantil. Revisa primero el
estado actual del repositorio, rama y cambios locales; no sobrescribas trabajo ajeno ni
hagas commit, push o cambios remotos. No modifiques auth ni pom.xml.

Prepara un plan seguro para desplegar el backend y frontend de US-06. Verifica el contrato
actual de perfiles, mediciones y la estrategia AES-GCM en código; no asumas que la
documentación anterior sigue vigente sin compararla con el código.

Importante: cambió el formato de almacenamiento de datos personales del perfil. Antes de
desplegar contra una base existente, exige:
1. Identificar motor/versión, esquema, configuración de ddl-auto, tamaño de datos y
   compatibilidad de tipos/longitudes.
2. Confirmar respaldo restaurable y probarlo en un entorno equivalente.
3. Detener todas las instancias escritoras y ejecutar UNA sola instancia de migración
   durante ventana de mantenimiento.
4. Usar la clave PERFIL_ENC_KEY estable que corresponde a los datos existentes; nunca
   imprimirla ni sustituirla por la clave de desarrollo. La clave de producción no debe
   tener valor por defecto.
5. Aplicar el perfil JPA `perfiles-migracion` junto con `prod` para actualizar esquema
   con JPA y activar `app.perfiles.cifrado.migrar-datos=true`. No escribir SQL manual.
6. Verificar la fila de versión de migración, que las columnas sensibles no contienen
   texto legible, y que la API descifra perfil e historial y ordena mediciones por fecha.
   No imprimir datos reales del menor; usar cuentas y datos sintéticos.
7. Parar la instancia de migración y volver a arrancar solo con `prod` (`ddl-auto=validate`).
   Confirmar health checks, logs sin datos personales, autorización por propiedad y
   pruebas de humo. Tener plan de rollback probado; no asumir que volver el binario sin
   restaurar la base revierte el cifrado.

Si cualquier dato no se puede confirmar (base, clave, backup, compatibilidad o ventana),
detén la ejecución y pide decisión al responsable. Devuelve un checklist por fases,
comandos exactos específicos del entorno solo después de confirmar sus detalles, riesgos,
validaciones/criterios de éxito y rollback. No despliegues por tu cuenta ni ejecutes una
migración productiva sin autorización explícita.
```

La migración actual se diseñó para una sola instancia de aplicación con las instancias
escritoras detenidas. No se debe ejecutar durante un despliegue rolling ni simultáneamente
desde varias réplicas.
