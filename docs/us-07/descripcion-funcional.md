# US-07 Descripción funcional — Motor de Recomendación Personalizada de Tallas

## 1. Objetivo
Comparar medidas reales del hijo con tablas de prendas y excluir/advertir
por alergias textiles para dar la talla óptima.

## 2. Alcance
Incluye estrategias EXACTA/PROXIMIDAD/HOLGURA, filtro de alergias,
`MotorRecomendacionService` con puntaje y orden, endpoints GET,
catálogo real de US-08 (`CatalogoRecomendacionAdapter`) y UI-6 con
tarjeta de producto (foto, precio, badge + advertencias).
No incluye carrito, ni ML.

## 3. Reglas
- EXACTA: estatura y peso dentro del mismo `RangoTalla`.
- PROXIMIDAD: solo si EXACTA vacía, mínima distancia al centro del rango.
- HOLGURA: si fue EXACTA y holgura=HOLGADA sube 1 talla.
- FIBRAS_SINTETICAS >=30% + TINTES/NIQUEL/FORMALDEHIDO: níquel, formol y
  sintético excluyen, resto advierte. Sin composición + OTRA advierte.
- Puntaje: 100 - distancia - 30 si advierte + 5 si hay stock. Orden desc,
  tope `max-resultados=24`.
- Degradación RNF-29: tabla vacía se salta, prenda rota se aísla,
  catálogo caído devuelve vacío.

## 4. Seguridad y datos
Solo CLIENTE dueño. `PerfilBiometricoAdapter` usa `findByIdAndClienteId`,
401 sin token, 403 otro rol, 404 perfil ajeno, 422 sin medición o
desactualizada >6 meses. Medidas cifradas en reposo, motor usa DTO
desencriptado en memoria.

## 5. Endpoints
- `GET /api/cliente/recomendaciones?perfilId=` → 200 lista + header
  `X-Recomendacion-Ms`, 401/403/404/422.
- `GET /api/cliente/recomendaciones/prenda/{id}?perfilId=` → 200 item,
  404 prenda, 422 excluida o sin talla.

## 6. Limitaciones
Las métricas de RNF-01/11/29 se miden en US-13; hasta el paso final
el header `X-Recomendacion-Ms` es la evidencia de tiempo de respuesta.