# Documentación de US-08 — Gestión de Catálogo de Prendas e Inventario

Yo como Vendedor, quiero registrar y actualizar mis prendas con fotos, tablas de tallas,
composición textil, stock y niveles mínimos de alerta, para comercializar mis productos
y controlar la disponibilidad en tiempo real.

## Qué se implementó

- **Catálogo (tarea 1):** `PrendaVendedorController` en `/api/vendedor/prendas` con
  listado, detalle, alta, edición y activación/desactivación. `PrendaService` valida
  dueño (`findByIdAndVendedorId`, 404 si es ajena) y `ValidadorPrenda` exige composición
  que sume 100 % sin materiales repetidos y tallas sin repetir ni rangos invertidos.
  Las prendas no se borran: se desactivan.
- **Modelo:** `Prenda` → `TallaPrenda` (rangos de estatura/peso + **stock por talla**)
  → `ImagenPrenda` (URL + clave del objeto). Colores, estampado y flags textiles
  (tintes, broches, níquel, formaldehído) alimentan el filtro de alergias de US-07.
- **Imágenes en S3 (tarea 2, ADR-10):** SDK de AWS v2 (`software.amazon.awssdk:s3`).
  Puerto `AlmacenamientoImagenes` con `S3AlmacenamientoImagenes` (prod) y
  `LocalAlmacenamientoImagenes` (dev/test, sirve `/media/**`). Carga masiva en una sola
  petición multipart, todo o nada: se validan todos los archivos por su firma (JPG,
  PNG, WEBP) antes de subir, y si la transacción no se confirma se borran los objetos
  ya subidos. La BD solo guarda URL y clave.
- **Stock mínimo y alertas (tarea 3):** `InventarioService` bloquea la prenda
  (`PESSIMISTIC_WRITE`), aplica el stock y llama a `AlertasInventarioService` en la
  misma transacción. Stock total ≤ mínimo → `STOCK_BAJO`; 0 → `AGOTADO`. Una alerta
  activa por prenda; escalar a agotado notifica de nuevo; superar el mínimo la
  resuelve. Se publica `AlertaReabastecimientoEmitida` y
  `NotificadorAlertasReabastecimiento` la procesa tras el commit (ADR-14).
- **Integración con US-07:** `CatalogoRecomendacionAdapter` reemplaza al catálogo de
  ejemplo. `CatalogoEjemploAdapter` queda solo con `app.recomendacion.catalogo=ejemplo`.
- **Panel del vendedor (tarea 4):** UI-18 con KPIs reales de productos y stock bajo,
  UI-19 Mis productos, UI-20 Crear/editar producto con fotos, UI-21 Inventario con
  edición en línea por talla y UI-22 Alertas de inventario.
- **Pruebas (tarea 5):** 49 pruebas JUnit/Mockito nuevas (incluida una de integración
  con H2) + 16 pruebas de Angular.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Modelo, reglas de stock y alertas, S3, endpoints, configuración y limitaciones |
| `casos-de-prueba.md` | CP-US08 con trazabilidad a pruebas y requests Postman |

## Evidencia

- Backend: `.\mvnw.cmd test` (141 pruebas, 0 fallos). Solo US-08:
  `.\mvnw.cmd test -Dtest="NivelStockTest,AlertasInventarioServiceTest,InventarioServiceTest,PrendaServiceTest,ValidadorPrendaTest,ImagenPrendaServiceTest,S3AlmacenamientoImagenesTest,CatalogoRecomendacionAdapterTest,CatalogoInventarioIntegrationTest"`
- Frontend: desde `frontend/`, `npm.cmd test -- --watch=false` (92 pruebas, 0 fallos).
