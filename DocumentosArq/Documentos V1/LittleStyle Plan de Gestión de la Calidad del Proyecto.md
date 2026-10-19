  

**HOJA DE CONTROL**

| Proyecto | LittleStyle |  |  |
| :---- | :---- | :---- | :---: |
| **Entregable** | Especificación de Requisitos |  |  |
| **Versión/Edición** | 0100 | **Fecha Versión** | 20/09/2026 |
| **Aprobado por** |  | **Fecha Aprobación** | DD/MM/AAAA |
|  |  | **Nº Total de Páginas** |  |

**REGISTRO DE CAMBIOS**

| Versión | Causa del Cambio | Responsable del Cambio | Fecha del Cambio |
| :---: | :---: | :---: | :---: |
| 01 | Versión inicial | Oscar Tomas Aristizabal Valentina Gónzalez Diaz Valentina Rodríguez Castro | 20/09/2026 |
|  |  |  |  |
|  |  |  |  |

**AUTORES**

| Nombre y Apellidos |
| :---- |
| Valentina Gonzalez Diaz (valentina.gonzalezd@uqvirtual.edu.co) |
| Tomas Aristizabal Velasquez (oscart.aristizabalv@uqvirtual.edu.co) |
| Valentina Rodriguez Castro (valentina.rodriguezc1@uqvirtual.edu.co) |

**TABLA DE CONTENIDO**

