LittleStyle Plan de Gestion de la Calidad del Proyecto 

Version:Fecha:0109/20261.00 



<!-- Start of picture text -->
SY<br>Ves<br>(4 {<br>\ \<br>a y<br>><br>(<br>©<br>— E>)<br>UNIQUINDIO<br>en conexion territorial<br>www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Plan de Gestión de la Calidad del Proyecto** _Curso: Ingeniería de software III_ 







<!-- Start of picture text -->
Página 2 de 48<br><!-- End of picture text -->

##### **HOJA DE CONTROL** 

|**Proyecto**|LittleStyle|||
|---|---|---|---|
|**Entregable**|Especificación de Requisitos|||
|**Versión/Edición**|0100|**Fecha Versión**|20/09/2026|
|**Aprobado por**||**Fecha Aprobación**|DD/MM/AAAA|
|||**Nº Total de Páginas**|48|



###### **REGISTRO DE CAMBIOS** 

|**Versión**|**Causa del Cambio**|**Responsable del Cambio**|**Fecha del**<br>**Cambio**|
|---|---|---|---|
|||Oscar Tomas Aristizabal||
|01|Versión inicial|Valentina Gónzalez Diaz<br>Valentina Rodríguez Castro|20/09/2026|



###### **AUTORES** 

|**Nombre y Apellidos**|
|---|
|Valentina Gonzalez Diaz (valentina.gonzalezd@uqvirtual.edu.co)|
|Tomas Aristizabal Velasquez (oscart.aristizabalv@uqvirtual.edu.co)|
|Valentina Rodriguez Castro (valentina.rodriguezc1@uqvirtual.edu.co)|



2 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** 

of 

**Página 3 de 48** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

##### **TABLA DE CONTENIDO** 

|**1. INTRODUCCIÓN..............................................................................................................6**|
|---|
|1.1 Alcance..................................................................................................................... 6|
|1.2 Objetivos................................................................................................................... 7|
|1.2.1 Objetivo general............................................................................................... 7|
|1.2.2 Objetivos específicos....................................................................................... 7<br>|
|1.3 Glosario de términos.................................................................................................8<br>|
|**2. INFORMACIÓN DEL CASO DE ESTUDIO....................................................................10**<br>|
|**3. PLAN DE GESTIÓN DE LA CALIDAD...........................................................................10**|
|3.1 Calidad del Software...............................................................................................10|
|3.2 Factores de Calidad del Software...........................................................................11|
|1. Confiabilidad..................................................................................................11|
|3.3 Métricas para la Calidad de un Proceso.................................................................12|
|3.3.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de|
|Tallas y Prendas......................................................................................................12|
|3.3.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario................ 13|
|3.3.3 Proceso 3: Gestión Administrativa y Procesos Internos................................ 14|
|3.4 Creación de Métricas para la Calidad.....................................................................15|
|3.4.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de<br>Tallas y Prendas......................................................................................................16|
|3.4.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario................ 18|
|3.4.3 Proceso 3: Gestión Administrativa y Procesos Internos................................ 21|
|3.5 Modelos y Estándares Aplicables...........................................................................23|
|3.6 Administración de la Calidad.................................................................................. 24|
|3.6.1 Planificación de la Calidad............................................................................. 24|
|3.6.2 Aseguramiento de la Calidad (SQA) y Auditoría de Software........................24|
|3.6.3 Control de Calidad (SQC) y Estrategia de Pruebas.......................................26|
|3.6.4 Gestión de Configuración y Control de Versiones..........................................26|
|3.6.5 Gestión y Ciclo de Vida de Defectos..............................................................26|
|3.6.6 Mejora Continua y Automatización del Ciclo de Vida (CI/CD)....................... 27|
|3.7 Características de la Administración de la Calidad.................................................27|
|3.8 Atributos de Calidad............................................................................................... 28|
|3.8.1 Adecuación Funcional....................................................................................28|
|3.8.2 Eficiencia de Desempeño...............................................................................28|
|3.8.3 Compatibilidad................................................................................................29|
|3.8.4 Capacidad de Interacción (Usabilidad).......................................................... 29|
|3.8.5 Fiabilidad........................................................................................................29|
|3.8.6 Seguridad.......................................................................................................30|
|3.9 Atributos Referenciados en la ISO/IEC 25010........................................................30|
|**4. REQUISITOS NO FUNCIONALES DEL SISTEMA (RNF)............................................. 33**|
|4.1 Característica de Calidad (ISO/IEC 25010): Eficiencia de desempeño...................33|
|4.1.1 RNF: 01 - Respuesta oportuna de la recomendación de tallas -<br>Proceso:Gestión del Perfil infantil y recomendación personalizada de tallas y<br>prendas................................................................................................................... 33<br>i|
|4.1.2 RNF: 02 - Agilidad de los filtros del catálogo - Proceso: Gestión del catálogo,<br>compras, pedidos e inventario................................................................................33|
|4.1.3 RNF: 03 - Sincronización del inventario tras el pago - Proceso: Gestión del<br>catálogo, compras, pedidos e inventario................................................................33|
|4.1.4 RNF: 04 - Tiempo de carga  del panel administrativo - Proceso: Gestión<br>administrativa y procesos internos..........................................................................34|
|415 RNF: 05 - Capacidad de respuesta ante concurrencia de clientes -|
|..<br>Proceso:Gestión del Catálogo, Compras, Pedidos e Inventario............................. 34|



3 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** _Curso: Ingeniería de software III_ 

## of 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 4 de 48** 

|4.2 Característica de Calidad (ISO/IEC 25010): Seguridad...........................................35<br>i|
|---|
|4.2.1 RNF: 06 - Confidencialidad de los datos del menor - Proceso: Gestión del<br>Perfil infantil y recomendación personalizada de tallas y prendas......................... 35|
|4.2.2 RNF: 07 - Autenticidad de notificaciones de pago(WebHooks) - Proceso:<br>Gestión del catálogo, compras, pedidos e inventario............................................ 35|
|4.2.3 RNF: 08 - Trazabilidad de acciones administrativas - Proceso: Gestión<br>administrativa y procesos internos......................................................................... 35|
|4.2.4 RNF: 09 - Resistencia ante vulnerabilidades en dependencias - Proceso:<br>Gestión del catálogo, compras, pedidos e inventario............................................ 36|
|4.2.5 RNF: 10 - Control de accesos por roles  - Proceso: Gestión Administrativa y<br>Procesos Internos................................................................................................... 36|
|4.3 Característica de Calidad (ISO/IEC 25010): Adecuación Funcional........................37|
|4.3.1 RNF: 11 - Precisión en la recomendación de tallas - Proceso: Gestión del<br>perfil infantil y recomendación personalizada de tallas y prendas......................... 37|
|4.3.2 RNF: 12 - Completitud del registro del perfil infantil - Proceso: Gestión del<br>perfil infantil y recomendación personalizada de tallas y prendas......................... 37|
|4.3.3 RNF: 13 - Trazabilidad del ciclo de vida del pedido - Proceso: Gestión del<br>catálogo, compras, pedido e inventario..................................................................38|
|4.3.4 RNF: 14 - Emisión oportuna de alertas de reabastecimiento - Proceso:<br>Gestión del catálogo, compras, pedidos e inventario............................................ 38|
|4.3.5 RNF: 15 - Consistencia de datos en reportes financieros y dashboards -<br>Proceso: Gestión administrativa y procesos internos.............................................38|
|4.4 Característica de Calidad (ISO/IEC 25010): Capacidad de Interacción..................39|
|4.4.1 RNF:  16 - Prevención de errores en el registro de medidas - Proceso:<br>Gestión del perfil infantil y recomendación personalizada de tallas y prendas...... 39|
|4.4.2 RNF: 17 - Asistencia al usuario mediante resolución de incidencias -<br>Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y<br>prendas................................................................................................................... 39|
|4.4.3 RNF: 18 - Operabilidad del flujo de compra - Proceso: Gestión del catálogo,<br>compras, pedidos e inventario................................................................................40|
|4.4.4 RNF: 19 - Facilidad de actualización del perfil infantil - Proceso: Gestión del<br>i|
|perfil infantil y recomendación personalizada de tallas y prendas......................... 40|
|4.4.5 RNF: 20 - Comprensión del panel administrativo - Proceso: Gestión<br>|
|administrativa y procesos internos......................................................................... 41|
|4.5 Característica de Calidad (ISO/IEC 25010): Compatibilidad...................................41|
|4.5.1 RNF: 21 - Interoperabilidad con la pasarela de pagos externa - Proceso:<br>Gestión del catálogo, compras, pedidos e inventario............................................ 41|
|4.5.2 RNF: 22 - Coexistencia y compatibilidad entre plataformas y navegadores<br>web - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas<br>y prendas.................................................................................................................42|
|4.5.3 RNF: 23 - Interoperabilidad para el envío de notificaciones por correo<br>electrónico SMTP - Proceso: Gestión del catálogo, compras, pedidos e inventario.<br>42|
|4.5.4 RNF: 24 - Interoperabilidad en la exportación de reportes analíticos -<br>Proceso: Gestión administrativa y procesos internos.............................................42|
|4.5.5 RNF: 25 Interoperabilidad con el servicio de almacenamiento multimedia en|
|<br>la nube - Proceso: Gestión del catálogo, compras, pedidos e inventario.............. 43|
|4.6 Característica de Calidad (ISO/IEC 25010): Fiabilidad............................................43|
|4.6.1 RNF: 26 - Disponibilidad operativa de la plataforma - Proceso: Gestión del<br>catálogo, compras, pedidos e inventario................................................................43|
|4.6.2 RNF: 27 - Disponibilidad del panel administrativo - Proceso: Gestión<br>administrativa y procesos internos......................................................................... 44|
|4.6.3 RNF: 28 - Tolerancia a fallos de la pasarela de pagos - Proceso: Gestión del|
|<br>catálogo, compras, pedidos e inventario................................................................44|



4 

## of 

###### **LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 5 de 48** 

4.6.4 RNF: 29 - Ausencia de fallos en el motor de recomendación - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas...... 45 4.6.5 RNF: 30 - Recuperabilidad ante caídas del servicio comercial - Proceso: Gestión administrativa y procesos internos............................................................45 **5. EVIDENCIAS MÉTRICAS.............................................................................................. 47** 

5 

**LittleStyle** 

of 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 6 de 48** 

##### **1. INTRODUCCIÓN** 

Obtener ropa infantil requiere atender diferentes necesidades tanto para los padres como para las empresas que se dedican a su comercialización. Seleccionar la ropa correcta requiere aspectos como la variabilidad en el crecimiento infantil, la precisión en las tallas y condiciones especiales de sensibilidad o alergias textiles. A su vez, los vendedores requieren mecanismos que les permitan administrar sus productos, controlar inventarios de prendas y procesar sus ventas. 

Teniendo en cuenta lo anterior, este proyecto se encarga de atender estas necesidades, siendo una plataforma digital de gestión y comercialización de ropa infantil (e-commerce). La plataforma permite administrar perfiles infantiles con información de medidas y alergias, facilitar la selección de ropa mediante un motor de recomendaciones personalizadas de tallas, consultar un catálogo de productos, gestionar carrito de compras y pedidos. Además de permitir a los vendedores la capacidad de administrar sus productos e inventarios. 

Adicionalmente, la plataforma busca proporcionar herramientas de generación de información, para facilitar el análisis de demanda y rendimiento de los productos, por medio de métricas y reportes. 

##### **1.1 Alcance** 

El proyecto comprende el análisis, diseño y desarrollo de una plataforma e-commerce web  para la gestión y comercialización de ropa infantil. 

Entrando más en detalle, la plataforma está dirigida a tres roles de usuarios y un servicio externo: Padre, madre o acudiente (Cliente), Vendedor, Administrador y Pasarela de Pago Externa. 

Para los clientes, se les permitirá gestionar sus cuentas y perfiles de sus hijos, todo lo relacionado con sus datos necesarios para la recomendación de tallas y ropa. Además tendrán acceso al catálogo de ropa infantil con filtros avanzados y visualizar el detalle de los productos, administrar un carrito de compras, realizar pedidos y monitorear su estado. 

Para los vendedores, podrán gestionar sus productos, controlar existencias de stock, recibir alertas de inventario bajo y podrán acceder a un panel de métricas, donde podrán generar reportes de venta específicos y generales de la plataforma, para saber cuales son las palabras clave más usadas y cuáles generan más ventas. 

Para los administradores, se les permitirá gestionar a los usuarios (clientes y vendedores), además de proporcionar un panel de información y reportes relacionados con la demanda de productos. 

Para los vendedores y administradores, se incluye la visualización de dashboards y la generación y descarga/exportación de reportes operacionales en formato PDF para su análisis fuera de línea. 

En el caso de la pasarela de pago externa, se incluye dentro del alcance la integración técnica permitiendo procesar transacciones, validar el resultado de pagos y comunicar a la plataforma si una transacción fue aprobada, rechazada o pendiente. 

Por último, la plataforma asegura las funcionalidades para soportar los procesos anteriores, no contempla dentro del alcance inicial, la gestión interna de procesos de manufactura o confección textil, ni la operación logística/transporte directo de paquetería externa. El sistema logístico dentro del sistema se limitará a la actualización de estados del pedido y el registro del número de guía correspondiente. 

6 

**LittleStyle** 

of 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 7 de 48** 

##### **1.2 Objetivos** 

##### **1.2.1 Objetivo general** 

Desarrollar una plataforma digital e-commerce para la gestión integral y comercialización de ropa infantil que permita administrar perfiles infantiles, facilitar la selección de tallas y prendas, apoyar los procesos de inventario de los vendedores, gestionar carritos y pedidos con integración a pasarela de pagos, proporcionando además herramientas de administración y generación de información para el seguimiento de la operación. 

##### **1.2.2 Objetivos específicos** 

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

7 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 8 de 48** 

##### **1.3 Glosario de términos** 

|**Término**|**Definición**|
|---|---|
|**Administrador**|Usuario encargado de gestionar y supervisar la información<br>general de la plataforma, gestionar las cuentas de usuarios y<br>analizar la métrica global.|
|**Carrito de compra**|Funcionalidad que permite al padre seleccionar y almacenar<br>temporalmente productos antes de generar un pedido.|
|**Catálogo**|Conjunto organizado de prendas infantiles disponibles para<br>consulta y adquisición dentro de la plataforma.|
|**Inventario / Stock**|Registro de las existencias disponibles de prendas terminadas<br>dentro de la plataforma.|
|**Padre / Madre /**<br>**Acudiente**|Usuario<br>de<br>la<br>plataforma<br>encargado<br>de<br>gestionar su<br>información, registrar perfiles infantiles, consultar productos y<br>realizar pedidos de ropa infantil.|
|**Vendedor**|Worker de negocio encargado de administrar el catálogo de<br>productos, actualizar el stock y consultar reportes de ventas.|
|**Pasarela de pago**<br>**externa**|Actor de negocio externo que procesa y valida transacciones<br>electrónicas de los pedidos en entorno de pruebas.|
|**Perfil infantil**|Registro de información asociada a un niño que permite<br>almacenar los datos necesarios para apoyar la recomendación<br>de tallas y prendas.|
|**Pedido**|Solicitud de adquisición de uno o varios productos realizada por<br>un padre a través de la plataforma.|
|**Prenda / Producto**|Producto de ropa infantil ofrecido mediante la plataforma y<br>asociado a características como talla, tipo, descripción y<br>disponibilidad.|
|**Recomendación de**<br>**prendas**|Funcionalidad mediante la cual la plataforma propone productos<br>que pueden ser adecuados para un perfil infantil.|
|**Recomendación de**<br>**talla**|Funcionalidad que permite determinar o sugerir una talla de<br>prenda a partir de la información registrada sobre el niño y las<br>características de la prenda.|
|**Talla**|Clasificación utilizada para representar las dimensiones o<br>tamaño de una prenda infantil y facilitar su selección de acuerdo<br>con las características del niño.|
|**Usuario**|Persona que interactúa con la plataforma mediante alguno de<br>los roles definidos para el sistema.|
|**KPIs**|_Key Performance Indicators_o Indicadores Clave de Desempeño:<br>Son métricas que permiten medir qué tan bien está funcionando<br>la plataforma y cómo se comportan los usuarios dentro de ella.|



8 

0 

**Plan de Gestión de la Calidad del Proyecto** 

**Página 9 de 48** 

###### **LittleStyle** 

_Curso: Ingeniería de software III_ 



|**API**|_Application Programming Interface_o Interfaz de Programación<br>de Aplicaciones es un mecanismo que permite que dos<br>sistemas diferentes se comuniquen e intercambien información<br>de forma estructurada. En este caso la conexión de la pasarela<br>de pagos con la plataforma para el procesamiento de<br>transacciones.|
|---|---|
|**Webhooks**|Método que permite a una aplicación web enviar datos de forma<br>automática a otra aplicación en tiempo real cuando ocurre un<br>evento específico.|
|**Habeas Data**|Derecho que tienen las personas a conocer, actualizar, rectificar<br>y, en los casos establecidos por la ley, solicitar la eliminación de<br>la información personal que se encuentre registrada en bases<br>de datos o archivos.|
|**Ley 1581 de 2012**|Ley colombiana que establece disposiciones generales para la<br>protección de datos personales y regula su recolección,<br>almacenamiento, uso y tratamiento.|
|**Decreto 1074 de**<br>**2015**|Decreto Único Reglamentario del Sector Comercio, Industria y<br>Turismo<br>que incluye disposiciones relacionadas con la<br>reglamentación del tratamiento de datos personales en<br>Colombia.|
|**Ley 1480 de 2011-**<br>**Estatuto del**<br>**consumidor**|Norma colombiana que establece medidas para la protección<br>de los derechos de los consumidores y regula aspectos<br>relacionados con la información de productos, comercio<br>electrónico, garantías y relaciones de consumo.|
|**Resolución Andina**<br>**2109 de 2019**|Reglamento Técnico Andino relacionado con el etiquetado de<br>confecciones, que establece requisitos sobre la información que<br>debe suministrarse al consumidor respecto a los productos<br>textiles comercializados.|
|**Tratamiento de**<br>**datos personales**|Operación o conjunto de operaciones realizadas sobre datos<br>personales,<br>como<br>la<br>recolección,<br>almacenamiento, uso,<br>actualización o eliminación de información.|
|**Stock minimo**|Cantidad mínima de existencias establecida para un producto, a<br>partir de la cual el sistema puede generar una alerta de<br>reabastecimiento.|
|**Dashboard**|Panel visual que presenta indicadores y métricas relevantes<br>mediante gráficos, tablas u otros elementos para facilitar el<br>seguimiento y la toma de decisiones.|



Reng? : 9 aa 2Wij is 

**LittleStyle** 

of 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 10 de 48** 

##### **2. INFORMACIÓN DEL CASO DE ESTUDIO** 

El caso de estudio se desarrolla en el contexto de la gestión y comercialización de ropa infantil, donde intervienen diferentes padres, madres o acudientes (clientes), vendedores de ropa y administradores del servicio. 

En este contexto, los clientes requieren consultar diferentes opciones de ropa y seleccionar prendas adecuadas para las características y necesidades de sus hijos. La elección de una talla puede representar una dificultad debido a las diferencias en las características físicas de cada niño y a la variedad de tallas utilizadas en las prendas infantiles. 

Por otra parte, los vendedores requieren canales digitales centralizados en la comercialización de sus productos, el control de inventario y la recopilación de datos sobre tendencias de compras. 

Asimismo se requiere integrar mecanismos para el procesamiento seguro de transacciones en línea y contar con un panel centralizado que facilite la administración de la información generada por los diferentes participantes y contar con mecanismos que permitan realizar seguimiento a la operación y obtener información para su análisis. 

A partir de este contexto se plantea el caso de estudio para el desarrollo de una solución informática que permita abordar las necesidades identificadas en este dominio. 

##### **3. PLAN DE GESTIÓN DE LA CALIDAD** 

A continuación, diligencie cada sección de este plan teniendo en cuenta el caso de estudio y los tres procesos de negocio definidos. 

##### **3.1 Calidad del Software** 

En el contexto de LittleStyle, la calidad del software se refiere a la capacidad de la plataforma para responder de forma confiable, segura y eficiente a las necesidades de sus tres tipos de usuarios: Cliente, Vendedor y Administrador, teniendo en cuenta los tres procesos de negocio definidos. 

Para el cliente, la calidad se relaciona principalmente con que la plataforma sea fácil de utilizar al momento de registrar la información de los perfiles infantiles y que las recomendaciones de tallas sean correctas, ya que esta es una de las principales características que diferencia a LittleStyle. También es importante que el proceso de compra y pago sea sencillo y seguro. 

Para el vendedor, la calidad implica que pueda administrar el catálogo y el inventario de manera rápida y sin errores. Además, debe recibir oportunamente las alertas relacionadas con el bajo stock y contar con reportes de ventas que muestren información correcta y actualizada para facilitar la toma de decisiones. 

En el caso del administrador, la calidad consiste en contar con herramientas que le permitan supervisar y gestionar las cuentas, publicaciones y diferentes operaciones de la plataforma desde un mismo lugar. También debe disponer de información organizada y confiable mediante los dashboards y reportes.Asimismo, las acciones críticas que ejecuta sobre la plataforma deben quedar registradas para garantizar su trazabilidad. 

10 

**LittleStyle** 

of 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 11 de 48** 

De manera general, debido a que LittleStyle maneja información relacionada con menores de edad y procesos de pago mediante una pasarela externa, la plataforma debe contar con buenas medidas de seguridad, protección de datos y mecanismos de autenticación. También es importante garantizar una buena disponibilidad, ya que las interrupciones del servicio pueden afectar tanto a los clientes como a los vendedores. 

En conclusión, la calidad de LittleStyle se basa principalmente en que el sistema sea usable, preciso, seguro, disponible, permitiendo que los tres procesos de negocio se realicen correctamente y que cada usuario pueda cumplir sus funciones de manera confiable. 

##### **3.2 Factores de Calidad del Software** 

Para la plataforma Little Style, los factores de calidad del software establecen los atributos esenciales que garantizan que el sistema cumpla con las necesidades operativas de los clientes (padres, madres o acudientes), vendedores y administradores, asegurando un funcionamiento continuo, confiable y ajustado al marco legal. A continuación, se definen los factores clave de calidad y su relación con los procesos principales del proyecto: 

###### **1. Confiabilidad** 

Capacidad del software para mantener su nivel de prestación bajo condiciones normales y de carga. 

###### **Relación con los Procesos Clave:** 

- Recomendación de Tallas y Prendas: El algoritmo debe proporcionar recomendaciones precisas e invariables según los datos de medidas y sensibilidad textil del perfil infantil. 

- Gestión de Compras y Pedidos: Se debe garantizar la alta disponibilidad de la plataforma para procesar compras y validar pagos con la pasarela externa sin interrupciones. 

- Control de Inventario: El inventario de productos debe actualizarse de forma precisa e inmediata tras cada transacción para evitar inconsistencias en el stock disponible. 

###### **2. Seguridad y Protección de Datos** 

Protección de la información contra accesos no autorizados, garantizando confidencialidad, integridad y autenticidad. 

###### **Relación con los Procesos Clave:** 

- Gestión de Perfiles Infantiles: Al gestionar datos personales de menores de edad, el sistema debe cumplir estrictamente con la normativa colombiana de Habeas Data (Ley 1581 de 2012 y Decreto 1074 de 2015), aplicando técnicas de cifrado en almacenamiento y transmisión. 

- Integración de Pagos: Garantizar conexiones cifradas y autenticación mediante tokens o Webhooks con la pasarela de pago externa para proteger las transacciones financieras. 

- Control de Acceso: Control de acceso basado en roles (Cliente, Vendedor, Administrador) para proteger las funcionalidades y datos administrativos. 

###### **3. Usabilidad y Capacidad de Interacción** 

Grado de facilidad con la que los usuarios interactúan con la interfaz para lograr sus 

11 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** _Curso: Ingeniería de software III_ 

o 

**Página 12 de 48** 



<!-- Start of picture text -->
y<br><!-- End of picture text -->

objetivos de forma eficiente e intuitiva. 

###### **Relación con los Procesos Clave:** 

- Experiencia del Cliente: Diseño limpio, adaptable (responsive) y fácil de navegar que permita a los padres registrar medidas, filtrar el catálogo y realizar pedidos sin fricciones. 

- Panel de Vendedores y Administradores: Dashboards con visualizaciones de métricas y reportes estructurados de forma clara para agilizar la gestión de stock y la toma de decisiones. 

###### **4. Eficiencia de Desempeño** 

Desempeño del sistema en función del tiempo de respuesta y la gestión adecuada de recursos. 

###### **Relación con los Procesos Clave:** 

- Navegación en el Catálogo: Tiempos de respuesta rápida (menores a 2 segundos) al consultar productos, aplicar filtros avanzados y cargar el detalle de las prendas. 

- Generación de Reportes: Respuesta eficiente al procesar volúmenes de datos de ventas, palabras clave más buscadas e indicadores del dashboard. 

###### **5. Adecuación y Correctitud Funcional** 

Capacidad del software para proveer un conjunto de funciones que satisfagan las necesidades explícitas e implícitas de los usuarios. 

###### **Relación con los Procesos Clave:** 

- Gestión de Inventario: Emisión oportuna de alertas automáticas cuando un producto alcance el _stock mínimo_ . 

- Seguimiento de Pedidos: Actualización correcta de los estados del pedido (creación, pago, despacho, entrega) y registro del número de guía logístico. 

##### **3.3 Métricas para la Calidad de un Proceso** 

Las métricas de calidad de un proceso evalúan estratégicamente la salud técnica, operativa y de negocio de la plataforma. estás métricas establecen indicadores clave de rendimiento transversales al sistema informático. 

A continuación, se formalizan las métricas globales clasificadas de acuerdo a los atributos estandarizados de la calidad de software 

###### **3.3.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de Tallas y Prendas** 

Este proceso comprende el registro de información biométrica de menores, sensibilidad textil y la ejecución del motor de reglas para sugerencia de prendas. Las métricas de proceso evalúan el desempeño computacional de las consultas, la integridad de los datos y la tolerancia a errores de captura. 

|**Nombre de la**<br>**Métrica**|**Descripción**|**Fórmula /**<br>**Unidad**|**Fuente de**<br>**Datos**|**Frecuencia**<br>**de**<br>**Medición**|
|---|---|---|---|---|



12 ~~<mark>ee</mark>~~ \ (||j vgwww uniquindi.edl.oo) 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

**Página 13 de 48** 





|Tiempo Medio<br>de Respuesta<br>del Motor (API<br>Latency)|Mide el tiempo<br>promedio de<br>respuesta del<br>servicio backend<br>al cruzar<br>medidas del<br>menor con la<br>tabla de tallas|Tiempo de<br>Σ<br>Respuesta  /<br>Número de<br>Consultas|Logs del<br>servidor /<br>Herramienta de<br>monitoreo APM|Por sprint y<br>Medición<br>Continua|
|---|---|---|---|---|
|Densidad de<br>Defectos del<br>Módulo de<br>Perfiles|Mide la cantidad<br>de errores o<br>defectos<br>detectados en el<br>módulo por miles<br>de líneas de<br>código|Total Defectos<br>Detectados /<br>Miles de Líneas<br>de Código|Análisis<br>Estático de<br>Código|Al finalizar<br>cada Sprint|
|Cumplimiento<br>de Cifrado en<br>Datos<br>Sensibles<br>(Habeas Data)|Evalúa el<br>porcentaje de<br>campos con<br>información<br>personal de<br>menores<br>almacenados<br>con cifrado en la<br>base de datos|(Campos<br>Sensibles<br>Cifrados / Total<br>Campos<br>Sensibles<br>Registrados) x<br>100|Auditoría de<br>esquema de<br>base de datos /<br>SAST|Antes del<br>Despliegue<br>/ Quincenal|
|Tiempo medio<br>entre fallos del<br>motor de reglas|Mide el tiempo<br>promedio<br>continuo que el<br>algoritmo de<br>recomendación<br>opera sin lanzar<br>excepciones no<br>controladas|Tiempo Total<br>Operativo /<br>Número Fallos<br>Presentados|Logs de<br>Excepciones<br>del backend|Mensual|
|Soporte de<br>Plataformas<br>Compatiblles|Mide la<br>proporción de<br>entornos en los<br>que la interfaz<br>del perfil se<br>renderiza sin<br>errores visuales o<br>funcionales.|(Plataformas con<br>prueba exitosa /<br>Total plataformas<br>objetivo) x 100|Pruebas de<br>Rendering<br>Automatizadas|Por<br>Despliegue|



###### **3.3.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario** 

Proceso transaccional y operativo más grande de la plataforma que involucra la consulta masiva de catálogo, actualización concurrente de inventario e integración con la Pasarela de Pagos Externa (Wompi) y correo electrónico. Las métricas evalúan la disponibilidad operativa, el consumo de infraestructura y la robustez transaccional. 

|**Nombre de la**<br>**Métrica**|**Descripción**|**Fórmula /**<br>**Unidad**|**Fuente de**<br>**Datos**|**Frecuencia de**<br>**Medición**|
|---|---|---|---|---|
|Disponibilidad<br>Operativa del<br>Servicio|Mide el<br>porcentaje de<br>tiempo que el<br>flujo|((Tiempo<br>Operativo Total<br>- Tiempo<br>Inactivo) /|AWS<br>CloudWatch /<br>Health Checks|Diario|



13 

0 

**Plan de Gestión de la Calidad del Proyecto** 

**Página 14 de 48** 

###### **LittleStyle** 

_Curso: Ingeniería de software III_ 



||transaccional<br>de compras y<br>catálogo se<br>mantiene en<br>línea y<br>receptivo.|Tiempo<br>Operativo Total)<br>x 100|||
|---|---|---|---|---|
|Consumo<br>promedio de<br>CPU y<br>memoria RAM|Mide el uso de<br>hardware del<br>servidor<br>backend<br>durante picos<br>de consulta de<br>catálogo y<br>ventas.|Promedio de<br>carga de CPU<br>y RAM<br>utilizaste en<br>EC2 /<br>Contenedor|Monitoreo de<br>infraestructura<br>AWS|Continuo /<br>Pruebas de<br>Carga|
|Cobertura de<br>Pruebas<br>Automatizadas|<br>Mide la<br>proporción del<br>código fuente<br>del módulo<br>ejecutado<br>durante las<br>pruebas<br>unitarias e<br>integración.|(Líneas de<br>Código<br>Evaluadas /<br>Líneas de<br>Código Totales)<br>x 100|Jenkins /<br>Github Actions|Por cada<br>Commit / Build|
|Tasa de<br>Excepciones<br>en Integración<br>con Pasarela<br>(Webhooks)|Mide la<br>proporción de<br>fallos de<br>comunicación<br>o errores<br>presentados al<br>recibir eventos<br>de<br>confirmación<br>de pago de la<br>pasarela.|(Peticiones<br>Fallidas de<br>Pasarela / Total<br>Transacciones<br>Notificadas) x<br>100|Logs de<br>Webhooks /<br>Auditoría<br>Wompi|Diaria|
|Tiempo Medio<br>de Reparación<br>Ante Caídas|Mide el tiempo<br>promedio<br>requerido para<br>solucionar y<br>restablecer la<br>plataforma tras<br>una<br>interrupción en<br>el módulo<br>comercial.|Tiempo Total<br>de Inactividad /<br>Número de<br>Incidentes de<br>Caída|Reportes de<br>Gestión de<br>Incidentes|Por Evento /<br>Mensual|
|Cobertura de<br>Pruebas de<br>Integración y<br>E2E<br>(End-to-End)|Mide la<br>proporción de<br>flujos<br>completos de<br>compra<br>cubiertos por<br>pruebas<br>automatizadas.|(Flujos E2E<br>Automatizados<br>Exitosos / Total<br>de Flujos<br>Críticos de<br>Compra) x 100|Postman /<br>Playwright /<br>Selenium|Antes de cada<br>Release|



**3.3.3 Proceso 3: Gestión Administrativa y Procesos Internos** 

14 ( a»a LizRow”Wilh LD~ S 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 

**Página 15 de 48** 



_Curso: Ingeniería de software III_ 



Este proceso abarca la administración de usuarios, moderación de contenidos y la generación de reportes gerenciales y dashboards de rendimiento. Sus métricas miden el desempeño en la generación de analítica masiva, mantenibilidad del código y la seguridad en el control de acceso de los administradores. 

|**Nombre de la**<br>**Métrica**|**Descripción**|**Fórmula /**<br>**Unidad**|**Fuente de**<br>**Datos**|**Frecuencia de**<br>**Medición**|
|---|---|---|---|---|
|Tiempo de<br>Carga de<br>Consultas del<br>Dashboard|Mide el tiempo<br>requerido por<br>el backend<br>para consolidar<br>ventas,<br>búsquedas e<br>inventario y<br>retornarlo al<br>frontend|Milisegundos<br>desde la<br>petición HTTP<br>hasta la<br>respuesta<br>JSON|Profiling SQL /<br>Network Tab<br>Browser|Semanal|
|Tasa de<br>Rechazo de<br>Accesos No<br>Autorizados|Mide el<br>porcentaje de<br>peticiones a<br>endpoints<br>administrativos<br>vulnerables o<br>sin rol que son<br>bloqueados<br>exitosamente<br>por Spring<br>Security.|(Peticiones<br>Denegadas /<br>Total Intentos<br>Sin Token<br>Válido) x 100|Logs de<br>Seguridad<br>(Spring<br>Security)|Semanal /<br>Auditoría|
|Índice de<br>Mantenibilidad<br>del Código|Mide qué tan<br>fácil es<br>mantener,<br>modificar y<br>entender el<br>código del<br>módulo<br>administrativo.<br>Asigna un valor<br>de 0 a 100<br>basado en<br>líneas de<br>código,<br>complejidad<br>ciclomática y<br>volumen<br>Halstead.|Formula<br>combinada<br>Halstead<br>´Cyclomatic +<br>LOC|SonarQube /<br>Visual Studio<br>Metrics|Por Sprint|
|Tiempo de<br>Reparación de<br>Dependencias<br>Vulnerables|Mide el tiempo<br>en días que le<br>toma al equipo<br>actualizar una<br>librería/depend<br>encia<br>desactualizada<br>con fallo de<br>seguridad|Días desde la<br>alerta del<br>scanner hasta<br>el merge del<br>PR|Snyk / Github<br>Dependabot|Semanal|



##### **3.4 Creación de Métricas para la Calidad** 

15 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 16 de 48** 

###### **3.4.1 Proceso 1: Gestión del Perfil Infantil y Recomendación Personalizada de Tallas y Prendas** 

Este conjunto de métricas evalúa la precisión, usabilidad y eficiencia del módulo de gestión de perfiles infantiles y del motor de recomendación de tallas. Su objetivo principal es asegurar que la recolección de datos antropométricos y preferencias textiles sea fluida, minimizando los errores de entrada y garantizando que las sugerencias de prendas generen alta confianza y aceptación en los acudientes. 

|**Nombre de la**<br>**Métrica**|**Proceso de**<br>**Negocio**<br>**Asociado**|**Descripción**<br>**del Indicador**|**Fórmula /**<br>**Criterio de**<br>**Medición**|**Interpretaci**<br>**ón**<br>**Esperada**|
|---|---|---|---|---|
|Tasa de<br>adopción de<br>recomendación<br>de tallas|Recomendac<br>ión<br>Personalizad<br>a|Mide el<br>porcentaje de<br>compras en<br>las que el<br>cliente confía<br>y selecciona<br>la talla<br>sugerida por<br>el motor.|(Compras con<br>talla<br>recomendada<br>/ Total de<br>compras con<br>recomendació<br>n) x 100|> 80%.<br>Indica que el<br>algoritmo es<br>preciso y<br>genera<br>confianza en<br>el cliente.|
|Completitud<br>del perfil infantil|Gestión del<br>Perfil Infantil|Evalúa<br>cuántos<br>perfiles<br>creados<br>tienen todos<br>los campos<br>opcionales y<br>obligatorios<br>(medidas,<br>alergias)<br>diligenciados.|(Perfiles<br>completos /<br>Total de<br>perfiles<br>registrados) x<br>100|> 90%.<br>Demuestra<br>que la<br>interfaz es<br>intuitiva y el<br>usuario ve<br>valor en dar<br>los datos.|
|Tiempo de<br>carga de<br>recomendacion<br>es|Recomendac<br>ión<br>Personalizad<br>a|Mide el<br>tiempo que<br>tarda el<br>sistema en<br>procesar los<br>datos del<br>perfil y<br>mostrar las<br>prendas<br>sugeridas.|Sumatoria del<br>tiempo de<br>respuesta /<br>Número total<br>de peticiones<br>al motor|< 2<br>segundos.<br>Garantiza<br>una<br>eficiencia de<br>desempeño<br>fluida<br>(ISO/IEC<br>25010).|
|Tasa de error<br>en captura de<br>medidas<br>biométricas|Gestión del<br>Perfil Infantil|Porcentaje de<br>intentos<br>fallidos o<br>datos<br>inconsistentes<br>rechazados al<br>guardar un<br>perfil.|(Errores de<br>validación /<br>Total de<br>intentos de<br>guardado) x<br>100|< 5%. Indica<br>una buena<br>usabilidad y<br>protección<br>contra<br>errores de<br>usuario.|



16 

### o 

###### **LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



**Página 17 de 48** 

|Frecuencia de<br>actualización<br>de perfiles|Gestión del<br>Perfil Infantil|Mide la<br>proporción de<br>clientes que<br>actualizan las<br>medidas de<br>los niños al<br>menos cada 6<br>meses (por<br>crecimiento).|(Perfiles<br>actualizados<br>en < 6 meses<br>/ Total de<br>perfiles) x 100|> 60%.<br>Refleja un<br>alto nivel de<br>engagement<br>y<br>mantenimien<br>to de datos<br>precisos.|
|---|---|---|---|---|
|Efectividad en<br>la protección<br>de datos<br>sensibles|Gestión del<br>Perfil Infantil|Mide la<br>proporción de<br>campos con<br>información<br>confidencial<br>de menores<br>almacenados<br>con cifrado en<br>reposo y<br>protegidos<br>contra<br>accesos no<br>autorizados.|(Campos<br>sensibles<br>cifrados /<br>Total campos<br>sensibles) x<br>100|> 96% de<br>campos<br>cifrados.|
|Tasa de<br>consultas de<br>recomendación<br>completadas<br>sin fallos|Recomendac<br>ión<br>Personalizad<br>a|Mide el<br>porcentaje de<br>consultas<br>realizadas al<br>motor de<br>recomendació<br>n que finalizan<br>correctamente<br>sin generar<br>excepciones<br>no<br>controladas<br>que<br>interrumpan la<br>obtención de<br>tallas o<br>prendas<br>recomendada<br>s.|(Consultas<br>completadas<br>sin fallos /<br>Total de<br>consultas<br>procesadas<br>por el motor)<br>× 100|≥ 99 %.<br>Indica que el<br>motor de<br>recomendaci<br>ón mantiene<br>un<br>comportamie<br>nto estable y<br>consistente<br>durante su<br>operación.|



###### **3.4.2 Proceso 2: Gestión del Catálogo, Compras, Pedidos e Inventario** 

Este bloque de métricas se enfoca en medir la eficiencia operativa, fiabilidad transaccional e integridad del ciclo de comercio electrónico. Evalúa la velocidad de navegación en el catálogo con filtros avanzados, la efectividad del checkout, la disponibilidad de la integración técnica con la pasarela de pagos externa y la sincronización en tiempo real del stock de los vendedores. 

|**Nombre de la**<br>**Métrica**|**Proceso**<br>**de**<br>**Negocio**<br>**Asociado**|**Descripción**<br>**del**<br>**Indicador**|**Fórmula /**<br>**Criterio de**<br>**Medición**|**Interpretació**<br>**n Esperada**|
|---|---|---|---|---|



17 

**Plan de Gestión de la Calidad del Proyecto** 



###### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 18 de 48** 

|Tasa de<br>abandono del<br>carrito|Compras<br>y Pedidos|Porcentaje<br>de carritos<br>de compras<br>creados que<br>no finalizan<br>en una<br>transacción<br>pagada.|(Carritos<br>abandonados<br>/ Total de<br>carritos<br>creados) x 100|< 30%. Un<br>porcentaje<br>bajo indica<br>que el flujo de<br>pago es<br>eficiente y sin<br>fricciones.|
|---|---|---|---|---|
|Tiempo de<br>sincronización de<br>stock|Gestión<br>de<br>Inventario|Mide los<br>milisegundos<br>que<br>transcurren<br>desde la<br>aprobación<br>del pago<br>hasta el<br>descuento<br>del inventario<br>en base de<br>datos.|Tiempo medio<br>(en ms) de la<br>transacción de<br>actualización<br>de stock|< 500 ms.<br>Garantiza la<br>concurrencia<br>y evita<br>sobreventas<br>de prendas<br>sin stock.|
|Tasa de<br>trazabilidad y<br>actualización de<br>estados|Compras<br>y Pedidos|Mide el<br>porcentaje<br>de pedidos<br>que registran<br>correctament<br>e la<br>transición de<br>sus estados<br>(creación,<br>pago,<br>despacho,<br>entrega) e<br>incluyen el<br>número de<br>guía logística<br>sin<br>inconsistenci<br>as.|(Pedidos con<br>historial de<br>estados y guía<br>registrados<br>correctamente<br>/ Total de<br>pedidos<br>pagados) x<br>100|> 98%.<br>Garantiza el<br>correcto<br>funcionamient<br>o de las reglas<br>de negocio<br>del módulo de<br>pedidos y el<br>seguimiento<br>logístico|
|Efectividad de<br>filtros en catálogo|Gestión<br>del<br>Catálogo|Mide qué tan<br>rápido se<br>aplican los<br>filtros<br>avanzados<br>(talla, edad,<br>tipo de<br>prenda) para<br>mostrar<br>resultados.|Tiempo total<br>de consultas<br>con filtro /<br>Número de<br>consultas|< 1.5<br>segundos. Un<br>desempeño<br>óptimo<br>mantiene la<br>atención del<br>comprador.|



18 

**Plan de Gestión de la Calidad del Proyecto** 

#### oO 

###### **LittleStyle** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
h7.,<br><!-- End of picture text -->

**Página 19 de 48** 

|Precisión de<br>alertas de<br>reabastecimiento|Gestión<br>de<br>Inventario|Porcentaje<br>de alertas de<br>stock mínimo<br>que se<br>disparan<br>correctament<br>e antes de<br>que el<br>inventario<br>llegue a cero.|(Alertas<br>generadas<br>oportunament<br>e / Total de<br>productos<br>agotados) x<br>100|> 99%. Es<br>vital para que<br>el vendedor<br>mantenga la<br>continuidad<br>de su<br>negocio.|
|---|---|---|---|---|
|Tasa de entrega<br>exitosa de<br>notificaciones<br>transaccionales|Gestión<br>del<br>catálogo,<br>compras,<br>pedidos e<br>inventario|Porcentaje<br>de<br>notificacione<br>s<br>transaccional<br>es<br>generados<br>por la<br>plataforma<br>que son<br>entregadas<br>correctament<br>e.|(Número de<br>notificaciones<br>entregadas<br>correctamente<br>/ Número total<br>de<br>notificaciones<br>enviadas) x<br>100|> 98. Permite<br>entregar<br>correctamente<br>las<br>notificaciones<br>asociadas al<br>proceso.|
|Tasa de<br>Disponibilidad de<br>recursos<br>multimedia|Gestión<br>del<br>catálogo,<br>compras,<br>pedidos e<br>inventario|Porcentaje<br>de recursos<br>multimedia<br>asociados a<br>los<br>productos<br>del catálogo<br>que pueden<br>ser<br>consultados<br>y<br>visualizados<br>correctament<br>e mediante el<br>servicio<br>externo de<br>almacenamie<br>nto<br>multimedia.|(Número de<br>recursos<br>multimedia<br>accesibles<br>correctamente<br>/ Número total<br>de recursos<br>multimedia<br>solicitados) x<br>100|> 99 %.<br>Recursos<br>disponibles<br>que pueden<br>ser utilizados<br>correctamente<br>por la<br>plataforma<br>mediante las<br>solicitudes<br>realizadas.|
|Tasa de<br>procesamiento de<br>carga simultanea|Gestión<br>del<br>Catálogo,<br>Compras,<br>Pedidos e<br>Inventario|Mide el<br>porcentaje<br>de peticiones<br>HTTP en el<br>catálogo que<br>se<br>completan<br>sin errores<br>cuando<br>múltiples<br>usuarios<br>interactúan<br>en<br>simultáneo.|(Peticiones<br>exitosas sin<br>error/Total de<br>peticiones<br>bajo carga) x<br>100|≥99% de éxito<br>con picos de<br>hasta 100<br>usuarios en<br>simultáneo.|



19 SN=a LeaLongaiiliC gaVi [ 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



###### **LittleStyle** 



**Página 20 de 48** 

|Tasa de<br>autenticación de<br>notificaciones de<br>pago|Compras<br>y Pedidos|Evalúa la<br>efectividad<br>del sistema<br>para verificar<br>la firma<br>HMAC  de<br>los<br>Webhooks<br>de la<br>pasarela.|(Notificaciones<br>con firma<br>válida / Total<br>notificaciones)<br>x 100|≥ 99.8% de<br>firmas válidas.|
|---|---|---|---|---|
|Efectividad en el<br>mantenimiento<br>seguro de<br>dependencias|Gestión<br>del<br>Catálogo,<br>Compras,<br>Pedidos e<br>Inventario|Mide el<br>porcentaje<br>de<br>dependencia<br>s y librerías<br>del producto<br>libres de<br>vulnerabilida<br>des<br>conocidas.|(Librerías sin<br>vulnerabilidad<br>es / Total<br>dependencias)<br>x  100|≥ 98% libres<br>de<br>vulnerabilidad<br>es.|
|Tasa de<br>preservación<br>transaccional<br>ante fallos<br>externos|Gestión<br>del<br>Catálogo,<br>Compras,<br>Pedidos e<br>Inventario|Mide el<br>porcentaje<br>de<br>operaciones<br>afectadas<br>por fallos<br>temporales<br>en la<br>comunicació<br>n con<br>servicios<br>externos,<br>especialment<br>e la pasarela<br>de pagos, en<br>las que el<br>sistema<br>conserva<br>correctament<br>e la<br>información<br>del pedido y<br>mantiene la<br>transacción<br>en un estado<br>válido, sin<br>pérdida ni<br>corrupción<br>de datos.|(Operaciones<br>preservadas<br>correctamente<br>/ Total de<br>operaciones<br>afectadas por<br>fallos<br>externos) ×<br>100|**≥ 99 %**. Indica<br>que el sistema<br>mantiene la<br>integridad y<br>continuidad<br>de las<br>transaccion|



###### **3.4.3 Proceso 3: Gestión Administrativa y Procesos Internos** 

Estas métricas evalúan la estabilidad, rendimiento y precisión de los módulos internos y herramientas administrativas. Garantizan que los administradores y vendedores dispongan de un dashboard continuo (alta disponibilidad), reportes financieros e indicadores de demanda consistentes y libres de errores, así como tiempos de respuesta idóneos para resolver incidencias operativas y solicitudes de soporte. 

20 

**LittleStyle** 



**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



**Página 21 de 48** 

|**Nombre de la**<br>**Métrica**|**Proceso de**<br>**Negocio**<br>**Asociado**|**Descripció**<br>**n del**<br>**Indicador**|**Fórmula /**<br>**Criterio de**<br>**Medición**|**Interpretaci**<br>**ón Esperada**|
|---|---|---|---|---|
|Disponibilidad<br>del panel<br>administrativo<br>(Uptime)|Gestión<br>Administrativa|Porcentaje<br>de tiempo<br>que el<br>dashboard<br>y los<br>reportes<br>están<br>accesibles<br>sin caídas<br>de servicio.|(Tiempo de<br>servicio activo<br>/ Tiempo total<br>planificado) x<br>100|> 99.9%.<br>Asegura que<br>los<br>administrado<br>res tengan<br>control<br>continuo de<br>la<br>plataforma.|
|Trazabilidad de<br>acciones<br>administrativas<br>(Auditoría)|Gestión<br>Administrativa|Mide la<br>capacidad<br>del sistema<br>para<br>registrar de<br>forma<br>automática<br>e inalterable<br>todas las<br>operaciones<br>críticas<br>realizadas<br>por los<br>administrad<br>ores<br>(modificació<br>n de roles,<br>bloqueos<br>de usuario<br>o<br>eliminación<br>de<br>contenidos).|(Eventos<br>administrativos<br>registrados en<br>el log de<br>auditoría /<br>Total de<br>operaciones<br>administrativas<br>realizadas) x<br>100|> 99.5%.<br>Garantiza la<br>responsabilid<br>ad,<br>autenticidad<br>y no repudio<br>(ISO/IEC<br>25010) sobre<br>las acciones<br>de gestión en<br>la<br>plataforma.|
|Tasa de<br>resolución de<br>incidencias de<br>usuarios|Gestión<br>Administrativa|Porcentaje<br>de<br>bloqueos,<br>moderación<br>o dudas de<br>vendedores<br>y clientes<br>que el<br>administrad<br>or resuelve<br>a tiempo.|(Incidencias<br>resueltas en <<br>24h / Total de<br>incidencias<br>reportadas) x<br>100|> 90%.<br>Garantiza un<br>buen soporte<br>y<br>mantenimient<br>o operativo<br>de las<br>cuentas.|



21 

**Plan de Gestión de la Calidad del Proyecto** 



###### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 22 de 48** 

|Consistencia<br>de datos en<br>reportes<br>financieros|Procesos<br>Internos|Nivel de<br>coincidenci<br>a exacta<br>entre las<br>ventas<br>reales<br>registradas<br>y las ventas<br>mostradas<br>en el<br>dashboard.|(Transacciones<br>coincidentes<br>entre BD y<br>Reporte / Total<br>de<br>transacciones<br>en BD) x 100|> 99.9%.<br>Coincidencia<br>entre<br>transaccione<br>s de la BD y<br>métricas del<br>dashboard.|
|---|---|---|---|---|
|Tasa de éxito<br>en la<br>exportación de<br>reportes|Procesos<br>Internos|Mide la<br>confiabilida<br>d del<br>sistema<br>cuando un<br>administrad<br>or o<br>vendedor<br>solicita<br>exportar o<br>descargar<br>un reporte<br>de métricas,<br>evaluando<br>cuántos se<br>generan sin<br>errores<br>(como un<br>"timeout" o<br>"error de<br>servidor").|(Reportes<br>descargados<br>exitosamente /<br>Total de<br>descargas<br>solicitadas) x<br>100|> 98%.<br>Demuestra<br>una alta<br>fiabilidad<br>(ISO 25010)<br>en los<br>procesos<br>internos y<br>que la<br>plataforma<br>soporta el<br>procesamien<br>to de<br>documentos<br>correctament<br>e.|
|Tiempo de<br>respuesta del<br>dashboard<br>administrativo|Gestión<br>administrativa|Mide los<br>segundos<br>requeridos<br>para la<br>renderizació<br>n visual<br>completa<br>de las<br>métricas<br>clave e<br>indicadores<br>del<br>dashboard.|Tiempo<br>transcurrido<br>(en segundos)<br>desde la<br>petición de<br>entrada hasta<br>la visualización<br>total de los<br>gráficos.|<3 segundos<br>en al menos<br>el 90% de<br>los accesos.|
|Tasa de<br>restricción de<br>accesos no<br>autorizados|Gestión<br>administrativa|Evalúa la<br>capacidad<br>del sistema<br>para<br>denegar<br>peticiones a<br>endpoints<br>cuando la<br>solicitud<br>carece de|(Solicitudes<br>bloqueadas /<br>Total intentos<br>sin rol) x  100|≥ 98.5% de<br>accesos no<br>autorizados<br>bloqueados.|



22 =a ec. Sil 

**LittleStyle** 

o 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

£7. 

**Página 23 de 48** 

|||token JWT<br>o rol válido.|||
|---|---|---|---|---|
|Tasa de<br>ejecución<br>correcta de<br>tareas<br>administrativas|Gestión<br>Administrativa|Mide el<br>porcentaje<br>de tareas<br>principales<br>del panel<br>administrati<br>vo que<br>vendedores<br>y<br>administrad<br>ores logran<br>completar<br>correctame<br>nte y sin<br>errores de<br>navegación<br>durante las<br>pruebas de<br>usabilidad.|(Tareas<br>completadas<br>correctamente<br>/ Total de<br>tareas<br>evaluadas) ×<br>100|≥ 90 %.<br>Indica que la<br>interfaz del<br>panel es<br>comprensible<br>y facilita el<br>aprendizaje y<br>la ejecución<br>de las<br>operaciones<br>administrativ<br>as<br>principales.|



##### **3.5 Modelos y Estándares Aplicables** 

Para el aseguramiento de la calidad del proyecto LittleStyle, se seguirán los siguientes modelos y estándares: 

- **ISO/IEC 25010 (System and Software Quality Models):** se utiliza para definir y clasificar los atributos de calidad del producto software (características y subcaracterísticas), sirviendo como base para estructurar los Requisitos No Funcionales (RNF) del sistema. Su aporte principal es garantizar que la calidad del producto se evalúe de forma estandarizada y no de manera subjetiva. 

- **Scrum (marco ágil):** aunque no es una norma de calidad de producto, se aplica como modelo de aseguramiento de calidad del _proceso_ , mediante revisiones incrementales al final de cada sprint, retrospectivas y criterios de aceptación por historia de usuario, permitiendo detectar y corregir desviaciones de calidad de forma temprana durante todo el desarrollo. 

- **OWASP Top 10 (Open Web Application Security Project):** se utiliza como referencia para identificar y mitigar las vulnerabilidades de seguridad más críticas en aplicaciones web, sirviendo de base para las auditorías de seguridad de los endpoints REST. 

En conjunto, estos modelos contribuyen a que la calidad de LittleStyle se controle desde dos frentes complementarios: el producto (atributos definidos por la ISO/IEC 25010) y el proceso de desarrollo (verificación continua mediante Scrum). 

##### **3.6 Administración de la Calidad** 

La administración de la calidad en LittleStyle tiene un enfoque integral y sistemático orientado a garantizar la excelencia técnica, operativa y funcional en todos los componentes del sistema. Su objetivo primario es satisfacer las necesidades y expectativas de los clientes (padres, madres o acudientes), vendedores y administradores, asegurando resultados consistentes, seguros y confiables en cada iteración del desarrollo. 

23 XD,wileeAy 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** 

of 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 24 de 48** 

Para lograrlo, la administración de la calidad en LittleStyle combina dos disciplinas esenciales de manera equilibrada: 

- **Aseguramiento de la Calidad del Software (SQA -** **_Software Quality Assurance_ ):** Es un enfoque preventivo enfocado en los procesos, orientando a establecer estándares, normativas, revisiones continuas y auditorías para evitar la aparición de errores durante el ciclo de vida del software. 

- **Control de la Calidad del Software (SQC -** **_Software Quality Control_ ):** Este enfoque es correctiivo y de verificación, se centra en el producto y se dedica a evaluar la correcta ejecución y funcionalidad del software mediante pruebas automatizadas y manuales por niveles. 

A continuación, se detallan los distintos componentes y mecanismos que componen el modelo de administración de la calidad: 

###### **3.6.1 Planificación de la Calidad** 

Desde el inicio del proyecto, se establece el marco de calidad que rige el desarrollo de la plataforma LittleStyle abarcando lo siguiente: 

- La definición explícita de los atributos y características de calidad bajo la norma ISO/IEC 25010. 

- La formulación de métricas objetivas de proceso y producto asociadas a los tres procesos principales del negocio. 

- El establecimiento de criterios de aceptación técnicos y funcionales para cada Historia de Usuario (US) definidos previo al inicio de cada _sprint_ . 

###### **3.6.2 Aseguramiento de la Calidad (SQA) y Auditoría de Software** 

Con base en los lineamientos metodológicos de auditoría de software, se define como auditoría a un proceso sistemático e independiente de revisión y evaluación de los componentes, código, configuraciones y procedimientos para verificar su conformidad con las normas, estándares de la industria y políticas del proyecto. 

- **_a._** _Auditoría de Código Fuente y Calidad Técnica_ 

- **Revisiones de Código por Pares (** **_Peer Reviews_ ):** Ningún cambio de código se integra directamente a la rama principal de producción sin un _Pull Request_ en GitHub que cuente con la revisión y aprobación obligatoria de al menos otro desarrollador del equipo. 

- **Cumplimiento de Estándares de Codificación:** Se verifica la adhesión a las guías de estilo para Java (Backend en Spring Boot) y TypeScript/HTML/CSS (Frontend en Angular SPA), la correcta implementación de los principios SOLID y patrones de diseño orientados a la mantenibilidad. 

- **Análisis Estático del Código:** Inspección automatizada para la detección de código duplicado, alta complejidad ciclomática, vulnerabilidades potenciales y demás. 

24 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** 

of 

**Página 25 de 48** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

- **_b._** _Auditoría de Arquitectura y Decisiones Técnicas (ADRs)_ 

- Verificación continua para el correcto cumplimiento e implementación de la arquitectura monolítica modular definida en Spring Boot, la separación clara de responsabilidades entre capas y el desacoplamiento con el frontend en Angular SPA. 

- Correcta aplicación de las Decisiones de Arquitectura adoptadas, como el manejo de autenticación sin estado mediante tokens JWT, el almacenamiento externo de imágenes en Amazon S3 y la recepción transaccional asíncrona mediante Webhooks con la Pasarela de Pagos Wompi. 

- **_c._** _Auditoría de Cumplimiento Legal y Protección de Datos (Habeas Data)_ 

- **Protección de Datos de Menores de Edad:** Dado que el primer proceso gestiona información sensible de menores (medidas corporales, edad, condiciones médicas/alergias textiles), se audita el cumplimiento de la Ley 1581 de 2012 y el Decreto 1074 de 2015. 

- **Garantía de Derechos ARCO:** Verificación de que el sistema permita a los titulares consultar, actualizar, rectificar y solicitar la eliminación de sus datos personales y los de sus hijos. 

- **Mecanismos de Cifrado:** Inspección para asegurar que los datos sensibles se encuentren cifrados tanto en tránsito (HTTPS/TLS) como en reposo en la base de datos. 

- **_d._** _Auditoría de Normatividad Comercial y Protección al Consumidor_ 

- Verificación de la Ley 1480 de 2011 (Estatuto del Consumidor) referente a la transparencia en precios, condiciones de compra, derecho de retracto y seguimiento transparente de pedidos en comercio electrónico. 

- Verificación de cumplimiento de la Resolución Andina 2109 de 2019, asegurando que las fichas técnicas del catálogo de prendas incluyan claramente la composición de materiales, tabla de medidas e instrucciones de cuidado textil. 

- **_e._** _Auditoría de Licencias y Seguridad_ 

- **Licenciamiento de terceros:** Evaluación de las librerías y dependencias utilizadas en el backend y frontend para que se empleen licencias de código abierto compatibles y evitar riesgos legales por uso no autorizado de software. 

- **Seguridad y vulnerabilidades:** Auditorías periódicas de seguridad sobre los endpoints REST para evitar fallos de inyección SQL, vulnerabilidades XSS, autenticación rota y validación de firma criptográfica HMAC en los Webhooks de pagos. 

###### **3.6.3 Control de Calidad (SQC) y Estrategia de Pruebas** 

25 

**LittleStyle** 

of 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 26 de 48** 

El control de Calidad ejecuta una estrategia de pruebas por niveles orientada a validar el correcto funcionamiento del producto software: 

- **Pruebas Unitarias:** Automatizadas en el Backend utilizando JUnit y Mockito, y en el Frontend utilizando Jasmine y Karma. Se exige una cobertura de código igual o superior al 80% en los módulos críticos (motor de recomendación de tallas y alergias, cálculo de carritos de compra y control de stock de inventario). 

- **Pruebas de Integración:** Verificación de una correcta comunicación entre el backend en Spring Boot y la base de datos MySQL (vía Spring Data JPA), la pasarela de pagos Wompi (procesamiento y Webhooks), el servicio de correo Gmail (SMTP) y el almacenamiento multimedia en Amazon S3. 

- **Pruebas de Sistema y End-to-End (E2E):** Automatización de flujos completos de negocio desde la perspectiva de los diferentes actores. 

- **Pruebas de Rendimiento y Carga:** Uso de herramientas como JMeter o Artillery para simular concurrencia de usuarios en el catálogo de productos y validar que los tiempos de respuesta de la API permanezcan en el rango de los 2 segundos. 

###### **3.6.4 Gestión de Configuración y Control de Versiones** 

Para manejar la integridad y trazabilidad del código fuente y los entregables del proyecto se tiene en cuenta: 

- Se utiliza Git con un modelo de ramificación estructurado garantizando un entorno aislado para el desarrollo de cada historia de usuario. 

- Se implementa un control de versiones registrado en la hoja de control de los documentos entregables y repositorios de código. 

- La gestión de dependencias se centraliza de manera declarativa mediante Apache Maven para el Backend y npm para el Frontend, fijando versiones estables para evitar errores en entornos de despliegue. 

###### **3.6.5 Gestión y Ciclo de Vida de Defectos** 

Cualquier anomalía o desviación identificada durante la ejecución de pruebas o auditorías se gestiona bajo un flujo formal: 

1. **Registro:** Creación del reporte del fallo en la plataforma de seguimiento especificando título, pasos para reproducir, comportamiento esperado, comportamiento obtenido y capturas o logs. 

2. **Clasificación:** Asignación de nivel de Severidad (Crítica, Alta, Media, Baja) y nivel de Prioridad (Alta, Media, Baja). 

3. **Asignación y Corrección:** Asignación al desarrollador responsable para su depuración y solución. 

4. **Re-verificación y Cierre:** Validación de la corrección en el entorno de pruebas antes de autorizar el cierre definitivo del fallo. 

###### **3.6.6 Mejora Continua y Automatización del Ciclo de Vida (CI/CD)** 

Se automatiza la verificación continua de la calidad a través de un pipeline de Integración Continua y Despliegue Continuo (CI/CD) implementado en Jenkins / Github Actions de la siguiente manera: 

26 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** _Curso: Ingeniería de software III_ 

of 

**Página 27 de 48** 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

1. **Compilación automática (Build)** : Verificación de la sintaxis y construcción de artefactos ejecutables para frontend y backend. 

2. **Ejecución de Pruebas Unitarias e Integración:** Validación inmediata de que los cambios no introducen regresiones en el sistema. 

3. **Análisis de Calidad y Cobertura:** Evaluación automática de umbrales de calidad; si el código no cumple con la cobertura mínima o presenta vulnerabilidades de seguridad, la integración es bloqueada de inmediato. 

4. **Despliegue Automático (Deploy):** Despliegue supervisado del artefacto validado hacia la infraestructura en la nube para pruebas de la aplicación. 

##### **3.7 Características de la Administración de la Calidad** 

Basado en los principios de un sistema de gestión de calidad, se identifican las siguientes características clave aplicadas al proyecto LittleStyle: 

- **Enfoque al cliente:** las funcionalidades priorizadas (recomendación de tallas, seguimiento de pedidos, gestión de perfiles infantiles) se definieron a partir de las necesidades reales de los tres tipos de usuario (Cliente, Vendedor, Administrador), asegurando que el desarrollo esté orientado a resolver sus problemas concretos y no solo a cumplir especificaciones técnicas. 

- **Gestión de las relaciones:** se establecen mecanismos de tolerancia a fallos y reintentos automáticos ante interrupciones de los proveedores externos (pasarela de pagos Wompi y servicio de correo Gmail), asegurando que la dependencia de terceros no comprometa la continuidad ni la calidad del servicio. 

- **Mejora continua:** el desarrollo se organiza en sprints iterativos con retrospectivas al final de cada uno, donde el equipo identifica errores, cuellos de botella o desviaciones de calidad y ajusta el proceso antes del siguiente sprint, en lugar de esperar hasta el final del proyecto para corregir. 

- **Liderazgo en calidad:** el equipo asume de forma conjunta la responsabilidad de velar por el cumplimiento de los criterios de calidad definidos en este plan, promoviendo que cada integrante valide su propio trabajo (código, pruebas, documentación) antes de considerarlo terminado. 

- **Enfoque basado en procesos:** la calidad se gestiona en función de los tres procesos de negocio definidos (gestión de perfil infantil, catálogo/compras/pedidos/inventario, y administración del sistema), permitiendo evaluar el cumplimiento de calidad de forma independiente para cada proceso. 

- **Toma de decisiones basada en evidencia:** las decisiones de ajuste o priorización se apoyan en las métricas e indicadores (por ejemplo, tiempos de respuesta, tasa de errores, cumplimiento de entregables), en lugar de criterios subjetivos. 

- **Compromiso de las personas:** cada historia de usuario tiene un responsable claramente asignado dentro del equipo, lo que fortalece la apropiación individual sobre la calidad del trabajo entregado, reforzada mediante revisiones de código por pares antes de integrar cualquier cambio. 

##### **3.8 Atributos de Calidad** 

Los atributos de calidad representan las propiedades y características que definen la efectividad, confiabilidad, usabilidad y valor global del sistema LittleStyle. De acuerdo con lo establecido en la norma ISO/IEC 25010, los atributos de calidad no solo definen el 

27 

**LittleStyle** 

of 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 28 de 48** 

comportamiento operativo del software, sino que también sirven como guía principal para la toma de decisiones arquitectónicas adecuadas. 

###### **3.8.1 Adecuación Funcional** 

Capacidad del producto para proporcionar funciones que satisfagan las necesidades declaradas e implícitas bajo condiciones específicas. 

Aplicación en LittleStyle: 

- **Corrección Funcional (Proceso 1):** El algoritmo de recomendación de tallas debe realizar cálculos exactos cruzando medidas biométricas del menor con la tabla de medidas del fabricante, garantizando sugerencias precisas. 

- **Completitud Funcional (Proceso 2):** Ejecución rigurosa de las reglas de negocio en la gestión del carrito de compras, cálculo automático de impuestos, aplicación de descuentos y actualización inmediata del stock de prendas tras la confirmación de pago. 

- **Pertinencia Funcional (Proceso 3):** El panel de control debe proveer información consolidada relevante sobre ventas, productos más consultados y nivel de inventarios para la toma de decisiones comerciales. 

###### **3.8.2 Eficiencia de Desempeño** 

Rendimiento del software en relación con la cantidad de recursos utilizados bajo condiciones establecidas. 

Aplicación en LittleStyle: 

- **Comportamiento Temporal:** El procesamiento del motor de recomendación de tallas debe responder en un tiempo inferior a 2 segundos y la carga del catálogo de prendas y aplicación de filtros en menos de 1.5 segundos. 

- **Uso de Recursos:** Optimización de consultas SQL mediante Spring Data JPA e indexación estratégica en la base de datos para minimizar el uso de CPU y memoria RAM en el servidor backend en Spring Boot. 

- **Capacidad:** Capacidad del sistema para soportar picos de concurrencia de usuarios durante temporadas comerciales sin degradación en los tiempos de respuesta de la API REST. 

###### **3.8.3 Compatibilidad** 

Capacidad de dos o más sistemas o componentes para intercambiar información y/o ejecutar sus funciones compartiendo el mismo entorno. 

Aplicación en LittleStyle: 

28 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** 

of 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 29 de 48** 

- **Interoperabilidad (Proceso 2):** Comunicación fluida y estructurada mediante API REST y Webhooks con la pasarela de pagos para la recepción y validación asíncrona de transacciones en tiempo real. 

- **Coexistencia:** Integración del backend con servicios externos en la nube, como Amazon S3 para el almacenamiento y entrega de recursos multimedia y servidores SMTP para el envío de correos transaccionales. 

###### **3.8.4 Capacidad de Interacción (Usabilidad)** 

Facilidad con la que el producto software puede ser entendido, aprendido, usado y resultar atractivo para los diferentes tipos de usuario. 

Aplicación en LittleStyle: 

- **Protección contra Errores de Usuario (Proceso 1):** El formulario de registro del perfil infantil incluye validaciones en tiempo real en la interfaz que previenen el ingreso de datos biométricos o medidas antropométricas incompletas o incorrectas. 

- **Operabilidad (Proceso 2):** Diseño del flujo de compra optimizado para completar el pedido con pocos clicks desde el carrito de compras. 

- **Capacidad de Aprendizaje (Proceso 3):** Interfaz limpia con gráficos intuitivos en el dashboard administrativo, facilitando el aprendizaje y la gestión rápida de productos e inventario por parte del vendedor. 

- **Adaptabilidad Multi-Dispositivo (Diseño Responsivo):** La interfaz de usuario del frontend, desarrollada como una Single Page Application (SPA) en Angular, implementa patrones de diseño responsivo. Esto garantiza que los procesos se adapten de forma fluida y optimizada a diferentes resoluciones de pantalla y dispositivos. 

###### **3.8.5 Fiabilidad** 

Grado en que el sistema realiza sus funciones de manera consistente y sin errores bajo condiciones normales y ante situaciones adversas. 

Aplicación en LittleStyle: 

- **Disponibilidad:** La plataforma mantendrá una disponibilidad operativa ( _Uptime_ ) continua superior al 99.5% en su entorno de producción. 

- **Tolerancia a fallos:** Si la pasarela de pagos Wompi o el servicio de correo presentan interrupciones temporales, el backend captura la excepción, registra el estado pendiente y ejecuta reintentos automáticos sin detener la sesión del usuario ni corromper los datos del pedido. 

###### **3.8.6 Seguridad** 

29 

**LittleStyle** 

o 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

4. **Página 30 de 48** 

Nivel de protección de los datos e información para evitar accesos no autorizados o modificaciones malintencionadas, garantizando confidencialidad, integridad, autenticidad y no repudio. 

Aplicación en LittleStyle: 

- **Confidencialidad y Cumplimiento Legal (Habeas Data):** En concordancia con la Ley 1581 de 2012 y el Decreto 1074 de 2015, la información sensible de los menores se almacena cifrada en la base de datos y se transmite exclusivamente mediante protocolo seguro HTTPS/TLS. 

- **Autenticidad y Autorización:** Implementación de control de acceso basado en roles (RBAC) gestionado con Spring Security y tokens JWT firmados digitalmente. 

- **Integridad Transaccional:** Validación obligatoria de la firma criptográfica HMAC en las notificaciones recibidas vía Webhooks de Wompi para asegurar que la confirmación del pago no haya sido alterada por terceros. 

##### **3.9 Atributos Referenciados en la ISO/IEC 25010** 

|**Característica**<br>**de Calidad**<br>**(ISO/IEC 25010)**|**Subcaracterística**<br>**s**|**¿Aplicable**<br>**al**<br>**proyecto?**|**Justificación**|
|---|---|---|---|
|Adecuación<br>Funcional|-Completitud<br>funcional.<br>-Corrección<br>funcional.<br>-Pertinencia<br>funcional.|Sí|Es fundamental para<br>el Proceso 1, ya que<br>el<br>algoritmo<br>de<br>recomendación debe<br>proveer sugerencias<br>de<br>tallas<br>estrictamente<br>correctas basadas en<br>los perfiles. En el<br>Proceso 2, asegura<br>que las reglas de<br>negocio (cálculo de<br>totales, descuentos y<br>actualización<br>de<br>inventario)<br>operen<br>con exactitud.|



30 6 

0 

**Plan de Gestión de la Calidad del Proyecto** 

###### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 31 de 48** 

||||Crítico<br>para<br>el|
|---|---|---|---|
|Eficiencia de<br>Desempeño|-Comportamiento<br>temporal.<br>-Utilización de<br>recursos.<br>- Capacidad.|Sí|Proceso<br>2;<br>el<br>catálogo<br>de<br>productos y los filtros<br>avanzados<br>deben<br>responder<br>en<br>milisegundos<br>para<br>evitar el abandono<br>del carrito. Para el<br>Proceso 3, garantiza<br>que la generación de<br>dashboards<br>y<br>reportes masivos no<br>consuma en exceso<br>los<br>recursos<br>del<br>servidor.|
|Compatibilidad|- Coexistencia.<br>- Interoperabilidad.|Sí|Indispensable para el<br>Proceso 2, dado que<br>el<br>sistema<br>debe<br>interoperar<br>sin<br>fricciones<br>con<br>la<br>pasarela de pagos<br>externa.<br>La<br>comunicación<br>vía<br>APIs<br>y<br>Webhooks<br>debe<br>asegurar<br>un<br>intercambio<br>de<br>información<br>transaccional<br>estructurado<br>y<br>en<br>tiempo real.|
||-Reconocibilidad de<br>la adecuación.<br>-Aprendizabilidad.<br>-Operabilidad.||En el Proceso 1, la<br>interfaz<br>debe<br>proteger<br>contra<br>errores de usuario al<br>ingresar las medidas<br>del<br>menor,<br>siendo<br>intuitiva|
|Capacidad de<br>Interacción|-Protección contra<br>errores de usuario.<br>-Involucración del<br>usuario.<br>-Inclusividad.<br>-Asistencia al<br>usuario.|Sí|(operabilidad). En el<br>Proceso<br>3,<br>los<br>paneles<br>administrativos<br>deben ser claros y<br>fáciles de aprender<br>para<br>que<br>los<br>vendedores|
||||gestionen su stock<br>de forma ágil.|



Reng? : 31 aa izWij Z 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

**Página 32 de 48** 



###### **LittleStyle** 



|Fiabilidad|- Ausencia de fallos.<br>- Disponibilidad.<br>- Tolerancia a fallos.|Sí|El<br>Proceso<br>2<br>(compras y pedidos)<br>exige<br>alta<br>disponibilidad<br>comercial.<br>Si<br>la<br>plataforma presenta<br>caídas o no tolera<br>fallos de red durante<br>el procesamiento del<br>pago,<br>se<br>pierden<br>ventas y se afecta la<br>confianza del cliente.|
|---|---|---|---|
||- Confidencialidad.<br>- Integridad.||Obligatorio<br>por<br>el<br>manejo<br>de<br>información.<br>En<br>el<br>Proceso 1, protege la<br>confidencialidad<br>de<br>los datos personales|
|Seguridad|- No repudio.<br>- Responsabilidad.<br>- Autenticidad.|Sí|de menores de edad<br>(Habeas Data). En el<br>Proceso 2, garantiza|
||- Resistencia.||la<br>integridad<br>y<br>autenticidad<br>en<br>la<br>validación técnica de<br>las<br>transacciones<br>procesadas.|



32 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 

**Página 33 de 48** 



_Curso: Ingeniería de software III_ 



##### **4. REQUISITOS NO FUNCIONALES DEL SISTEMA (RNF)** 

##### **4.1 Característica de Calidad (ISO/IEC 25010): Eficiencia de desempeño** 

###### **4.1.1 RNF: 01 - Respuesta oportuna de la recomendación de tallas - Proceso:Gestión del Perfil infantil y recomendación personalizada de tallas y prendas.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-01|
|**Categoría**|Comportamiento temporal|
|**Descripción**<br>**del Requisito**|El<br>sistema<br>debe<br>procesar<br>las<br>peticiones<br>del<br>motor<br>de<br>recomendación, garantizando que al menos el 95% de las consultas<br>realizadas por los clientes al catálogo infantil muestran las prendas y<br>tallas recomendadas en un tiempo menor o igual a 2 segundos, bajo<br>condiciones normales de carga.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|El tiempo de carga de recomendaciones debe ser ≤ 2 segundos.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Garantiza que el algoritmo de recomendación responda con la<br>rapidez necesaria para evitar la deserción del cliente durante la<br>búsqueda de prendas.|



###### **4.1.2 RNF: 02 - Agilidad de los filtros del catálogo - Proceso: Gestión del catálogo, compras, pedidos e inventario.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-02|
|**Categoría**|Comportamiento temporal|
|**Descripción**<br>**del Requisito**|El sistema debe ejecutar la aplicación de criterios de búsqueda,<br>garantizando que al menos el 90% de los clientes que consultan el<br>catálogo de prendas obtengan los resultados filtrados por talla, edad<br>o tipo en un tiempo menor a 1.5 segundos.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La efectividad de filtros en catálogo es < 1.5 segundos en la<br>respuesta de búsqueda.|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Un filtrado lento del catálogo puede provocar que el cliente<br>abandone la búsqueda antes de encontrar la prenda adecuada.|



###### **4.1.3 RNF: 03 - Sincronización del inventario tras el pago - Proceso: Gestión del catálogo, compras, pedidos e inventario.** 

|**Ítem**||**Descripción**|
|---|---|---|
|**ID**|RNF-03||



33 <<oeS NieNeWil 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 34 de 48** 

|**Categoría**|Comportamiento temporal|
|---|---|
|**Descripción**<br>**del Requisito**|El sistema debe realizar la actualización de existencias en base de<br>datos, asegurando que en al menos el 95% de los pedidos pagados<br>por los clientes se descuente el stock del producto en un tiempo<br>menor a 500 milisegundos.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|El tiempo de sincronización de stock debe ser < 500 milisegundos<br>tras la aprobación de la transacción.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Evita la sobreventa de productos al garantizar que el stock se<br>actualice de forma casi inmediata tras confirmarse el pago.|



###### **4.1.4 RNF: 04 - Tiempo de carga  del panel administrativo - Proceso: Gestión administrativa y procesos internos.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-04|
|**Categoría**|Comportamiento temporal|
|**Descripción**<br>**del Requisito**|El sistema debe desplegar los indicadores clave de la plataforma,<br>asegurando que al menos el 90% de los usuarios administrativos<br>visualicen la totalidad de los componentes del dashboard en un<br>tiempo menor a 3 segundos.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|El tiempo de respuesta del dashboard administrativo debe ser <3<br>segundos|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Permite que el personal administrativo consulte rápidamente las<br>métricas de negocio para la toma oportuna de decisiones.|



###### **4.1.5 RNF: 05 - Capacidad de respuesta ante concurrencia de clientes - Proceso:Gestión del Catálogo, Compras, Pedidos e Inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-05|
|**Categoría**|Capacidad|
|**Descripción**<br>**del Requisito**|El sistema debe mantener la capacidad de procesamiento de<br>peticiones, logrando que al menos el 99% de las interacciones<br>simultáneas realizadas por hasta 100 usuarios explorando el catálogo<br>finalicen con éxito sin errores de pantalla.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La tasa de procesamiento bajo carga simultánea debe ser ≥99% ante<br>picos de 100 usuarios en simultaneo.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Asegurarla estabilidad del comercio electrónico durante eventos de<br>alta demanda.|



34 = Se, aeEN\ EZZa 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 35 de 48** 

##### **4.2 Característica de Calidad (ISO/IEC 25010): Seguridad** 

###### **4.2.1 RNF: 06 - Confidencialidad de los datos del menor - Proceso: Gestión del Perfil infantil y recomendación personalizada de tallas y prendas.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-06|
|**Categoría**|Confidencialidad|
|**Descripción**<br>**del Requisito**|El sistema debe ejecutar el cifrado de datos personales, garantizando<br>que en un porcentaje mayor al 96% de los campos con información<br>de menores registrados por los acudientes garantizando que la<br>información se mantenga cifrada en reposo y en tránsito sin accesos<br>no autorizados.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La efectividad en la protección de datos menores debe ser > 96%|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Protege la información confidencial de los menores de edad y<br>asegura el cumplimiento legal de la Ley 1581 de 2012 y el Decreto<br>1074 de 2015 sobre Habeas Data.|



###### **4.2.2 RNF: 07 - Autenticidad de notificaciones de pago(WebHooks) - Proceso: Gestión del catálogo, compras, pedidos e inventario.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-07|
|**Categoría**|Integridad y autenticidad|
|**Descripción**<br>**del Requisito**|El sistema debe realizar la validación por firma de peticiones<br>externas, asegurando que un porcentaje mayor o igual al 99.8% de<br>los eventos Webhook enviados por la pasarela de pagos sean<br>autenticados mediante la verificación de la firma HMAC antes de<br>actualizar el estado de la compra.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La tasa de autenticación de notificaciones de pago debe ser ≥<br>99.8%.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Previene fraudes por suplantación al validar la firma antes de<br>procesar cualquier transacción comercial.|



**4.2.3 RNF: 08 - Trazabilidad de acciones administrativas - Proceso: Gestión administrativa y procesos internos.** 

# **Ítem Descripción** ~~<mark>a</mark>~~ 

35 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 36 de 48** 

|**ID**|RNF-08|
|---|---|
|**Categoría**|Responsabilidad/ No repudio|
|**Descripción**<br>**del Requisito**|El sistema debe registrar las operaciones críticas del sistema,<br>garantizando que en un porcentaje mayor al 99.5%  de las acciones<br>ejecutadas por los usuarios administrativos los eventos queden<br>almacenados en un log de auditoría inalterable indicando usuario,<br>fecha, hora y detalle.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La trazabilidad de acciones administrativas debe ser >99.5%.|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Facilita<br>el seguimiento de responsabilidades y garantiza la<br>trazabilidad sobre cambios en cuentas, roles y moderación de<br>contenido.|



###### **4.2.4 RNF: 09 - Resistencia ante vulnerabilidades en dependencias - Proceso: Gestión del catálogo, compras, pedidos e inventario.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-09|
|**Categoría**|Resistencia|
|**Descripción**<br>**del Requisito**|El<br>sistema<br>debe<br>gestionar<br>el<br>estado<br>de las librerías de<br>código,logrando que un porcentaje mayor o igual al 98% de las<br>dependencias utilizadas en los módulos de comercio electrónico<br>permanezcan libres de fallos de seguridad conocidos mediante su<br>actualización oportuna.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La efectividad en el mantenimiento seguro de dependencias debe ser<br>≥ 98%.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Reduce la ventana de exposición ante vulnerabilidades conocidas en<br>librerías de terceros, protegiendo la confidencialidad e integridad de<br>la información y la continuidad del servicio.|



###### **4.2.5 RNF: 10 - Control de accesos por roles  - Proceso: Gestión Administrativa y Procesos Internos** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-10|
|**Categoría**|Autorización|
|**Descripción**<br>**del Requisito**|El sistema debe controlar el acceso a funciones restringidas,<br>asegurando que un porcentaje mayor o igual al 98.5% de los intentos<br>de consulta a endpoints protegidos realizados por usuarios sin rol o<br>token JWT válido sean denegados automáticamente por el módulo<br>de seguridad.|



36 

**LittleStyle** 

o 

4 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

**Página 37 de 48** 

|**Métrica o**<br>**Criterio de**|La tasa de restricción de acceso no autorizado debe ser ≥ 98.5%.|
|---|---|
|**Aceptación**||
|**Prioridad**|Alta|
|**Comentarios /**<br>**i**|Garantiza la separación estricta de privilegios entre clientes,|
|**Justificación**|vendedores y administradores dentro del sistema.|



##### **4.3 Característica de Calidad (ISO/IEC 25010): Adecuación Funcional** 

###### **4.3.1 RNF: 11 - Precisión en la recomendación de tallas - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-11|
|**Categoría**|Correcciónn Funcional|
|**Descripción**<br>**del Requisito**|El sistema debe generar recomendaciones de talla a partir de las<br>medidas registradas en el perfil infantil y las tablas de tallas<br>disponibles,<br>de<br>manera<br>que<br>la tasa de adopción de la<br>recomendación de talla sea superior al 80 % de las compras<br>finalizadas en las que el motor haya generado una recomendación.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de adopción de la talla recomendada > 80% sobre las compras<br>finalizadas en las que el motor generó una sugerencia.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Garantiza la exactitud del motor de recomendación de tallas y<br>prendas para generar alta confianza en las decisiones de compra del<br>cliente.|



###### **4.3.2 RNF: 12 - Completitud del registro del perfil infantil - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-12|
|**Categoría**|Completitud Funcional|
|**Descripción**<br>**del Requisito**|El sistema debe permitir registrar y almacenar la información<br>necesaria de los perfiles infantiles, incluyendo los datos definidos<br>para medidas, edad y restricciones por alergias textiles, de manera<br>que más del 90 % de los perfiles registrados contengan la totalidad<br>de los campos establecidos para el perfil.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Completitud del perfil infantil > 90 %|
|**Prioridad**|Alta|



a 37 < ee | 

**LittleStyle Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

**Página 38 de 48** 





**Comentarios /** Permite disponer de la información necesaria para que el motor de **Justificación** recomendación sugiera prendas correctas y seguras. 

###### **4.3.3 RNF: 13 - Trazabilidad del ciclo de vida del pedido - Proceso: Gestión del catálogo, compras, pedido e inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-13|
|**Categoría**|Completitud Funcional|
|**Descripción**<br>**del Requisito**|El sistema debe registrar y actualizar correctamente las transiciones<br>del ciclo de vida de cada pedido, incluyendo el número de guía<br>cuando corresponda, de manera que más del 98 % de los pedidos<br>pagados presentan un historial completo de estados y la guía<br>logística registrada sin inconsistencias.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de trazabilidad y actualización de estados > 98 %|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Asegurar que el flujo comercial cumpla rigurosamente las reglas de<br>negocio de pedidos y la trazabilidad del envío para el cliente.|



###### **4.3.4 RNF: 14 - Emisión oportuna de alertas de reabastecimiento - Proceso: Gestión del catálogo, compras, pedidos e inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-14|
|**Categoría**|Completitud Funcional|
|**Descripción**<br>**del Requisito**|El<br>sistema<br>debe<br>monitorear<br>el nivel de stock y generar<br>oportunamente las alertas de reabastecimiento cuando un producto<br>alcance el valor mínimo configurado, de manera que el 99 % de los<br>productos que lleguen a agotarse hayan recibido previamente la<br>alerta correspondiente.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Precisión de alertas de reabastecimiento > 99 %|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Previene el desabastecimiento imprevisto y garantiza la continuidad<br>en la oferta de productos por parte de los vendedores.|



###### **4.3.5 RNF: 15 - Consistencia de datos en reportes financieros y dashboards - Proceso: Gestión administrativa y procesos internos** 

**Ítem Descripción ID** RNF-15 

38 ~~ee~~ = a \ 

**Plan de Gestión de la Calidad del Proyecto** 



###### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 39 de 48** 

|**Categoría**|Corrección Funcional|
|---|---|
|**Descripción**<br>**del Requisito**|El<br>sistema<br>debe<br>presentar en los dashboards y reportes<br>administrativos<br>información<br>consistente<br>con<br>los<br>registros<br>transaccionales de ventas almacenados en la base de datos, de<br>manera que más del 99.9 % de las transacciones evaluadas<br>coinciden exactamente con la información consolidada presentada<br>en los reportes.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Consistencia de datos en reportes financieros > 99.9%.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Garantiza la entrega de datos fiables e íntegros a administradores y<br>vendedores para la toma de decisiones comerciales.|



##### **4.4 Característica de Calidad (ISO/IEC 25010): Capacidad de Interacción** 

###### **4.4.1 RNF:  16 - Prevención de errores en el registro de medidas - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-16|
|**Categoría**|Protección frente a errores de usuario|
|**Descripción**<br>**del Requisito**|El sistema debe validar los datos ingresados en el formulario del perfil<br>infantil antes de almacenarlos, identificando valores faltantes,<br>inválidos o inconsistentes. El cumplimiento se medirá mediante la<br>Tasa de error en captura de medidas biométricas, el porcentaje de<br>errores deberá ser menor al 5 %|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La tasa de errores de validación durante el registro de medidas<br>biométricas debe ser menor al 5 % del total de intentos de guardado.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Reduce los errores de usuario durante el registro del perfil infantil y<br>contribuye a que el motor de recomendación trabaje con información<br>válida y consistente.|



###### **4.4.2 RNF: 17 - Asistencia al usuario mediante resolución de incidencias - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-17|
|**Categoría**|Asistencia al usuario|
|**Descripción**|El sistema debe facilitar la gestión y seguimiento de las incidencias|



39 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 40 de 48** 

|**del Requisito**|reportadas por clientes y vendedores, permitiendo que los<br>administradores<br>atiendan<br>oportunamente<br>las<br>dificultades<br>relacionadas con el uso de la plataforma. El cumplimiento se medirá<br>mediante la Tasa de resolución de incidencias de usuarios, el<br>porcentaje de incidencias resueltas en menos de 24 horas deberá ser<br>superior al 90 %.|
|---|---|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de resolución de incidencias de usuarios > 90 %.|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Una atención oportuna de las incidencias permite brindar asistencia<br>a clientes y vendedores cuando encuentran dificultades durante el<br>uso de la plataforma, reduciendo el impacto de estos problemas<br>sobre la realización de sus actividades.|



###### **4.4.3 RNF: 18 - Operabilidad del flujo de compra - Proceso: Gestión del catálogo, compras, pedidos e inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-18|
|**Categoría**|Operabilidad|
|**Descripción**<br>**del Requisito**|El sistema debe proporcionar un flujo de compra claro y continuo<br>desde la selección de productos, el carrito y la confirmación del<br>pedido hasta el inicio del pago. El cumplimiento se medirá mediante<br>la Tasa de abandono del carrito el porcentaje de abandono deberá<br>mantenerse por debajo del 30 %|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La tasa de abandono del carrito debe mantenerse por debajo del 30<br>% sobre el total de carritos creados.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Una tasa reducida de abandono permite evidenciar que el flujo de<br>compra es comprensible y no presenta afecciones significativas que<br>impidan al cliente finalizar el proceso.|



###### **4.4.4 RNF: 19 - Facilidad de actualización del perfil infantil - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-19|
|**Categoría**|Involucración del usuario|
|**Descripción**<br>**del Requisito**|El sistema debe permitir que el cliente consulte y actualice de manera<br>clara y accesible las medidas registradas en los perfiles infantiles,<br>favoreciendo su actualización periódica a medida que el menor<br>crece. El cumplimiento se medirá mediante la Frecuencia de<br>actualización de perfiles, más del 60 % deberá haber sido<br>actualizado en menos de seis meses.|



40 <<oeS NieNeWil 

**LittleStyle** 

o 

4 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

**Página 41 de 48** 

**Métrica o** Más del 60 % de los perfiles infantiles deben haber sido actualizados **Criterio de** en un periodo inferior a 6 meses. **Aceptación** 

**Prioridad** Media Facilitar la actualización periódica de las medidas favorece la **Comentarios /** involucración del usuario con la plataforma y permite mantener **Justificación** información actualizada para mejorar la precisión de las recomendaciones de tallas y prendas. 

###### **4.4.5 RNF: 20 - Comprensión del panel administrativo - Proceso: Gestión administrativa y procesos internos** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-20|
|**Categoría**|Aprendizabilidad|
|**Descripción**<br>**del Requisito**|El dashboard administrativo debe presentar los indicadores,<br>controles y opciones de ventas, inventario, búsquedas y operación<br>de forma organizada y comprensible, permitiendo que vendedores y<br>administradores aprendan a realizar las operaciones principales del<br>panel. El cumplimiento se medirá mediante la Tasa de ejecución<br>correcta de tareas administrativas, al menos el 90 % deberá<br>completarse correctamente.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de ejecución correcta de tareas administrativas ≥ 90 %.|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Favorece la capacidad de aprendizaje y la operabilidad del sistema,<br>reduciendo la dificultad para interpretar información y ejecutar<br>operaciones desde el panel.|



##### **4.5 Característica de Calidad (ISO/IEC 25010): Compatibilidad** 

###### **4.5.1 RNF: 21 - Interoperabilidad con la pasarela de pagos externa - Proceso: Gestión del catálogo, compras, pedidos e inventario.** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF- 21|
|**Categoría**|Interoperabilidad|
|**Descripción**<br>**del Requisito**|El sistema debe interoperar con la pasarela de pagos externa<br>mediante API y Webhooks para recibir y procesar las notificaciones<br>de las transacciones, de manera que la tasa de excepciones en la<br>integración sea inferior al 2 % del total de transacciones notificadas<br>por la pasarela.|
|**Métrica o**||
|**Criterio de**<br>**Aceptación**|Tasa de excepciones en integración con pasarela (Webhooks) < 2 %.|



a 41 < ee | 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 42 de 48** 

**Prioridad** Alta **Comentarios /** Asegura una integración fluida con la pasarela externa sin perder **Justificación** confirmaciones de pago. 

###### **4.5.2 RNF: 22 - Coexistencia y compatibilidad entre plataformas y navegadores web - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-22|
|**Categoría**|Coexistencia|
|**Descripción**<br>**del Requisito**|La aplicación web debe coexistir correctamente en los navegadores y<br>plataformas objetivo, manteniendo disponibles sus funcionalidades<br>principales, de manera que más del 98 % de las plataformas<br>evaluadas presenten una ejecución exitosa sin errores visuales o<br>funcionales.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Soporte de Plataformas Compatibles > 98 %|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Garantiza que cualquier usuario acceda y gestione perfiles infantiles<br>desde su navegador o dispositivo de preferencia sin bloqueos<br>visuales o técnicos.|



###### **4.5.3 RNF: 23 - Interoperabilidad para el envío de notificaciones por correo electrónico SMTP - Proceso: Gestión del catálogo, compras, pedidos e inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-23|
|**Categoría**|Interoperabilidad|
|**Descripción**<br>**del Requisito**|El sistema debe interoperar con el servicio externo de correo para<br>enviar las notificaciones transaccionales asociadas al registro,<br>compras y actualización de pedidos, de manera que la tasa de<br>entrega exitosa de notificaciones sea superior al 98 % sobre el total<br>de notificaciones enviadas.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de entrega exitosa de notificaciones  > 98%.|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Permite<br>enviar<br>notificaciones<br>automatizadas<br>utilizando<br>la<br>infraestructura estándar de mensajería en la nube.|



###### **4.5.4 RNF: 24 - Interoperabilidad en la exportación de reportes analíticos - Proceso: Gestión administrativa y procesos internos** 

42 Oe= aGC,\ 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 43 de 48** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-24|
|**Categoría**|Interoperabilidad|
|**Descripción**<br>**del Requisito**|El sistema debe generar y exportar los reportes administrativos de<br>ventas, búsquedas e inventario en un formato que pueda ser<br>procesado por aplicaciones externas, de manera que más del 98 %<br>de las solicitudes de exportación realizadas por vendedores y<br>administradores finalicen correctamente.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de éxito en la exportación de reportes > 98 %.|
|**Prioridad**|Media|
|**Comentarios /**<br>**Justificación**|Facilita la descarga y posterior análisis de datos comerciales en hojas<br>de cálculo o visores externos.|



###### **4.5.5 RNF: 25 Interoperabilidad con el servicio de almacenamiento multimedia en la nube - Proceso: Gestión del catálogo, compras, pedidos e inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-25|
|**Categoría**|Interoperabilidad|
|**Descripción**<br>**del Requisito**|El sistema debe coexistir con el servicio externo de almacenamiento<br>multimedia utilizado para gestionar las imágenes del catálogo,<br>permitiendo consultar y visualizar correctamente los recursos<br>asociados a los productos, de manera que la tasa de disponibilidad<br>de recursos multimedia sea superior al 99 % sobre el total de<br>recursos solicitados.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de disponibilidad de recursos multimedia > 99 %.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Permite desacoplar el manejo de archivos multimedia del servidor<br>principal y de la base de datos optimizando el ancho de banda.|



##### **4.6 Característica de Calidad (ISO/IEC 25010): Fiabilidad** 

###### **4.6.1 RNF: 26 - Disponibilidad operativa de la plataforma - Proceso: Gestión del catálogo, compras, pedidos e inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-26|
|**Categoría**|Disponibilidad|
|**Descripción**|El sistema debe mantener disponibles las funcionalidades principales|



43 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 

**Página 44 de 48** 





|**del Requisito**|de catálogo, carrito, pedidos y consulta de productos durante la<br>operación planificada. El cumplimiento se medirá mediante la<br>Disponibilidad Operativa del Servicio, el sistema deberá permanecer<br>disponible en un porcentaje superior al 99.5 %.|
|---|---|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|La disponibilidad operativa de la plataforma debe ser superior al 99.5<br>% del tiempo de operación planificado.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Las interrupciones del servicio pueden impedir consultas y compras,<br>generando pérdida de operaciones y afectando directamente tanto a<br>clientes como a vendedores.|



###### **4.6.2 RNF: 27 - Disponibilidad del panel administrativo - Proceso: Gestión administrativa y procesos internos** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-27|
|**Categoría**|Disponibilidad|
|**Descripción**<br>**del Requisito**|El dashboard administrativo y las funcionalidades asociadas a<br>reportes deben permanecer accesibles durante el tiempo de servicio<br>planificado. El cumplimiento se medirá mediante la Disponibilidad del<br>panel administrativo (Uptime), la disponibilidad deberá ser superior al<br>99.9 %.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Disponibilidad del panel administrativo superior al 99.9 %.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Garantiza que los usuarios administrativos puedan supervisar<br>continuamente el comportamiento de la plataforma y consultar la<br>información necesaria para sus actividades.|



###### **4.6.3 RNF: 28 - Tolerancia a fallos de la pasarela de pagos - Proceso: Gestión del catálogo, compras, pedidos e inventario** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-28|
|**Categoría**|Tolerancia a fallos|
|**Descripción**<br>**del Requisito**|Ante errores temporales o interrupciones en la comunicación con la<br>pasarela de pagos, el sistema debe conservar íntegros los datos del<br>pedido y mantener la transacción en un estado válido sin detener el<br>resto de la plataforma. El cumplimiento se medirá mediante la Tasa<br>de preservación transaccional ante fallos externos, al menos el 99 %<br>deberá conservar su información correctamente.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de preservación transaccional ante fallos ≥ 99 %.|



44 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 45 de 48** 

**Prioridad** Alta Evita que interrupciones temporales de un servicio externo **Comentarios /** provoquen pérdida de pedidos, inconsistencias en las transacciones **Justificación** o interrupciones innecesarias del proceso comercial. 

###### **4.6.4 RNF: 29 - Ausencia de fallos en el motor de recomendación - Proceso: Gestión del perfil infantil y recomendación personalizada de tallas y prendas** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-29|
|**Categoría**|Ausencia de fallos|
|**Descripción**<br>**del Requisito**|El motor de recomendación debe procesar las solicitudes de tallas y<br>prendas manteniendo un funcionamiento estable, evitando que<br>excepciones no controladas interrumpan las consultas realizadas por<br>los clientes. El cumplimiento se medirá mediante la Tasa de consultas<br>de recomendación completadas sin fallos, al menos el 99 % deberán<br>completarse sin fallos.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tasa de consultas de recomendación completadas sin fallos ≥ 99 %.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|La recomendación personalizada constituye una funcionalidad<br>central de LittleStyle; una alta proporción de consultas completadas<br>sin fallos permite asegurar un comportamiento consistente del motor<br>y evitar interrupciones durante la consulta de tallas y prendas.|



###### **4.6.5 RNF: 30 - Recuperabilidad ante caídas del servicio comercial - Proceso: Gestión administrativa y procesos internos** 

|**Ítem**|**Descripción**|
|---|---|
|**ID**|RNF-30|
|**Categoría**|Recuperabilidad|
|**Descripción**<br>**del Requisito**|Ante una caída que interrumpa las funcionalidades de catálogo,<br>carrito, pedidos o inventario, el sistema debe poder restablecer su<br>operación después de la atención del incidente. El cumplimiento se<br>medirá mediante el Tiempo Medio de Reparación Ante Caídas, la<br>medida mensual deberá ser igual o inferior a 30 minutos.|
|**Métrica o**<br>**Criterio de**<br>**Aceptación**|Tiempo Medio de Reparación Ante Caídas ≤ 30 minutos.|
|**Prioridad**|Alta|
|**Comentarios /**<br>**Justificación**|Permite determinar la capacidad de la plataforma para recuperar su<br>operación después de una interrupción y reducir el periodo durante<br>el<br>cual los clientes y vendedores no pueden utilizar las<br>funcionalidades comerciales.|



45 

**LittleStyle** 

**Plan de Gestión de la Calidad del Proyecto** 

of 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 46 de 48** 

46 

**LittleStyle** 

of 

**Plan de Gestión de la Calidad del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 47 de 48** 

##### **5. EVIDENCIAS MÉTRICAS** 

47 

**PROGRAMA DE INGENIERÍA DE SISTEMAS Y COMPUTACIÓN** 

**Tel: (57) 6 735 9300 Ext 355 Carrera 15 Calle 12 Norte Armenia, Quindío – Colombia ingesis@uniquindio.edu.co** 

