# Casos de prueba — US-08 Catálogo de Prendas e Inventario

> Trazabilidad: cada CP mapea a un RNF y a una prueba automatizada o a un request Postman numerado (PM-01…PM-12).

## 1. Matriz de casos

| ID | RNF | Descripción | Precondiciones | Resultado esperado | Tipo | Evidencia |
|---|---|---|---|---|---|---|
| CP-US08-01 | VAL | Crear prenda con tallas, composición y stock inicial | Vendedor autenticado | 201, tallas normalizadas, stock total 12, `DISPONIBLE`, `ACTIVA` | Unitaria / integración | `PrendaServiceTest.creaLaPrendaDelVendedorAutenticadoConTallasNormalizadasYEvaluaAlertas`, `CatalogoInventarioIntegrationTest` + PM-03 |
| CP-US08-02 | VAL | Composición que no suma 100 % o repite material | Prenda con 80 % + 10 % | 400 con `errores.composicion`, nada guardado | Unitaria / integración | `ValidadorPrendaTest.exigeQueLaComposicionSume100`, `rechazaMaterialesRepetidos`, `PrendaServiceTest.noGuardaNadaSiLaComposicionEsInvalida`, `CatalogoInventarioIntegrationTest.validaLaPrendaYLasImagenes` |
| CP-US08-03 | VAL | Tallas repetidas ("xs" y " XS ") o rangos invertidos | — | 400 "La talla XS está repetida" / rango inválido | Unitaria | `ValidadorPrendaTest.rechazaTallasRepetidasAunqueDifieranEnMayusculasYEspacios`, `rechazaRangosInvertidos` |
| CP-US08-04 | VAL | Editar no pisa el stock existente | Talla 4 con 8 uds; form envía 99 | Stock de la 4 sigue en 8, rangos actualizados, talla nueva con su stock | Unitaria | `PrendaServiceTest.editarConservaElStockDeTallasExistentesAgregaNuevasYQuitaLasVacias` |
| CP-US08-05 | VAL | No quitar una talla con unidades | Talla 6 con 4 uds | 409 con mensaje para dejarla en 0 desde inventario | Unitaria | `PrendaServiceTest.noPermiteQuitarUnaTallaConExistencias` |
| CP-US08-06 | RNF-25 | Carga masiva de fotos | Prenda creada | 201 con 2 imágenes, claves `prendas/{id}/…`, tipo por contenido, URL accesible | Unitaria / integración | `ImagenPrendaServiceTest.subeVariasImagenesDetectandoElTipoPorSuContenido`, `CatalogoInventarioIntegrationTest.altaFotosStockYAlertasDeReabastecimiento` + PM-04 |
| CP-US08-07 | RNF-25 | Archivo que no es imagen, demasiado grande o fuera de límite | Archivo `.png` con texto | 400; no se sube ningún archivo | Unitaria / integración | `ImagenPrendaServiceTest.siUnArchivoNoEsImagenNoSeSubeNinguno`, `rechazaArchivosQueSuperanElTamanoMaximo`, `respetaLosLimitesPorCargaYPorPrenda` |
| CP-US08-08 | RNF-25 | Consistencia BD ↔ S3 | 2.ª subida falla | Se borra el objeto ya subido al revertir; al eliminar, el objeto se borra solo tras commit | Unitaria | `ImagenPrendaServiceTest.siLaTransaccionNoSeConfirmaBorraLoQueYaSeSubio`, `alEliminarBorraElObjetoSoloDespuesDeConfirmar` |
| CP-US08-09 | RNF-25 | Integración con Amazon S3 (SDK v2) | Cliente S3 simulado | Bucket, clave con prefijo, content-type, URL pública o CDN; fallo → 502 sin detalle técnico | Unitaria | `S3AlmacenamientoImagenesTest` (5 casos) |
| CP-US08-10 | RNF-14 | Nivel de stock por mínimo | — | 0 → agotado; ≤ mínimo → bajo; > mínimo → disponible | Unitaria | `NivelStockTest` (6 casos) |
| CP-US08-11 | RNF-14 | Emisión y ciclo de vida de alertas | Prenda con mínimo 5 | Emite bajo y notifica; no repite; escala a agotado y notifica; reabastecer parcial reemplaza sin notificar; superar mínimo o desactivar resuelve | Unitaria | `AlertasInventarioServiceTest` (6 casos) |
| CP-US08-12 | RNF-14 | Actualizar stock emite y resuelve alertas en la misma transacción | PM-03 | 3 uds → `STOCK_BAJO`; 0 → `AGOTADO`; 20 → sin alertas; 2 alertas en historial | Integración / Postman | `CatalogoInventarioIntegrationTest.altaFotosStockYAlertasDeReabastecimiento` + PM-07…PM-10 |
| CP-US08-13 | VAL | Stock por talla inválido | Talla inexistente o repetida | 400 sin evaluar alertas | Unitaria | `InventarioServiceTest.rechazaUnaTallaQueLaPrendaNoTieneSinEvaluarAlertas`, `rechazaTallasRepetidasEnLaMismaPeticion` |
| CP-US08-14 | RNF-06 | Aislamiento entre vendedores y por rol | Vendedores A y B, cliente | B → 404 sobre prenda de A; cliente → 403; sin token → 401 | Unitaria / integración | `CatalogoInventarioIntegrationTest.cadaVendedorSoloAccedeASusPrendasYElClienteNoAccede`, `InventarioServiceTest.noPermiteModificarElInventarioDeOtroVendedor` + PM-11 |
| CP-US08-15 | RNF-29 | El motor de US-07 lee el catálogo real | Prenda activa con níquel | Adapter expone tabla, composición, stock por talla y marca; una prenda rota se omite; inactiva desaparece | Unitaria / integración | `CatalogoRecomendacionAdapterTest` (2), `CatalogoInventarioIntegrationTest.elMotorDeRecomendacionesLeeElCatalogoRealYSoloLasPrendasActivas` + PM-12 |
| CP-US08-16 | RNF-19 | UI-19 lista, busca sin tildes y publica/oculta | Catálogo con 2 prendas | Fila con composición, precio COP y nivel; filtro; PATCH estado | Frontend | `productos-lista.spec.ts` (4 casos) |
| CP-US08-17 | RNF-16 | UI-20 crea con fotos y recupera fallo parcial | Formulario válido | POST + 1 multipart; si fallan fotos: modo edición, fotos conservadas, reintento; formulario inválido no envía | Frontend | `prenda-form.spec.ts` (7 casos) |
| CP-US08-18 | RNF-19 | UI-21/UI-22 inventario y alertas | 1 prenda en stock bajo | Edición por talla con total en vivo, PUT correcto, contador de alertas; tarjetas con diferencia y mensaje | Frontend | `inventario.spec.ts` (5 casos) |
| CP-US08-19 | RNF-25 | Subida real a un bucket S3 | Bucket con lectura pública y credenciales IAM | Imagen visible desde la URL pública en Angular | Manual | Pendiente de ejecución en AWS |

