# US-08 Descripción funcional — Gestión de Catálogo de Prendas e Inventario

## 1. Objetivo
Permitir al vendedor publicar prendas con fotos, tabla de tallas y composición textil,
controlar su stock por talla y recibir alertas de reabastecimiento al llegar al mínimo.

## 2. Alcance
Incluye CRUD de prendas (sin borrado físico), carga masiva de fotos a S3, stock por
talla, stock mínimo por prenda, alertas de reabastecimiento con historial, adaptador real
del catálogo para el motor de US-07 y las pantallas UI-18 a UI-22.
No incluye catálogo público para clientes (US-09), descuento de stock por compras
(US-10/US-11), envío de correos (ADR-07), categorías administrables (Proceso 3) ni
métricas de ventas del panel.

## 3. Modelo de datos
| Tabla | Contenido |
|---|---|
| `prendas` | vendedor, nombre, categoría, descripción, marca, precio, estado (`ACTIVA`/`INACTIVA`), `stock_minimo`, estampado y flags textiles |
| `prenda_tallas` | talla (única por prenda), estatura mín/máx, peso mín/máx, `stock` |
| `prenda_composicion` | material + porcentaje |
| `prenda_colores` | colores de la prenda |
| `prenda_imagenes` | URL pública, clave del objeto en el almacenamiento, orden (0 = principal) |
| `alertas_reabastecimiento` | prenda, nivel, stock y mínimo al emitir, fecha de emisión y de resolución |

El stock se lleva **por talla** porque el motor de US-07 ya consulta `stockPorTalla`.
UI-19/UI-21 muestran el **total** de la prenda, que es el que se compara con el mínimo.

## 4. Reglas
- **Prenda:** nombre 3–120, precio $100–$10.000.000, mínimo 0–10000, máximo 10 colores,
  1–6 materiales que suman exactamente 100 % sin repetir, 1–15 tallas. Las tallas se
  normalizan (`" xs "` → `XS`) y no pueden repetirse. Estatura 40–200 cm y peso 2–120 kg
  (mismos límites que el perfil infantil); mínimo ≤ máximo.
- **Edición:** cambia datos y rangos; el stock de tallas existentes **no** se toca (se
  gestiona en el inventario, para que un formulario viejo no pise ventas recientes).
  Las tallas nuevas toman su stock inicial. No se puede quitar una talla con unidades
  (409).
- **Nivel de stock:** total = 0 → `AGOTADO`; total ≤ mínimo → `STOCK_BAJO`; si no,
  `DISPONIBLE`.
- **Alertas (RNF-14):** se evalúan en la misma transacción que cambia stock, mínimo,
  tallas o estado:
  - Sin alerta activa y nivel ≠ disponible → se emite y se notifica.
  - Mismo nivel que la activa → no se repite.
  - Bajo → agotado → se resuelve la anterior y se emite otra notificada.
  - Agotado → bajo (reabastecimiento parcial) → se reemplaza sin notificar.
  - Disponible o prenda inactiva → se resuelve la activa.
- **Concurrencia:** stock, edición y estado bloquean la fila de la prenda
  (`PESSIMISTIC_WRITE`) hasta el commit.
- **Notificación (ADR-14):** `AlertaReabastecimientoEmitida` se procesa con
  `@TransactionalEventListener(AFTER_COMMIT)`. Hoy deja una línea `INFO` en el log; el
  correo al vendedor se conectará ahí sin tocar el inventario.

## 5. Imágenes (ADR-10, RNF-25)
- `POST /api/vendedor/prendas/{id}/imagenes`, multipart con varios archivos en `archivos`.
- Límites: 5 MB por archivo, 10 por carga, 8 por prenda (`app.storage.imagenes.*`).
- El tipo se detecta por la firma del archivo (JPEG, PNG, WEBP), no por el nombre ni el
  Content-Type del navegador. Si un archivo es inválido no se sube ninguno.
- Clave: `prendas/{prendaId}/{uuid}.{ext}` con prefijo configurable. En S3 se sube con
  `Cache-Control: immutable` (cada carga usa una clave nueva).
- Consistencia BD ↔ S3: si la transacción falla, se borran los objetos ya subidos; al
  eliminar una foto el objeto se borra solo después del commit. Un fallo de S3 responde
  502 con mensaje genérico.
