# Pruebas de carga (US-13, fase 5)

Prueba con [Artillery](https://www.artillery.io/) del catálogo con filtros y del motor de recomendación.
Cubre estas métricas del *Plan de Gestión de la Calidad*:

| Métrica | Meta | Dónde se lee en `informe.html` |
|---------|------|--------------------------------|
| Tasa de procesamiento de carga simultánea | ≥ 99 % sin error con 100 usuarios | *Métricas del Plan de Calidad* |
| Efectividad de filtros en catálogo | < 1.5 s | Ídem y tabla por endpoint (`Filtro…`, `Búsqueda…`, `Catálogo…`) |
| Tiempo de carga de recomendaciones / tiempo medio de respuesta del motor | < 2 s | Ídem y fila `Recomendaciones` |
| Consultas de recomendación sin fallos | ≥ 99 % | Ídem |
| Consumo de CPU y RAM | — | No sale del informe: ver *Durante la prueba* |

## Qué hace

1. **Una sola vez:** registra un cliente nuevo (`carga.<número>@littlestyle.test`) con un perfil infantil.
2. Durante 3.5 minutos:
   - **Calentamiento** (30 s): 2 usuarios nuevos por segundo.
   - **Rampa** (60 s): de 2 a 25 por segundo.
   - **Pico** (120 s): 25 por segundo, con un tope de **100 usuarios simultáneos**.
3. Cada usuario hace uno de dos recorridos:
   - **Explorar catálogo** (60 %): sin filtros, categoría y género, talla + precio + orden, búsqueda por
     texto, "solo mi talla" y el detalle de una prenda.
   - **Recomendaciones** (40 %): recomendaciones del perfil, ids recomendados y el catálogo del perfil.

Además del tiempo que ve el usuario (incluye la red), el informe muestra el tiempo **dentro del servidor**,
que el backend reporta en las cabeceras `X-Catalogo-Ms` y `X-Recomendacion-Ms`.

## Ejecutar contra producción

Desde tu PC, con Node instalado (no hace falta instalar nada más). La EC2 y la RDS deben estar prendidas.

```bash
cd pruebas-carga
TARGET=https://littlestyle.duckdns.org npx -y artillery@2.0.34 run --output informe.json catalogo-recomendaciones.yml
node informe.js informe.json
```

En PowerShell, la primera línea es:

```powershell
$env:TARGET="https://littlestyle.duckdns.org"; npx -y artillery@2.0.34 run --output informe.json catalogo-recomendaciones.yml
```

Abre `informe.html` en el navegador. Si alguna meta no se cumple, Artillery termina con
`ensure` en rojo (p95 general > 1.5 s o más de 1 % de errores).

Para probar en local, con el stack de Docker arriba: `TARGET=http://localhost`.

## Durante la prueba (CPU y RAM)

Mientras corre, en otra terminal por SSH a la EC2:

```bash
docker stats --no-stream      # repetir 2 o 3 veces durante el pico (minutos 1:30 a 3:30)
```

Al terminar: EC2 → la instancia → pestaña **Monitoring** → **CPU utilization**. Selecciona la última
hora para ver el pico de la prueba.

## Datos que deja

Cada ejecución crea un cliente `carga.*@littlestyle.test` con un perfil completo y no hace compras. No
afecta las métricas de compras ni de pedidos.
