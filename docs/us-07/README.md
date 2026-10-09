# Documentación de US-07 — Motor de Recomendación Personalizada de Tallas

Yo como Cliente, quiero que el sistema compare las medidas de mi hijo con las prendas
y filtre telas con alérgenos, para seleccionar la talla óptima y evitar irritaciones.

## Qué se implementó

- **Estrategias de talla (ADR-12, Patrón Strategy):** `EXACTA` contiene estatura y peso,
  `PROXIMIDAD` mínima distancia al centro del rango, `HOLGURA` decorador que sube
  una talla si es HOLGADA. `MotorRecomendacionService` solo depende de `EstrategiaTalla`.
- **Filtro de alergias:** `FiltroAlergiaEstandar` mapea `AlergiaTextil` a `MaterialTextil`
    + flags (`tintesSinteticos, contieneNiquel, tratamientoFormaldehido`). Níquel,
      formaldehído y sintético >=30% = EXCLUIDA, resto = ADVERTENCIA.
- **Motor:** puntaje 100 - distancia - 30 si advierte + 5 si hay stock, ordenado
  descendente, limitado por `app.recomendacion.max-resultados=24`.
  Degradación RNF-29: tabla vacía se salta, prenda rota se loguea y sigue,
  catálogo caído retorna lista vacía, nunca 500 por una prenda.
- **Perfil y endpoint:** `GET /api/cliente/recomendaciones?perfilId=` y
  `GET /api/cliente/recomendaciones/prenda/{prendaId}?perfilId=`.
  Valida dueño por `findByIdAndClienteId`, exige medición vigente
  (`vigencia-medidas-meses=6`), header `X-Recomendacion-Ms` para RNF-01.
- **Datos con US-08:** el motor consume el catálogo real mediante
  `CatalogoRecomendacionAdapter` (prendas `ACTIVA` con tabla de tallas,
  composición, stock e imagen). Los datos fijos de ejemplo se eliminaron
  al integrar US-08.
- **UI-6:** componente `app-talla-sugerida` con badge + etiqueta de advertencia,
  botón `Ver talla sugerida` en lista de perfiles. `tallaProvisional()` queda
  `@deprecated`.

## Archivos de documentación

| Archivo | Contenido |
|---|---|
| `descripcion-funcional.md` | Reglas biométricas, mapeo alergias, endpoints, RNF-01/11/29 |
| `casos-de-prueba.md` | CP-US07 con trazabilidad a tests |

## Evidencia

- Backend: `.\mvnw.cmd test -Dtest=EstrategiaTallaTest,FiltroAlergiaTest,MotorRecomendacionServiceTest,RecomendacionControllerTest`
- Frontend: desde `frontend/`, `npm.cmd test -- --watch=false`