## 2. Environment Postman
baseUrl = http://localhost:8080 (perfil `local`, almacenamiento local)

### PM-01 POST registro vendedor
POST {{baseUrl}}/api/auth/register
Content-Type: application/json

{
  "nombre": "Valeria",
  "apellido": "Gomez",
  "email": "vendedor@demo.com",
  "telefono": "3001234567",
  "password": "Clave1234",
  "confirmarPassword": "Clave1234",
  "rol": "VENDEDOR",
  "nombreTienda": "Mundo Pequeño",
  "aceptaTerminos": true
}

### PM-02 POST login
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{ "email": "vendedor@demo.com", "password": "Clave1234" }

### PM-03 POST crear prenda (CP-01)
POST {{baseUrl}}/api/vendedor/prendas
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "nombre": "Pantalón Jogger Azul",
  "categoria": "PANTALONES",
  "descripcion": "Pantalón cómodo con pretina elástica",
  "precio": 32900,
  "stockMinimo": 5,
  "colores": ["AZUL"],
  "estampado": "LISO",
  "tintesSinteticos": false,
  "brochesMetalicos": false,
  "contieneNiquel": false,
  "tratamientoFormaldehido": false,
  "composicion": [
    { "material": "ALGODON", "porcentaje": 80 },
    { "material": "POLIESTER", "porcentaje": 20 }
  ],
  "tallas": [
    { "talla": "4", "estaturaMinCm": 100, "estaturaMaxCm": 110, "pesoMinKg": 15, "pesoMaxKg": 20, "stock": 8 },
    { "talla": "6", "estaturaMinCm": 111, "estaturaMaxCm": 122, "pesoMinKg": 21, "pesoMaxKg": 26, "stock": 4 }
  ]
}

### PM-04 POST fotos (CP-06)
POST {{baseUrl}}/api/vendedor/prendas/{{prendaId}}/imagenes
Authorization: Bearer {{token}}
Body form-data: `archivos` (File) = frente.jpg, `archivos` (File) = espalda.png

### PM-05 GET mis prendas
GET {{baseUrl}}/api/vendedor/prendas?q=pantalon
Authorization: Bearer {{token}}

### PM-06 GET inventario
GET {{baseUrl}}/api/vendedor/inventario
Authorization: Bearer {{token}}

### PM-07 PUT stock bajo (CP-12)
PUT {{baseUrl}}/api/vendedor/inventario/{{prendaId}}
Authorization: Bearer {{token}}
Content-Type: application/json

{ "tallas": [ { "talla": "4", "stock": 2 }, { "talla": "6", "stock": 1 } ] }

### PM-08 GET alertas (CP-12)
GET {{baseUrl}}/api/vendedor/inventario/alertas
Authorization: Bearer {{token}}

### PM-09 PUT agotado (CP-12)
PUT {{baseUrl}}/api/vendedor/inventario/{{prendaId}}
Authorization: Bearer {{token}}
Content-Type: application/json

{ "tallas": [ { "talla": "4", "stock": 0 }, { "talla": "6", "stock": 0 } ] }

### PM-10 PUT reabastecer y cambiar mínimo (CP-12)
PUT {{baseUrl}}/api/vendedor/inventario/{{prendaId}}
Authorization: Bearer {{token}}
Content-Type: application/json

{ "tallas": [ { "talla": "4", "stock": 10 }, { "talla": "6", "stock": 10 } ], "stockMinimo": 4 }

### PM-11 GET prenda con token de cliente (CP-14)
GET {{baseUrl}}/api/vendedor/prendas/{{prendaId}}
Authorization: Bearer {{tokenCliente}}

→ 403. Con token de otro vendedor → 404.

### PM-12 GET recomendaciones con el catálogo real (CP-15)
Con el cliente y perfil de PM-01…PM-03 de US-07:

GET {{baseUrl}}/api/cliente/recomendaciones?perfilId={{perfilId}}
Authorization: Bearer {{tokenCliente}}

→ incluye la prenda de PM-03 con `tallaSugerida` según la tabla de tallas y `tieneStock`.
