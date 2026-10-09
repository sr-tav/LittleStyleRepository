# Documentación de US-09 — Exploración del Catálogo y Filtros Avanzados para Clientes

Yo como Cliente, quiero explorar el catálogo de ropa infantil utilizando filtros
por categoría, talla sugerida, edad y composición, para encontrar fácilmente
las prendas adecuadas para mis hijos.

## Qué se implementó

- **Vitrina del cliente (solo CLIENTE):** `GET /api/cliente/catalogo` con filtros
  en base de datos (texto, categoría, rango de precios, disponibilidad, material,
  talla) y paginación (`pagina`, `tamano` con tope 50, `orden` entre novedades,
  precioAsc, precioDesc y nombre). `GET /api/cliente/catalogo/{id}` con la ficha
  completa. Los filtros inválidos responden 400 con campo; las prendas
  inactivas o inexistentes, 404. No se toca `SecurityConfig`: `/api/cliente/**`
  ya exige rol CLIENTE.
- **DTO vitrina sin datos internos:** `PrendaVitrinaResponse` (precio, foto
  principal, disponibilidad booleana) y `PrendaDetalleResponse` (galería,
  composición, colores, estampado, tabla de tallas con disponibilidad por talla).
  Nunca se exponen stock mínimo ni existencias exactas.
- **Paginación propia:** `PaginaResponse` en `shared.dto`, porque `Page` de
  Spring Data no tiene representación JSON estable y además evita exponer
  objetos del framework (ADR-13).
- **Frontend `features/vitrina`:** página `Explorar` con filtros laterales
  (búsqueda con debounce, categoría, material, talla, precios,
  disponibilidad, orden, "Solo mi talla" con el motor de US-07) y paginado;
  página `DetalleProducto` con galería, ficha técnica textil y tabla de medidas
  resaltando la talla sugerida. Tile de Catálogo habilitado en el inicio.
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
