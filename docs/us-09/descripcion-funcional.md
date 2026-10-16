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
- Filtros: texto (nombre, marca, descripción con `LIKE '%x%'`, sin índice:
  no es búsqueda full-text), categoría, género (niños, niñas, unisex; sin
  valor aplican todos), precio mín/máx, disponibilidad (alguna talla con
  stock), material (coma-separados, coincide si ALGÚN componente está;
  p. ej. `ALGODON,POLIESTER`), talla (talla con existencias), edad en meses
  (`edadMeses`: coincide si alguna talla cubre la edad o no declara rango;
  rango nulo = compatible hacia atrás), e `ids` de recomendados (US-07).
- "Solo mi talla" filtra en el SERVIDOR (`GET /ids` + `?ids=`): la
  paginación y el contador los calcula la base de datos. Las tarjetas
  muestran además el badge de talla sugerida y aviso de alergia (mejor
  esfuerzo, tope 24 del motor).
- Paginación en DOS pasos (RNF-02): primero página de ids sin fetch de
  colecciones, luego fetch de tallas reordenado. Así se evita el paginado
  en memoria de Hibernate (HHH90003004) que provocaba
  `@EntityGraph("tallas")` + `Pageable` en una sola consulta.
- Orden con desempate por `id` para paginación estable. Parámetros
  inválidos (`pagina=abc`, `tamano=x`, `/catalogo/abc`, edad negativa,
  material desconocido) → 400, nunca 500.
- La ficha muestra composición con porcentajes, marca, categoría, colores,
  estampado, cuidado y tabla de tallas con rangos, edad y disponibilidad
  por talla. DECISIÓN: las existencias por talla SÍ son exactas (el cliente
  las necesita para elegir cantidad hasta el stock real); lo que nunca se
  expone es `stockMinimo`, `stockTotal` ni datos del vendedor.

## 4. Seguridad y datos
Solo CLIENTE (`/api/cliente/**` + `@PreAuthorize`). Sin datos del vendedor.

## 5. Endpoints
- `GET /api/cliente/catalogo?q=&categoria=&genero=&precioMin=&precioMax=&disponible=&material=ALGODON,POLIESTER&talla=&edadMeses=&ids=&pagina=0&tamano=12&orden=novedades` → 200 página, 400 filtro inválido o mal tipado, 401/403.
- `GET /api/cliente/catalogo/{id}` → 200 ficha, 400 id no numérico, 404 inactiva o inexistente.

## 6. Supuestos y brechas
- El rango de edad por talla (`edadMinMeses/edadMaxMeses`, opcional) lo
  declara el vendedor en `TallaRequest`; la UI del panel vendedor para
  cargarlo queda pendiente (US-08), pero la API ya lo acepta y valida
  (mín ≤ máx). Sin rango declarado la talla coincide con cualquier edad.
- Resolución Andina 2109 de 2019: la ficha muestra composición con
  porcentajes, marca, instrucciones de cuidado, categoría, colores y
  estampado. País de origen e importador no están en el modelo (ver US-12).
- RNF-02 (< 1,5 s por filtro) se verifica manual en Network; la medición con
  carga va en US-13.
