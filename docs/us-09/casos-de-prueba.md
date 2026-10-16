# Casos de prueba — US-09 Exploración del Catálogo

> Trazabilidad: cada CP mapea a un RNF y a una prueba automatizada.

## 1. Matriz de casos

| ID | RNF | Descripción | Precondiciones | Resultado esperado | Tipo | Evidencia |
|---|---|---|---|---|---|---|
| CP-US09-01 | RNF-02 | Explora solo publicadas con paginación y orden | 1 activa + filtros válidos | 200, solo la activa, orden precio | Unitaria mock | `CatalogoClienteServiceTest.exploraSoloPublicadasConPaginacionYOrden` |
| CP-US09-02 | RNF-02 | Tamaño de página recortado a 50 | size=500 | `tamano` 50 en el `Pageable` | Unitaria mock | `CatalogoClienteServiceTest.limitaElTamanoDePaginaParaProtegerLaBaseDeDatos` |
| CP-US09-03 | VAL | Filtros inválidos devuelven 400 | categoría, precios, orden o página mal | 400 con campo | Unitaria | `CatalogoClienteServiceTest.rechazaFiltrosInvalidosCon400`, `materialInvalidoDevuelve400` |
| CP-US09-04 | RNF-02 | Detalle con existencias exactas por talla, sin mínimo ni totales | Prenda con tallas 4 (8) y 6 (4) | disponible true, tallas [true, false], stock 8/4, sin `stockMinimo` | Unitaria mock | `CatalogoClienteServiceTest.detalleMuestraExistenciasPorTallaPeroOcultaElMinimo` |
| CP-US09-05 | VAL | Detalle de inactiva o inexistente 404 | Prenda oculta o id libre | 404 | Unitaria mock | `CatalogoClienteServiceTest.detalleDePrendaInexistenteOInactivaDevuelve404` |
| CP-US09-06 | VAL | Vitrina no expone datos internos | Prenda activa | Sin `stockMinimo` ni `stockTotal` en el JSON | Controller | `CatalogoClienteControllerTest.explorarDevuelvePaginaSinDatosInternos` |
| CP-US09-07 | VAL | Filtros contra H2 real | Prenda activa + inactiva | Solo la activa; material y talla filtran | Integración | `CatalogoClienteIntegrationTest.exploraSoloPublicadasConFiltrosEnBaseDeDatos` |
| CP-US09-08 | RNF-02 | Detalle y roles contra H2 real | Prenda activa, luego oculta | 200 ficha con stock 8/4 y edad 24–72, 404 al ocultar, 403 vendedor, 401 anónimo | Integración | `CatalogoClienteIntegrationTest.detalleMuestraExistenciasPorTallaSinExponerElMinimoYOcultaInactivas`, `vitrinaExigeRolCliente` |
| CP-US09-09 | RNF-02 | Filtros viajan como query params | Servicio frontend | `categoria`, `material`, `talla`, `orden` en la URL | Frontend | `vitrina.service.spec.ts` (3 casos) |
| CP-US09-10 | RNF-02 | Explorar muestra, filtra y pagina | Página con 1 prenda | Precio, enlace a detalle, filtro por categoría, vacío sin coincidencias, edad e ids | Frontend | `explorar.spec.ts` (6 casos) |
| CP-US09-11 | VAL | Tile de catálogo habilitado | Inicio del cliente | Enlace a `/cliente/catalogo`, 2 tiles deshabilitados | Frontend | `cliente-inicio.spec.ts` |
| CP-US09-12 | RNF-02 | Filtros responden en menos de 1,5 s | Catálogo con datos | Tiempos en pestaña Network | Manual | Pendiente de medición con carga en US-13 |
| CP-US09-13 | RNF-02 | Galería con miniaturas y flechas; tabla con edad | Detalle con 2 fotos | Navega 1→2→1, contador y tabla con estados y edad | Frontend | `detalle.spec.ts` (5 casos) |
| CP-US09-14 | VAL | Filtro por género | Prenda de niña | `genero=NINA` la incluye; género inválido 400 | Unitaria + integración | `CatalogoClienteServiceTest.generoInvalidoDevuelve400`, `CatalogoClienteIntegrationTest` |
| CP-US09-15 | RNF-02 | Menú aplica filtros sin recargar | En el catálogo, elegir Niñas en el menú | Nueva petición con `genero=NINA` y resultados | Frontend | `explorar.spec.ts` |
| CP-US09-16 | RNF-02 | Filtro por edad en meses | Talla 4 con 24–72, talla 6 con 72–144 | `edadMeses=36` → 1, `=200` → 0, `=-1` → 400 | Unitaria + integración | `CatalogoClienteServiceTest.edadFiltraPorRangoDeclaradoYNegativaEs400`, `CatalogoClienteIntegrationTest` |
| CP-US09-17 | RNF-02 | Materiales múltiples | Prenda algodón+poliéster | `ALGODON,POLIESTER` → 1, `CUERO` → 0 | Unitaria + integración | `CatalogoClienteServiceTest.materialesMultiplesFiltranPorAlgunComponente`, `CatalogoClienteIntegrationTest` |
| CP-US09-18 | RNF-02 | Parámetros mal tipados responden 400 | `pagina=abc`, `/catalogo/abc` | 400 con campo, no 500 | Controller | `CatalogoClienteControllerTest.paginaNoNumericaDevuelve400EnVezDe500`, `idNoNumericoDevuelve400EnVezDe500` |
| CP-US09-19 | RNF-02 | Tarjetas con badge y petición cancelada | Filtro mi-talla; filtros rápidos | Badge "Talla sugerida: 4"; `switchMap` cancela la anterior | Frontend | `explorar.spec.ts` (mi-talla + edad) |