- Credenciales: cadena estándar del SDK (variables `AWS_*`, perfil o rol IAM, ADR-09);
  nunca en el código. El bucket debe permitir lectura pública de los objetos.
- Desarrollo/pruebas: `app.storage.tipo=local` guarda en `./data/uploads` y sirve en
  `/media/**` (proxy de Angular incluido).

## 6. Seguridad
Todo bajo `/api/vendedor/**` (rol `VENDEDOR`). Cada vendedor solo ve y modifica sus
prendas: una prenda ajena responde 404. 401 sin token, 403 con otro rol. `/media/**`
es público (solo lectura, solo en almacenamiento local).

## 7. Endpoints
| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/vendedor/prendas?q=` | 200 lista (búsqueda sin tildes por nombre o categoría) |
| GET | `/api/vendedor/prendas/{id}` | 200 / 404 |
| POST | `/api/vendedor/prendas` | 201 / 400 |
| PUT | `/api/vendedor/prendas/{id}` | 200 / 400 / 404 / 409 |
| PATCH | `/api/vendedor/prendas/{id}/estado` | 200 (`{"estado":"ACTIVA"\|"INACTIVA"}`) |
| POST | `/api/vendedor/prendas/{id}/imagenes` | 201 lista de imágenes / 400 / 413 / 502 |
| PUT | `/api/vendedor/prendas/{id}/imagenes/{imagenId}/principal` | 200 |
| DELETE | `/api/vendedor/prendas/{id}/imagenes/{imagenId}` | 204 |
| GET | `/api/vendedor/inventario` | 200 stock total, mínimo, nivel y stock por talla |
| PUT | `/api/vendedor/inventario/{prendaId}` | 200 / 400 / 404 (`{"tallas":[{"talla","stock"}],"stockMinimo"?}`, valores absolutos) |
| GET | `/api/vendedor/inventario/alertas` | 200 alertas activas con actual, mínimo y diferencia |

## 8. Frontend (`features/catalogoCompras`)
- **UI-18 `/vendedor`:** KPIs de productos activos y stock bajo, accesos y vista previa
  de alertas. Pedidos y reportes quedan como "Próximamente".
- **UI-19 `/vendedor/productos`:** tabla con miniatura, composición, categoría, precio,
  stock con nivel, estado, editar y publicar/ocultar; búsqueda sin tildes.
- **UI-20 `/vendedor/productos/nuevo` y `/:id`:** secciones Información,
  Características, Composición (total en vivo), Tabla de tallas, Inventario y Fotos. Las
  fotos se suben al guardar. Si la prenda se guarda pero las fotos fallan, pasa a modo
  edición con las fotos pendientes y botón de reintento.
- **UI-21 `/vendedor/inventario`:** edición en línea del stock de cada talla y del
  mínimo, con total en vivo; botón Alertas con contador.
- **UI-22 `/vendedor/inventario/alertas`:** tarjetas con actual, mínimo, diferencia y
  acceso directo a la edición de stock.

## 9. Configuración
| Propiedad | Defecto | Prod |
|---|---|---|
| `app.storage.tipo` | `local` | `s3` |
| `app.storage.s3.bucket` / `region` | — / `us-east-1` | `AWS_S3_BUCKET` / `AWS_REGION` |
| `app.storage.s3.url-publica-base` | URL del bucket | `AWS_S3_URL_PUBLICA` (p. ej. CloudFront) |
| `app.storage.s3.prefijo` | `catalogo` | `AWS_S3_PREFIJO` |
| `app.recomendacion.catalogo` | `jpa` | `jpa` |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | 5MB / 52MB | igual |

## 10. Limitaciones
- Las alertas se ven en UI-21/UI-22 y en el log; aún no hay correo (ADR-07).
- Producción usa `ddl-auto=validate`: las tablas nuevas se crean activando una vez el
  perfil `perfiles-migracion` (`ddl-auto=update`) o con un script equivalente.
- El almacenamiento local no es para producción. S3 no se probó contra AWS real en esta
  historia: la integración se verificó con el cliente S3 simulado.
- Las categorías son fijas (enum) hasta que el administrador pueda gestionarlas.
- La tasa > 99 % de RNF-14 y de RNF-25 no se mide automáticamente; las pruebas verifican
  el mecanismo.
