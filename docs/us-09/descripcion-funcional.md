# US-09 Descripción funcional — Exploración del Catálogo y Filtros Avanzados

## 1. Objetivo
Explorar el catálogo publicado con filtros multidimensionales y consultar la
ficha técnica de cada prenda con su tabla de medidas.

## 2. Alcance
Incluye `CatalogoClienteService` con búsqueda paginada, `CatalogoClienteController`
(`GET /api/cliente/catalogo` y `GET /api/cliente/catalogo/{id}`), DTOs vitrina sin
datos internos, páginas `Explorar` y `DetalleProducto`, y tile de Catálogo.
No incluye carrito (US-10) ni compra.

## 3. Reglas
- Solo prendas `ACTIVA`. Una prenda oculta o inexistente es 404 en el detalle.
- Filtros: texto (nombre, marca, descripción), categoría, género (niños,
  niñas, unisex; sin valor aplican todos), precio mín/máx,
  disponibilidad (alguna talla con stock), material (algún componente),
  talla (talla con existencias).
- "Solo mi talla" usa el motor de US-07 y filtra en el cliente sobre la página
  actual; el filtro servidor de talla sugerida queda como mejora futura.
- Paginación: página desde 0, tamaño 1–50 (se recorta a 50), orden entre
  novedades, precioAsc, precioDesc y nombre. Parámetros inválidos → 400.
- La ficha muestra composición con porcentajes, marca, categoría, colores,
  estampado y tabla de tallas con rangos y disponibilidad por talla, sin
  existencias exactas.

## 4. Seguridad y datos
Solo CLIENTE (`/api/cliente/**` + `@PreAuthorize`). Sin datos del vendedor.

## 5. Endpoints
- `GET /api/cliente/catalogo?q=&categoria=&genero=&precioMin=&precioMax=&disponible=&material=&talla=&pagina=0&tamano=12&orden=novedades` → 200 página, 400 filtro inválido, 401/403.
- `GET /api/cliente/catalogo/{id}` → 200 ficha, 404 inactiva o inexistente.

## 6. Supuestos y brechas
- El filtro es por talla del catálogo, sin equivalencia con edad.
- Resolución Andina 2109:  `Prenda`; la ficha muestra composición, marca,
  categoría, colores, estampado y advertencias de materiales. No inventar datos.
- RNF-02 (< 1,5 s por filtro) se verifica manual en Network; la medición con
  carga va en US-13.
