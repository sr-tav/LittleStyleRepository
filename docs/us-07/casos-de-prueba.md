# Casos de prueba — US-07 Motor de Recomendación

> Trazabilidad: cada CP mapea a un RNF y a una prueba automatizada o a un request Postman numerado (PM-01…PM-09).

## 1. Matriz de casos

| ID | RNF | Descripción | Precondiciones | Resultado esperado | Tipo | Evidencia |
|---|---|---|---|---|---|---|
| CP-US07-01 | RNF-11 | Talla exacta en bordes (110cm/20kg → talla 4) | Tabla 4: 100-110 / 15-20 | Retorna talla 4 | Unitaria | `EstrategiaTallaTest.exactaEnBorde` |
| CP-US07-02 | RNF-11 | Sin exacta usa proximidad cerca (109/21 → talla 6); muy lejos (150/40) queda sin talla compatible | Bio fuera de rango | Exacta vacía; proximidad retorna 6 o vacío según umbral 6.0 | Unitaria | `EstrategiaTallaTest.proximidadCuandoNoHayExacta`, `proximidadMuyLejosDevuelveVacioRNF11` |
| CP-US07-03 | RNF-11 | HOLGADA sube 1 talla (con tope), AJUSTADA baja 1, REGULAR no | Bio 105/18 exacta 4; 115/24 exacta 6 | HOLGADA→6, REGULAR→4, AJUSTADA→4, HOLGADA en última→6 | Unitaria | `EstrategiaTallaTest.holgadaSubeUnaTalla`, `holgadaEnUltimaTallaNoSeSaleDeLaTabla`, `ajustadaBajaUnaTalla` |
| CP-US07-04 | RNF-29 | Tabla vacía no rompe | Tabla sin rangos | Optional vacío | Unitaria | `EstrategiaTallaTest.tablaVaciaRetornaEmpty` |
| CP-US07-05 | RNF-11 | Poliéster 100% + FIBRAS_SINTETICAS excluye; 29% queda APTA | Alergia declarada | EXCLUIDA con motivo / APTA bajo el umbral 30% | Unitaria | `FiltroAlergiaTest.sintetico100Excluye`, `sinteticoEnElUmbralExcluyeYBajoElUmbralNo` |
| CP-US07-06 | RNF-11 | Tintes, lana, látex/elastano, cuero, algodón y broches solo advierten | Alergia declarada | ADVERTENCIA, sigue listada | Unitaria | `FiltroAlergiaTest.tintesSoloAdvierte`, `lanaSoloAdvierte`, `latexOElastanoSoloAdvierte`, `cueroSoloAdvierte`, `algodonSoloAdvierte`, `brochesSoloAdvierten` |
| CP-US07-07 | RNF-11 | Níquel y formaldehído excluyen; varias alergias acumulan motivos | Alergia declarada | EXCLUIDA | Unitaria | `FiltroAlergiaTest.niquelExcluye`, `formaldehidoExcluye`, `variasAlergiasAcumulanMotivosYExcluyenSiAlgunaExcluye` |
| CP-US07-07b | RNF-11 | OTRA sin composición advierte; con composición es apta; texto libre advierte | Alergia OTRA ± texto | ADVERTENCIA / APTA | Unitaria | `FiltroAlergiaTest.composicionVaciaSoloAdvierteConOtra`, `otraConComposicionDeclaradaEsApta`, `MotorRecomendacionServiceTest.otraAlergiaConTextoAdvierteAunqueHayaComposicion` |
| CP-US07-08 | RNF-11/29 | Motor filtra níquel, ignora sin tabla y ordena | Mock 3 prendas | Solo 1 buena, primera | Unitaria mock | `MotorRecomendacionServiceTest.ordenaExcluyeEIgnoraTablaVacia` |
| CP-US07-09 | RNF-29 | Catálogo caído degrada a vacío | Port lanza Runtime | Lista vacía, no lanza | Unitaria | `MotorRecomendacionServiceTest.degradacionAnteFalloCatalogo` |
| CP-US07-10 | RNF-29 | Prenda null no tumba motor | Lista con null + buena | 1 resultado | Unitaria | `MotorRecomendacionServiceTest.unaPrendaRotaNoTumbaLasDemas` |
| CP-US07-11 | RNF-01 | GET lista 200 + header tiempo | PM-01→PM-03 ok | 200 + `X-Recomendacion-Ms` + `tallaSugerida` | Controller/Postman | `RecomendacionControllerTest` + PM-05 |
| CP-US07-12 | RNF-11 | GET por prenda apta 200 | Prenda 1 algodón | 200 talla 4, puntaje calculado | Controller/Postman | PM-06 |
| CP-US07-13 | RNF-11 | GET prenda excluida 422 | Prenda 2 níquel + alergia | 422 motivo níquel | Controller/Postman | PM-07 |
| CP-US07-14 | RNF-06 | 401 sin token, 403 vendedor, 404 ajeno | Tokens varios | 401 / 403 / 404 | Postman | PM-08 |
| CP-US07-15 | RNF-29 | 422 sin medición o desactualizada | Perfil sin medición o 7 meses vieja | 422 mensaje claro (error controlado, no 500) | Postman | PM-09 |
| CP-US07-16 | RNF-11 | UI badge + advertencia + stock; 422 por alergia muestra motivo EXCLUIDA | PM-05 con ADVERTENCIA; ficha con 422 | Badge visible, alerta amarilla; ficha muestra "No compatible" con motivo | Front auto | `talla-sugerida.spec`, `recomendaciones-lista.spec`, `detalle.spec` (US-07) |
| CP-US07-17 | RNF-29 | Sin perfilId o con tipo inválido responde 400, no 500 | GET sin `perfilId` / `perfilId=abc` | 400 con campo señalado | Controller | `RecomendacionControllerTest.sinPerfilIdDevuelve400EnVezDe500`, `perfilIdNoNumericoDevuelve400EnVezDe500` |
| CP-US07-18 | RNF-02 | "Solo mi talla" filtra en el servidor con ids sin tope | Perfil 3, catálogo paginado | `GET /ids` + `GET /catalogo?ids=` pagina consistente | Front auto | `recomendacion.service.spec`, `explorar.spec` (US-07) |
| CP-US07-19 | RNF-11 | Preferencias van en afinidad aparte, sin contaminar puntaje; estrategia configurable | Perfil con preferencias; prenda advertida a juego; config por categoría | Igual puntaje, afinidad decide; advertida queda debajo aunque tenga afinidad 10; categoría usa su estrategia | Unitaria | `MotorRecomendacionServiceTest.preferenciasDeColorYEstampadoSubenElPuntaje`, `afinidadNuncaCompensaUnaAdvertencia`, `estrategiaSeConfiguraPorCategoria` |
| CP-US07-20 | RNF-11 | La talla sugerida viaja del carrito al pedido | Agregar con sugerida "4" | `talla_sugerida` persistida en ítem y línea | Unitaria | `CheckoutFacadeTest.conservaLaTallaSugeridaDelMotorParaMedirAdopcionRNF11` |

