LittleStyle Definici6n del Proyecto Version: Fecha: 2.00 20/09/2026 



<!-- Start of picture text -->
SY<br>Ves<br>(4 {<br>\<br>WY<br>a y<br>-<br>=<br>> (<br>ey<br>UNIQUINDIO<br>en conexion territorial<br><!-- End of picture text -->

www.uniquindio.edu.co 

**LittleStyle Definición del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 2 de 20** 

# **HOJA DE CONTROL** 

|**Proyecto**|LittleStyle|||
|---|---|---|---|
|**Entregable**|Definición del Proyecto|||
|**Versión/Edición**|0200|**Fecha Versión**|20/09/2026|
|**Aprobado por**||**Fecha Aprobación**|DD/MM/AAAA|
|||**Nº Total de Páginas**|20|



## **REGISTRO DE CAMBIOS** 

|**Versión**|**Causa del Cambio**|**Responsable del Cambio**|**Fecha del**<br>**Cambio**|
|---|---|---|---|
|01|Versión inicial|Oscar Tomas Aristizabal<br>Valentina Gónzalez Díaz<br>Valentina Rodríguez Castro|06/09/2026|
|02|Modificación de los<br>procesos y cronograma|Valentina Gonzalez Diaz<br>Valentina Rodríguez Castro|20/09/2026|



## **AUTORES** 

|**Nombre y Apellidos**|
|---|
|Valentina Gonzalez Diaz (valentina.gonzalezd@uqvirtual.edu.co)|
|Valentina Rodriguez Castro (valentina.rodriguezc1@uqvirtual.edu.co)|
|Oscar Tomas Aristizábal Velásquez (Oscart.aristizabalv@uqvirtual.edu.co)|



2 

**LittleStyle Definición del Proyecto** _Curso: Ingeniería de software III_ 

of 

**Página 3 de 20** 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

# **TABLA DE CONTENIDO** 

|**1. INTRODUCCIÓN..............................................................................................................4**|
|---|
|1.1 Alcance..................................................................................................................... 4|
|1.2 Objetivos................................................................................................................... 4|
|1.2.1 Objetivo general............................................................................................... 5|
|1.2.2 Objetivos específicos.......................................................................................5|
|**2. INFORMACIÓN DEL CASO DE ESTUDIO......................................................................6**|
|2.1 Glosario de términos.................................................................................................6|
|**3. DESCRIPCIÓN DE LA SOLUCIÓN PROPUESTA AL CASO DE ESTUDIO...................9**|
|3.1 Descripción de los actores de negocio...................................................................10|
|3.1.1. Padre, madre o acudiente (Cliente).............................................................. 10|
|3.1.2. Pasarela de Pago Externa............................................................................ 10|
|3.2 Descripción de los workers.....................................................................................10|
|3.2.1. Vendedor.......................................................................................................10|
|3.2.2. Administrador................................................................................................ 11|
|**4. PROCESOS DE NEGOCIO............................................................................................12**|
|4.1 Proceso 1: Gestión del Perfil infantil y recomendación personalizada de tallas y|
|prendas......................................................................................................................... 12|
|4.2 Proceso 2: Gestión del catálogo, compras y pedidos............................................13|
|4.3 Proceso 3: Gestión de procesos internos...............................................................15|
|**5. CRONOGRAMA DE TRABAJO PROPUESTO..............................................................17**|
|5.1. Metodología del trabajo..........................................................................................17|
|5.2. Distribución de historias de usuario por sprint....................................................... 17|
|5.3. Historias de usuario............................................................................................... 17|



3 

**LittleStyle Definición del Proyecto** _Curso: Ingeniería de software III_ 

of 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 4 de 20** 

# **1. INTRODUCCIÓN** 

Obtener ropa infantil requiere atender diferentes necesidades tanto para los padres como para las empresas que se dedican a su comercialización. Seleccionar la ropa correcta requiere aspectos como la variabilidad en el crecimiento infantil, la precisión en las tallas y condiciones especiales de sensibilidad o alergias textiles. A su vez, los vendedores requieren mecanismos que les permitan administrar sus productos, controlar inventarios de prendas y procesar sus ventas.. 

Teniendo en cuenta lo anterior, este proyecto se encarga de atender estas necesidades, siendo una plataforma digital de gestión y comercialización de ropa infantil (e-commerce). La plataforma permite administrar perfiles infantiles con información de medidas y alergias, facilitar la selección de ropa mediante un motor de recomendaciones personalizadas de tallas, consultar un catálogo de productos, gestionar carrito de compras y pedidos. Además de permitir a los vendedores la capacidad de administrar sus productos e inventarios. 

Adicionalmente, la plataforma busca proporcionar herramientas de generación de información, para facilitar el análisis de demanda y rendimiento de los productos, por medio de métricas y reportes. 

# **1.1 Alcance** 

El proyecto comprende el análisis, diseño y desarrollo de una plataforma e-commerce web  para la gestión y comercialización de ropa infantil. 

Entrando más en detalle, la plataforma está dirigida a tres roles de usuarios y un servicio externo: Padre, madre o acudiente (Cliente), Vendedor, Administrador y Pasarela de Pago Externa. 

Para los clientes, se les permitirá gestionar sus cuentas y perfiles de sus hijos, todo lo relacionado con sus datos necesarios para la recomendación de tallas y ropa. Además tendrán acceso al catálogo de ropa infantil con filtros avanzados y visualizar el detalle de los productos, administrar un carrito de compras, realizar pedidos y monitorear su estado. 

Para los vendedores, podrán gestionar sus productos, controlar existencias de stock, recibir alertas de inventario bajo y podrán acceder a un panel de métricas, donde podrán generar reportes de venta específicos y generales de la plataforma, para saber cuales son las palabras clave más usadas y cuáles generan más ventas. 

Para los administradores, se les permitirá gestionar a los usuarios (clientes y vendedores), además de proporcionar un panel de información y reportes relacionados con la demanda de productos. 

Para los vendedores y administradores, se incluye la visualización de dashboards y la generación y descarga/exportación de reportes operacionales en formato PDF para su análisis fuera de línea. 

En el caso de la pasarela de pago externa, se incluye dentro del alcance la integración 

4 

**LittleStyle** 

of 

**Definición del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 5 de 20** 

técnica permitiendo procesar transacciones, validar el resultado de pagos y comunicar a la plataforma si una transacción fue aprobada, rechazada o pendiente. 

Por último, la plataforma asegura las funcionalidades para soportar los procesos anteriores, no contempla dentro del alcance inicial, la gestión interna de procesos de manufactura o confección textil, ni la operación logística/transporte directo de paquetería externa. El sistema logístico dentro del sistema se limitará a la actualización de estados del pedido y el registro del número de guía correspondiente.. 

# **1.2 Objetivos** 

# **1.2.1 Objetivo general** 

Desarrollar una plataforma digital e-commerce para la gestión integral y comercialización de ropa infantil que permita administrar perfiles infantiles, facilitar la selección de tallas y prendas, apoyar los procesos de inventario de los vendedores, gestionar carritos y pedidos con integración a pasarela de pagos, proporcionando además herramientas de administración y generación de información para el seguimiento de la operación. 

# **1.2.2 Objetivos específicos** 

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

5 

**LittleStyle** 

**Definición del Proyecto** 



_Curso: Ingeniería de software III_ 



**Página 6 de 20** 

# **2. INFORMACIÓN DEL CASO DE ESTUDIO** 

El caso de estudio se desarrolla en el contexto de la gestión y comercialización de ropa infantil, donde intervienen diferentes padres, madres o acudientes (clientes), vendedores de ropa y administradores del servicio. 

En este contexto, los clientes requieren consultar diferentes opciones de ropa y seleccionar prendas adecuadas para las características y necesidades de sus hijos. La elección de una talla puede representar una dificultad debido a las diferencias en las características físicas de cada niño y a la variedad de tallas utilizadas en las prendas infantiles. 

Por otra parte, los vendedores requieren canales digitales centralizados en la comercialización de sus productos, el control de inventario y la recopilación de datos sobre tendencias de compras. 

Asimismo se requiere integrar mecanismos para el procesamiento seguro de transacciones en línea y contar con un panel centralizado que facilite la administración de la información generada por los diferentes participantes y contar con mecanismos que permitan realizar seguimiento a la operación y obtener información para su análisis. 

A partir de este contexto se plantea el caso de estudio para el desarrollo de una solución informática que permita abordar las necesidades identificadas en este dominio. 

# **2.1 Glosario de términos** 

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



6 > NGig 

**Definición del Proyecto** 



### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 7 de 20** 

|**Pedido**|Solicitud de adquisición de uno o varios productos realizada por<br>un padre a través de la plataforma.|
|---|---|
|**Prenda / Producto**|Producto de ropa infantil ofrecido mediante la plataforma y<br>asociado a características como talla, tipo, descripción y<br>disponibilidad.|
|**Recomendación de**<br>**prendas**|Funcionalidad mediante la cual la plataforma propone productos<br>que pueden ser adecuados para un perfil infantil.|
|**Recomendación de**<br>**talla**|Funcionalidad que permite determinar o sugerir una talla de<br>prenda a partir de la información registrada sobre el niño y las<br>características de la prenda.|
|**Talla**|Clasificación utilizada para representar las dimensiones o<br>tamaño de una prenda infantil y facilitar su selección de acuerdo<br>con las características del niño.|
|**Usuario**|Persona que interactúa con la plataforma mediante alguno de<br>los roles definidos para el sistema.|
|**KPIs**|_Key Performance Indicators_o Indicadores Clave de Desempeño:<br>Son métricas que permiten medir qué tan bien está funcionando<br>la plataforma y cómo se comportan los usuarios dentro de ella.|
|**API**|_Application Programming Interface_o Interfaz de Programación<br>de Aplicaciones es un mecanismo que permite que dos<br>sistemas diferentes se comuniquen e intercambien información<br>de forma estructurada. En este caso la conexión de la pasarela<br>de pagos con la plataforma para el procesamiento de<br>transacciones.|
|**Webhooks**|Método que permite a una aplicación web enviar datos de forma<br>automática a otra aplicación en tiempo real cuando ocurre un<br>evento específico.|
|**Habeas Data**|Derecho que tienen las personas a conocer, actualizar, rectificar<br>y, en los casos establecidos por la ley, solicitar la eliminación de<br>la información personal que se encuentre registrada en bases<br>de datos o archivos.|
|**Ley 1581 de 2012**|Ley colombiana que establece disposiciones generales para la<br>protección de datos personales y regula su recolección,<br>almacenamiento, uso y tratamiento.|
|**Decreto 1074 de**<br>**2015**|Decreto Único Reglamentario del Sector Comercio, Industria y<br>Turismo<br>que incluye disposiciones relacionadas con la<br>reglamentación del tratamiento de datos personales en<br>Colombia.|
|**Ley 1480 de 2011-**<br>**Estatuto del**<br>**consumidor**|Norma colombiana que establece medidas para la protección<br>de los derechos de los consumidores y regula aspectos<br>relacionados con la información de productos, comercio<br>electrónico, garantías y relaciones de consumo.|
|**Resolución Andina**<br>**2109 de 2019**|Reglamento Técnico Andino relacionado con el etiquetado de<br>confecciones, que establece requisitos sobre la información que<br>debe suministrarse al consumidor respecto a los productos<br>textiles comercializados.|
|**Tratamiento de**<br>**datos personales**|Operación o conjunto de operaciones realizadas sobre datos<br>personales,<br>como<br>la<br>recolección,<br>almacenamiento, uso,|



7 

**LittleStyle Definición del Proyecto** 

_Curso: Ingeniería de software III_ 





**Página 8 de 20** 

||actualización o eliminación de información.|
|---|---|
|**Stock minimo**|Cantidad mínima de existencias establecida para un producto, a<br>partir de la cual el sistema puede generar una alerta de<br>reabastecimiento.|
|**Dashboard**|Panel visual que presenta indicadores y métricas relevantes<br>mediante gráficos, tablas u otros elementos para facilitar el<br>seguimiento y la toma de decisiones.|



8 

**LittleStyle Definición del Proyecto** 

of 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 9 de 20** 

# **3. DESCRIPCIÓN DE LA SOLUCIÓN PROPUESTA AL CASO DE ESTUDIO** 

La solución propuesta consiste en el desarrollo de una plataforma web e-commerce  para la gestión y comercialización de ropa infantil denominada _LittleStyle_ , cuyo objetivo principal es centralizar, automatizar y optimizar las actividades de consulta, selección personalizada, adquisición y administración de ropa infantil en un entorno digital seguro y eficiente. 

Esta plataforma permitirá a los padres, madres y acudientes gestionar sus cuentas de usuario y registrar perfiles de sus hijos con información clave como medidas, edad, preferencias y restricción por sensibilidad o alergia a ciertos productos textiles. A partir de estos datos, la plataforma activará un motor de recomendación inteligente que recomendará tallas y prendas. Adicionalmente, podrán gestionar un carrito de compras, efectuar el pago mediante la integración de una pasarela de pagos y realizar el seguimiento continuo del estado de sus pedidos hasta que se entregue a la empresa de transporte logístico. 

Asimismo, los vendedores contarán con herramientas para administrar su catálogo de prendas (imágenes, precios, descripciones técnicas, tablas de tallas y composición textil), controlar las existencias de stock de productos en tiempo real y recibir alertas automáticas ante niveles bajos de inventario. Por otro lado, el administrador del sistema dispondrá de un panel de control centralizado para supervisar y modelar las cuentas de usuarios, supervisar las publicaciones del catálogo y acceder a dashboards y reportes analíticos para el seguimiento operativo y el análisis de la demanda del mercado. 

Con esta solución se espera: 

- **Facilitar la búsqueda y selección segura de prendas:** Permitir a los padres, madres o acudientes encontrar ropa infantil adecuada a las dimensiones físicas y particularidades dermatológicas de los niños. 

- **Optimizar la elección de tallas y prevenir reacciones alérgicas:** Automatizar la recomendación de talla y evitar la recomendación de fibras o materiales potencialmente irritantes o alérgenos de acuerdo a la información suministrada. 

- **Centralizar la oferta comercial infantil:** Proporcionar un catálogo digital interactivo estructurado con filtros avanzados y fichas técnicas detalladas por producto. 

- **Proveer un flujo de compra digital completo e integrado:** Facilitar la experiencia mediante la gestión de carrito de compras, la integración con pasarela de pagos y el monitoreo transparente del estado del pedido. 

- **Garantizar el control del inventario de productos terminados:** Permitir a los vendedores administrar eficientemente sus existencias de prendas y recibir notificaciones automáticas de reabastecimiento por stock mínimo. 

- **Ofrecer inteligencia de negocios y analítica operativa:** Proporcionar dashboards y reportes para el análisis de ventas, productos de alta rotación, demandas por talla y palabras clave más buscadas. 

- **Asegurar la gobernanza de la plataforma:** Dotar al Administrador de herramientas para la moderación de usuarios, gestión de categorías y supervisión general del funcionamiento del sistema. 

9 

**LittleStyle** 

of 

**Definición del Proyecto** 

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
hz.<br><!-- End of picture text -->

**Página 10 de 20** 

# **3.1 Descripción de los actores de negocio** 

Los actores de negocio identificados corresponden a las entidades o usuarios externos al sistema que interactúan con la plataforma para consumir sus servicios o generar solicitudes de compra. 

### **3.1.1. Padre, madre o acudiente (Cliente)** 

Usuario externo comprador final que interactúa con la plataforma para gestionar la información del perfil infantil y adquirir prendas. 

_Responsabilidades y participación_ 

- Crear y gestionar la cuenta de usuario personal. 

- Registrar y actualizar perfiles infantiles (medidas corporales, alergias a fibras/materiales y gustos). 

   - Explorar el catálogo de productos mediante búsqueda y filtros. 

- Consultar las sugerencias personalizadas de talla. 

# **3.2 Descripción de los workers** 

Corresponden a los roles internos de la plataforma encargados de ejecutar, gestionar y supervisar los procesos de comercialización, inventario de productos y administración del sistema. 

### **3.2.1. Vendedor** 

Encargado de ofrecer y administrar prendas infantiles dentro de la plataforma. 

_Responsabilidades y participación_ 

- Registrar y actualizar prendas en el catálogo (fotografías, precios, tabla de tallas, composición textil y especificaciones). 

   - Registrar y actualizar el stock de inventario de productos. 

   - Recibir notificaciones de pedidos. 

- Consultar métricas e informe de ventas y demanda de sus productos. 

### **3.2.2. Administrador** 

Personal interno de la plataforma encargado de la supervisión, gobernanza de datos y mantenimiento operativo de la plataforma. 

_Responsabilidades y participación_ 

- Gestionar cuentas de usuario (habilitar, suspender o moderar las cuentas). 

- Supervisar las publicaciones del catálogo y categorías generales. 

- Configurar parámetros globales del motor de recomendación de tallas y alergias. 

- Consultar y generar dashboards/reportes analíticos de comportamiento de mercado, búsquedas y ventas. 

# **4. PROCESOS DE NEGOCIO** 

- **4.1 Proceso 1: Gestión del Perfil infantil y recomendación personalizada de tallas y prendas** . 

10 

oO 

**Definición del Proyecto** 

### **LittleStyle** 

#### _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
h7.,<br><!-- End of picture text -->

**Página 11 de 20** 

|**Ítem**|**Descripción**|
|---|---|
|**Nombre del**<br>**Proceso**|Gestión del perfil infantil y recomendación personalizada de<br>tallas y prendas.|
|**Objetivo del**<br>**proceso**|Busca permitir al cliente (Padre, madre o acudiente) registrar y<br>administrar múltiples perfiles infantiles asociados a su cuenta<br>para capturar, actualizar y almacenar de forma estructurada los<br>datos del menor, historial de crecimiento, preferencias de estilo<br>y condiciones especiales(como alergias) de los niños.|
|**Alcance**|Inicia cuando el cliente ingresa a la plataforma para crear,<br>seleccionar o editar un/uno de sus perfiles infantiles asociado a<br>su cuenta, registrando los datos del menor y finaliza con la<br>validación, almacenamiento e indexación exitosa de dicha<br>información en la base de datos, habilitando el perfil para la<br>generación de recomendaciones en el sistema.|
|**Entradas**|Identificador de cuenta del cliente, datos del menor o menores<br>(fecha de nacimiento/edad, estatura en cm, peso en kg,<br>contextura corporal), perfil de sensibilidad textil(alergias a fibras<br>sintéticas,lana,tintes,broches<br>metálicos),<br>preferencias<br>estéticas(colores, estampados, holgura).|
|**Salidas**|Ficha de perfil infantil/es creado o actualizado con la<br>información de medidas, preferencias y restricciones derivadas<br>de alergias, junto con el historial de crecimiento registrado.|
|**Actividades/Tareas**<br>**Clave**|-<br>Autenticación del usuario (Cliente) en la plataforma.<br>-<br>Apertura del formulario de creación/modificación de<br>perfil(es) infantil(es).<br>-<br>Consulta del listado de perfiles infantiles asociados a su<br>cuenta personal.<br>-<br>Registro/actualización de medidas físicas y fecha de<br>medición.<br>-<br>Selección de restricciones por alergias textiles y<br>preferencias de estilo.<br>-<br>Validación de consistencia de datos e historial por parte<br>del sistema.<br>-<br>Almacenamiento y guardado de la ficha infantil en la<br>base de datos.|
|**Roles o actores**<br>**involucrados**|Padre, madre o acudiente (Cliente)|
|**Herramientas o**<br>**sistemas usados**|Formulario dinámico de captura de datos con soporte para<br>múltiples perfiles, módulo de gestión de usuarios/perfiles y<br>base de datos relacional.|
|**Frecuencia de**<br>**ejecución**|A demanda (al registrar un menor(es), al cambiar las medidas<br>del niño o al identificar nuevas preferencias/alergias).|
|**Indicadores clave**<br>**(KPIs)**|-<br>Promedio de perfiles infantiles creados por cuenta.<br>-<br>Porcentaje<br>de<br>perfiles infantiles con información<br>completa de alergias e historial de medidas.<br>-<br>Frecuencia de actualización de datos físicos por perfil.|
|**Problemas o**<br>**dolencias actuales**|Compra de ropa en materiales que causan irritaciones o<br>alergias a los niños, falta de un registro centralizado sobre la<br>evolución del crecimiento infantil y selección errónea de<br>prendas por no considerar holgura deseada.|



11 SN=a LeaLongaiiliC gaVi [ 

o 

**Definición del Proyecto** 

### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 12 de 20** 

|**Normativas y**<br>**políticas**<br>**relacionadas**|Ley 1581 de 2012, relacionada con la protección de datos<br>personales y el derecho de los titulares a conocer, actualizar y<br>rectificar su información. Decreto 1074 de 2015, en lo<br>relacionado con el tratamiento de datos personales de niños,<br>niñas y adolescentes y la obligación de contar con políticas<br>para el tratamiento de la información. Asimismo, se aplicarán<br>las políticas de privacidad y tratamiento de datos personales de<br>la plataforma, teniendo en cuenta que el proceso registra<br>información asociada a perfiles infantiles.|
|---|---|
|**Dependencias con**<br>**otros procesos**|Suministra al Proceso 2 (Gestión del Catálogo, Compras y<br>Pedidos):<br>La<br>ficha<br>técnica<br>del<br>menor<br>seleccionado(<br>restricciones por alergias textiles y gustos de cada hijo) para<br>alimentar el motor de recomendación, calcular la talla ideal y<br>validar la seguridad de los materiales en la compra.|
|**Mapa o diagrama**<br>**del proceso**||



# **4.2 Proceso 2: Gestión del catálogo, compras, pedidos e inventario.** 

|**Ítem**|**Descripción**|
|---|---|
|**Nombre del**<br>**Proceso**|Gestión del catálogo,compras, pedidos e inventario.|
|**Objetivo del**<br>**proceso**|Permitir al Vendedor la administración de prendas e inventario<br>en la plataforma (incluyendo el control de existencias y alertas<br>de reabastecimiento), y facilitar al Cliente la exploración del<br>catálogo, consulta de recomendaciones de talla y alergias para<br>el hijo seleccionado, empaquetado en el carrito, realización de<br>la compra y seguimiento del pedido dentro de la plataforma<br>hasta la notificación del envío con número de guía.|
|**Alcance**|Inicia con el registro y administración de productos e inventario<br>por parte del Vendedor (incluyendo la definición de niveles<br>mínimos de stock y la generación de alertas automáticas de<br>reabastecimiento); pasa por la búsqueda, filtrado, consulta de<br>recomendaciones y adición al carrito realizada por el Cliente; y<br>finaliza con la creación formal del pedido, el descuento<br>automático de existencias y la consulta del estado del pedido<br>dentro de la plataforma.|
|**Entradas**|Catálogo registrado por el Vendedor (prendas, fotos, precios,<br>composición textil, tabla de medidas), niveles de inventario y<br>niveles mínimos de stock definidos por el Vendedor, perfil<br>infantil activo seleccionado por el Cliente (medidas, alergias y<br>gustos del Proceso 1), datos de envío y confirmación de la<br>orden.|
|**Salidas**|Publicación de productos, inventario actualizado, alertas<br>automáticas de stock bajo, sugerencias de talla óptima por<br>producto y alergias registradas, carrito de compras actualizado,<br>orden de pedido generada, descuento automático del stock y<br>actualización del estado del pedido con número de guía<br>logística.|
|**Actividades/Tareas**<br>**Clave**|-<br>Registro y actualización de prendas en el catálogo por<br>parte del vendedor (precios, tallas, composición textil).|



12 

**Definición del Proyecto** 



### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 13 de 20** 

- Registro y actualización de las existencias disponibles de cada producto por parte del vendedor. - Definición de niveles mínimos de stock por producto. - Monitoreo automático de la disponibilidad de las prendas y generación de alertas cuando las existencias alcancen o estén por debajo del nivel mínimo establecido. - Elegir el perfil del hijo, buscar y filtrar prendas en el catálogo. - Cruzar medidas/composición para mostrar la "Talla sugerida" y recomendaciones basadas en su preferencia/alergias. - Selección de productos y gestión del carrito de compras. - Confirmación del pedido e ingreso de datos de envío. - Procesamiento y comprobación del pago (interacción transaccional con Pasarela de Pago Externa). - Reserva automática del stock de prendas y notificación de la orden. - Consulta y seguimiento del estado del pedido dentro de la plataforma (registrado, en preparación y entregado), además permitir la cancelación del pedido dentro de un tiempo establecido. - Al cambiar el pedido al estado ENVIADO se enviará un correo electrónico al usuario informando su número de guía. **Roles o actores** Padre, madre o acudiente (Cliente) y Vendedor **involucrados** Módulo e-commerce, catálogo digital con reglas de **Herramientas o** recomendación, módulo de gestión de inventario y alertas, **sistemas usados** motor de carrito de compras, integración API/Webhooks de pago y sistema transaccional de pedidos. Continua/Diaria para el Vendedor (administración de productos e inventario) y a demanda para el Padre (sesiones de búsqueda **Frecuencia de** y compra). Las alertas de stock bajo se generan **ejecución** automáticamente cuando se cumplen las condiciones definidas. - Tasa de conversión (visitas vs compras realizadas por el Padre). - Porcentaje de carritos de compra abandonados. - Tasa de compras realizadas utilizando la talla recomendada. **Indicadores clave** - Tiempo promedio de ciclo de vida del pedido (desde **(KPIs)** registró hasta envío). - Cantidad de productos con niveles de stock bajo. - Porcentaje de productos que requieren reabastecimiento. - Tiempo promedio de respuesta ante alertas de stock bajo. Pérdida de ventas por falta de visibilidad en el catálogo, compra involuntaria de prendas con telas que producen **Problemas o** alergias, falta de seguimiento transparente del pedido, **dolencias actuales** desconocimiento oportuno de productos próximos a agotarse y pérdida de oportunidades de venta por falta de reabastecimiento. 

13 

**Definición del Proyecto** 



### **LittleStyle** 

_Curso: Ingeniería de software III_ 



**Página 14 de 20** 

|**Normativas o**<br>**políticas**<br>**relacionadas**|Ley 1480 de 2011 – Estatuto del Consumidor, especialmente en<br>lo relacionado con la protección del consumidor en las ventas a<br>distancia y mediante comercio electrónico, la información clara<br>sobre los productos, precios, disponibilidad, medios de pago,<br>tiempos de entrega y condiciones aplicables al derecho de<br>retracto. Resolución Andina 2109 de 2019 – Reglamento<br>Técnico<br>Andino<br>para<br>el<br>Etiquetado<br>de<br>Confecciones,<br>relacionada<br>con<br>la información sobre composición de<br>materiales, talla e instrucciones de cuidado de las prendas<br>comercializadas. Adicionalmente, se aplicarán las políticas<br>internas de cancelación de pedidos, devoluciones y tratamiento<br>de pagos de la plataforma.|
|---|---|
|**Dependencias con**<br>**otros procesos**|Recibe del Proceso 1 el perfil e historial del menor seleccionado<br>por el Cliente (incluyendo sus medidas, alergias y gustos).<br>Suministra al Proceso 3 los datos transaccionales de ventas,<br>pedidos, inventario y demanda requeridos para la generación<br>de reportes gerenciales y la supervisión administrativa.|
|**Mapa o diagrama**<br>**del proceso**||



# **4.3 Proceso 3: Gestión administrativa y procesos internos.** 

|**Ítem**|**Descripción**|
|---|---|
|**Nombre del**<br>**Proceso**|Gestión administrativa y procesos internos.|
|**Objetivo del**<br>**proceso**|Garantizar la correcta administración y supervisión de la<br>plataforma mediante la gestión y moderación de cuentas de<br>usuario, la supervisión de publicaciones y productos, y la<br>consolidación de información mediante dashboards y reportes<br>que apoyen la toma de decisiones.|
|**Alcance**|El proceso inicia con la administración de las cuentas y la<br>información<br>disponible<br>en<br>la<br>plataforma, incluyendo la<br>moderación de usuarios y publicaciones por parte del<br>Administrador, y la gestión de categorías generales. Finaliza con<br>la consulta y generación de dashboards y reportes sobre<br>ventas, inventario, productos y comportamiento de la demanda,<br>a partir de la información consolidada de los Procesos 1 y 2.|
|**Entradas**|Cuentas registradas de clientes y vendedores, solicitudes o<br>incidencias<br>relacionadas<br>con<br>usuarios,<br>publicaciones<br>y<br>productos<br>registrados,<br>información<br>histórica<br>de<br>ventas,<br>inventario, pedidos realizados, productos consultados y datos<br>de comportamiento de búsqueda dentro de la plataforma<br>(provenientes del Proceso 2).|
|**Salidas**|Cuentas<br>habilitadas,<br>suspendidas<br>o<br>moderadas<br>según<br>corresponda,<br>publicaciones<br>y<br>productos<br>supervisados,<br>dashboards operativos y reportes sobre ventas, demanda,<br>productos,<br>inventario<br>y<br>comportamiento<br>general<br>de<br>la<br>plataforma. Archivos de reportes generados y descargados en<br>formato PDF.|
|**Actividades/Tareas**<br>**Clave**|-<br>Consultar y gestionar las cuentas registradas de clientes<br>y vendedores.<br>-<br>Supervisar y moderar la información publicada por los|



Reng? 14 =a NGa iil 



### **LittleStyle** 

#### **Definición del Proyecto** 

#### _Curso: Ingeniería de software III_ 



**Página 15 de 20** 

||vendedores en el catálogo.<br>-<br>Gestionar<br>categorías<br>generales<br>y<br>mantener<br>la<br>organización de la información de la plataforma.<br>-<br>Consolidar información generada por ventas, pedidos,<br>inventario y comportamiento de los usuarios.<br>-<br>Generar reportes generales para el Administrador y<br>reportes específicos relacionados con los productos y<br>ventas de cada Vendedor.<br>-<br>Analizar productos de mayor demanda, productos con<br>baja disponibilidad, ventas por categoría y tendencias de<br>búsqueda dentro de la plataforma.<br>-<br>Exportar y descargar reportes consolidados de ventas,<br>demandas por talla e inventario en formato PDF.|
|---|---|
|**Roles o actores**<br>**involucrados**|Administrador|
|**Herramientas o**<br>**sistemas usados**|Módulo de administración de usuarios, sistema de roles y<br>permisos, módulo de moderación de publicaciones, dashboard<br>administrativo y módulo de generación de reportes.|
|**Frecuencia de**<br>**ejecución**|A demanda, de acuerdo con las necesidades de moderación y<br>administración de la plataforma. Los dashboards pueden<br>consultarse continuamente y los reportes se generan de forma<br>periódica o bajo demanda.|
|**Indicadores clave**<br>**(KPIs)**|-<br>Número de cuentas activas, suspendidas o moderadas.<br>-<br>Tiempo<br>promedio<br>de<br>respuesta<br>ante<br>solicitudes/incidencias de moderación.<br>-<br>Productos con mayor y menor rotación.<br>-<br>Productos más consultados y más vendidos.|
|**Problemas o**<br>**dolencias actuales**|Dificultad para supervisar de manera centralizada las cuentas y<br>publicaciones de los usuarios, riesgo de que existan productos<br>con información inadecuada dentro del catálogo, y dificultad<br>para<br>consolidar<br>información<br>sobre ventas, inventario y<br>comportamiento de la demanda para apoyar la toma de<br>decisiones.|
|**Normativas o**<br>**políticas**<br>**relacionadas**|Ley 1581 de 2012 sobre protección de datos personales y<br>Habeas Data; Decreto 1074 de 2015 en lo relacionado con la<br>reglamentación aplicable al tratamiento de datos personales;<br>Ley 1480 de 2011 – Estatuto del Consumidor, relacionada con la<br>protección de los consumidores y la información suministrada<br>en la comercialización de productos; Resolución Andina 2109<br>de 2019 – Reglamento Técnico Andino para el Etiquetado de<br>Confecciones, aplicable a la información que debe acompañar<br>los productos de confección comercializados; políticas internas<br>de privacidad, tratamiento de datos, moderación de usuarios y<br>publicaciones, control de inventario y gestión de accesos de la<br>plataforma.|
|**Dependencias con**<br>**otros procesos**<br>**Mapa o diagrama**|Recibe del Proceso 1 información relacionada con perfiles<br>infantiles y preferencias que puede contribuir al análisis general<br>de demanda, tallas y características de los productos<br>consultados. Recibe del Proceso 2 la información generada por<br>productos, inventario, pedidos, ventas, comportamiento de<br>compra y alertas de stock bajo ya gestionadas en dicho<br>proceso.|



15 

**LittleStyle Definición del Proyecto** 



<!-- Start of picture text -->
o<br><!-- End of picture text -->

_Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
y<br><!-- End of picture text -->

**Página 16 de 20** 

**del proceso** ~~ee~~ 

16 

**LittleStyle** 

**Definición del Proyecto** 

**Página 17 de 20** 



_Curso: Ingeniería de software III_ 



# **5. CRONOGRAMA DE TRABAJO PROPUESTO** 

# **5.1. Metodología del trabajo** 

Para la planificación y ejecución del proyecto _LittleStyle_ se adoptó una metodología ágil basada en **Scrum** , organizando el trabajo en historias de usuarios y sprints, gestionados a través de la plataforma Jira Software. 

Cada uno de los procesos de negocio definidos en el presente documento fue traducido en un conjunto de historias de usuario que abarcan desde el diseño conceptual y arquitectónico hasta el desarrollo de servicios backend, interfaces frontend y pruebas. A su vez cada historia de usuario se descompone en subtareas técnicas con su responsable asignado y rango de fechas . 

El desarrollo se estructura en 3 Sprints principales de la siguiente manera: 

- **Sprint 1 (Diseño Técnico, Arquitectura y Calidad):** Enfocado en la cimentación del proyecto, modelos C4, definición de calidad y diseño UX/UI. 

- **Sprint 2 (Core del Negocio - Cliente y Vendedor):** Enfocado en la autenticación, gestión del perfil infantil, motor de recomendaciones, catálogo, inventario, carrito de compras, pasarela de pago y seguimiento de pedidos. 

- **Sprint 3 (Administración, Inteligencia de Negocio y Despliegue Cloud):** Enfocado en moderación de cuentas, dashboards de métricas, exportación de reportes PDF, pruebas integrales E2E y despliegue final en la infraestructura de AWS. 

# **5.2. Distribución de historias de usuario por sprint** 

|**Sprint**|**Fechas**|**Proceso de**<br>**negocio**<br>**relacionado**|**Historias de usuario**|
|---|---|---|---|
|Sprint 1|07/09/2026 - 21/09/2026|Diseño técnico<br>(transversal)|US-01, US-02, US-03,<br>US-04|
|Sprint 2|21/09/2026 - 18/10/2026|<sup>Proceso 1 y Proceso</sup><br>2|US-05, US-06, US-07,<br>US-08, US-09, US-10,<br>US-11, US-12|
|Sprint 3|19/10/2026 - 15/11/2026|Proceso 3|US-13, US-14, US-15,<br>US-16|



# **5.3. Historias de usuario** 

|**ID**|**Historia de usuario**|**Responsable**|
|---|---|---|
|US01|Formulación de la Definición del Proyecto y<br>Procesos de Negocio|Todos los Integrantes|



17 

**Definición del Proyecto** 

_Curso: Ingeniería de software III_ 



### **LittleStyle** 



**Página 18 de 20** 

|US02|Diseño de la Arquitectura de Software y<br>Modelado C4|Todos los Integrantes|
|---|---|---|
|US03|Estructuración del Plan de Gestión de la<br>Calidad (ISO/IEC 25010)|Todos los Integrantes|
|US04|Diseño de Prototipos de Interfaz de Usuario<br>(Mockups / UX-UI)|Todos los Integrantes|
|US05|Módulo de Autenticación, Registro y<br>Autorización de Usuarios (JWT)|Oscar Tomás<br>Aristizabal V.|
|US06|Gestión Integral del Perfil Infantil (Biometría y<br>Alergias)|Valentina González<br>Diaz|
|US07|Motor de Recomendación Personalizada de<br>Tallas y Materiales|Valentina Rodríguez<br>Castro|
|US08|Gestión de Catálogo de Prendas e Inventario<br>(Vendedor)|Oscar Tomás<br>Aristizabal V.|
|US09|Exploración del Catálogo y Filtros Avanzados<br>para Clientes|Valentina Rodríguez<br>Castro|
|US10|Carrito de Compras y Coordinación del<br>Checkout|Valentina González<br>Diaz|
|US11|Integración con Pasarela de Pago Externa<br>Wompi|Oscar Tomás<br>Aristizabal V.|
|US12|Seguimiento de Estados del Pedido y<br>Notificaciones por Correo|Valentina Rodríguez<br>Castro|
|US13|Gestión Administrativa y Moderación de<br>Cuentas / Contenido|Valentina Rodríguez<br>Castro|
|US14|Dashboard Visual de Métricas e Indicadores<br>de Operación|Oscar Tomás<br>Aristizabal V.|
|US15|Generación y Exportación de Reportes<br>Analíticos en PDF|Valentina Rodríguez<br>Castro|
|US16|Pruebas Integrales E2E, Corrección de<br>Errores y Despliegue en AWS|Valentina González<br>Diaz|



18 

**LittleStyle Definición del Proyecto** _Curso: Ingeniería de software III_ 





**Página 19 de 20** 



<!-- Start of picture text -->
G Littiestyle é<br><!-- End of picture text -->

**Figura 1.** Listado de historias de usuario con responsables, parte 1. 



<!-- Start of picture text -->
G Littlestyle +<br><!-- End of picture text -->

**Figura 2.** Listado de historias de usuario con responsables, parte 2. 



<!-- Start of picture text -->
Cc e<br>US-05: Médulo de Autenticacién, Registro y Autorizacién de Usuarios Sin Iniciar 5 +<br>Peiree) Detalles S<br>@ oscar TOMAS ARISTIZABAL VELA.<br>Subtareas Oo<br>63 18 oct 2026<br><!-- End of picture text -->

**Figura 3.** Visualización de subtareas de una historia de usuario. 

Enlace a Jira: <u>Jira</u> 



<!-- Start of picture text -->
19<br>| 4LG uciaay gL onsn e ny rite<br>= he SS 7 en conexion territorial<br>ae ee \ www. uniquindio. edu.cO<br><!-- End of picture text -->

**PROGRAMA DE INGENIERÍA DE SISTEMAS Y COMPUTACIÓN** 

**Tel: (57) 6 735 9300 Ext 355 Carrera 15 Calle 12 Norte Armenia, Quindío – Colombia ingesis@uniquindio.edu.co** 

