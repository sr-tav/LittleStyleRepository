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
- PROXIMIDAD: solo si EXACTA vacía, mínima distancia normalizada al rango
  (5 cm = 1 kg; antes se mezclaban cm y kg sin normalizar) y solo hasta
  `DISTANCIA_MAXIMA = 6.0`. Por encima es "sin talla compatible" (422 en
  por-prenda, prenda omitida en el listado): 150 cm / 40 kg frente a un
  máximo de 122 cm / 26 kg ya no sugiere la talla 6 (RNF-11).
- HOLGURA: sobre coincidencia exacta, HOLGADA sube 1 talla (con tope en la
  última), AJUSTADA baja 1 (con tope en la primera), REGULAR no cambia. El
  stock de la talla resultante no se decide en la estrategia (no conoce
  inventario): el motor lo informa con `tieneStock` y el checkout lo valida
  antes de vender (RNF-03).
- Alergias (decisión documentada: solo lo desconocido o lo grave excluye):
  níquel, formaldehído y FIBRAS_SINTETICAS >= 30% = EXCLUIDA; lana, látex/
  elastano, cuero, algodón, tintes y broches metálicos = ADVERTENCIA (se
  muestran con el motivo y la prenda sigue disponible). Sintético < 30% =
  APTA sin advertencia (umbral de diseño: trazas de elastano para ajuste).
  Sin composición + OTRA = ADVERTENCIA; el texto libre `otraAlergia` antes
  se perdía y ahora se agrega como motivo de ADVERTENCIA en todas las
  prendas cuando se declaró OTRA.
- Preferencias (Definición, proceso 1): +5 si un color de la prenda está
  entre los preferidos, +5 si coincide el estampado, con tope 10. Van en el
  campo aparte `afinidad`: solo reordenan a igualdad de compatibilidad;
  nunca excluyen ni compensan una advertencia. La contextura se captura en
  el perfil pero no puntúa (el ajuste lo cubre la holgura).
- Estrategia configurable (ADR-12): `app.recomendacion.estrategia-por-defecto`,
  `estrategia-por-categoria.*` y `estrategia-por-marca.*` (la marca manda);
  nombre desconocido o ausente usa el bean `@Primary` sin romper.
- Puntaje ("Compatibilidad %"): 100 - distancia normalizada - 30 si advierte
  + 5 si hay stock. SOLO talla y seguridad: es lo que mide RNF-11, sin
  contaminar con gustos. Orden desc por puntaje y luego por afinidad, tope
  `max-resultados=24` en el listado; `GET /ids` no tiene tope para el filtro
  de servidor de la vitrina (`GET /catalogo?ids=` pagina en BD, sin falsos
  negativos).
- RNF-11 medible: `tallaSugerida` viaja del carrito (`carrito_items`) a la
  línea del pedido (`pedido_items`); nula si se compró sin sugerencia. Ver
  `migracion-talla-sugerida.sql` (prod usa `ddl-auto=validate`).
- Degradación RNF-29: tabla vacía se salta, prenda rota se aísla,
  catálogo caído devuelve vacío; parámetros ausentes o mal tipados
  responden 400 (no 500).

## 4. Seguridad y datos
Solo CLIENTE dueño. `PerfilBiometricoAdapter` usa `findByIdAndClienteId`,
401 sin token, 403 otro rol, 404 perfil ajeno, 422 sin medición o
desactualizada >6 meses. Medidas cifradas en reposo, motor usa DTO
desencriptado en memoria. El adapter accede directo al repositorio de
perfiles: es legítimo en hexagonal (el adapter implementa el puerto y no
fuga entidades, solo devuelve `PerfilBiometrico`). `Pedido` usa
`TextoLegadoCifradoConverter`: es un conversor criptográfico genérico sin
lógica del dominio de perfiles, por eso se conserva donde está en lugar de
duplicarlo en `shared`.
RNF-01: el listado carga todas las prendas ACTIVA porque el ranking
(top-24) exige barrido completo; el tiempo queda en `X-Recomendacion-Ms`
y la medición del percentil 95 ≤ 2 s es de US-13.

## 5. Endpoints
- `GET /api/cliente/recomendaciones?perfilId=` → 200 lista + header
  `X-Recomendacion-Ms`, 400 sin `perfilId` o con tipo inválido,
  401/403/404/422.
- `GET /api/cliente/recomendaciones/ids?perfilId=` → 200 con solo ids
  (sin tope) para el filtro de servidor de la vitrina.
- `GET /api/cliente/recomendaciones/prenda/{id}?perfilId=` → 200 item,
  404 prenda, 422 excluida o sin talla.
- Vitrina (US-09): `GET /api/cliente/catalogo?...&ids=1,2` pagina en BD la
  intersección con los recomendados.

## 6. Limitaciones
Las métricas de RNF-01/11/29 se miden en US-13; hasta el paso final
el header `X-Recomendacion-Ms` es la evidencia de tiempo de respuesta.
La seguridad 401/403/404 sigue verificada solo con Postman (PM-08): no
hay test de integración con Spring Security para este endpoint.