[**1\. INTRODUCCIÓN	6**](#1.-introducción)

[1.1 Alcance	6](#1.1-alcance)

[1.2 Objetivos	7](#1.2-objetivos)

[1.2.1 Objetivo general	7](#1.2.1-objetivo-general)

[1.2.2 Objetivos específicos	7](#1.2.2-objetivos-específicos)

[1.3 Glosario de términos	8](#1.3-glosario-de-términos)

[**2\. INFORMACIÓN DEL CASO DE ESTUDIO	10**](#2.-información-del-caso-de-estudio)

[**3\. PLAN DE GESTIÓN DE LA CALIDAD	10**](#3.-plan-de-gestión-de-la-calidad)

[3.1 Calidad del Software	10](#3.1-calidad-del-software)

[3.2 Factores de Calidad del Software	11](#3.2-factores-de-calidad-del-software)

[1\. Confiabilidad	11](#confiabilidad)

[3.3 Métricas para la Calidad de un Proceso	12](#3.3-métricas-para-la-calidad-de-un-proceso)

[3.3.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de Tallas y Prendas	12](#3.3.1-proceso-1:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[3.3.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario	13](#3.3.2-proceso-2:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[3.3.3 Proceso 3: Gestión Administrativa y Procesos Internos	14](#3.3.3-proceso-3:-gestión-administrativa-y-procesos-internos)

[3.4 Creación de Métricas para la Calidad	15](#3.4-creación-de-métricas-para-la-calidad)

[3.4.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de Tallas y Prendas	16](#3.4.1-proceso-1:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[3.4.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario	18](#3.4.2-proceso-2:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[3.4.3 Proceso 3: Gestión Administrativa y Procesos Internos	21](#3.4.3-proceso-3:-gestión-administrativa-y-procesos-internos)

[3.5 Modelos y Estándares Aplicables	23](#3.5-modelos-y-estándares-aplicables)

[3.6 Administración de la Calidad	24](#3.6-administración-de-la-calidad)

[3.6.1 Planificación de la Calidad	24](#3.6.1-planificación-de-la-calidad)

[3.6.2 Aseguramiento de la Calidad (SQA) y Auditoría de Software	24](#3.6.2-aseguramiento-de-la-calidad-\(sqa\)-y-auditoría-de-software)

[3.6.3 Control de Calidad (SQC) y Estrategia de Pruebas	26](#3.6.3-control-de-calidad-\(sqc\)-y-estrategia-de-pruebas)

[3.6.4 Gestión de Configuración y Control de Versiones	26](#3.6.4-gestión-de-configuración-y-control-de-versiones)

[3.6.5 Gestión y Ciclo de Vida de Defectos	26](#3.6.5-gestión-y-ciclo-de-vida-de-defectos)

[3.6.6 Mejora Continua y Automatización del Ciclo de Vida (CI/CD)	27](#3.6.6-mejora-continua-y-automatización-del-ciclo-de-vida-\(ci/cd\))

[3.7 Características de la Administración de la Calidad	27](#3.7-características-de-la-administración-de-la-calidad)

[3.8 Atributos de Calidad	28](#3.8-atributos-de-calidad)

[3.8.1 Adecuación Funcional	28](#3.8.1-adecuación-funcional)

[3.8.2 Eficiencia de Desempeño	28](#3.8.2-eficiencia-de-desempeño)

[3.8.3 Compatibilidad	29](#3.8.3-compatibilidad)

[3.8.4 Capacidad de Interacción (Usabilidad)	29](#3.8.4-capacidad-de-interacción-\(usabilidad\))

[3.8.5 Fiabilidad	29](#3.8.5-fiabilidad)

[3.8.6 Seguridad	30](#3.8.6-seguridad)

[3.9 Atributos Referenciados en la ISO/IEC 25010	30](#3.9-atributos-referenciados-en-la-iso/iec-25010)

[**4\. REQUISITOS NO FUNCIONALES DEL SISTEMA (RNF)	33**](#4.-requisitos-no-funcionales-del-sistema-\(rnf\))

[4.1 Característica de Calidad (ISO/IEC 25010): Eficiencia de desempeño	33](#4.1-característica-de-calidad-\(iso/iec-25010\):-eficiencia-de-desempeño)

[4.1.1 RNF: 01 \- Respuesta oportuna de la recomendación de tallas \- Proceso:Gestión del Perfil infantil y recomendación personalizada de tallas y prendas.	33](#4.1.1-rnf:-01---respuesta-oportuna-de-la-recomendación-de-tallas---proceso:gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas.)

[4.1.2 RNF: 02 \- Agilidad de los filtros del catálogo \- Proceso: Gestión del catálogo, compras, pedidos e inventario.	33](#4.1.2-rnf:-02---agilidad-de-los-filtros-del-catálogo---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.)

[4.1.3 RNF: 03 \- Sincronización del inventario tras el pago \- Proceso: Gestión del catálogo, compras, pedidos e inventario.	33](#4.1.3-rnf:-03---sincronización-del-inventario-tras-el-pago---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.)

[4.1.4 RNF: 04 \- Tiempo de carga  del panel administrativo \- Proceso: Gestión administrativa y procesos internos.	34](#4.1.4-rnf:-04---tiempo-de-carga-del-panel-administrativo---proceso:-gestión-administrativa-y-procesos-internos.)

[4.1.5 RNF: 05 \- Capacidad de respuesta ante concurrencia de clientes \- Proceso:Gestión del Catálogo, Compras, Pedidos e Inventario	34](#4.1.5-rnf:-05---capacidad-de-respuesta-ante-concurrencia-de-clientes---proceso:gestión-del-catálogo,-compras,-pedidos-e-inventario)

[4.2 Característica de Calidad (ISO/IEC 25010): Seguridad	35](#4.2-característica-de-calidad-\(iso/iec-25010\):-seguridad)

[4.2.1 RNF: 06 \- Confidencialidad de los datos del menor \- Proceso: Gestión del Perfil infantil y recomendación personalizada de tallas y prendas.	35](#4.2.1-rnf:-06---confidencialidad-de-los-datos-del-menor---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas.)

[4.2.2 RNF: 07 \- Autenticidad de notificaciones de pago(WebHooks) \- Proceso: Gestión del catálogo, compras, pedidos e inventario.	35](#4.2.2-rnf:-07---autenticidad-de-notificaciones-de-pago\(webhooks\)---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.)

[4.2.3 RNF: 08 \- Trazabilidad de acciones administrativas \- Proceso: Gestión administrativa y procesos internos.	35](#4.2.3-rnf:-08---trazabilidad-de-acciones-administrativas---proceso:-gestión-administrativa-y-procesos-internos.)

[4.2.4 RNF: 09 \- Resistencia ante vulnerabilidades en dependencias \- Proceso: Gestión del catálogo, compras, pedidos e inventario.	36](#4.2.4-rnf:-09---resistencia-ante-vulnerabilidades-en-dependencias---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.)

[4.2.5 RNF: 10 \- Control de accesos por roles  \- Proceso: Gestión Administrativa y Procesos Internos	36](#4.2.5-rnf:-10---control-de-accesos-por-roles---proceso:-gestión-administrativa-y-procesos-internos)

[4.3 Característica de Calidad (ISO/IEC 25010): Adecuación Funcional	37](#4.3-característica-de-calidad-\(iso/iec-25010\):-adecuación-funcional)

[4.3.1 RNF: 11 \- Precisión en la recomendación de tallas \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas.	37](#4.3.1-rnf:-11---precisión-en-la-recomendación-de-tallas---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas.)

[4.3.2 RNF: 12 \- Completitud del registro del perfil infantil \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas	37](#4.3.2-rnf:-12---completitud-del-registro-del-perfil-infantil---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[4.3.3 RNF: 13 \- Trazabilidad del ciclo de vida del pedido \- Proceso: Gestión del catálogo, compras, pedido e inventario	38](#4.3.3-rnf:-13---trazabilidad-del-ciclo-de-vida-del-pedido---proceso:-gestión-del-catálogo,-compras,-pedido-e-inventario)

[4.3.4 RNF: 14 \- Emisión oportuna de alertas de reabastecimiento \- Proceso: Gestión del catálogo, compras, pedidos e inventario	38](#4.3.4-rnf:-14---emisión-oportuna-de-alertas-de-reabastecimiento---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[4.3.5 RNF: 15 \- Consistencia de datos en reportes financieros y dashboards \- Proceso: Gestión administrativa y procesos internos	38](#4.3.5-rnf:-15---consistencia-de-datos-en-reportes-financieros-y-dashboards---proceso:-gestión-administrativa-y-procesos-internos)

[4.4 Característica de Calidad (ISO/IEC 25010): Capacidad de Interacción	39](#4.4-característica-de-calidad-\(iso/iec-25010\):-capacidad-de-interacción)

[4.4.1 RNF:  16 \- Prevención de errores en el registro de medidas \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas	39](#4.4.1-rnf:-16---prevención-de-errores-en-el-registro-de-medidas---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[4.4.2 RNF: 17 \- Asistencia al usuario mediante resolución de incidencias \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas	39](#4.4.2-rnf:-17---asistencia-al-usuario-mediante-resolución-de-incidencias---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[4.4.3 RNF: 18 \- Operabilidad del flujo de compra \- Proceso: Gestión del catálogo, compras, pedidos e inventario	40](#4.4.3-rnf:-18---operabilidad-del-flujo-de-compra---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[4.4.4 RNF: 19 \- Facilidad de actualización del perfil infantil \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas	40](#4.4.4-rnf:-19---facilidad-de-actualización-del-perfil-infantil---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[4.4.5 RNF: 20 \- Comprensión del panel administrativo \- Proceso: Gestión administrativa y procesos internos	41](#4.4.5-rnf:-20---comprensión-del-panel-administrativo---proceso:-gestión-administrativa-y-procesos-internos)

[4.5 Característica de Calidad (ISO/IEC 25010): Compatibilidad	41](#4.5-característica-de-calidad-\(iso/iec-25010\):-compatibilidad)

[4.5.1 RNF: 21 \- Interoperabilidad con la pasarela de pagos externa \- Proceso: Gestión del catálogo, compras, pedidos e inventario.	41](#4.5.1-rnf:-21---interoperabilidad-con-la-pasarela-de-pagos-externa---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.)

[4.5.2 RNF: 22 \- Coexistencia y compatibilidad entre plataformas y navegadores web \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas	42](#4.5.2-rnf:-22---coexistencia-y-compatibilidad-entre-plataformas-y-navegadores-web---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[4.5.3 RNF: 23 \- Interoperabilidad para el envío de notificaciones por correo electrónico SMTP \- Proceso: Gestión del catálogo, compras, pedidos e inventario	42](#4.5.3-rnf:-23---interoperabilidad-para-el-envío-de-notificaciones-por-correo-electrónico-smtp---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[4.5.4 RNF: 24 \- Interoperabilidad en la exportación de reportes analíticos \- Proceso: Gestión administrativa y procesos internos	42](#4.5.4-rnf:-24---interoperabilidad-en-la-exportación-de-reportes-analíticos---proceso:-gestión-administrativa-y-procesos-internos)

[4.5.5 RNF: 25 Interoperabilidad con el servicio de almacenamiento multimedia en la nube \- Proceso: Gestión del catálogo, compras, pedidos e inventario	43](#4.5.5-rnf:-25-interoperabilidad-con-el-servicio-de-almacenamiento-multimedia-en-la-nube---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[4.6 Característica de Calidad (ISO/IEC 25010): Fiabilidad	43](#4.6-característica-de-calidad-\(iso/iec-25010\):-fiabilidad)

[4.6.1 RNF: 26 \- Disponibilidad operativa de la plataforma \- Proceso: Gestión del catálogo, compras, pedidos e inventario	43](#4.6.1-rnf:-26---disponibilidad-operativa-de-la-plataforma---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[4.6.2 RNF: 27 \- Disponibilidad del panel administrativo \- Proceso: Gestión administrativa y procesos internos	44](#4.6.2-rnf:-27---disponibilidad-del-panel-administrativo---proceso:-gestión-administrativa-y-procesos-internos)

[4.6.3 RNF: 28 \- Tolerancia a fallos de la pasarela de pagos \- Proceso: Gestión del catálogo, compras, pedidos e inventario	44](#4.6.3-rnf:-28---tolerancia-a-fallos-de-la-pasarela-de-pagos---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario)

[4.6.4 RNF: 29 \- Ausencia de fallos en el motor de recomendación \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas	45](#4.6.4-rnf:-29---ausencia-de-fallos-en-el-motor-de-recomendación---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas)

[4.6.5 RNF: 30 \- Recuperabilidad ante caídas del servicio comercial \- Proceso: Gestión administrativa y procesos internos	45](#4.6.5-rnf:-30---recuperabilidad-ante-caídas-del-servicio-comercial---proceso:-gestión-administrativa-y-procesos-internos)

[**5\. EVIDENCIAS MÉTRICAS	47**](#5.-evidencias-métricas)

# **1\. INTRODUCCIÓN** {#1.-introducción}

Obtener ropa infantil requiere atender diferentes necesidades tanto para los padres como para las empresas que se dedican a su comercialización. Seleccionar la ropa correcta requiere aspectos como la variabilidad en el crecimiento infantil, la precisión en las tallas y condiciones especiales de sensibilidad o alergias textiles. A su vez, los vendedores requieren mecanismos que les permitan administrar sus productos, controlar inventarios de prendas y procesar sus ventas.

Teniendo en cuenta lo anterior, este proyecto se encarga de atender estas necesidades, siendo una plataforma digital de gestión y comercialización de ropa infantil (e-commerce). La plataforma permite administrar perfiles infantiles con información de medidas y alergias, facilitar la selección de ropa mediante un motor de recomendaciones personalizadas de tallas, consultar un catálogo de productos, gestionar carrito de compras y pedidos. Además de permitir a los vendedores la capacidad de administrar sus productos e inventarios.

Adicionalmente, la plataforma busca proporcionar herramientas de generación de información, para facilitar el análisis de demanda y rendimiento de los productos, por medio de métricas y reportes.

## **1.1 Alcance** {#1.1-alcance}

El proyecto comprende el análisis, diseño y desarrollo de una plataforma e-commerce web  para la gestión y comercialización de ropa infantil.

Entrando más en detalle, la plataforma está dirigida a tres roles de usuarios y un servicio externo: Padre, madre o acudiente (Cliente), Vendedor, Administrador y Pasarela de Pago Externa.

Para los clientes, se les permitirá gestionar sus cuentas y perfiles de sus hijos, todo lo relacionado con sus datos necesarios para la recomendación de tallas y ropa. Además tendrán acceso al catálogo de ropa infantil con filtros avanzados y visualizar el detalle de los productos, administrar un carrito de compras, realizar pedidos y monitorear su estado.

Para los vendedores, podrán gestionar sus productos, controlar existencias de stock, recibir alertas de inventario bajo y podrán acceder a un panel de métricas, donde podrán generar reportes de venta específicos y generales de la plataforma, para saber cuales son las palabras clave más usadas y cuáles generan más ventas.

Para los administradores, se les permitirá gestionar a los usuarios (clientes y vendedores), además de proporcionar un panel de información y reportes relacionados con la demanda de productos.

Para los vendedores y administradores, se incluye la visualización de dashboards y la generación y descarga/exportación de reportes operacionales en formato PDF para su análisis fuera de línea.

En el caso de la pasarela de pago externa, se incluye dentro del alcance la integración técnica permitiendo procesar transacciones, validar el resultado de pagos y comunicar a la plataforma si una transacción fue aprobada, rechazada o pendiente. 

Por último, la plataforma asegura las funcionalidades para soportar los procesos anteriores, no contempla dentro del alcance inicial, la gestión interna de procesos de manufactura o confección textil, ni la operación logística/transporte directo de paquetería externa. El sistema logístico dentro del sistema se limitará a la actualización de estados del pedido y el registro del número de guía correspondiente.

## **1.2 Objetivos** {#1.2-objetivos}

### **1.2.1	Objetivo general** {#1.2.1-objetivo-general}

Desarrollar una plataforma digital e-commerce para la gestión integral y comercialización de ropa infantil que permita administrar perfiles infantiles, facilitar la selección de tallas y prendas, apoyar los procesos de inventario de los vendedores, gestionar carritos y pedidos con integración a pasarela de pagos, proporcionando además herramientas de administración y generación de información para el seguimiento de la operación.

### **1.2.2	Objetivos específicos** {#1.2.2-objetivos-específicos}

1. Diseñar e implementar un módulo para la gestión de usuarios y perfiles infantiles que permita registrar y administrar la información necesaria para apoyar la selección de ropa y tallas.  
2. Desarrollar un mecanismo de recomendación de tallas y ropa basado en la información registrada en los perfiles infantiles y en las características de los productos disponibles.  
3. Implementar un catálogo digital que permita consultar, filtrar y visualizar información detallada de las prendas infantiles disponibles.  
4. Desarrollar el módulo de comercio electrónico que abarque la gestión del carrito de compras, creación de pedidos y seguimiento de sus estados.  
5. Integrar una pasarela de pagos que permita procesar y validar transacciones asociadas a los pedidos.  
6. Desarrollar funcionalidades para que los vendedores puedan administrar sus productos y la información asociada a estos.  
7. Implementar mecanismos para la gestión y control del inventario y la generación de alertas ante niveles bajos de stock.  
8. Implementar un módulo administrativo para gestionar usuarios registrados en la plataforma.  
9. Desarrollar un dashboard y reportes que permitan visualizar información relevante sobre la operación y la demanda de productos.  
10. Integrar las funcionalidades desarrolladas en una plataforma que proporcione una experiencia coherente para los diferentes actores involucrados en el proceso de gestión y comercialización de ropa infantil.

## **1.3 Glosario de términos** {#1.3-glosario-de-términos}

| Término | Definición |
| :---: | ----- |
| **Administrador** | Usuario encargado de gestionar y supervisar la información general de la plataforma, gestionar las cuentas de usuarios y analizar la métrica global. |
| **Carrito de compra** | Funcionalidad que permite al padre seleccionar y almacenar temporalmente productos antes de generar un pedido. |
| **Catálogo** | Conjunto organizado de prendas infantiles disponibles para consulta y adquisición dentro de la plataforma. |
| **Inventario / Stock** | Registro de las existencias disponibles de prendas terminadas dentro de la plataforma. |
| **Padre / Madre / Acudiente** | Usuario de la plataforma encargado de gestionar su información, registrar perfiles infantiles, consultar productos y realizar pedidos de ropa infantil. |
| **Vendedor** | Worker de negocio encargado de administrar el catálogo de productos, actualizar el stock y consultar reportes de ventas. |
| **Pasarela de pago externa** | Actor de negocio externo que procesa y valida transacciones electrónicas de los pedidos en entorno de pruebas. |
| **Perfil infantil** | Registro de información asociada a un niño que permite almacenar los datos necesarios para apoyar la recomendación de tallas y prendas. |
| **Pedido** | Solicitud de adquisición de uno o varios productos realizada por un padre a través de la plataforma. |
| **Prenda / Producto** | Producto de ropa infantil ofrecido mediante la plataforma y asociado a características como talla, tipo, descripción y disponibilidad. |
| **Recomendación de prendas** | Funcionalidad mediante la cual la plataforma propone productos que pueden ser adecuados para un perfil infantil. |
| **Recomendación de talla** | Funcionalidad que permite determinar o sugerir una talla de prenda a partir de la información registrada sobre el niño y las características de la prenda. |
| **Talla** | Clasificación utilizada para representar las dimensiones o tamaño de una prenda infantil y facilitar su selección de acuerdo con las características del niño. |
| **Usuario** | Persona que interactúa con la plataforma mediante alguno de los roles definidos para el sistema. |
| **KPIs** | *Key Performance Indicators* o Indicadores Clave de Desempeño: Son métricas que permiten medir qué tan bien está funcionando la plataforma y cómo se comportan los usuarios dentro de ella. |
| **API** | *Application Programming Interface* o Interfaz de Programación de Aplicaciones es un mecanismo que permite que dos sistemas diferentes se comuniquen e intercambien información de forma estructurada. En este caso la conexión de la pasarela de pagos con la plataforma para el procesamiento de transacciones. |
| **Webhooks** | Método que permite a una aplicación web enviar datos de forma automática a otra aplicación en tiempo real cuando ocurre un evento específico. |
| **Habeas Data** | Derecho que tienen las personas a conocer, actualizar, rectificar y, en los casos establecidos por la ley, solicitar la eliminación de la información personal que se encuentre registrada en bases de datos o archivos.  |
| **Ley 1581 de 2012** | Ley colombiana que establece disposiciones generales para la protección de datos personales y regula su recolección, almacenamiento, uso y tratamiento.  |
| **Decreto 1074 de 2015**  | Decreto Único Reglamentario del Sector Comercio, Industria y Turismo que incluye disposiciones relacionadas con la reglamentación del tratamiento de datos personales en Colombia.  |
| **Ley 1480 de 2011- Estatuto del consumidor**  | Norma colombiana que establece medidas para la protección de los derechos de los consumidores y regula aspectos relacionados con la información de productos, comercio electrónico, garantías y relaciones de consumo.  |
| **Resolución Andina 2109 de 2019** | Reglamento Técnico Andino relacionado con el etiquetado de confecciones, que establece requisitos sobre la información que debe suministrarse al consumidor respecto a los productos textiles comercializados.  |
| **Tratamiento de datos personales**  | Operación o conjunto de operaciones realizadas sobre datos personales, como la recolección, almacenamiento, uso, actualización o eliminación de información.  |
| **Stock minimo** | Cantidad mínima de existencias establecida para un producto, a partir de la cual el sistema puede generar una alerta de reabastecimiento.  |
| **Dashboard** | Panel visual que presenta indicadores y métricas relevantes mediante gráficos, tablas u otros elementos para facilitar el seguimiento y la toma de decisiones.  |

# **2\. INFORMACIÓN DEL CASO DE ESTUDIO** {#2.-información-del-caso-de-estudio}

El caso de estudio se desarrolla en el contexto de la gestión y comercialización de ropa infantil, donde intervienen diferentes padres, madres o acudientes (clientes), vendedores de ropa y administradores del servicio.

En este contexto, los clientes requieren consultar diferentes opciones de ropa y seleccionar prendas adecuadas para las características y necesidades de sus hijos. La elección de una talla puede representar una dificultad debido a las diferencias en las características físicas de cada niño y a la variedad de tallas utilizadas en las prendas infantiles.

Por otra parte, los vendedores requieren canales digitales centralizados en la comercialización de sus productos, el control de inventario y la recopilación de datos sobre tendencias de compras.

Asimismo se requiere integrar mecanismos para el procesamiento seguro de transacciones en línea y contar con un panel centralizado que facilite la administración de la información generada por los diferentes participantes y contar con mecanismos que permitan realizar seguimiento a la operación y obtener información para su análisis.

A partir de este contexto se plantea el caso de estudio para el desarrollo de una solución informática que permita abordar las necesidades identificadas en este dominio.

# **3\. PLAN DE GESTIÓN DE LA CALIDAD** {#3.-plan-de-gestión-de-la-calidad}

A continuación, diligencie cada sección de este plan teniendo en cuenta el caso de estudio y los tres procesos de negocio definidos.

## **3.1 Calidad del Software**  {#3.1-calidad-del-software}

En el contexto de LittleStyle, la calidad del software se refiere a la capacidad de la plataforma para responder de forma confiable, segura y eficiente a las necesidades de sus tres tipos de usuarios: Cliente, Vendedor y Administrador, teniendo en cuenta los tres procesos de negocio definidos.

Para el cliente, la calidad se relaciona principalmente con que la plataforma sea fácil de utilizar al momento de registrar la información de los perfiles infantiles y que las recomendaciones de tallas sean correctas, ya que esta es una de las principales características que diferencia a LittleStyle. También es importante que el proceso de compra y pago sea sencillo y seguro.

Para el vendedor, la calidad implica que pueda administrar el catálogo y el inventario de manera rápida y sin errores. Además, debe recibir oportunamente las alertas relacionadas con el bajo stock y contar con reportes de ventas que muestren información correcta y actualizada para facilitar la toma de decisiones.

En el caso del administrador, la calidad consiste en contar con herramientas que le permitan supervisar y gestionar las cuentas, publicaciones y diferentes operaciones de la plataforma desde un mismo lugar. También debe disponer de información organizada y confiable mediante los dashboards y reportes.Asimismo, las acciones críticas que ejecuta sobre la plataforma deben quedar registradas para garantizar su trazabilidad. 

De manera general, debido a que LittleStyle maneja información relacionada con menores de edad y procesos de pago mediante una pasarela externa, la plataforma debe contar con buenas medidas de seguridad, protección de datos y mecanismos de autenticación. También es importante garantizar una buena disponibilidad, ya que las interrupciones del servicio pueden afectar tanto a los clientes como a los vendedores.

En conclusión, la calidad de LittleStyle se basa principalmente en que el sistema sea usable, preciso, seguro, disponible, permitiendo que los tres procesos de negocio se realicen correctamente y que cada usuario pueda cumplir sus funciones de manera confiable.

## **3.2 Factores de Calidad del Software**  {#3.2-factores-de-calidad-del-software}

Para la plataforma Little Style, los factores de calidad del software establecen los atributos esenciales que garantizan que el sistema cumpla con las necesidades operativas de los clientes (padres, madres o acudientes), vendedores y administradores, asegurando un funcionamiento continuo, confiable y ajustado al marco legal. A continuación, se definen los factores clave de calidad y su relación con los procesos principales del proyecto: 

1. ####  **Confiabilidad**  {#confiabilidad}

Capacidad del software para mantener su nivel de prestación bajo condiciones normales y de carga.

**Relación con los Procesos Clave:**

- Recomendación de Tallas y Prendas: El algoritmo debe proporcionar recomendaciones precisas e invariables según los datos de medidas y sensibilidad textil del perfil infantil.  
- Gestión de Compras y Pedidos: Se debe garantizar la alta disponibilidad de la plataforma para procesar compras y validar pagos con la pasarela externa sin interrupciones.  
- Control de Inventario: El inventario de productos debe actualizarse de forma precisa e inmediata tras cada transacción para evitar inconsistencias en el stock disponible.  
    
2. **Seguridad y Protección de Datos** 

Protección de la información contra accesos no autorizados, garantizando confidencialidad, integridad y autenticidad. 

**Relación con los Procesos Clave:** 

- Gestión de Perfiles Infantiles: Al gestionar datos personales de menores de edad, el sistema debe cumplir estrictamente con la normativa colombiana de Habeas Data (Ley 1581 de 2012 y Decreto 1074 de 2015), aplicando técnicas de cifrado en almacenamiento y transmisión.   
- Integración de Pagos: Garantizar conexiones cifradas y autenticación mediante tokens o Webhooks con la pasarela de pago externa para proteger las transacciones financieras.   
- Control de Acceso: Control de acceso basado en roles (Cliente, Vendedor, Administrador) para proteger las funcionalidades y datos administrativos.   
    
3. **Usabilidad y Capacidad de Interacción**

Grado de facilidad con la que los usuarios interactúan con la interfaz para lograr sus objetivos de forma eficiente e intuitiva.

**Relación con los Procesos Clave:**

- Experiencia del Cliente: Diseño limpio, adaptable (responsive) y fácil de navegar que permita a los padres registrar medidas, filtrar el catálogo y realizar pedidos sin fricciones.  
- Panel de Vendedores y Administradores: Dashboards con visualizaciones de métricas y reportes estructurados de forma clara para agilizar la gestión de stock y la toma de decisiones.

 

4. **Eficiencia de Desempeño**

Desempeño del sistema en función del tiempo de respuesta y la gestión adecuada de recursos.

**Relación con los Procesos Clave:**

- Navegación en el Catálogo: Tiempos de respuesta rápida (menores a 2 segundos) al consultar productos, aplicar filtros avanzados y cargar el detalle de las prendas.  
- Generación de Reportes: Respuesta eficiente al procesar volúmenes de datos de ventas, palabras clave más buscadas e indicadores del dashboard.

 

5. **Adecuación y Correctitud Funcional** 

Capacidad del software para proveer un conjunto de funciones que satisfagan las necesidades explícitas e implícitas de los usuarios.

**Relación con los Procesos Clave:**

- Gestión de Inventario: Emisión oportuna de alertas automáticas cuando un producto alcance el *stock mínimo*.  
- Seguimiento de Pedidos: Actualización correcta de los estados del pedido (creación, pago, despacho, entrega) y registro del número de guía logístico.

## **3.3 Métricas para la Calidad de un Proceso** {#3.3-métricas-para-la-calidad-de-un-proceso}

Las métricas de calidad de un proceso evalúan estratégicamente la salud técnica, operativa y de negocio de la plataforma. estás métricas establecen indicadores clave de rendimiento transversales al sistema informático.

A continuación, se formalizan las métricas globales clasificadas de acuerdo a los atributos estandarizados de la calidad de software

### **3.3.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de Tallas y Prendas** {#3.3.1-proceso-1:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

Este proceso comprende el registro de información biométrica de menores, sensibilidad textil y la ejecución del motor de reglas para sugerencia de prendas. Las métricas de proceso evalúan el desempeño computacional de las consultas, la integridad de los datos y la tolerancia a errores de captura.

| Nombre de la Métrica | Descripción | Fórmula / Unidad | Fuente de Datos | Frecuencia de Medición |
| :---: | ----- | :---: | :---: | :---: |
| Tiempo Medio de Respuesta del Motor (API Latency) | Mide el tiempo promedio de respuesta del servicio backend al cruzar medidas del menor con la tabla de tallas | $\Sigma$ Tiempo de Respuesta  / Número de Consultas  | Logs del servidor / Herramienta de monitoreo APM | Por sprint y Medición Continua |
| Densidad de Defectos del Módulo de Perfiles  | Mide la cantidad de errores o defectos detectados en el módulo por miles de líneas de código | Total Defectos Detectados / Miles de Líneas de Código  | Análisis Estático de Código  | Al finalizar cada Sprint |
| Cumplimiento de Cifrado en Datos Sensibles (Habeas Data) | Evalúa el porcentaje de campos con información personal de menores almacenados con cifrado en la base de datos | (Campos Sensibles Cifrados / Total Campos Sensibles Registrados) x 100 | Auditoría de esquema de base de datos / SAST | Antes del Despliegue / Quincenal |
| Tiempo medio entre fallos del motor de reglas | Mide el tiempo promedio continuo que el algoritmo de recomendación opera sin lanzar excepciones no controladas | Tiempo Total Operativo / Número Fallos Presentados | Logs de Excepciones del backend | Mensual |
| Soporte de Plataformas Compatiblles  | Mide la proporción de entornos en los que la interfaz del perfil se renderiza sin errores visuales o funcionales. | (Plataformas con prueba exitosa / Total plataformas objetivo) x 100 | Pruebas de Rendering Automatizadas | Por Despliegue |

### **3.3.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario** {#3.3.2-proceso-2:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

Proceso transaccional y operativo más grande de la plataforma que involucra la consulta masiva de catálogo, actualización concurrente de inventario e integración con la Pasarela de Pagos Externa (Wompi) y correo electrónico. Las métricas evalúan la disponibilidad operativa, el consumo de infraestructura y la robustez transaccional.

| Nombre de la Métrica | Descripción | Fórmula / Unidad | Fuente de Datos | Frecuencia de Medición |
| :---: | ----- | :---: | :---: | :---: |
| Disponibilidad Operativa del Servicio | Mide el porcentaje de tiempo que el flujo transaccional de compras y catálogo se mantiene en línea y receptivo. | ((Tiempo Operativo Total \- Tiempo Inactivo) / Tiempo Operativo Total) x 100 | AWS CloudWatch / Health Checks | Diario |
| Consumo promedio de CPU y memoria RAM | Mide el uso de hardware del servidor backend durante picos de consulta de catálogo y ventas. | Promedio de carga de CPU y RAM utilizaste en EC2 / Contenedor | Monitoreo de infraestructura AWS | Continuo / Pruebas de Carga |
| Cobertura de Pruebas Automatizadas  | Mide la proporción del código fuente del módulo ejecutado durante las pruebas unitarias e integración. | (Líneas de Código Evaluadas / Líneas de Código Totales) x 100 | Jenkins / Github Actions | Por cada Commit / Build |
| Tasa de Excepciones en Integración con Pasarela (Webhooks) | Mide la proporción de fallos de comunicación o errores presentados al recibir eventos de confirmación de pago de la pasarela. | (Peticiones Fallidas de Pasarela / Total Transacciones Notificadas) x 100 | Logs de Webhooks / Auditoría Wompi | Diaria  |
| Tiempo Medio de Reparación Ante Caídas  | Mide el tiempo promedio requerido para solucionar y restablecer la plataforma tras una interrupción en el módulo comercial. | Tiempo Total de Inactividad / Número de Incidentes de Caída | Reportes de Gestión de Incidentes | Por Evento / Mensual |
| Cobertura de Pruebas de Integración y E2E (End-to-End) | Mide la proporción de flujos completos de compra cubiertos por pruebas automatizadas. | (Flujos E2E Automatizados Exitosos / Total de Flujos Críticos de Compra) x 100 | Postman / Playwright / Selenium | Antes de cada Release |

### **3.3.3 Proceso 3: Gestión Administrativa y Procesos Internos** {#3.3.3-proceso-3:-gestión-administrativa-y-procesos-internos}

Este proceso abarca la administración de usuarios, moderación de contenidos y la generación de reportes gerenciales y dashboards de rendimiento. Sus métricas miden el desempeño en la generación de analítica masiva, mantenibilidad del código y la seguridad en el control de acceso de los administradores.

| Nombre de la Métrica | Descripción | Fórmula / Unidad | Fuente de Datos | Frecuencia de Medición |
| :---: | ----- | :---: | :---: | :---: |
| Tiempo de Carga de Consultas del Dashboard  | Mide el tiempo requerido por el backend para consolidar ventas, búsquedas e inventario y retornarlo al frontend | Milisegundos desde la petición HTTP hasta la respuesta JSON | Profiling SQL / Network Tab Browser  | Semanal |
| Tasa de Rechazo de Accesos No Autorizados | Mide el porcentaje de peticiones a endpoints administrativos vulnerables o sin rol que son bloqueados exitosamente por Spring Security. | (Peticiones Denegadas / Total Intentos Sin Token Válido) x 100 | Logs de Seguridad (Spring Security) | Semanal / Auditoría |
| Índice de Mantenibilidad del Código | Mide qué tan fácil es mantener, modificar y entender el código del módulo administrativo. Asigna un valor de 0 a 100 basado en líneas de código, complejidad ciclomática y volumen Halstead. | Formula combinada Halstead ´Cyclomatic \+ LOC | SonarQube / Visual Studio Metrics | Por Sprint |
| Tiempo de Reparación de Dependencias Vulnerables  | Mide el tiempo en días que le toma al equipo actualizar una librería/dependencia desactualizada con fallo de seguridad | Días desde la alerta del scanner hasta el merge del PR | Snyk / Github Dependabot | Semanal |

## **3.4 Creación de Métricas para la Calidad** {#3.4-creación-de-métricas-para-la-calidad}

### **3.4.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de Tallas y Prendas**  {#3.4.1-proceso-1:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

Este conjunto de métricas evalúa la precisión, usabilidad y eficiencia del módulo de gestión de perfiles infantiles y del motor de recomendación de tallas. Su objetivo principal es asegurar que la recolección de datos antropométricos y preferencias textiles sea fluida, minimizando los errores de entrada y garantizando que las sugerencias de prendas generen alta confianza y aceptación en los acudientes. 

| Nombre de la Métrica | Proceso de Negocio Asociado | Descripción del Indicador | Fórmula / Criterio de Medición | Interpretación Esperada |
| ----- | ----- | :---- | :---- | :---- |
| Tasa de adopción de recomendación de tallas | Recomendación Personalizada | Mide el porcentaje de compras en las que el cliente confía y selecciona la talla sugerida por el motor. | (Compras con talla recomendada / Total de compras con recomendación) x 100 | \> 80%. Indica que el algoritmo es preciso y genera confianza en el cliente. |
| Completitud del perfil infantil | Gestión del Perfil Infantil | Evalúa cuántos perfiles creados tienen todos los campos opcionales y obligatorios (medidas, alergias) diligenciados. | (Perfiles completos / Total de perfiles registrados) x 100 | \> 90%. Demuestra que la interfaz es intuitiva y el usuario ve valor en dar los datos. |
| Tiempo de carga de recomendaciones | Recomendación Personalizada | Mide el tiempo que tarda el sistema en procesar los datos del perfil y mostrar las prendas sugeridas. | Sumatoria del tiempo de respuesta / Número total de peticiones al motor | \< 2 segundos. Garantiza una eficiencia de desempeño fluida (ISO/IEC 25010). |
| Tasa de error en captura de medidas biométricas | Gestión del Perfil Infantil | Porcentaje de intentos fallidos o datos inconsistentes rechazados al guardar un perfil. | (Errores de validación / Total de intentos de guardado) x 100 | \< 5%. Indica una buena usabilidad y protección contra errores de usuario. |
| Frecuencia de actualización de perfiles | Gestión del Perfil Infantil | Mide la proporción de clientes que actualizan las medidas de los niños al menos cada 6 meses (por crecimiento). | (Perfiles actualizados en \< 6 meses / Total de perfiles) x 100 | \> 60%. Refleja un alto nivel de engagement y mantenimiento de datos precisos. |
| Efectividad en la protección de datos sensibles | Gestión del Perfil Infantil  | Mide la proporción de campos con información confidencial de menores almacenados con cifrado en reposo y protegidos contra accesos no autorizados.  | (Campos sensibles cifrados / Total campos sensibles) x  100  | \> 96% de campos cifrados.  |
| Tasa de consultas de recomendación completadas sin fallos  | Recomendación Personalizada  | Mide el porcentaje de consultas realizadas al motor de recomendación que finalizan correctamente sin generar excepciones no controladas que interrumpan la obtención de tallas o prendas recomendadas.  | (Consultas completadas sin fallos / Total de consultas procesadas por el motor) × 100  | ≥ 99 %. Indica que el motor de recomendación mantiene un comportamiento estable y consistente durante su operación.  |

### **3.4.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario** {#3.4.2-proceso-2:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

Este bloque de métricas se enfoca en medir la eficiencia operativa, fiabilidad transaccional e integridad del ciclo de comercio electrónico. Evalúa la velocidad de navegación en el catálogo con filtros avanzados, la efectividad del checkout, la disponibilidad de la integración técnica con la pasarela de pagos externa y la sincronización en tiempo real del stock de los vendedores.  

| Nombre de la Métrica | Proceso de Negocio Asociado | Descripción del Indicador | Fórmula / Criterio de Medición | Interpretación Esperada |
| ----- | ----- | ----- | ----- | ----- |
| Tasa de abandono del carrito | Compras y Pedidos | Porcentaje de carritos de compras creados que no finalizan en una transacción pagada. | (Carritos abandonados / Total de carritos creados) x 100 | \< 30%. Un porcentaje bajo indica que el flujo de pago es eficiente y sin fricciones. |
| Tiempo de sincronización de stock | Gestión de Inventario | Mide los milisegundos que transcurren desde la aprobación del pago hasta el descuento del inventario en base de datos. | Tiempo medio (en ms) de la transacción de actualización de stock | \< 500 ms. Garantiza la concurrencia y evita sobreventas de prendas sin stock. |
| Tasa de trazabilidad y actualización de estados  | Compras y Pedidos  | Mide el porcentaje de pedidos que registran correctamente la transición de sus estados (creación, pago, despacho, entrega) e incluyen el número de guía logística sin inconsistencias.  | (Pedidos con historial de estados y guía registrados correctamente / Total de pedidos pagados) x 100  | \> 98%. Garantiza el correcto funcionamiento de las reglas de negocio del módulo de pedidos y el seguimiento logístico  |
| Efectividad de filtros en catálogo | Gestión del Catálogo | Mide qué tan rápido se aplican los filtros avanzados (talla, edad, tipo de prenda) para mostrar resultados. | Tiempo total de consultas con filtro / Número de consultas | \< 1.5 segundos. Un desempeño óptimo mantiene la atención del comprador. |
| Precisión de alertas de reabastecimiento | Gestión de Inventario | Porcentaje de alertas de stock mínimo que se disparan correctamente antes de que el inventario llegue a cero. | (Alertas generadas oportunamente / Total de productos agotados) x 100 | \> 99%. Es vital para que el vendedor mantenga la continuidad de su negocio. |
| Tasa de entrega exitosa de notificaciones transaccionales | Gestión del catálogo, compras, pedidos e inventario | Porcentaje de notificaciones transaccionales generados por la plataforma que son entregadas correctamente. | (Número de notificaciones entregadas correctamente / Número total de notificaciones enviadas) x 100 | \> 98\. Permite entregar correctamente las notificaciones asociadas al proceso. |
| Tasa de Disponibilidad de recursos multimedia  | Gestión del catálogo, compras, pedidos e inventario | Porcentaje de recursos multimedia asociados a los productos del catálogo que pueden ser consultados y visualizados correctamente mediante el servicio externo de almacenamiento multimedia. | (Número de recursos multimedia accesibles correctamente / Número total de recursos multimedia solicitados) x 100 | \> 99 %. Recursos disponibles que pueden ser utilizados correctamente por la plataforma mediante las solicitudes realizadas. |
| Tasa de procesamiento de carga simultanea | Gestión del Catálogo, Compras, Pedidos e Inventario  | Mide el porcentaje de peticiones HTTP en el catálogo que se completan sin errores cuando múltiples usuarios interactúan en simultáneo.  | (Peticiones exitosas sin error/Total de  peticiones bajo carga) x 100 | ≥99% de éxito con picos de hasta 100 usuarios en simultáneo. |
| Tasa de autenticación de notificaciones de pago  | Compras y Pedidos  | Evalúa la efectividad del sistema para verificar la firma HMAC  de los Webhooks de la pasarela.  | (Notificaciones con firma válida / Total notificaciones) x 100  | ≥ 99.8% de firmas válidas. |
| Efectividad en el mantenimiento seguro de dependencias | Gestión del Catálogo, Compras, Pedidos e Inventario | Mide el porcentaje de dependencias y librerías del producto libres de vulnerabilidades conocidas.  | (Librerías sin vulnerabilidades / Total dependencias) x  100  | ≥ 98% libres de vulnerabilidades.  |
| Tasa de preservación transaccional ante fallos externos  | Gestión del Catálogo, Compras, Pedidos e Inventario  | Mide el porcentaje de operaciones afectadas por fallos temporales en la comunicación con servicios externos, especialmente la pasarela de pagos, en las que el sistema conserva correctamente la información del pedido y mantiene la transacción en un estado válido, sin pérdida ni corrupción de datos.  | (Operaciones preservadas correctamente / Total de operaciones afectadas por fallos externos) × 100  | **≥ 99 %**. Indica que el sistema mantiene la integridad y continuidad de las transaccion |

### **3.4.3 Proceso 3: Gestión Administrativa y Procesos Internos** {#3.4.3-proceso-3:-gestión-administrativa-y-procesos-internos}

###  

Estas métricas evalúan la estabilidad, rendimiento y precisión de los módulos internos y herramientas administrativas. Garantizan que los administradores y vendedores dispongan de un dashboard continuo (alta disponibilidad), reportes financieros e indicadores de demanda consistentes y libres de errores, así como tiempos de respuesta idóneos para resolver incidencias operativas y solicitudes de soporte. 

| Nombre de la Métrica | Proceso de Negocio Asociado | Descripción del Indicador | Fórmula / Criterio de Medición | Interpretación Esperada |
| ----- | ----- | ----- | ----- | ----- |
| Disponibilidad del panel administrativo (Uptime) | Gestión Administrativa | Porcentaje de tiempo que el dashboard y los reportes están accesibles sin caídas de servicio. | (Tiempo de servicio activo / Tiempo total planificado) x 100 | \> 99.9%. Asegura que los administradores tengan control continuo de la plataforma. |
| Trazabilidad de acciones administrativas (Auditoría)  | Gestión Administrativa  | Mide la capacidad del sistema para registrar de forma automática e inalterable todas las operaciones críticas realizadas por los administradores (modificación de roles, bloqueos de usuario o eliminación de contenidos).  | (Eventos administrativos registrados en el log de auditoría / Total de operaciones administrativas realizadas) x 100  | \> 99.5%. Garantiza la responsabilidad, autenticidad y no repudio (ISO/IEC 25010\) sobre las acciones de gestión en la plataforma.  |
| Tasa de resolución de incidencias de usuarios | Gestión Administrativa | Porcentaje de bloqueos, moderación o dudas de vendedores y clientes que el administrador resuelve a tiempo. | (Incidencias resueltas en \< 24h / Total de incidencias reportadas) x 100 | \> 90%. Garantiza un buen soporte y mantenimiento operativo de las cuentas. |
| Consistencia de datos en reportes financieros | Procesos Internos | Nivel de coincidencia exacta entre las ventas reales registradas y las ventas mostradas en el dashboard. | (Transacciones coincidentes entre BD y Reporte / Total de transacciones en BD) x 100  | \> 99.9%. Coincidencia entre transacciones de la BD y métricas del dashboard. |
| Tasa de éxito en la exportación de reportes  | Procesos Internos  | Mide la confiabilidad del sistema cuando un administrador o vendedor solicita exportar o descargar un reporte de métricas, evaluando cuántos se generan sin errores (como un "timeout" o "error de servidor").  | (Reportes descargados exitosamente / Total de descargas solicitadas) x 100  | \> 98%. Demuestra una alta fiabilidad (ISO 25010\) en los procesos internos y que la plataforma soporta el procesamiento de documentos correctamente.  |
| Tiempo de respuesta del dashboard administrativo | Gestión administrativa | Mide los segundos requeridos para la renderización visual completa de las métricas clave e indicadores del dashboard.  | Tiempo transcurrido (en segundos) desde la petición de entrada hasta la visualización total de los gráficos.  | \<3 segundos en al menos el 90% de los accesos. |
| Tasa de restricción de accesos no autorizados  | Gestión administrativa | Evalúa la capacidad del sistema para denegar peticiones a endpoints cuando la solicitud carece de token JWT o rol válido.  | (Solicitudes bloqueadas / Total intentos sin rol) x  100  | ≥ 98.5% de accesos no autorizados bloqueados.  |
| Tasa de ejecución correcta de tareas administrativas  | Gestión Administrativa  | Mide el porcentaje de tareas principales del panel administrativo que vendedores y administradores logran completar correctamente y sin errores de navegación durante las pruebas de usabilidad.  | (Tareas completadas correctamente / Total de tareas evaluadas) × 100  | ≥ 90 %. Indica que la interfaz del panel es comprensible y facilita el aprendizaje y la ejecución de las operaciones administrativas principales.  |

## **3.5 Modelos y Estándares Aplicables**  {#3.5-modelos-y-estándares-aplicables}

Para el aseguramiento de la calidad del proyecto LittleStyle, se seguirán los siguientes modelos y estándares:

- **ISO/IEC 25010 (System and Software Quality Models):** se utiliza para definir y clasificar los atributos de calidad del producto software (características y subcaracterísticas), sirviendo como base para estructurar los Requisitos No Funcionales (RNF) del sistema. Su aporte principal es garantizar que la calidad del producto se evalúe de forma estandarizada y no de manera subjetiva.  
- **Scrum (marco ágil):** aunque no es una norma de calidad de producto, se aplica como modelo de aseguramiento de calidad del *proceso*, mediante revisiones incrementales al final de cada sprint, retrospectivas y criterios de aceptación por historia de usuario, permitiendo detectar y corregir desviaciones de calidad de forma temprana durante todo el desarrollo.  
- **OWASP Top 10 (Open Web Application Security Project):** se utiliza como referencia para identificar y mitigar las vulnerabilidades de seguridad más críticas en aplicaciones web, sirviendo de base para las auditorías de seguridad de los endpoints REST.

En conjunto, estos modelos contribuyen a que la calidad de LittleStyle se controle desde dos frentes complementarios: el producto (atributos definidos por la ISO/IEC 25010\) y el proceso de desarrollo (verificación continua mediante Scrum).

## **3.6 Administración de la Calidad**  {#3.6-administración-de-la-calidad}

La administración de la calidad en LittleStyle tiene un enfoque integral y sistemático orientado a garantizar la excelencia técnica, operativa y funcional en todos los componentes del sistema. Su objetivo primario es satisfacer las necesidades y expectativas de los clientes (padres, madres o acudientes), vendedores y administradores, asegurando resultados consistentes, seguros y confiables en cada iteración del desarrollo.

Para lograrlo, la administración de la calidad en LittleStyle combina dos disciplinas esenciales de manera equilibrada:

- **Aseguramiento de la Calidad del Software (SQA \- *Software Quality Assurance*):** Es un enfoque preventivo enfocado en los procesos, orientando a establecer estándares, normativas, revisiones continuas y auditorías para evitar la aparición de errores durante el ciclo de vida del software.

- **Control de la Calidad del Software (SQC \- *Software Quality Control*):** Este enfoque es correctiivo y de verificación, se centra en el producto y se dedica a evaluar la correcta ejecución y funcionalidad del software mediante pruebas automatizadas y manuales por niveles.

A continuación, se detallan los distintos componentes y mecanismos que componen el modelo de administración de la calidad:

### **3.6.1 Planificación de la Calidad** {#3.6.1-planificación-de-la-calidad}

Desde el inicio del proyecto, se establece el marco de calidad que rige el desarrollo de la plataforma LittleStyle abarcando lo siguiente:

- La definición explícita de los atributos y características de calidad bajo la norma ISO/IEC 25010\.

- La formulación de métricas objetivas de proceso y producto asociadas a los tres procesos principales del negocio.

- El establecimiento de criterios de aceptación técnicos y funcionales para cada Historia de Usuario (US) definidos previo al inicio de cada *sprint*.

  ### **3.6.2 Aseguramiento de la Calidad (SQA) y Auditoría de Software** {#3.6.2-aseguramiento-de-la-calidad-(sqa)-y-auditoría-de-software}

Con base en los lineamientos metodológicos de auditoría de software, se define como auditoría a un proceso sistemático e independiente de revisión y evaluación de los componentes, código, configuraciones y procedimientos para verificar su conformidad con las normas, estándares de la industria y políticas del proyecto.

1. *Auditoría de Código Fuente y Calidad Técnica*   
     
- **Revisiones de Código por Pares (*Peer Reviews*):** Ningún cambio de código se integra directamente a la rama principal de producción sin un *Pull Request* en GitHub que cuente con la revisión y aprobación obligatoria de al menos otro desarrollador del equipo.  
    
- **Cumplimiento de Estándares de Codificación:** Se verifica la adhesión a las guías de estilo para Java (Backend en Spring Boot) y TypeScript/HTML/CSS (Frontend en Angular SPA), la correcta implementación de los principios SOLID y patrones de diseño orientados a la mantenibilidad.  
    
- **Análisis Estático del Código:** Inspección automatizada para la detección de código duplicado, alta complejidad ciclomática, vulnerabilidades potenciales y demás.

2. *Auditoría de Arquitectura y Decisiones Técnicas (ADRs)*

- Verificación continua para el correcto cumplimiento e implementación de la arquitectura monolítica modular definida en Spring Boot, la separación clara de responsabilidades entre capas y el desacoplamiento con el frontend en Angular SPA.

- Correcta aplicación de las Decisiones de Arquitectura adoptadas, como el manejo de autenticación sin estado mediante tokens JWT, el almacenamiento externo de imágenes en Amazon S3 y la recepción transaccional asíncrona mediante Webhooks con la Pasarela de Pagos Wompi.

3. *Auditoría de Cumplimiento Legal y Protección de Datos (Habeas Data)*

- **Protección de Datos de Menores de Edad:** Dado que el primer proceso gestiona información sensible de menores (medidas corporales, edad, condiciones médicas/alergias textiles), se audita el cumplimiento de la Ley 1581 de 2012 y el Decreto 1074 de 2015\.

- **Garantía de Derechos ARCO:** Verificación de que el sistema permita a los titulares consultar, actualizar, rectificar y solicitar la eliminación de sus datos personales y los de sus hijos.

- **Mecanismos de Cifrado:** Inspección para asegurar que los datos sensibles se encuentren cifrados tanto en tránsito (HTTPS/TLS) como en reposo en la base de datos.  
    
4. *Auditoría de Normatividad Comercial y Protección al Consumidor*

- Verificación de la Ley 1480 de 2011 (Estatuto del Consumidor) referente a la transparencia en precios, condiciones de compra, derecho de retracto y seguimiento transparente de pedidos en comercio electrónico.

- Verificación de cumplimiento de la Resolución Andina 2109 de 2019, asegurando que las fichas técnicas del catálogo de prendas incluyan claramente la composición de materiales, tabla de medidas e instrucciones de cuidado textil.

5. *Auditoría de Licencias y Seguridad*

- **Licenciamiento de terceros:** Evaluación de las librerías y dependencias utilizadas en el backend y frontend para que se empleen licencias de código abierto compatibles y evitar riesgos legales por uso no autorizado de software.  
    
- **Seguridad y vulnerabilidades:** Auditorías periódicas de seguridad sobre los endpoints REST para evitar fallos de inyección SQL, vulnerabilidades XSS, autenticación rota y validación de firma criptográfica HMAC en los Webhooks de pagos.

  ### **3.6.3 Control de Calidad (SQC) y Estrategia de Pruebas**  {#3.6.3-control-de-calidad-(sqc)-y-estrategia-de-pruebas}

El control de Calidad ejecuta una estrategia de pruebas por niveles orientada a validar el correcto funcionamiento del producto software:

- **Pruebas Unitarias:** Automatizadas en el Backend utilizando JUnit y Mockito, y en el Frontend utilizando Jasmine y Karma. Se exige una cobertura de código igual o superior al 80% en los módulos críticos (motor de recomendación de tallas y alergias, cálculo de carritos de compra y control de stock de inventario).

- **Pruebas de Integración:** Verificación de una correcta comunicación entre el backend en Spring Boot y la base de datos MySQL (vía Spring Data JPA), la pasarela de pagos Wompi (procesamiento y Webhooks), el servicio de correo Gmail (SMTP) y el almacenamiento multimedia en Amazon S3.  
    
- **Pruebas de Sistema y End-to-End (E2E):** Automatización de flujos completos de negocio desde la perspectiva de los diferentes actores.  
    
- **Pruebas de Rendimiento y Carga:** Uso de herramientas como JMeter o Artillery para simular concurrencia de usuarios en el catálogo de productos y validar que los tiempos de respuesta de la API permanezcan en el rango de los 2 segundos.

  ### **3.6.4 Gestión de Configuración y Control de Versiones**  {#3.6.4-gestión-de-configuración-y-control-de-versiones}


Para manejar la integridad y trazabilidad del código fuente y los entregables del proyecto se tiene en cuenta:

- Se utiliza Git con un modelo de ramificación estructurado garantizando un entorno aislado para el desarrollo de cada historia de usuario.

- Se implementa un control de versiones registrado en la hoja de control de los documentos entregables y repositorios de código.

- La gestión de dependencias se centraliza de manera declarativa mediante Apache Maven para el Backend y npm para el Frontend, fijando versiones estables para evitar errores en entornos de despliegue.

  ### **3.6.5 Gestión y Ciclo de Vida de Defectos**  {#3.6.5-gestión-y-ciclo-de-vida-de-defectos}

Cualquier anomalía o desviación identificada durante la ejecución de pruebas o auditorías se gestiona bajo un flujo formal:

1. **Registro:** Creación del reporte del fallo en la plataforma de seguimiento especificando título, pasos para reproducir, comportamiento esperado, comportamiento obtenido y capturas o logs.  
     
2. **Clasificación:** Asignación de nivel de Severidad (Crítica, Alta, Media, Baja) y nivel de Prioridad (Alta, Media, Baja).  
     
3. **Asignación y Corrección:** Asignación al desarrollador responsable para su depuración y solución.  
     
4. **Re-verificación y Cierre:** Validación de la corrección en el entorno de pruebas antes de autorizar el cierre definitivo del fallo.

   ### **3.6.6 Mejora Continua y Automatización del Ciclo de Vida (CI/CD)** {#3.6.6-mejora-continua-y-automatización-del-ciclo-de-vida-(ci/cd)}

Se automatiza la verificación continua de la calidad a través de un pipeline de Integración Continua y Despliegue Continuo (CI/CD) implementado en Jenkins / Github Actions de la siguiente manera:

1. **Compilación automática (Build)**: Verificación de la sintaxis y construcción de artefactos ejecutables para frontend y backend.

2.  **Ejecución de Pruebas Unitarias e Integración:** Validación inmediata de que los cambios no introducen regresiones en el sistema.

3. **Análisis de Calidad y Cobertura:** Evaluación automática de umbrales de calidad; si el código no cumple con la cobertura mínima o presenta vulnerabilidades de seguridad, la integración es bloqueada de inmediato.

4. **Despliegue Automático (Deploy):** Despliegue supervisado del artefacto validado hacia la infraestructura en la nube para pruebas de la aplicación.

## **3.7 Características de la Administración de la Calidad**  {#3.7-características-de-la-administración-de-la-calidad}

Basado en los principios de un sistema de gestión de calidad, se identifican las siguientes características clave aplicadas al proyecto LittleStyle:

* **Enfoque al cliente:** las funcionalidades priorizadas (recomendación de tallas, seguimiento de pedidos, gestión de perfiles infantiles) se definieron a partir de las necesidades reales de los tres tipos de usuario (Cliente, Vendedor, Administrador), asegurando que el desarrollo esté orientado a resolver sus problemas concretos y no solo a cumplir especificaciones técnicas.  
    
* **Gestión de las relaciones:** se establecen mecanismos de tolerancia a fallos y reintentos automáticos ante interrupciones de los proveedores externos (pasarela de pagos Wompi y servicio de correo Gmail), asegurando que la dependencia de terceros no comprometa la continuidad ni la calidad del servicio.   
    
* **Mejora continua:** el desarrollo se organiza en sprints iterativos con retrospectivas al final de cada uno, donde el equipo identifica errores, cuellos de botella o desviaciones de calidad y ajusta el proceso antes del siguiente sprint, en lugar de esperar hasta el final del proyecto para corregir.  
    
* **Liderazgo en calidad:** el equipo asume de forma conjunta la responsabilidad de velar por el cumplimiento de los criterios de calidad definidos en este plan, promoviendo que cada integrante valide su propio trabajo (código, pruebas, documentación) antes de considerarlo terminado.  
    
* **Enfoque basado en procesos:** la calidad se gestiona en función de los tres procesos de negocio definidos (gestión de perfil infantil, catálogo/compras/pedidos/inventario, y administración del sistema), permitiendo evaluar el cumplimiento de calidad de forma independiente para cada proceso.  
    
* **Toma de decisiones basada en evidencia:** las decisiones de ajuste o priorización se apoyan en las métricas e indicadores (por ejemplo, tiempos de respuesta, tasa de errores, cumplimiento de entregables), en lugar de criterios subjetivos.  
    
* **Compromiso de las personas:** cada historia de usuario tiene un responsable claramente asignado dentro del equipo, lo que fortalece la apropiación individual sobre la calidad del trabajo entregado, reforzada mediante revisiones de código por pares antes de integrar cualquier cambio. 

## **3.8 Atributos de Calidad**  {#3.8-atributos-de-calidad}

Los atributos de calidad representan las propiedades y características que definen la efectividad, confiabilidad, usabilidad y valor global del sistema LittleStyle. De acuerdo con lo establecido en la norma ISO/IEC 25010, los atributos de calidad no solo definen el comportamiento operativo del software, sino que también sirven como guía principal para la toma de decisiones arquitectónicas adecuadas.

### **3.8.1 Adecuación Funcional** {#3.8.1-adecuación-funcional}

Capacidad del producto para proporcionar funciones que satisfagan las necesidades declaradas e implícitas bajo condiciones específicas.

Aplicación en LittleStyle:

- **Corrección Funcional (Proceso 1):** El algoritmo de recomendación de tallas debe realizar cálculos exactos cruzando medidas biométricas del menor con la tabla de medidas del fabricante, garantizando sugerencias precisas.

- **Completitud Funcional (Proceso 2):** Ejecución rigurosa de las reglas de negocio en la gestión del carrito de compras, cálculo automático de impuestos, aplicación de descuentos y actualización inmediata del stock de prendas tras la confirmación de pago.

- **Pertinencia Funcional (Proceso 3):** El panel de control debe proveer información consolidada relevante sobre ventas, productos más consultados y nivel de inventarios para la toma de decisiones comerciales.

  ### **3.8.2 Eficiencia de Desempeño** {#3.8.2-eficiencia-de-desempeño}

Rendimiento del software en relación con la cantidad de recursos utilizados bajo condiciones establecidas.

Aplicación en LittleStyle:

- **Comportamiento Temporal:** El procesamiento del motor de recomendación de tallas debe responder en un tiempo inferior a 2 segundos y la carga del catálogo de prendas y aplicación de filtros en menos de 1.5 segundos.

- **Uso de Recursos:** Optimización de consultas SQL mediante Spring Data JPA e indexación estratégica en la base de datos para minimizar el uso de CPU y memoria RAM en el servidor backend en Spring Boot.

- **Capacidad:** Capacidad del sistema para soportar picos de concurrencia de usuarios durante temporadas comerciales sin degradación en los tiempos de respuesta de la API REST.

  ### **3.8.3 Compatibilidad** {#3.8.3-compatibilidad}

Capacidad de dos o más sistemas o componentes para intercambiar información y/o ejecutar sus funciones compartiendo el mismo entorno.

Aplicación en LittleStyle:

- **Interoperabilidad (Proceso 2):** Comunicación fluida y estructurada mediante API REST y Webhooks con la pasarela de pagos para la recepción y validación asíncrona de transacciones en tiempo real.

- **Coexistencia:** Integración del backend con servicios externos en la nube, como Amazon S3 para el almacenamiento y entrega de recursos multimedia y servidores SMTP para el envío de correos transaccionales.


  ### **3.8.4 Capacidad de Interacción (Usabilidad)** {#3.8.4-capacidad-de-interacción-(usabilidad)}

Facilidad con la que el producto software puede ser entendido, aprendido, usado y resultar atractivo para los diferentes tipos de usuario.

Aplicación en LittleStyle:

- **Protección contra Errores de Usuario (Proceso 1):** El formulario de registro del perfil infantil incluye validaciones en tiempo real en la interfaz que previenen el ingreso de datos biométricos o medidas antropométricas incompletas o incorrectas.

- **Operabilidad (Proceso 2):** Diseño del flujo de compra optimizado para completar el pedido con pocos clicks desde el carrito de compras.

- **Capacidad de Aprendizaje (Proceso 3):** Interfaz limpia con gráficos intuitivos en el dashboard administrativo, facilitando el aprendizaje y la gestión rápida de productos e inventario por parte del vendedor.

- **Adaptabilidad Multi-Dispositivo (Diseño Responsivo):** La interfaz de usuario del frontend, desarrollada como una Single Page Application (SPA) en Angular, implementa patrones de diseño responsivo. Esto garantiza que los procesos se adapten de forma fluida y optimizada a diferentes resoluciones de pantalla y dispositivos.


  ### **3.8.5 Fiabilidad** {#3.8.5-fiabilidad}

Grado en que el sistema realiza sus funciones de manera consistente y sin errores bajo condiciones normales y ante situaciones adversas.

Aplicación en LittleStyle:

- **Disponibilidad:** La plataforma mantendrá una disponibilidad operativa (*Uptime*) continua superior al 99.5% en su entorno de producción.

- **Tolerancia a fallos:** Si la pasarela de pagos Wompi o el servicio de correo presentan interrupciones temporales, el backend captura la excepción, registra el estado pendiente y ejecuta reintentos automáticos sin detener la sesión del usuario ni corromper los datos del pedido.


  ### **3.8.6 Seguridad** {#3.8.6-seguridad}

Nivel de protección de los datos e información para evitar accesos no autorizados o modificaciones malintencionadas, garantizando confidencialidad, integridad, autenticidad y no repudio.

Aplicación en LittleStyle:

- **Confidencialidad y Cumplimiento Legal (Habeas Data):** En concordancia con la Ley 1581 de 2012 y el Decreto 1074 de 2015, la información sensible de los menores se almacena cifrada en la base de datos y se transmite exclusivamente mediante protocolo seguro HTTPS/TLS.

- **Autenticidad y Autorización:** Implementación de control de acceso basado en roles (RBAC) gestionado con Spring Security y tokens JWT firmados digitalmente.

- **Integridad Transaccional:** Validación obligatoria de la firma criptográfica HMAC en las notificaciones recibidas vía Webhooks de Wompi para asegurar que la confirmación del pago no haya sido alterada por terceros.

## **3.9 Atributos Referenciados en la [ISO/IEC 25010](https://www.iso25000.com/index.php/normas-iso-25000/iso-25010)**  {#3.9-atributos-referenciados-en-la-iso/iec-25010}

| Característica de Calidad (ISO/IEC 25010\) | Subcaracterísticas | ¿Aplicable al proyecto? | Justificación |
| ----- | :---- | ----- | :---- |
| Adecuación Funcional | \-Completitud funcional. \-Corrección funcional. \-Pertinencia funcional.  | Sí | Es fundamental para el Proceso 1, ya que el algoritmo de recomendación debe proveer sugerencias de tallas estrictamente correctas basadas en los perfiles. En el Proceso 2, asegura que las reglas de negocio (cálculo de totales, descuentos y actualización de inventario) operen con exactitud. |
| Eficiencia de Desempeño | \-Comportamiento temporal. \-Utilización de recursos. \- Capacidad.  | Sí | Crítico para el Proceso 2; el catálogo de productos y los filtros avanzados deben responder en milisegundos para evitar el abandono del carrito. Para el Proceso 3, garantiza que la generación de dashboards y reportes masivos no consuma en exceso los recursos del servidor. |
| Compatibilidad | \- Coexistencia. \- Interoperabilidad.  | Sí | Indispensable para el Proceso 2, dado que el sistema debe interoperar sin fricciones con la pasarela de pagos externa. La comunicación vía APIs y Webhooks debe asegurar un intercambio de información transaccional estructurado y en tiempo real. |
| Capacidad de Interacción | \-Reconocibilidad de la adecuación. \-Aprendizabilidad. \-Operabilidad. \-Protección contra errores de usuario. \-Involucración del usuario. \-Inclusividad. \-Asistencia al usuario. | Sí | En el Proceso 1, la interfaz debe proteger contra errores de usuario al ingresar las medidas del menor, siendo intuitiva (operabilidad). En el Proceso 3, los paneles administrativos deben ser claros y fáciles de aprender para que los vendedores gestionen su stock de forma ágil. |
| Fiabilidad | \- Ausencia de fallos. \- Disponibilidad. \- Tolerancia a fallos.  | Sí | El Proceso 2 (compras y pedidos) exige alta disponibilidad comercial. Si la plataforma presenta caídas o no tolera fallos de red durante el procesamiento del pago, se pierden ventas y se afecta la confianza del cliente. |
| Seguridad | \- Confidencialidad. \- Integridad. \- No repudio. \- Responsabilidad. \- Autenticidad. \- Resistencia. | Sí | Obligatorio por el manejo de información. En el Proceso 1, protege la confidencialidad de los datos personales de menores de edad (Habeas Data). En el Proceso 2, garantiza la integridad y autenticidad en la validación técnica de las transacciones procesadas. |

# **4\. REQUISITOS NO FUNCIONALES DEL SISTEMA (RNF)** {#4.-requisitos-no-funcionales-del-sistema-(rnf)}

## **4.1 Característica de Calidad (ISO/IEC 25010): Eficiencia de desempeño**  {#4.1-característica-de-calidad-(iso/iec-25010):-eficiencia-de-desempeño}

### **4.1.1 RNF: 01 \- Respuesta oportuna de la recomendación de tallas \- Proceso:Gestión del Perfil infantil y recomendación personalizada de tallas y prendas.** {#4.1.1-rnf:-01---respuesta-oportuna-de-la-recomendación-de-tallas---proceso:gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas.}

| Ítem | Descripción |
| :---- | ----- |
| **ID** | RNF-01 |
| **Categoría** | Comportamiento temporal |
| **Descripción del Requisito** | El sistema debe procesar las peticiones del motor de recomendación, garantizando que al menos el 95% de las consultas realizadas por los clientes al catálogo infantil muestran las prendas y tallas recomendadas en un tiempo menor o igual a 2 segundos, bajo condiciones normales de carga.  |
| **Métrica o Criterio de Aceptación** | El tiempo de carga de recomendaciones debe ser ≤ 2 segundos.  |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Garantiza que el algoritmo de recomendación responda con la rapidez necesaria para evitar la deserción del cliente durante la búsqueda de prendas.  |

### **4.1.2 RNF: 02 \- Agilidad de los filtros del catálogo \- Proceso: Gestión del catálogo, compras, pedidos e inventario.** {#4.1.2-rnf:-02---agilidad-de-los-filtros-del-catálogo---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-02 |
| **Categoría** | Comportamiento temporal  |
| **Descripción del Requisito** | El sistema debe ejecutar la aplicación de criterios de búsqueda, garantizando que al menos el 90% de los clientes que consultan el catálogo de prendas obtengan los resultados filtrados por talla, edad o tipo en un tiempo menor a 1.5 segundos. |
| **Métrica o Criterio de Aceptación** | La efectividad de filtros en catálogo es \< 1.5 segundos en la respuesta de búsqueda. |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Un filtrado lento del catálogo puede provocar que el cliente abandone la búsqueda antes de encontrar la prenda adecuada.  |

### **4.1.3 RNF: 03 \- Sincronización del inventario tras el pago \- Proceso: Gestión del catálogo, compras, pedidos e inventario.** {#4.1.3-rnf:-03---sincronización-del-inventario-tras-el-pago---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-03 |
| **Categoría** | Comportamiento temporal |
| **Descripción del Requisito** | El sistema debe realizar la actualización de existencias en base de datos, asegurando que en al menos el 95% de los pedidos pagados por los clientes se descuente el stock del producto en un tiempo menor a 500 milisegundos.  |
| **Métrica o Criterio de Aceptación** | El tiempo de sincronización de stock debe ser \< 500 milisegundos tras la aprobación de la transacción. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Evita la sobreventa de productos al garantizar que el stock se actualice de forma casi inmediata tras confirmarse el pago.  |

### **4.1.4 RNF: 04 \- Tiempo de carga  del panel administrativo \- Proceso: Gestión administrativa y procesos internos.** {#4.1.4-rnf:-04---tiempo-de-carga-del-panel-administrativo---proceso:-gestión-administrativa-y-procesos-internos.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-04 |
| **Categoría** | Comportamiento temporal |
| **Descripción del Requisito** | El sistema debe desplegar los indicadores clave de la plataforma, asegurando que al menos el 90% de los usuarios administrativos visualicen la totalidad de los componentes del dashboard en un tiempo menor a 3 segundos.  |
| **Métrica o Criterio de Aceptación** | El tiempo de respuesta del dashboard administrativo debe ser \<3 segundos |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Permite que el personal administrativo consulte rápidamente las métricas de negocio para la toma oportuna de decisiones.  |

### **4.1.5 RNF: 05 \- Capacidad de respuesta ante concurrencia de clientes \- Proceso:Gestión del Catálogo, Compras, Pedidos e Inventario**  {#4.1.5-rnf:-05---capacidad-de-respuesta-ante-concurrencia-de-clientes---proceso:gestión-del-catálogo,-compras,-pedidos-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-05 |
| **Categoría** | Capacidad |
| **Descripción del Requisito** | El sistema debe mantener la capacidad de procesamiento de peticiones, logrando que al menos el 99% de las interacciones simultáneas realizadas por hasta 100 usuarios explorando el catálogo finalicen con éxito sin errores de pantalla.  |
| **Métrica o Criterio de Aceptación** | La tasa de procesamiento bajo carga simultánea debe ser ≥99% ante picos de 100 usuarios en simultaneo. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Asegurarla estabilidad del comercio electrónico durante eventos de alta demanda.  |

## 

## **4.2 Característica de Calidad (ISO/IEC 25010): Seguridad** {#4.2-característica-de-calidad-(iso/iec-25010):-seguridad}

### **4.2.1 RNF: 06 \- Confidencialidad de los datos del menor \- Proceso: Gestión del Perfil infantil y recomendación personalizada de tallas y prendas.** {#4.2.1-rnf:-06---confidencialidad-de-los-datos-del-menor---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-06  |
| **Categoría** | Confidencialidad |
| **Descripción del Requisito** | El sistema debe ejecutar el cifrado de datos personales, garantizando que en un porcentaje mayor al 96% de los campos con información de menores registrados por los acudientes garantizando que la información se mantenga cifrada en reposo y en tránsito sin accesos no autorizados.  |
| **Métrica o Criterio de Aceptación** | La efectividad en la protección de datos menores debe ser \> 96%  |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Protege la información confidencial de los menores de edad y asegura el cumplimiento legal de la Ley 1581 de 2012 y el Decreto 1074 de 2015 sobre Habeas Data.  |

###   **4.2.2 RNF: 07 \- Autenticidad de notificaciones de pago(WebHooks) \- Proceso: Gestión del catálogo, compras, pedidos e inventario.** {#4.2.2-rnf:-07---autenticidad-de-notificaciones-de-pago(webhooks)---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-07 |
| **Categoría** | Integridad y autenticidad |
| **Descripción del Requisito** | El sistema debe realizar la validación por firma de peticiones externas, asegurando que un porcentaje mayor o igual al 99.8% de los eventos Webhook enviados por la pasarela de pagos sean autenticados mediante la verificación de la firma HMAC antes de actualizar el estado de la compra.  |
| **Métrica o Criterio de Aceptación** | La tasa de autenticación de notificaciones de pago debe ser ≥ 99.8%. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Previene fraudes por suplantación al validar la firma antes de procesar cualquier transacción comercial.  |

### **4.2.3 RNF: 08 \- Trazabilidad de acciones administrativas \- Proceso: Gestión administrativa y procesos internos.** {#4.2.3-rnf:-08---trazabilidad-de-acciones-administrativas---proceso:-gestión-administrativa-y-procesos-internos.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-08  |
| **Categoría** | Responsabilidad/ No repudio |
| **Descripción del Requisito** | El sistema debe registrar las operaciones críticas del sistema, garantizando que en un porcentaje mayor al 99.5%  de las acciones ejecutadas por los usuarios administrativos los eventos queden almacenados en un log de auditoría inalterable indicando usuario, fecha, hora y detalle.  |
| **Métrica o Criterio de Aceptación** | La trazabilidad de acciones administrativas debe ser \>99.5%. |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Facilita el seguimiento de responsabilidades y garantiza la trazabilidad sobre cambios en cuentas, roles y moderación de contenido.  |

### **4.2.4 RNF: 09 \- Resistencia ante vulnerabilidades en dependencias \- Proceso: Gestión del catálogo, compras, pedidos e inventario.** {#4.2.4-rnf:-09---resistencia-ante-vulnerabilidades-en-dependencias---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-09  |
| **Categoría** | Resistencia |
| **Descripción del Requisito** | El sistema debe gestionar el estado de las librerías de código,logrando que un porcentaje mayor o igual al 98% de las dependencias utilizadas en los módulos de comercio electrónico permanezcan libres de fallos de seguridad conocidos mediante su actualización oportuna. |
| **Métrica o Criterio de Aceptación** | La efectividad en el mantenimiento seguro de dependencias debe ser  ≥ 98%. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Reduce la ventana de exposición ante vulnerabilidades conocidas en librerías de terceros, protegiendo la confidencialidad e integridad de la información y la continuidad del servicio.  |

### **4.2.5 RNF: 10 \- Control de accesos por roles  \- Proceso: Gestión Administrativa y Procesos Internos**  {#4.2.5-rnf:-10---control-de-accesos-por-roles---proceso:-gestión-administrativa-y-procesos-internos}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-10 |
| **Categoría** | Autorización |
| **Descripción del Requisito** | El sistema debe controlar el acceso a funciones restringidas, asegurando que un porcentaje mayor o igual al 98.5% de los intentos de consulta a endpoints protegidos realizados por usuarios sin rol o token JWT válido sean denegados automáticamente por el módulo de seguridad. |
| **Métrica o Criterio de Aceptación** | La tasa de restricción de acceso no autorizado debe ser ≥ 98.5%. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Garantiza la separación estricta de privilegios entre clientes, vendedores y administradores dentro del sistema.  |

## **4.3 Característica de Calidad (ISO/IEC 25010): Adecuación Funcional** {#4.3-característica-de-calidad-(iso/iec-25010):-adecuación-funcional}

### **4.3.1 RNF: 11 \- Precisión en la recomendación de tallas \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas.** {#4.3.1-rnf:-11---precisión-en-la-recomendación-de-tallas---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-11 |
| **Categoría** | Correcciónn Funcional |
| **Descripción del Requisito** | El sistema debe generar recomendaciones de talla a partir de las medidas registradas en el perfil infantil y las tablas de tallas disponibles, de manera que la tasa de adopción de la recomendación de talla sea superior al 80 % de las compras finalizadas en las que el motor haya generado una recomendación. |
| **Métrica o Criterio de Aceptación** | Tasa de adopción de la talla recomendada \> 80% sobre las compras finalizadas en las que el motor generó una sugerencia. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Garantiza la exactitud del motor de recomendación de tallas y prendas para generar alta confianza en las decisiones de compra del cliente. |

### **4.3.2 RNF: 12 \- Completitud del registro del perfil infantil \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** {#4.3.2-rnf:-12---completitud-del-registro-del-perfil-infantil---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-12 |
| **Categoría** | Completitud Funcional |
| **Descripción del Requisito** | El sistema debe permitir registrar y almacenar la información necesaria de los perfiles infantiles, incluyendo los datos definidos para medidas, edad y restricciones por alergias textiles, de manera que más del 90 % de los perfiles registrados contengan la totalidad de los campos establecidos para el perfil. |
| **Métrica o Criterio de Aceptación** | Completitud del perfil infantil \> 90 % |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Permite disponer de la información necesaria para que el motor de recomendación sugiera prendas correctas y seguras. |

### **4.3.3 RNF: 13 \- Trazabilidad del ciclo de vida del pedido \- Proceso: Gestión del catálogo, compras, pedido e inventario** {#4.3.3-rnf:-13---trazabilidad-del-ciclo-de-vida-del-pedido---proceso:-gestión-del-catálogo,-compras,-pedido-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-13 |
| **Categoría** | Completitud Funcional |
| **Descripción del Requisito** | El sistema debe registrar y actualizar correctamente las transiciones del ciclo de vida de cada pedido, incluyendo el número de guía cuando corresponda, de manera que más del 98 % de los pedidos pagados presentan un historial completo de estados y la guía logística registrada sin inconsistencias. |
| **Métrica o Criterio de Aceptación** | Tasa de trazabilidad y actualización de estados \> 98 % |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Asegurar que el flujo comercial cumpla rigurosamente las reglas de negocio de pedidos y la trazabilidad del envío para el cliente. |

### **4.3.4 RNF: 14 \- Emisión oportuna de alertas de reabastecimiento \- Proceso: Gestión del catálogo, compras, pedidos e inventario** {#4.3.4-rnf:-14---emisión-oportuna-de-alertas-de-reabastecimiento---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-14 |
| **Categoría** | Completitud Funcional |
| **Descripción del Requisito** | El sistema debe monitorear el nivel de stock y generar oportunamente las alertas de reabastecimiento cuando un producto alcance el valor mínimo configurado, de manera que el 99 % de los productos que lleguen a agotarse hayan recibido previamente la alerta correspondiente. |
| **Métrica o Criterio de Aceptación** | Precisión de alertas de reabastecimiento \> 99 % |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Previene el desabastecimiento imprevisto y garantiza la continuidad en la oferta de productos por parte de los vendedores. |

### **4.3.5 RNF: 15 \- Consistencia de datos en reportes financieros y dashboards \- Proceso: Gestión administrativa y procesos internos** {#4.3.5-rnf:-15---consistencia-de-datos-en-reportes-financieros-y-dashboards---proceso:-gestión-administrativa-y-procesos-internos}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-15 |
| **Categoría** | Corrección Funcional |
| **Descripción del Requisito** | El sistema debe presentar en los dashboards y reportes administrativos información consistente con los registros transaccionales de ventas almacenados en la base de datos, de manera que más del 99.9 % de las transacciones evaluadas coinciden exactamente con la información consolidada presentada en los reportes. |
| **Métrica o Criterio de Aceptación** | Consistencia de datos en reportes financieros \> 99.9%. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Garantiza la entrega de datos fiables e íntegros a administradores y vendedores para la toma de decisiones comerciales. |

## **4.4 Característica de Calidad (ISO/IEC 25010): Capacidad de Interacción** {#4.4-característica-de-calidad-(iso/iec-25010):-capacidad-de-interacción}

### **4.4.1 RNF:  16 \- Prevención de errores en el registro de medidas \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas**  {#4.4.1-rnf:-16---prevención-de-errores-en-el-registro-de-medidas---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-16 |
| **Categoría** | Protección frente a errores de usuario |
| **Descripción del Requisito** | El sistema debe validar los datos ingresados en el formulario del perfil infantil antes de almacenarlos, identificando valores faltantes, inválidos o inconsistentes. El cumplimiento se medirá mediante la Tasa de error en captura de medidas biométricas, el porcentaje de errores deberá ser menor al 5 %  |
| **Métrica o Criterio de Aceptación** | La tasa de errores de validación durante el registro de medidas biométricas debe ser menor al 5 % del total de intentos de guardado.  |
| **Prioridad** | Alta  |
| **Comentarios / Justificación** | Reduce los errores de usuario durante el registro del perfil infantil y contribuye a que el motor de recomendación trabaje con información válida y consistente.  |

### **4.4.2 RNF: 17 \- Asistencia al usuario mediante resolución de incidencias \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas**  {#4.4.2-rnf:-17---asistencia-al-usuario-mediante-resolución-de-incidencias---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-17  |
| **Categoría** | Asistencia al usuario  |
| **Descripción del Requisito** | El sistema debe facilitar la gestión y seguimiento de las incidencias reportadas por clientes y vendedores, permitiendo que los administradores atiendan oportunamente las dificultades relacionadas con el uso de la plataforma. El cumplimiento se medirá mediante la Tasa de resolución de incidencias de usuarios, el porcentaje de incidencias resueltas en menos de 24 horas deberá ser superior al 90 %. |
| **Métrica o Criterio de Aceptación** | Tasa de resolución de incidencias de usuarios \> 90 %.  |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Una atención oportuna de las incidencias permite brindar asistencia a clientes y vendedores cuando encuentran dificultades durante el uso de la plataforma, reduciendo el impacto de estos problemas sobre la realización de sus actividades.  |

### **4.4.3 RNF: 18 \- Operabilidad del flujo de compra \- Proceso: Gestión del catálogo, compras, pedidos e inventario**  {#4.4.3-rnf:-18---operabilidad-del-flujo-de-compra---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-18  |
| **Categoría** | Operabilidad |
| **Descripción del Requisito** | El sistema debe proporcionar un flujo de compra claro y continuo desde la selección de productos, el carrito y la confirmación del pedido hasta el inicio del pago. El cumplimiento se medirá mediante la Tasa de abandono del carrito el porcentaje de abandono deberá mantenerse por debajo del 30 %  |
| **Métrica o Criterio de Aceptación** | La tasa de abandono del carrito debe mantenerse por debajo del 30 % sobre el total de carritos creados.  |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Una tasa reducida de abandono permite evidenciar que el flujo de compra es comprensible y no presenta afecciones significativas que impidan al cliente finalizar el proceso.  |

### **4.4.4 RNF: 19 \- Facilidad de actualización del perfil infantil \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas**  {#4.4.4-rnf:-19---facilidad-de-actualización-del-perfil-infantil---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-19  |
| **Categoría** | Involucración del usuario  |
| **Descripción del Requisito** | El sistema debe permitir que el cliente consulte y actualice de manera clara y accesible las medidas registradas en los perfiles infantiles, favoreciendo su actualización periódica a medida que el menor crece. El cumplimiento se medirá mediante la Frecuencia de actualización de perfiles, más del 60 % deberá haber sido actualizado en menos de seis meses. |
| **Métrica o Criterio de Aceptación** | Más del 60 % de los perfiles infantiles deben haber sido actualizados en un periodo inferior a 6 meses.  |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Facilitar la actualización periódica de las medidas favorece la involucración del usuario con la plataforma y permite mantener información actualizada para mejorar la precisión de las recomendaciones de tallas y prendas.  |

### **4.4.5 RNF: 20 \- Comprensión del panel administrativo \- Proceso: Gestión administrativa y procesos internos**  {#4.4.5-rnf:-20---comprensión-del-panel-administrativo---proceso:-gestión-administrativa-y-procesos-internos}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-20  |
| **Categoría** | Aprendizabilidad  |
| **Descripción del Requisito** | El dashboard administrativo debe presentar los indicadores, controles y opciones de ventas, inventario, búsquedas y operación de forma organizada y comprensible, permitiendo que vendedores y administradores aprendan a realizar las operaciones principales del panel. El cumplimiento se medirá mediante la Tasa de ejecución correcta de tareas administrativas, al menos el 90 % deberá completarse correctamente. |
| **Métrica o Criterio de Aceptación** | Tasa de ejecución correcta de tareas administrativas ≥ 90 %.  |
| **Prioridad** | Media  |
| **Comentarios / Justificación** | Favorece la capacidad de aprendizaje y la operabilidad del sistema, reduciendo la dificultad para interpretar información y ejecutar operaciones desde el panel.  |

## **4.5 Característica de Calidad (ISO/IEC 25010): Compatibilidad** {#4.5-característica-de-calidad-(iso/iec-25010):-compatibilidad}

### **4.5.1 RNF: 21 \- Interoperabilidad con la pasarela de pagos externa \- Proceso: Gestión del catálogo, compras, pedidos e inventario.** {#4.5.1-rnf:-21---interoperabilidad-con-la-pasarela-de-pagos-externa---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario.}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF- 21 |
| **Categoría** | Interoperabilidad |
| **Descripción del Requisito** | El sistema debe interoperar con la pasarela de pagos externa mediante API y Webhooks para recibir y procesar las notificaciones de las transacciones, de manera que la tasa de excepciones en la integración sea inferior al 2 % del total de transacciones notificadas por la pasarela. |
| **Métrica o Criterio de Aceptación** | Tasa de excepciones en integración con pasarela (Webhooks) \< 2 %. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Asegura una integración fluida con la pasarela externa sin perder confirmaciones de pago. |

### **4.5.2 RNF: 22 \- Coexistencia y compatibilidad entre plataformas y navegadores web \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** {#4.5.2-rnf:-22---coexistencia-y-compatibilidad-entre-plataformas-y-navegadores-web---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-22 |
| **Categoría** | Coexistencia |
| **Descripción del Requisito** | La aplicación web debe coexistir correctamente en los navegadores y plataformas objetivo, manteniendo disponibles sus funcionalidades principales, de manera que más del 98 % de las plataformas evaluadas presenten una ejecución exitosa sin errores visuales o funcionales. |
| **Métrica o Criterio de Aceptación** | Soporte de Plataformas Compatibles \> 98 % |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Garantiza que cualquier usuario acceda y gestione perfiles infantiles desde su navegador o dispositivo de preferencia sin bloqueos visuales o técnicos. |

### **4.5.3 RNF: 23 \- Interoperabilidad para el envío de notificaciones por correo electrónico SMTP \- Proceso: Gestión del catálogo, compras, pedidos e inventario** {#4.5.3-rnf:-23---interoperabilidad-para-el-envío-de-notificaciones-por-correo-electrónico-smtp---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-23 |
| **Categoría** | Interoperabilidad |
| **Descripción del Requisito** | El sistema debe interoperar con el servicio externo de correo para enviar las notificaciones transaccionales asociadas al registro, compras y actualización de pedidos, de manera que la tasa de entrega exitosa de notificaciones sea superior al 98 % sobre el total de notificaciones enviadas. |
| **Métrica o Criterio de Aceptación** | Tasa de entrega exitosa de notificaciones  \> 98%. |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Permite enviar notificaciones automatizadas utilizando la infraestructura estándar de mensajería en la nube. |

### **4.5.4 RNF: 24 \- Interoperabilidad en la exportación de reportes analíticos \- Proceso: Gestión administrativa y procesos internos** {#4.5.4-rnf:-24---interoperabilidad-en-la-exportación-de-reportes-analíticos---proceso:-gestión-administrativa-y-procesos-internos}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-24 |
| **Categoría** | Interoperabilidad  |
| **Descripción del Requisito** | El sistema debe generar y exportar los reportes administrativos de ventas, búsquedas e inventario en un formato que pueda ser procesado por aplicaciones externas, de manera que más del 98 % de las solicitudes de exportación realizadas por vendedores y administradores finalicen correctamente. |
| **Métrica o Criterio de Aceptación** | Tasa de éxito en la exportación de reportes \> 98 %. |
| **Prioridad** | Media |
| **Comentarios / Justificación** | Facilita la descarga y posterior análisis de datos comerciales en hojas de cálculo o visores externos. |

### **4.5.5 RNF: 25 Interoperabilidad con el servicio de almacenamiento multimedia en la nube \- Proceso: Gestión del catálogo, compras, pedidos e inventario** {#4.5.5-rnf:-25-interoperabilidad-con-el-servicio-de-almacenamiento-multimedia-en-la-nube---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-25 |
| **Categoría** | Interoperabilidad |
| **Descripción del Requisito** | El sistema debe coexistir con el servicio externo de almacenamiento multimedia utilizado para gestionar las imágenes del catálogo, permitiendo consultar y visualizar correctamente los recursos asociados a los productos, de manera que la tasa de disponibilidad de recursos multimedia sea superior al 99 % sobre el total de recursos solicitados. |
| **Métrica o Criterio de Aceptación** | Tasa de disponibilidad de recursos multimedia \> 99 %. |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Permite desacoplar el manejo de archivos multimedia del servidor principal y de la base de datos optimizando el ancho de banda. |

## **4.6 Característica de Calidad (ISO/IEC 25010): Fiabilidad**  {#4.6-característica-de-calidad-(iso/iec-25010):-fiabilidad}

### **4.6.1 RNF: 26 \- Disponibilidad operativa de la plataforma \- Proceso: Gestión del catálogo, compras, pedidos e inventario**  {#4.6.1-rnf:-26---disponibilidad-operativa-de-la-plataforma---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-26 |
| **Categoría** | Disponibilidad |
| **Descripción del Requisito** | El sistema debe mantener disponibles las funcionalidades principales de catálogo, carrito, pedidos y consulta de productos durante la operación planificada. El cumplimiento se medirá mediante la Disponibilidad Operativa del Servicio, el sistema deberá permanecer disponible en un porcentaje superior al 99.5 %.  |
| **Métrica o Criterio de Aceptación** | La disponibilidad operativa de la plataforma debe ser superior al 99.5 % del tiempo de operación planificado.  |
| **Prioridad** | Alta  |
| **Comentarios / Justificación** | Las interrupciones del servicio pueden impedir consultas y compras, generando pérdida de operaciones y afectando directamente tanto a clientes como a vendedores.  |

### **4.6.2 RNF: 27 \- Disponibilidad del panel administrativo \- Proceso: Gestión administrativa y procesos internos**  {#4.6.2-rnf:-27---disponibilidad-del-panel-administrativo---proceso:-gestión-administrativa-y-procesos-internos}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-27 |
| **Categoría** | Disponibilidad |
| **Descripción del Requisito** | El dashboard administrativo y las funcionalidades asociadas a reportes deben permanecer accesibles durante el tiempo de servicio planificado. El cumplimiento se medirá mediante la Disponibilidad del panel administrativo (Uptime), la disponibilidad deberá ser superior al 99.9 %.  |
| **Métrica o Criterio de Aceptación** | Disponibilidad del panel administrativo superior al 99.9 %. |
| **Prioridad** | Alta  |
| **Comentarios / Justificación** | Garantiza que los usuarios administrativos puedan supervisar continuamente el comportamiento de la plataforma y consultar la información necesaria para sus actividades.  |

### **4.6.3 RNF: 28 \- Tolerancia a fallos de la pasarela de pagos \- Proceso: Gestión del catálogo, compras, pedidos e inventario**  {#4.6.3-rnf:-28---tolerancia-a-fallos-de-la-pasarela-de-pagos---proceso:-gestión-del-catálogo,-compras,-pedidos-e-inventario}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-28 |
| **Categoría** | Tolerancia a fallos  |
| **Descripción del Requisito** | Ante errores temporales o interrupciones en la comunicación con la pasarela de pagos, el sistema debe conservar íntegros los datos del pedido y mantener la transacción en un estado válido sin detener el resto de la plataforma. El cumplimiento se medirá mediante la Tasa de preservación transaccional ante fallos externos, al menos el 99 % deberá conservar su información correctamente.  |
| **Métrica o Criterio de Aceptación** | Tasa de preservación transaccional ante fallos ≥ 99 %.  |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Evita que interrupciones temporales de un servicio externo provoquen pérdida de pedidos, inconsistencias en las transacciones o interrupciones innecesarias del proceso comercial.  |

### **4.6.4 RNF: 29 \- Ausencia de fallos en el motor de recomendación \- Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas**  {#4.6.4-rnf:-29---ausencia-de-fallos-en-el-motor-de-recomendación---proceso:-gestión-del-perfil-infantil-y-recomendación-personalizada-de-tallas-y-prendas}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-29 |
| **Categoría** | Ausencia de fallos  |
| **Descripción del Requisito** | El motor de recomendación debe procesar las solicitudes de tallas y prendas manteniendo un funcionamiento estable, evitando que excepciones no controladas interrumpan las consultas realizadas por los clientes. El cumplimiento se medirá mediante la Tasa de consultas de recomendación completadas sin fallos, al menos el 99 % deberán completarse sin fallos. |
| **Métrica o Criterio de Aceptación** | Tasa de consultas de recomendación completadas sin fallos ≥ 99 %.  |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | La recomendación personalizada constituye una funcionalidad central de LittleStyle; una alta proporción de consultas completadas sin fallos permite asegurar un comportamiento consistente del motor y evitar interrupciones durante la consulta de tallas y prendas.  |

### **4.6.5 RNF: 30 \- Recuperabilidad ante caídas del servicio comercial \- Proceso: Gestión administrativa y procesos internos**  {#4.6.5-rnf:-30---recuperabilidad-ante-caídas-del-servicio-comercial---proceso:-gestión-administrativa-y-procesos-internos}

| Ítem | Descripción |
| ----- | ----- |
| **ID** | RNF-30 |
| **Categoría** | Recuperabilidad  |
| **Descripción del Requisito** | Ante una caída que interrumpa las funcionalidades de catálogo, carrito, pedidos o inventario, el sistema debe poder restablecer su operación después de la atención del incidente. El cumplimiento se medirá mediante el Tiempo Medio de Reparación Ante Caídas, la medida mensual deberá ser igual o inferior a 30 minutos.  |
| **Métrica o Criterio de Aceptación** | Tiempo Medio de Reparación Ante Caídas ≤ 30 minutos.  |
| **Prioridad** | Alta |
| **Comentarios / Justificación** | Permite determinar la capacidad de la plataforma para recuperar su operación después de una interrupción y reducir el periodo durante el cual los clientes y vendedores no pueden utilizar las funcionalidades comerciales.  |

# **5\. EVIDENCIAS MÉTRICAS**  {#5.-evidencias-métricas}

**PROGRAMA DE INGENIERÍA DE SISTEMAS Y COMPUTACIÓN**

**Tel: (57) 6 735 9300 Ext 355**  
**Carrera 15 Calle 12 Norte**  
**Armenia, Quindío – Colombia**  
**ingesis@uniquindio.edu.co**  
