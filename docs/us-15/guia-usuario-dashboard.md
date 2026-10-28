# US-15 — Guía de uso interactivo del dashboard

## 1. Acceso

Desde la aplicación, el administrador o vendedor puede acceder al dashboard desde el menú principal o desde la vista de inicio de su rol.

- Administrador: menú de operación y analítica.
- Vendedor: vista de desempeño de su catálogo.

## 2. Filtros del dashboard

Los filtros principales se encuentran en la parte superior del panel:

- Fecha inicial (`startDate`)
- Fecha final (`endDate`)
- Categoría (`categoryId`)

### Cómo usarlo

1. Selecciona la fecha de inicio.
2. Selecciona la fecha final.
3. Opcionalmente, filtra por categoría.
4. El sistema recarga los indicadores automáticamente.

## 3. Panel del administrador

### Indicadores principales

- Ventas totales en el periodo.
- Número de pedidos procesados.
- Ticket promedio.
- Ingresos del negocio.
- Stock total y alertas de inventario.

### Gráficas disponibles

- Ventas por categoría
- Demanda por talla
- Productos más vendidos
- Principales búsquedas

### Recomendación operativa

- Si las ventas por categoría bajan, revisar campañas y promociones.
- Si el stock bajo crece, ajustar reposición.
- Si las búsquedas muestran foco en un término, priorizar ese catálogo.

## 4. Panel del vendedor

### Indicadores principales

- Ventas del vendedor.
- Productos más vendidos de su negocio.
- Tallas con mayor demanda.
- Inventario crítico.
- Búsquedas relevantes para su catálogo.

### Recomendación operativa

- Revisar tallas con mayor salida para ajustar inventario.
- Notificar stock bajo de modelos populares.
- Mantener catálogo optimizado según búsquedas reales.

## 5. Interpretación visual

- Las tarjetas KPI sirven para lectura rápida.
- Los gráficos permiten detectar patrones por temporada, categoría o talla.
- Los colores se deben interpretar como alertas de rendimiento y riesgo de inventario.

## 6. Casos de uso comunes

### Caso 1 — Analizar el mes actual

1. Seleccionar mes actual.
2. Revisar total de ventas.
3. Corroborar top categorías.
4. Evaluar tallas con mayor demanda.

### Caso 2 — Revisar stock crítico

1. Fijar rango de 30 días.
2. Observa la métrica de stock bajo.
3. Priorizar reposición en prendas con demanda sostenida.

### Caso 3 — Confirmar impacto de una campaña

1. Filtrar por la categoría promocionada.
2. Compara ventas en el intervalo de campaña.
3. Revisa búsquedas relevantes y productos más vendidos.

## 7. Buenas prácticas

- Usar rangos cortos para análisis operativos diarios.
- Usar rangos amplios para comparación mensual o trimestral.
- Fijar una categoría para aislar la causa de cambios.
- Verificar que la fecha final no sea anterior a la fecha inicial.

## 8. Estado vacío

Cuando no hay datos en el rango elegido, el sistema indica:

- sin ventas registradas,
- sin búsquedas relevantes,
- sin productos vendidos,
- sin stock bajo en la categoría filtrada.

Esto evita confusiones y facilita la acción del usuario.

## 9. Trazabilidad de uso por subtarea 

-  la vista gráfica del dashboard está diseñada para administradores y vendedores con distintas métricas por rol.
- `los filtros de fechas y categoría se aplican en tiempo real a la visualización del panel.
-  los indicadores que se muestran corresponden a las agregaciones definidas en SQL y validadas en la capa analítica.
