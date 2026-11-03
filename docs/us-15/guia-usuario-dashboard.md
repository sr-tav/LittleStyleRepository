# Guía de usuario — Dashboard y reportes

Esta guía explica cómo usar el panel analítico de LittleStyle. Está dirigida a administradores y
vendedores.

## 1. Acceso

1. Inicia sesión como **administrador** o **vendedor**.
2. Abre **Reportes** en el menú principal.
3. El panel muestra el alcance de tu sesión: el vendedor ve su tienda; el administrador, toda la
   plataforma.
4. Un usuario con rol **cliente** no ve esta opción.

## 2. Filtros

En la cabecera del panel encontrarás cuatro controles:

| Control | Para qué sirve |
|---|---|
| **Periodo** | Atajos: últimos 30 días, este mes o últimos 6 meses |
| **Desde** | Fecha inicial del rango |
| **Hasta** | Fecha final del rango |
| **Categorías** | Limita ventas, inventario y rankings a una categoría |

Cómo usarlo:

1. Elige un período o escribe las fechas manualmente.
2. Opcionalmente, selecciona una categoría.
3. El panel se actualiza solo; no hace falta recargar la página.
4. Si la fecha inicial queda después de la final, el sistema no aplica el cambio y muestra un
   mensaje para corregirlo.

> El **inventario** no depende de las fechas: siempre muestra la foto actual del stock.

## 3. Indicadores principales

Las seis tarjetas de la parte superior responden a una lectura rápida:

| Indicador | Qué significa |
|---|---|
| **Ventas totales** | Dinero vendido en el período, en COP |
| **Unidades vendidas** | Prendas vendidas, sumando cantidades |
| **Ticket promedio** | Ventas totales divididas entre pedidos |
| **Pedidos** | Pedidos que cuentan como venta |
| **Inventario disponible** | Unidades en stock de tus prendas activas |
| **Variantes con stock bajo** | Combinaciones de talla y color que llegaron a su mínimo |

Una **variante** es una combinación de prenda, talla y color. Si un buzo tiene la talla 4 en azul y
en rojo, son dos variantes con stock propio. Por eso el contador de stock bajo puede subir más de
una vez por la misma talla: cada color se repone por separado.

Una variante sin mínimo configurado no genera alerta. Si ves un número en ámbar, hay al menos una
variante en el umbral.

El **margen bruto** no se muestra porque las prendas todavía no registran su costo. El panel lo
explica en lugar de mostrar un cero engañoso.

## 4. Panel del administrador

Además de los KPI, el administrador ve:

- **Más consultados:** prendas cuya ficha se abrió más veces en el período.
- **Más vendidos:** prendas con más unidades vendidas.
- **Palabras más buscadas:** términos usados en el catálogo. Es una señal de toda la plataforma, no
  de una tienda en particular.
- **Demanda por talla:** unidades vendidas por tamaño. El color no se separa en este gráfico; se
  conserva en el pedido para auditoría.
- **Ventas por categoría:** monto vendido en cada categoría, en COP.

Cada lista muestra hasta cinco ítems para mantener la lectura rápida; el resto se consulta en los
reportes exportables.

## 5. Panel del vendedor

El vendedor ve los mismos seis KPI, calculados solo con su catálogo, y los dos gráficos:

- **Demanda por talla (unidades)** de sus ventas.
- **Ventas por categoría (COP)** de su tienda.

No se muestran las listas de “más consultados”, “más vendidos” y “más buscadas”: el panel del
vendedor prioriza la evolución de su catálogo y evita duplicar lo que ya puede exportar.

## 6. Interpretación visual

- Las tarjetas KPI son para lectura rápida; los gráficos, para detectar patrones.
- La línea de demanda por talla responde “qué tamaño se vende más”.
- Las barras de categoría responden “dónde se concentra el ingreso”.
- El color ámbar en la tarjeta de stock bajo indica riesgo de reposición; no es un error.
- Los estados vacíos (“Sin ventas en el período seleccionado”) significan que no hay datos, no que
  la pantalla haya fallado.

## 7. Casos de uso comunes

### Caso 1 — Analizar el mes actual

1. Elige **Este mes**.
2. Revisa ventas totales y ticket promedio.
3. Identifica la categoría con más ingreso.
4. Mira qué tallas concentran la demanda.

### Caso 2 — Revisar stock crítico

1. Deja el período en **Últimos 30 días**.
2. Observa **Variantes con stock bajo**.
3. Abre el inventario o el reporte de inventario para ver la talla y el color exactos.
4. Prioriza la reposición de las variantes con más demanda.

### Caso 3 — Confirmar el impacto de una campaña

1. Filtra por la categoría promocionada.
2. Ajusta las fechas al intervalo de la campaña.
3. Compara ventas por categoría y demanda por talla.
4. Revisa las palabras más buscadas para entender el interés del momento.

### Caso 4 — Exportar lo que estás viendo

1. Ajusta período y categoría.
2. Baja hasta **Exportar reporte**.
3. Elige PDF, Excel o CSV.
4. La exportación usa los mismos filtros y el alcance de tu rol.

## 8. Diseño y pantallas pequeñas

- En escritorio, los KPI se ven en tres columnas y los gráficos en dos.
- En pantallas de hasta 900 px, todo pasa a una columna y los filtros se acomodan en varias filas.
- Los controles son táctiles (42 px de alto) y tienen foco visible para navegación con teclado.
- Los gráficos conservan su proporción y no se desbordan de la tarjeta.
- Si el panel no carga, aparece **Error al cargar datos** con un botón **Reintentar**.

## 9. Buenas prácticas

- Usa rangos cortos para la operación diaria y rangos amplios para comparar meses.
- Fija una categoría cuando quieras aislar la causa de un cambio.
- Verifica que la fecha final no sea anterior a la inicial.
- Recuerda que las búsquedas y las vistas se conservan 12 meses: los rangos más largos no
  reconstruyen historia anterior a la retención.
- Para el detalle por color, usa el inventario o el reporte de inventario; el gráfico de demanda
  solo separa por talla.
