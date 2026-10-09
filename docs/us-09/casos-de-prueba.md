# Casos de prueba — US-09 Exploración del Catálogo

> Trazabilidad: cada CP mapea a un RNF y a una prueba automatizada.

## 1. Matriz de casos

| ID | RNF | Descripción | Precondiciones | Resultado esperado | Tipo | Evidencia |
|---|---|---|---|---|---|---|
| CP-US09-01 | RNF-02 | Explora solo publicadas con paginación y orden | 1 activa + filtros válidos | 200, solo la activa, orden precio | Unitaria mock | `CatalogoClienteServiceTest.exploraSoloPublicadasConPaginacionYOrden` |
| CP-US09-02 | RNF-02 | Tamaño de página recortado a 50 | size=500 | `tamano` 50 en el `Pageable` | Unitaria mock | `CatalogoClienteServiceTest.limitaElTamanoDePaginaParaProtegerLaBaseDeDatos` |
| CP-US09-03 | VAL | Filtros inválidos devuelven 400 | categoría, precios, orden o página mal | 400 con campo | Unitaria | `CatalogoClienteServiceTest.rechazaFiltrosInvalidosCon400`, `materialInvalidoDevuelve400` |
| CP-US09-04 | VAL | Detalle oculta existencias y mínimo | Prenda con tallas 4 (3) y 6 (0) | disponible true, tallas [true, false], sin stock exacto | Unitaria mock | `CatalogoClienteServiceTest.detalleOcultaExistenciasExactasYMinimo` |
| CP-US09-05 | VAL | Detalle de inactiva o inexistente 404 | Prenda oculta o id libre | 404 | Unitaria mock | `CatalogoClienteServiceTest.detalleDePrendaInexistenteOInactivaDevuelve404` |
| CP-US09-06 | VAL | Vitrina no expone datos internos | Prenda activa | Sin `stockMinimo` ni `stockTotal` en el JSON | Controller | `CatalogoClienteControllerTest.explorarDevuelvePaginaSinDatosInternos` |
| CP-US09-07 | VAL | Filtros contra H2 real | Prenda activa + inactiva | Solo la activa; material y talla filtran | Integración | `CatalogoClienteIntegrationTest.exploraSoloPublicadasConFiltrosEnBaseDeDatos` |
| CP-US09-08 | VAL | Detalle y roles contra H2 real | Prenda activa, luego oculta | 200 ficha, 404 al ocultar, 403 vendedor, 401 anónimo | Integración | `CatalogoClienteIntegrationTest.detalleMuestraFichaSinExistenciasExactasYOcultaInactivas`, `vitrinaExigeRolCliente` |
| CP-US09-09 | RNF-02 | Filtros viajan como query params | Servicio frontend | `categoria`, `material`, `talla`, `orden` en la URL | Frontend | `vitrina.service.spec.ts` (3 casos) |
| CP-US09-10 | VAL | Explorar muestra, filtra y pagina | Página con 1 prenda | Precio, enlace a detalle, filtro por categoría, vacío sin coincidencias | Frontend | `explorar.spec.ts` (3 casos) |
| CP-US09-11 | VAL | Tile de catálogo habilitado | Inicio del cliente | Enlace a `/cliente/catalogo`, 2 tiles deshabilitados | Frontend | `cliente-inicio.spec.ts` |
| CP-US09-12 | RNF-02 | Filtros responden en menos de 1,5 s | Catálogo con datos | Tiempos en pestaña Network | Manual | Pendiente de medición con carga en US-13 |
| CP-US09-13 | VAL | Galería con miniaturas y flechas | Detalle con 2 fotos | Navega 1→2→1, contador y tabla con estados | Frontend | `detalle.spec.ts` (2 casos) |
| CP-US09-14 | VAL | Filtro por género | Prenda de niña | `genero=NINA` la incluye; género inválido 400 | Unitaria + integración | `CatalogoClienteServiceTest.generoInvalidoDevuelve400`, `CatalogoClienteIntegrationTest` |
| CP-US09-15 | VAL | Menú aplica filtros sin recargar | En el catálogo, elegir Niñas en el menú | Nueva petición con `genero=NINA` y resultados | Frontend | `explorar.spec.ts` |