## 2. Environment Postman
baseUrl = http://localhost:8080 

### PM-01 POST registro
POST {{baseUrl}}/api/auth/register
Content-Type: application/json

{
  "nombre": "Laura",
  "apellido": "Gomez",
  "email": "cliente@demo.com",
  "telefono": "3001234567",
  "password": "Clave1234",
  "confirmarPassword": "Clave1234",
  "rol": "CLIENTE",
  "nombreTienda": null,
  "aceptaTerminos": true
}

### PM-02 POST login
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{ "email": "cliente@demo.com", "password": "Clave1234" }

### PM-03 POST perfil con medición inicial
POST {{baseUrl}}/api/cliente/perfiles/con-medicion-inicial
Authorization: Bearer {{token}}
Content-Type: application/json

{
"perfil": {
"nombre": "Sofi",
"fechaNacimiento": "2021-03-10",
"contextura": "MEDIA",
"holgura": "HOLGADA",
"alergias": ["NIQUEL"],
"otraAlergia": null,
"sinAlergias": false,
"coloresPreferidos": ["AZUL"],
"estampadosPreferidos": ["LISO"],
"otroColor": null,
"otroEstampado": null
},
"medicionInicial": {
"fechaMedicion": "2026-10-05",
"estaturaCm": 105,
"pesoKg": 18
}
}

### PM-04 POST medición adicional
POST {{baseUrl}}/api/cliente/perfiles/{{perfilId}}/mediciones
Authorization: Bearer {{token}}
Content-Type: application/json

{ "fechaMedicion": "2026-10-06", "estaturaCm": 106, "pesoKg": 18.5 }

### PM-05 GET recomendaciones (CP-11)
GET {{baseUrl}}/api/cliente/recomendaciones?perfilId={{perfilId}}
Authorization: Bearer {{token}}

### PM-06 GET por prenda apta (CP-12)
GET {{baseUrl}}/api/cliente/recomendaciones/prenda/1?perfilId={{perfilId}}
Authorization: Bearer {{token}}

### PM-07 GET por prenda excluida (CP-13)
GET {{baseUrl}}/api/cliente/recomendaciones/prenda/2?perfilId={{perfilId}}
Authorization: Bearer {{token}}

### PM-08 seguridad y dueño (CP-14)
# 401: mismo GET sin header Authorization → 401
# 403: repetir con token de VENDEDOR → 403
# 404: GET con perfilId de otro cliente (token válido) → 404 "Perfil infantil no encontrado"

### PM-09 medición ausente o desactualizada (CP-15)
# Perfil sin mediciones:
GET {{baseUrl}}/api/cliente/recomendaciones?perfilId={{perfilSinMedicion}}
Authorization: Bearer {{token}}
# → 422 "El perfil no tiene mediciones"
# Perfil con última medición de hace 7+ meses → 422 "Medición desactualizada, registra una nueva"