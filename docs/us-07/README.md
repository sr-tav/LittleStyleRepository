# Documentación de US-07 — Motor de Recomendación Personalizada de Tallas

Yo como Cliente, quiero que el sistema compare las medidas de mi hijo con las prendas
y filtre telas con alérgenos, para seleccionar la talla óptima y evitar irritaciones.

## Qué se implementó

- **Estrategias de talla (ADR-12, Patrón Strategy):** `EXACTA` contiene estatura y peso,
  `PROXIMIDAD` mínima distancia normalizada al rango (5 cm = 1 kg) con umbral
  máximo 6.0: muy lejos es "sin talla compatible" (RNF-11). `HOLGURA` decorador:
  HOLGADA sube 1 (con tope), AJUSTADA baja 1 (con tope). Selección configurable
  por marca/categoría (`app.recomendacion.estrategia-por-*`); el `MaterialTextil`
  vive en `catalogo.model` para no ciclar módulos (ADR-02).
  `MotorRecomendacionService` depende de `EstrategiaTalla` + lista por nombre.
- **Filtro de alergias:** `FiltroAlergiaEstandar` mapea `AlergiaTextil` a `MaterialTextil`
    + flags (`tintesSinteticos, contieneNiquel, tratamientoFormaldehido`). Níquel,
      formaldehído y sintético >=30% = EXCLUIDA; lana, látex/elastano, cuero,
      algodón, tintes y broches = ADVERTENCIA; sintético <30% = APTA. El texto
      libre `otraAlergia` se agrega como motivo de advertencia.
- **Motor:** puntaje ("Compatibilidad %") 100 - distancia normalizada - 30 si
  advierte + 5 si hay stock: SOLO talla y seguridad, que es lo que mide RNF-11.
  La afinidad de estilo (+5 color, +5 estampado, tope 10) viaja en campo aparte
  y solo desempata; nunca excluye ni compensa advertencias. Orden por puntaje
  y luego afinidad, limitado por `app.recomendacion.max-resultados=24` (salvo
  `GET /ids`, sin tope para el filtro de servidor de la vitrina). La
  `tallaSugerida` viaja del carrito al pedido para medir adopción (RNF-11, ver
  `migracion-talla-sugerida.sql`).
  Degradación RNF-29: tabla vacía se salta, prenda rota se loguea y sigue,
  catálogo caído retorna lista vacía, parámetros malos responden 400: nunca 500
  por una prenda o un query param.
- **Perfil y endpoint:** `GET /api/cliente/recomendaciones?perfilId=` y
  `GET /api/cliente/recomendaciones/prenda/{prendaId}?perfilId=`.
  Valida dueño por `findByIdAndClienteId`, exige medición vigente
  (`vigencia-medidas-meses=6`), header `X-Recomendacion-Ms` para RNF-01.
- **Datos con US-08:** el motor consume el catálogo real mediante
  `CatalogoRecomendacionAdapter` (prendas `ACTIVA` con tabla de tallas,
  composición, stock e imagen). Los datos fijos de ejemplo se eliminaron
  al integrar US-08.
- **UI:** componente `app-talla-sugerida` con badge + etiqueta de advertencia y rama
  EXCLUIDA con motivo (la ficha convierte el 422 por alergia en ítem EXCLUIDA
  en vez del mensaje genérico); "Solo mi talla sugerida" filtra en el servidor
  (`GET /ids` + `GET /catalogo?ids=`), botón `Ver talla sugerida` en lista de
  perfiles. `tallaProvisional()` queda `@deprecated`.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Reglas biométricas, mapeo alergias, endpoints, RNF-01/11/29 |
| `casos-de-prueba.md` | CP-US07 con trazabilidad a tests |

## Evidencia

- Backend: `.\mvnw.cmd test -Dtest=EstrategiaTallaTest,FiltroAlergiaTest,MotorRecomendacionServiceTest,RecomendacionControllerTest`
- Frontend: desde `frontend/`, `npm.cmd test -- --watch=false`