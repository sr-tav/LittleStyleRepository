# Documentación de US-09 — Exploración del Catálogo y Filtros Avanzados para Clientes

Yo como Cliente, quiero explorar el catálogo de ropa infantil utilizando filtros
por categoría, talla sugerida, edad y composición, para encontrar fácilmente
las prendas adecuadas para mis hijos.

## Qué se implementó

- **Vitrina del cliente (solo CLIENTE):** `GET /api/cliente/catalogo` con filtros
  en base de datos (texto, categoría, rango de precios, disponibilidad, material
  simple o múltiple coma-separado, talla, edad en meses, `ids` de recomendados)
  y paginación en dos pasos (`pagina`, `tamano` con tope 50, `orden` entre novedades,
  precioAsc, precioDesc y nombre con desempate por `id`). `GET /api/cliente/catalogo/{id}`
  con la ficha completa. Los filtros inválidos o mal tipados responden 400 con
  campo; las prendas inactivas o inexistentes, 404. No se toca `SecurityConfig`:
  `/api/cliente/**` ya exige rol CLIENTE.
- **DTO vitrina sin datos internos:** `PrendaVitrinaResponse` (precio, foto
  principal, disponibilidad booleana, nombres de tallas) y `PrendaDetalleResponse`
  (galería, composición, colores, estampado, cuidado, tabla de tallas con rangos,
  edad y disponibilidad por talla). Nunca se exponen stock mínimo, stock total
  ni datos del vendedor; las existencias por talla sí son exactas porque el
  cliente las necesita para elegir la cantidad (decisión documentada en §3).
- **Paginación propia:** `PaginaResponse` en `shared.dto`, porque `Page` de
  Spring Data no tiene representación JSON estable y además evita exponer
  objetos del framework (ADR-13). La página se calcula en BD en dos pasos
  (ids y luego fetch) para no paginar en memoria (HHH90003004).
- **Frontend `features/vitrina`:** página `Explorar` con filtros laterales
  (búsqueda con debounce, categoría, material, talla, edad en años, precios,
  disponibilidad, orden, "Solo mi talla" con el motor de US-07 filtrando en el
  servidor) y paginado con `switchMap` (cancela peticiones superadas);
  tarjetas con badge de talla sugerida y aviso de alergia; página
  `DetalleProducto` con galería, ficha técnica textil (Resolución Andina 2109:
  composición, marca, cuidado) y tabla de medidas resaltando la talla sugerida.
  Tile de Catálogo habilitado en el inicio.
- **Rendimiento (RNF-02):** filtros en JPQL con paginación e índices de
  categoría, precio y fecha de actualización en `prendas`. Medición con carga
  en US-13.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Reglas de filtrado, endpoints, RNF-02 |
| `casos-de-prueba.md` | CP-US09 con trazabilidad a tests |

## Evidencia

- Backend: `mvn test -Dtest=CatalogoClienteServiceTest,CatalogoClienteControllerTest,CatalogoClienteIntegrationTest`
- Frontend: desde `frontend/`, `npx ng test --watch=false --include='**/vitrina/**/*.spec.ts'`
