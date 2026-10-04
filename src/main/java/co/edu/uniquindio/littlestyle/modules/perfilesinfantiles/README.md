# Módulo de perfiles infantiles

Módulo backend de US-06 para gestionar perfiles infantiles y mediciones de crecimiento asociadas al cliente autenticado.

## Estructura

```text
perfilesinfantiles/
├── controller/
│   ├── PerfilInfantilController.java
│   └── PerfilEnumExceptionHandler.java
├── dto/
│   ├── PerfilRequest.java
│   ├── PerfilResponse.java
│   ├── MedicionRequest.java
│   └── MedicionResponse.java
├── model/
│   ├── PerfilInfantil.java
│   ├── MedicionCrecimiento.java
│   ├── AlergiaTextil.java
│   ├── ColorPreferido.java
│   ├── Estampado.java
│   ├── Contextura.java
│   └── Holgura.java
├── repository/
│   ├── PerfilInfantilRepository.java
│   ├── MedicionCrecimientoRepository.java
│   └── converter/
│       ├── CifradoPerfil.java
│       ├── PerfilCryptoConfiguration.java
│       ├── PerfilDatosCifradoMigracion.java
│       ├── PerfilDatosCifradoVersion.java
│       ├── FechaNacimientoConverter.java
│       ├── FechaMedicionConverter.java
│       ├── NombreCifradoConverter.java
│       ├── TextoLegadoCifradoConverter.java
│       ├── StringCifradoConverter.java
│       ├── AlergiasConverter.java
│       └── BigDecimalCifradoConverter.java
└── service/
    ├── PerfilInfantilService.java
    ├── ValidadorMedidas.java
    └── CalculadoraCompletitud.java
```

## Responsabilidades

| Clase | Responsabilidad |
|---|---|
| `PerfilInfantilController` | Publica los endpoints REST de perfiles y mediciones bajo `/api/cliente/perfiles`. |
| `PerfilEnumExceptionHandler` | Convierte errores de lectura de enums del módulo en respuestas HTTP 400 con mensajes por campo cuando reconoce alergias, colores o estampados. |
| `PerfilRequest`, `MedicionRequest` | Definen datos de entrada y restricciones Bean Validation. |
| `PerfilResponse`, `MedicionResponse` | Definen los datos expuestos por la API; la respuesta de perfil incluye edad, última medición y completitud. |
| `PerfilInfantil`, `MedicionCrecimiento` | Entidades JPA. La relación perfil-mediciones elimina mediciones en cascada al borrar el perfil. |
| `AlergiaTextil`, `ColorPreferido`, `Estampado`, `Contextura`, `Holgura` | Catálogos enum aceptados por los modelos y DTO. |
| `PerfilInfantilRepository` | Lista y cuenta perfiles por cliente y busca por ID junto con ID propietario. |
| `MedicionCrecimientoRepository` | Consulta historial filtrado por perfil y cliente; el orden se aplica en servicio después de descifrar las fechas. |
| `PerfilInfantilService` | Aplica operaciones CRUD, valida propietario, límite de perfiles, reglas de alergia/preferencias, ordena el historial descifrado y construye respuestas. |
| `ValidadorMedidas` | Valida fechas, rangos generales y rangos biométricos por edad cumplida. |
| `CalculadoraCompletitud` | Calcula la puntuación de completitud sobre ocho grupos de información. |
| `CifradoPerfil` | Implementa AES-GCM con IV aleatorio y clave AES de 256 bits. |
| `PerfilCryptoConfiguration` | Lee `app.crypto.perfil-key` e inicializa el cifrado. |
| `FechaNacimientoConverter`, `FechaMedicionConverter`, `NombreCifradoConverter`, `AlergiasConverter`, `StringCifradoConverter`, `TextoLegadoCifradoConverter`, `BigDecimalCifradoConverter` | Adaptan datos personales y de salud a cadenas cifradas y de vuelta para persistencia JPA; el converter de valores legados distingue las columnas que históricamente guardaban texto claro. |
| `PerfilDatosCifradoMigracion` | Cuando `app.perfiles.cifrado.migrar-datos=true`, convierte una sola vez y de forma transaccional los valores legados en claro o cifrados con el formato previo al formato versionado actual; no registra valores del perfil. |
| `PerfilDatosCifradoVersion` | Persiste mediante JPA la versión aplicada de la migración para no repetirla en cada arranque. |

## Configuración de clave

La propiedad `app.crypto.perfil-key` se vincula a la variable **`PERFIL_ENC_KEY`**. La clave debe ser Base64 y decodificar a 32 bytes; la configuración productiva requiere definir la variable sin depender de un valor predeterminado. No se incluyen valores, ejemplos ni credenciales en este README. La clave debe gestionarse como secreto del entorno y mantenerse estable mientras existan datos cifrados con ella.

El cifrado protege los datos personales, físicos, textiles y de preferencias enumerados en “Decisiones de diseño”. Los IDs, la relación de propiedad y las fechas de auditoría permanecen como metadatos operativos; el inventario se detalla en `docs/us-06/descripcion-funcional.md`.

## Pruebas

Desde la raíz del repositorio:

```sh
./mvnw test
```

La suite de perfiles se encuentra bajo `src/test/java/co/edu/uniquindio/littlestyle/modules/perfilesinfantiles/` e incluye pruebas de integración MockMvc, servicio, validación de medidas, completitud y converters de cifrado.

## Decisiones de diseño

- El propietario se obtiene del usuario autenticado mediante `SecurityUtils`; el cliente no envía el ID propietario en el DTO. Las operaciones por ID usan consultas restringidas al cliente y ocultan recursos ajenos como 404.
- Las rutas dependen de la regla común de seguridad para `/api/cliente/**`, que exige rol `CLIENTE`.
- Alergias, colores y estampados son enums/set y no texto libre; los campos “otra” son limitados y cifrados.
- Los datos de crecimiento se modelan como mediciones históricas separadas; fecha, estatura y peso se cifran en reposo. El historial se ordena en memoria luego de descifrar las fechas, ya que la base de datos no puede ordenar cronológicamente el texto cifrado. El alta es append-only a través de los endpoints actuales.
- Se cifran en reposo el nombre, la fecha de nacimiento, contextura, holgura, alergias, declaración de ausencia de alergias, preferencias enumeradas y libres, y fecha/estatura/peso de cada medición. Los identificadores, la relación de propiedad y las fechas de auditoría continúan como metadatos relacionales.
- El cifrado versionado admite leer filas del formato anterior. Para migrar datos existentes en producción, detener todas las instancias, ejecutar una única instancia con los perfiles JPA `prod,perfiles-migracion`, detenerla y reiniciar solo con `prod` para volver a `ddl-auto=validate`; no se incluyen scripts DDL manuales. Respaldar la base y hacer el cambio en ventana de mantenimiento.
- El esquema relacional se deriva de las entidades JPA y su configuración de generación de esquema; el módulo no mantiene scripts SQL manuales.
- Los rangos por edad son reglas explícitas centralizadas en `ValidadorMedidas` y duplicadas en el frontend para feedback temprano; el servidor vuelve a validar.
- La talla por edad de la interfaz se mantiene como función provisional separada hasta US-07; el backend no la calcula como recomendación.
