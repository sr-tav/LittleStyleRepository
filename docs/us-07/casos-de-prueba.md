# Casos de prueba — US-07 Motor de Recomendación

> Trazabilidad: cada CP mapea a un RNF y a una prueba automatizada o a un request Postman numerado (PM-01…PM-09).

## 1. Matriz de casos

| ID | RNF | Descripción | Precondiciones | Resultado esperado | Tipo | Evidencia |
|---|---|---|---|---|---|---|
| CP-US07-01 | RNF-11 | Talla exacta en bordes (110cm/20kg → talla 4) | Tabla 4: 100-110 / 15-20 | Retorna talla 4 | Unitaria | `EstrategiaTallaTest.exactaEnBorde` |
| CP-US07-02 | RNF-11 | Sin exacta usa proximidad (150/40 → talla 6) | Bio fuera de rango | Exacta vacía, proximidad retorna 6 | Unitaria | `EstrategiaTallaTest.proximidadCuandoNoHayExacta` |
| CP-US07-03 | RNF-11 | HOLGADA sube 1 talla, REGULAR no | Bio 105/18 exacta 4 | HOLGADA→6, REGULAR→4 | Unitaria | `EstrategiaTallaTest.holgadaSubeUnaTalla` |
| CP-US07-04 | RNF-29 | Tabla vacía no rompe | Tabla sin rangos | Optional vacío | Unitaria | `EstrategiaTallaTest.tablaVaciaRetornaEmpty` |
| CP-US07-05 | VAL | Poliéster 100% + FIBRAS_SINTETICAS excluye | Alergia declarada | EXCLUIDA con motivo | Unitaria | `FiltroAlergiaTest.sintetico100Excluye` |
| CP-US07-06 | VAL | Tintes solo advierte | Alergia TINTES | ADVERTENCIA, sigue listada | Unitaria | `FiltroAlergiaTest.tintesSoloAdvierte` |
| CP-US07-07 | VAL | Níquel excluye | Alergia NIQUEL | EXCLUIDA | Unitaria | `FiltroAlergiaTest.niquelExcluye` |
| CP-US07-08 | RNF-11/29 | Motor filtra níquel, ignora sin tabla y ordena | Mock 3 prendas | Solo 1 buena, primera | Unitaria mock | `MotorRecomendacionServiceTest.ordenaExcluyeEIgnoraTablaVacia` |
| CP-US07-09 | RNF-29 | Catálogo caído degrada a vacío | Port lanza Runtime | Lista vacía, no lanza | Unitaria | `MotorRecomendacionServiceTest.degradacionAnteFalloCatalogo` |
| CP-US07-10 | RNF-29 | Prenda null no tumba motor | Lista con null + buena | 1 resultado | Unitaria | `MotorRecomendacionServiceTest.unaPrendaRotaNoTumbaLasDemas` |
| CP-US07-11 | RNF-01 | GET lista 200 + header tiempo | PM-01→PM-03 ok | 200 + `X-Recomendacion-Ms` + `tallaSugerida` | Controller/Postman | `RecomendacionControllerTest` + PM-05 |
| CP-US07-12 | VAL | GET por prenda apta 200 | Prenda 1 algodón | 200 talla 4, puntaje calculado | Controller/Postman | PM-06 |
| CP-US07-13 | VAL | GET prenda excluida 422 | Prenda 2 níquel + alergia | 422 motivo níquel | Controller/Postman | PM-07 |
| CP-US07-14 | RNF-06 | 401 sin token, 403 vendedor, 404 ajeno | Tokens varios | 401 / 403 / 404 | Postman | PM-08 |
| CP-US07-15 | RNF-16 | 422 sin medición o desactualizada | Perfil sin medición o 7 meses vieja | 422 mensaje claro | Postman | PM-09 |
| CP-US07-16 | RNF-19 | UI badge + advertencia + stock | PM-05 con ADVERTENCIA | Badge visible, alerta amarilla | Manual front | `/cliente/recomendaciones?perfilId=` |

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