# **LittleStyle Software Architecture Description – C4 Model** 

**Autor:** Valentina Gonzalez Diaz Valentina Rodriguez Castro Oscar Tomas Aristizabal Velasquez 

Versión: 1.0 Fecha: 20/09/26 

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **1. RESUMEN ARQUITECTÓNICO** 

**LittleStyle** es una plataforma digital de e-commerce para ropa infantil, cuyo propósito es centralizar y automatizar la gestión de perfiles infantiles, la recomendación personalizada de tallas, la compra de prendas y el control de inventario mediante canales web diferenciados por rol. El cliente (padre, madre o acudiente) gestiona perfiles infantiles, consulta el catálogo y realiza pedidos; el vendedor administra su catálogo e inventario; y el administrador supervisa la plataforma y gestiona usuarios. El sistema se integra con una pasarela de pago externa para procesar las transacciones. 

#### **1.1 Usuarios principales** 

- **Cliente(Padre,madre o acudiente):** gestiona perfiles infantiles, consulta el catálogo con recomendación de talla, administra el carrito y hace seguimiento a sus pedidos. 

- **Vendedor:** administra su catálogo de prendas, controla el stock, recibe alertas de reabastecimiento y consulta reportes de ventas. 

- **Administrador:** gestiona cuentas de usuarios, supervisa el catálogo y consulta dashboards y reportes analíticos. 

### **1.2 Problema que resuelve** 

LittleStyle resuelve la dificultad de los padres para elegir tallas y prendas adecuadas para sus hijos, dada la variabilidad del crecimiento infantil y posibles alergias textiles. Además, centraliza para los vendedores la administración de productos e inventario, e integra un pago seguro mediante un tercero, mejorando la experiencia del cliente y la eficiencia operativa. 

### **1.3 Alcance** 

#### ¿Qué incluye? 

   - Registro y autenticación de clientes, vendedores y administradores. 

   - Gestión de perfiles infantiles (medidas, alergias, preferencias). 

   - ● Recomendación personalizada de tallas y prendas. 

   - Consulta del catálogo con filtros avanzados. 

   - Gestión del carrito de compras y pedidos. 

   - Seguimiento del estado del pedido. 

   - Integración con pasarela de pago externa. 

   - Administración de productos e inventario, con alertas de stock bajo. 

   - Gestión de usuarios, catálogo y generación de reportes/dashboards. 

- ¿Qué **NO** incluye? 

   - Gestión interna de manufactura o confección textil. 

   - Operación logística/transporte directo de paquetería externa (solo se actualizan estados y número de guía). 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





### **1.4 Procesos involucrados en el sistema LittleStyle** 

- **Proceso de gestión del perfil infantil y recomendación de tallas** 

**Objetivo:** permitir al cliente registrar y administrar perfiles infantiles. 

_Incluye:_ 

   - Registro/actualización de medidas y fecha de medición. 

   - Selección de alergias textiles y preferencias. 

   - Validación y almacenamiento del perfil. 

- **Proceso de gestión del catálogo,compras,pedidos e inventario** 

**Objetivo:** permitir al vendedor administrar su catálogo de prendas e inventario. 

_Incluye:_ 

- Registro y actualización de prendas (fotos, precios, tallas). 

- Publicación de productos en el catálogo. 

- ○ Registro/actualización de stock y niveles mínimos. 

- Generación automática de alertas de stock bajo. 

**Objetivo:** permitir al cliente comprar y hacer seguimiento a su pedido. 

_Incluye:_ 

Búsqueda, filtrado y recomendación de talla. Carrito de compras y confirmación del pedido. Pago mediante pasarela externa. 

Reserva de stock y seguimiento del estado del pedido. 

- **Proceso de gestión de procesos internos** 

**Objetivo:** garantizar la administración y moderación de cuentas y publicaciones. 

_Incluye:_ 

- Gestión y moderación de cuentas (clientes y vendedores). 

- Supervisión de publicaciones del catálogo. 

**Objetivo:** consolidar información para la toma de decisiones. _Incluye:_ 

- Generación, visualización y exportación descargable de dashboards y reportes de ventas, inventario y demanda en formato PDF. 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
o<br><!-- End of picture text -->



<!-- Start of picture text -->
4...<br><!-- End of picture text -->

## **2. C4MODEL** 

El sistema **LittleStyle** contempla las interacciones de tres roles de usuario principales (Cliente, Vendedor y Administrador) y servicios externos integrados. El modelo C4 representa el alcance completo de la plataforma, detallando el frontend, backend, la base de datos y las integraciones externas. Cabe aclarar que las operaciones logísticas externas de paquetería y manufactura no forman parte del alcance directo del sistema, limitándose la plataforma al registro de guías de envío y actualización de estados del pedido. 

### **2.1 Diagrama de contexto (N1)** 

Presenta el sistema LittleStyle como “caja negra”, usuarios y sistemas externos. 

- **Cliente (Padre,  madre o acudiente)** 



<!-- Start of picture text -->
Cliente<br>[Persona] Envía correos<br>Un usuario (padre, madre o<br>acudiente) que registra perfiles  de<br>infantiles, explora prendas, recibe  confirmación y<br>talla/alergias, realiza pedidos y recomendaciones de  guía de envío a<br>hace seguimiento a sus compras.<br>Realiza pedidos, paga, Servicio de Correo<br>gestiona perfiles Gmail<br>infantiles y consulta [Sistema de Software]<br>estado.<br>Servicio en la nube que gestiona el envío<br>de confirmaciones de pedido,<br>Envía actualizaciones de estado y guía de<br>transporte al cliente.<br>notificaciones<br>(push/correo)<br>LittleStyle<br>[Sistema de Software]<br>Plataforma e-commerce web que permite<br>la gestión de perfiles infantiles,<br>recomendación de tallas, catálogo, carrito<br>de compras y seguimiento de pedidos.<br>Procesa pago /<br>autoriza Pasarela de Pagos<br>transacción<br>Wompi<br>[Sistema de Software]<br>Se encarga del procesamiento de pagos<br>en línea y notifica el estado de la<br>transacción a través de Webhooks.<br>Confirmación<br>del pago a<br><!-- End of picture text -->



<!-- Start of picture text -->
<SN — NSSae/ Ly UNIQUINDLOUND UM<br>\ vil www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
o<br><!-- End of picture text -->



<!-- Start of picture text -->
4...<br><!-- End of picture text -->

### **2.2 Diagrama de contexto (N2)** 

Presenta el sistema LitlleStyle como “caja negra”, usuarios y sistemas externos. 

#### ● **Vendedor** 



<!-- Start of picture text -->
Vendedor<br>[Persona] Envía alertas de<br>Worker de negocio encargado de  stock bajo y<br>administrar el catálogo de ropa<br>aviso de<br>infantil, gestionar el stock, definir<br>alertas de inventario y consultar  nuevos pedidos<br>reportes.<br>Publica prendas, Servicio de Correo<br>actualiza inventario y Gmail<br>consulta reportes [Sistema de Software]<br>Servicio en la nube que gestiona el envío<br>de notificaciones de stock<br>Solicita envío de bajo/reabastecimiento y aviso de nuevos<br>pedidos al vendedor.<br>alertas y<br>notificaciones<br>LittleStyle<br>[Sistema de Software]<br>Plataforma e-commerce web que provee<br>panel de vendedor, gestión de stock,<br>alertas automáticas y generación de<br>métricas de venta.<br><!-- End of picture text -->

### **2.3 Diagrama de contexto (N3)** 

Presenta el sistema LitlleStyle como “caja negra”, usuarios y sistemas externos. 

#### ● **Administrador** 



<!-- Start of picture text -->
Administrador<br>[Persona] Envía reportes<br>Personal interno encargado de  de sistema y<br>moderar usuarios y<br>alertas<br>publicaciones, gestionar<br>categorías generales y supervisar  administrativas<br>métricas globales del sistema.<br>Gestiona cuentas, Servicio de Correo<br>modera catálogo y Gmail<br>analiza dashboards [Sistema de Software]<br>Servicio en la nube que gestiona el envío<br>de avisos de moderación de cuentas y<br>Solicita envío de notificaciones administrativas del<br>sistema.<br>avisos de<br>moderación y<br>LittleStyle reportes<br>[Sistema de Software]<br>Plataforma e-commerce web que provee<br>panel de administración centralizado,<br>control de accesos y analítica global de la<br>operación.<br><!-- End of picture text -->



<!-- Start of picture text -->
FoaA .  SilanyCZ / en conexiouoNauNDwww uniqui dio.edu.co n  terri orial t o<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
o<br><!-- End of picture text -->



<!-- Start of picture text -->
4...<br><!-- End of picture text -->

### **2.4 Diagrama de contenedores (N1)** 

Presenta “qué partes ejecutables” tiene el sistema LittleStyle (web, API, BD, app móvil, colas, etc.). 

- **Cliente(Padre,madre o acudiente)** 



<!-- Start of picture text -->
Envía correos<br>electrónicos y<br>Cliente<br>mensajes de<br>confirmaciones<br>.<br>Explora productos,gestiona<br>Servicio de<br>perfiles infantiles, agrega<br>productos y realiza pedidos. Correo<br>[Sistema de Software:<br>Gmail]<br>Envía solicitudes para<br>generar correos y<br>Web App notificaciones<br>[Container: Angular  [HTTPS/API]]<br>SPA]<br>Realiza llamadas API<br>Pasarela de<br>[HTTPS/JSON (REST) / JWT]<br>Pagos<br>[Sistema de<br>Software:Wompi]<br>Realiza<br>>_Backend<br>llamadas API a<br>[Container: Java  la Pasarela de<br>(Spring Boot)]<br>[Monolito modular] Pagos usando<br>[Maven + JWT] [HTTPS/API de<br>pagos]<br>Lee y escribe en la Notifica el<br>base de datos resultado de la<br>[SQL/JPA] transacción<br>[HTTPS/Webhook]<br>Consulta las fotos<br>de los productos<br>[HTTPS/API]<br>Database<br>STATEMENT<br>[Container: MySql]<br>[AWS] STORE<br>[Container:Amazon S3]<br>Sistema de Comercio Electrónico LittleStyle<br><SN — NSSae/ Ly UNIQUINDLOUND UM<br>\ vil www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
o<br><!-- End of picture text -->



<!-- Start of picture text -->
4...<br><!-- End of picture text -->

### **2.5 Diagrama de contenedores (N2)** 

Presenta “qué partes ejecutables” tiene el sistema LittleStyle (web, API, BD, app móvil, colas, etc.). 



<!-- Start of picture text -->
● Vendedor<br>Envía correos<br>electrónicos y<br>Vendedor<br>mensajes de<br>confirmaciones<br>Gestiona productos,<br>Servicio de<br>inventarios y pedidos.<br>Correo<br>[Sistema de Software:<br>Gmail]<br>Envía solicitudes para<br>generar correos y<br>Web App notificaciones<br>[Container: Angular  [HTTPS/API]]<br>SPA]<br>Realiza llamadas API<br>[HTTPS/JSON (REST) /JWT]<br>>_Backend<br>[Container: Java<br>(Spring Boot)]<br>[Monolito modular]<br>[Maven + JWT]<br>Lee y escribe en la<br>Sube y actualiza las fotos de  base de datos<br>las prendas [HTTPS/API]<br>[SQL/JPA]<br>Database<br>STATEMENT<br>[Container: MySql]<br>STORE<br>[AWS]<br>[Container: Amazon S3]<br>Sistema de Comercio Electrónico LittleStyle<br><SN — NSSae/ Ly UNIQUINDLOUND UM<br>\ vil www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
o<br><!-- End of picture text -->



<!-- Start of picture text -->
huc€,<br><!-- End of picture text -->

### **2.6 Diagrama de contenedores (N3)** 

Presenta “qué partes ejecutables” tiene el sistema LittleStyle (web, API, BD, app móvil, colas, etc.). 

- **Administrador** 



<!-- Start of picture text -->
Envía correos<br>electrónicos y<br>Administrador<br>mensajes de<br>confirmaciones<br>a<br>Gestiona Servicio de<br>usuarios,productos,categori<br>Correo<br>as,catalogo, consulta de [Sistema de Software:<br>reportes y métricas Gmail]<br>Envía solicitudes para<br>generar correos y<br>Web App notificaciones<br>[Container: Angular  [HTTPS/API]]<br>SPA]<br>Realiza llamadas API<br>[HTTPS/JSON (REST) / JWT]<br>>_Backend<br>[Container: Java<br>(Spring Boot)]<br>[Monolito modular]<br>[Maven + JWT]<br>Consulta las fotos<br>Lee y escribe en la<br>de los productos<br>base de datos<br>[SQL/JPA] para moderación<br>[HTTPS/API]<br>STATEMENT<br>Database<br>STORE<br>[Container: MySql]<br>[AWS] [Container: Amazon<br>S3]<br>Sistema de Comercio Electrónico LittleStyle<br>=Set Sae/ £B UNICUMNDIOUND UM<br>\ vil www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
a<br><!-- End of picture text -->



<!-- Start of picture text -->
kd.<br><!-- End of picture text -->

### **2.7 Diagrama de componentes (N1)** 

Presenta el interior de los componentes de LittleStyle (por ejemplo, el Backend/API). Separados por tipo de usuario 



<!-- Start of picture text -->
Web App Servicio de<br>[Container: Angular] Notificaciones y<br>Correo<br>[Sistema de Software]<br>Envía Accede las<br>credenciales funcionalidades<br>del vendedor<br>API de<br>API de  Envía<br>Inicio de  Vendedores notificaciones<br>Sesión (JSON/HTTPS)<br>Aplica<br>credencialesValida negocio dereglas deproducto<br>Gestión de<br>Gestión de  Reportes y<br>productos Analitica Carrito y<br>pedidos<br>Notifica al Notifica al<br>vendedor vendedor<br>cuando cuando se<br>hayan hace un<br>reportes pedido<br>Componente de<br>Notificaciones<br>Componente<br>de seguridad<br>Lee/escribe<br>usuarios/sesiones Gestión de<br>Inventario<br>Backend<br>Consulta las fotos de<br>los productos para<br>lee/escribe moderación<br>actualizaciones en lee productos/escribe [HTTPS/API]<br>en<br>STATEMENT<br>STORE<br>Database [Container: Amazon<br>S3]<br>[Container: MySql<br>2 Database Schema]<br>a<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 



<!-- Start of picture text -->
e<br><!-- End of picture text -->



<!-- Start of picture text -->
kz.<br><!-- End of picture text -->

### **2.8 Diagrama de componentes (N2)** 

Presenta el interior de los componentes de LittleStyle (por ejemplo, el Backend/API). Separados 



<!-- Start of picture text -->
Web App Servicio de<br>[Container: Angular] Notificaciones<br>y Correo<br>[Sistema de Software]<br>Accede las<br>funcionalidades<br>del cliente<br>Envía<br>notificaciones<br>(JSON/HTTPS)<br>API de<br>Clientes<br>Aplica<br>reglas de Gestión de<br>negocio delcarrito perfiles<br>Gestión de<br>Carrito y pedidos productosal carroAgrega Componente de Catálogo<br>Consulta<br>disponibi<br>lidad Motor de<br>recomendación<br>Gestión de<br>Inventario<br>Solicita Gestión de<br>pagos pagos<br>Notificar<br>Acerca del Componente de<br>pedido Notificaciones<br>Backend<br>Realiza<br>llamadas API a<br>lee perfiles en/escribe Consulta las fotos de los perfiles para  Pagos usandola Pasarela de<br>moderación  [HTTPS/API de<br>[HTTPS/API]  pagos]<br>escribe Database<br>pedidos en [Container: MySql  Pasarela de<br>Database Schema] Pagos<br>[Sistema de Software:<br>Wompi]<br>STATEMENT<br>STORE<br>[Container: Amazon<br>S3]<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





### **2.9 Diagrama de componentes (N3)** 

Presenta el interior de los componentes de LittleStyle (por ejemplo, el Backend/API). Separados 



<!-- Start of picture text -->
Servicio de<br>Web App Notificaciones y<br>[Container: Angular] Correo<br>[Sistema de Software]<br>“—<br>Accede las<br>funcionalidades<br>del<br>administrador<br>Envía<br>notificaciones<br>(JSON/HTTPS)<br>API de<br>Administradores<br>Gestión de<br>Reportes y  Componente<br>usuarios<br>Analitica de Catálogo<br>Jot]<br>Motor de<br>recomendación<br>Componente de  Gestión de<br>Notificaciones pagos<br>Backend<br>Realiza<br>llamadas API a<br>lee/escribe Database lee/escribe la Pasarela de<br>reportes usuarios Pagos usando<br>Database Schema][Container: MySql  [HTTPS/API de<br>pagos]<br>Consulta las fotos de<br>los productos para  Pasarela de<br>moderación  Pagos<br>[HTTPS/API]  [Sistema de Software:<br>Wompi]<br>STATEMENT<br>STORE<br>[Container: Amazon<br>S3]<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





### **2.10 Código (N4)** 

### **2.10.1 Estructura del repositorio y módulos** 

El sistema se implementa en dos módulos principales alineados con el Nivel 2 (Contenedores): una aplicación móvil Android (Java) como canal del cliente y un Backend en Java (Spring Boot) que centraliza la lógica de negocio e integraciones externas. 

**Estructura (árbol):** 

/frontend/ /angular.json (Configuración CLI) /package.json (Dependencias de npm) /package-lock.json /tsconfig.json /Dockerfile (Servidor Nginx) /.gitignore /README.md /src/ /index.html /main.ts /styles.scss /assests/ (Íconos y gráficos locales) /app/ /app.config.ts (Proveedores locales) /app.routes.ts (Rutas principales) /app.component.ts /core/ (Servicios singleton e interceptores) /guards/ (Restricciones) /interceptors/ /services/ /shared/ (UI reutilizable) /components/ /pipes/ /directives/ /features/ (Módulos de vista) /auth/ (Autenticación) /pages/ /services/ /auth.route.ts /perfilesInfantiles/ (Proceso 1) /pages/ /services/ /perfiles.route.ts /catalogoCompras/ (Proceso 2) /pages/ /services/ /catalogoCompras.route.ts /inventario_Dashboards/ (Proceso 3) /pages/ /services/ /inventario_dashboards.route.ts 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





### **2.10 Código (N4)** 

#### **Estructura (árbol):** 

/backend/ 

/pom.xml /Dockerfile /jenkinsfile /.gitignore /README.md /mvnw / mvnw.cmd /src/main/java/com/littlestyle/api/ 

/config/           (configuraciones globales de infraestructura, Spring Security, JWT) /shared/           (Componentes transversales reutilizables) /exception           (Excepciones) /dto           (Transporte de datos reutilizable) /util           (SecurityUtils.JAVA) /modules/           (Módulos de negocio) /auth/           (Módulo 1: Autenticación y Gestión de Usuarios ) /controller (Endpoints) /service (Lógica de negocio) /repository (Comunicación con BD) /model (clases) /dto (Transporte de datos) /perfilesInfantiles/ (Módulo 1: Corresponde al proceso de negocio 1) /controller (Endpoints) /service (Lógica de negocio) /repository (Comunicación con BD) /model (clases) /dto (Transporte de datos) /catalogoCompras/ (Módulo 2: Corresponde al proceso de negocio 2) /controller (Endpoints) /service (Lógica de negocio) /repository (Comunicación con BD) /model (clases) /dto (Transporte de datos) /administracion_dashboards/(Módulo 1: Corresponde al proceso 3) /controller (Endpoints) /service (Lógica de negocio) /repository (Comunicación con BD) /model (clases) /dto (Transporte de datos) 

/src/test/ 

/src/main/resources/ application.yml application-dev.yml application-prod.yml /docs/ C4Model/ arquitectura/ decisiones-arquitectonicas/ 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-01: Canal de usuario mediante aplicación web frontend en Angular** 

- **Decisión:** Desarrollar la interfaz cliente/usuario como una Single Page Application (SPA). 

- **Contexto:** Los tres usuarios del sistema (Clientes, Vendedores y Administradores) requieren acceder desde exploradores web para gestionar perfiles infantiles, catálogo, inventario, pedidos y dashboards. 

- **Alternativas:** Aplicación web tradicional con renderizado en servidor. 

- **Justificación:** Permite un marco de trabajo completo con tipado fuerte, inyección de dependencias y manejo de formularios reactivos. 

- **Consecuencias:** Se requiere configurar el enrutamiento del lado del cliente, implementar Guards para restringir vistas según el rol y manejar interceptores HTTP para adjuntar el token JWT. 

### **ADR-02: Backend estructurado en una arquitectura Monolítica Modular con Spring Boot** 

- **Decisión:** Construir el backend central como una aplicación de Monolito Modular utilizando Java con Spring Boot. 

- **Contexto:** Se requiere una API REST que gestione la lógica de negocio dividida en módulos bien definidos. 

- **Alternativas:** Arquitectura en capas. 

- **Justificación:** El monolito modular permite mantener la lógica organizada por paquetes/módulos independientes con límites claros, facilitando el desarrollo y despliegue unificado. 

- **Consecuencias:** Se debe mantener una rigurosa separación de dependencias entre módulos dentro del proyecto Java para evitar un alto acoplamiento. 

### **ADR-03: Gestión de dependencias y ciclo de vida del proyecto con Apache Maven** 

- **Decisión:** Adoptar _Apache Maven_ como herramienta de gestión de dependencias y automatización del proceso de compilación (build) 

- **Contexto:** El backend requiere integrar múltiples módulos y librerías de Spring (Spring Security, Spring Data JPA, Spring Mail) e integraciones externnas de forma automatizada y estructurada. 

- **Alternativas:** Gradle. 

- **Justificación:** Maven proporciona una estructura de proyecto estandarizada, una gestión clara mediante el archivo _pom.xml_ y una amplia compatibilidad con entornos de integración y despliegue continuo (CI/CD). 

- **Consecuencias:** Se debe mantener una correcta gestión de las versiones de dependencias en el archivo pom.xml para prevenir conflictos de transitividad. 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-04: Autenticación y Autorización basada en JSON Web Tokens (JWT)** 

- **Decisión:** Implementar un mecanismo de seguridad sin estado ( _stateless)_ mediante tokens JWT integrados con Spring Security. 

- **Contexto:** La API REST del backend debe autenticar solicitudes HTTP y validar el rol del usuario de manera segura y eficiente. 

- **Alternativas:** Protocolo OAuth2/OpenID Connect 

- **Justificación:** Al ser una API consumida por una SPA en Angular, JWT evita guardar estado de sesión en el servidor, permitiendo validar permisos mediante la firma digital en cada petición sin realizar consultas continuas a la base de datos. 

- **Consecuencias:** No es posible revocar un token JWT emitido antes de su tiempo de expiración a menos de que se implemente una lista negra en caché/memoria. 

### **ADR-05: Persistencia de datos relacional con MySQL** 

- **Decisión:** Utilizar MySQL como motor de base de datos relacional. 

- ● **Contexto:** La plataforma debe almacenar información estructurada con relaciones complejas e integridad transaccional. 

- **Alternativas:** PostgreSQL. 

- **Justificación:** MySQL ofrece un rendimiento óptimo en transacciones ACID. 

- **Consecuencias:** Se requiere un diseño eficiente del esquema relacional, con índices adecuados para garantizar tiempos de respuesta rápidos en las  en las consultas. 

### **ADR-06: Integración con Pasarela de Pagos Wompi y recepción de eventos mediante Webhook** 

- **Decisión:** Delegar la recepción y procesamiento de pagos con tarjetas/PSE a la pasarela Wompi, sincronizando el estado de las transacciones mediante un endpoint Webhook en el backend. 

- **Contexto:** Los usuarios deben pagar sus pedidos dentro del flujo de compra y la plataforma debe actualizar automáticamente el estado del pedido. 

- **Alternativas:** Verificación manual o por consulta periódica. Procesamiento directo de datos de tarjeta en el backend (se requiere certificación PCI-DSS) 

- **Justificación:** Wompi abstra el cumplimiento de PCI-DSS para el manejo de tarjetas. La arquitectura orientada a Webhooks permite que Wompi notifique en tiempo real al backend cuando un pago es aprobado, garantizando la actualización automática de pedidos e inventario. 

- **Consecuencias:** El backend debe validar la firma de seguridad del Webhook para evitar peticiones maliciosas y controlar la re-notificación de eventos duplicados. 



<!-- Start of picture text -->
< ReurZ!7 FF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-07: Notificaciones transaccionales vía correo electrónico con Servicio Gmail (SMTP)** 

- **Decisión:** Integrar el módulo de notificaciones transaccionales del backend con el Servicio de correo de Gmail mediante protocolo SMTP. 

- **Contexto:** El sistema debe notificar por correo electrónico a los Clientes sobre la confirmación de compras y el envío del paquete con su número de guía, asi como emitir alertas de stock bajo a los Vendedores. 

- **Alternativas:** Amazon SES, SendGrid, Mailgun. 

- **Justificación:** Facilita la configuración y pruebas de desarrollo sin incurrir en costos iniciales de servicios transaccionales dedicados, permitiendo enviar plantillas de correo automatizadas desde Spring Boot. 

- **Consecuencias:** Se deben gestionar contraseñas de aplicación o credenciales de forma segura (variables de entorno) y tener en cuenta los límites diarios de envío del servicio de Gmail. 

### **ADR-08: Acceso a datos mediante Mapeo Objeto-Relacional con Spring Data JPA / Hibernate** 

- **Decisión:** Utilizar Spring DATA JPA (con Hibernate como proveedor ORM) para la capa de accesos a datos del backend. 

- **Contexto:** El backend necesita interactuar con MySQL para realizar operaciones CRUD y consultas complejas de manera segura y abstracta. 

- **Alternativas:** JDBC Template puro, SQL nativo en repositorios. MyBatis. 

- **Justificación:** Reduce el código repetitivo, automatiza el mapeo de tablas a entidades Java, previene vulnerabilidad de Inyección SQL y proporciona soporte nativo para paginación y ordenamiento. 

- **Consecuencias:** Se debe supervisar el rendimiento de las consultas y configurar adecuadamente la carga perezosa para evitar problemas de rendimiento. 





<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-09: Amazon Web Services (AWS) como Plataforma de Despliegue** 

- **Decisión:** Seleccionar Amazon Web Services (AWS) como el proveedor principal de infraestructura en la nube para el alojamiento, despliegue y gestión de recursos del sistema LittleStyle. 

- **Contexto:** La plataforma requiere una infraestructura en la nube altamente disponible, segura y escalable para alojar la base de datos y ejecutar las instancias del backend y frontend. 

- **Alternativas:** Microsoft Azure, Google Cloud Platform. 

- **Justificación:** AWS ofrece integración nativa de diferentes servicios simplificando la configuración de despliegue automatizado desde jenkins. Además cuenta con alta disponibilidad global y herramientas de gestión de identidad muy robustas. 

- **Consecuencias:** Se debe configurar AWS IAM para gestionar roles y permisos de forma segura, definir variables de entorno protegidas dentro de los pipelines de CI/CD para evitar la exposición de credenciales. 

### **ADR-10: Almacenamiento multimedia externo en Amazon S3 y referencia mediante URLs en Base de Datos.** 

- **Decisión:** Seleccionar Amazon S3 como la plataforma de almacenamiento de objetos externos para alojar las imágenes del catálogo, guardando exclusivamente las direcciones URL en la base de datos. 

- **Contexto:** La plataforma requiere almacenar y servir archivos multimedia de forma rápida sin sobrecargar el motor relacional de base de datos ni saturar el servidor de aplicaciones backend con el envío de archivos estáticos. 

- **Alternativas:** Cloudflare R2, Backblaze B2. 

- **Justificación:** Amazon S3 se integra de forma nativa con la infraestructura principal alojada en AWS. Delega la descarga directa de imágenes desde S3 hacia el cliente en Angular optimizando el uso del ancho de banda del backend en Spring Boot. 

- **Consecuencias:** El backend en Spring Boot debe incluir el SDK de AWS para procesar la subida, validación y borrado de imágenes. Se deben configurar políticas de lectura pública en el bucket S3 para servir los archivos directamente sobre HTTPS. 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-11:  Selección de JasperReports para la generación y Exportación de Reportes en PDF** 

- **Decisión:** Adopción de JasperReports Engine integrado en el backend como motor centralizado para el diseño, compilación y generación de reportes analíticos y operativos exportables en formato PDF. 

- **Contexto:** Los vendedores y administradores requieren exportar consolidados de ventas, métricas de inventario, demandas por talla y productos más buscados en un formato impreso y ejecutivo en PDF. 

- **Alternativas:** iText, Apache PDFBox. 

- **Justificación:** Permite maquetar la estructura en plantillas evitando la contaminación del código fuente con coordenadas o píxeles y se conecta de forma nativa con colecciones de DTOs y repositorios de Spring Data JPA. 

- **Consecuencias:** Registro de dependencias. 

### **ADR-12: Patrón de Diseño Strategy para el Motor de Recomendaciones de Tallas y Filtrado de Sensibilidad Textil** 

- **Decisión:** Implementar el patrón de diseño Strategy en la capa de servicio del backend para desacoplar las reglas de cálculo biométrico de tallas y los algoritmos de filtrado/exclusión de telas por alergias sensibles. 

- **Contexto:** El proceso de negocio uno requiere evaluar las medidas de los niños cruzadas con las tablas del fabricante, así como aplicar restricciones por alergias textiles. Estas reglas pueden variar según la categoría del producto o la marca. 

- **Alternativas:** Condiciones complejas como if-else o switch centralizadas en una sola clase de servicio. 

- **Justificación:** Permite encapsular cada algoritmo de recomendación y regla de filtrado en clases independientes que implementan una interfaz común. Esto facilita la adición o modificación de algoritmos sin alterar el código existente. 

- **Consecuencias:** Aumenta ligeramente la cantidad de clases pero elimina el acoplamiento y previene errores de regresión. 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-13: Patrón Data Transfer Object (DTO) para el Desacoplamiento entre Entidades de Dominio y la API REST** 

- **Decisión:** Adoptar el patrón DTO en la totalidad de las comunicaciones entre el frontend y los controladores REST del backend. 

- **Contexto:** La base de datos gestiona entidades complejas con relaciones ORM. exponer estas entidades directamente a la API REST genera riesgos de seguridad y cuellos de botella por serialización. 

- **Alternativas:** Serialización y exposición directa de las entidades JPA anotadas con @Entity hacia el frontend. 

- **Justificación:** Oculta campos sensibles de la base de datos garantizando el cumplimineto de Habeas Data. Mantiene la compatibilidad estandarizada de contratos JSON entre Angular y Spring Boot. 

- **Consecuencias:** Se requiere implementar mapeadores para convertir  entre DTOs y Entidades de JPA. 

### **ADR-14: Patrón Observer / Event-Driven para la Confirmación de Transacciones y Notificaciones** 

- **Decisión:** Utilizar el patrón Observer para procesar de forma asíncrona y desacoplada las acciones derivadas de la aprobación de una compra. 

- **Contexto:** Cuando la pasarela Wompi confirma un pago mediante un Webhook, el sistema debe ejecutar múltiples acciones consecutivas: actualizar el estado del pedido, descontar el stock del vendedor, generar la notificación por correo electrónico y registrar la auditoría administrativa. 

- **Alternativas:** Ejecución procedimental secuencial dentro del controlador del Webhook de pagos. 

- **Justificación:** Desacopla la recepción del Webhoo de las tareas secundarias. Si el servicio de correo presenta lentitud, no afectará la confirmación del pago ni la actualización inmediata del inventario. Además, mantiene la resiliencia y la tolerancia a fallos del sistema. 

- **Consecuencias:** Requiere gestionar transacciones de base de datos asíncronas y manejar políticas de reintento en listeners que fallen. 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-15: Patrón Facade para la Coordinación del Proceso de Checkout y Creación de Pedidos** 

- **Decisión:** Implementar un servicio facade en el módulo de catálogo y compras que organice la interacción entre los submódulos de perfiles, inventario, catálogo, pasarela de pago y pedidos. 

- **Contexto:** El flujo de compra involucra validar la disponibilidad de productos en inventario, consultar restricciones del perfil infantil activo, liquidar subtotales e impuestos y comunicarse con la pasarela de pagos. 

- **Alternativas:** Permitir que los controladores HTTP consuman e interconectan directamente múltiples repositorios y servicios de diferentes módulos. 

- **Justificación:** Ofrece una interfaz simplificada y unificada para el controlador de checkout. Oculta la complejidad de la interacción entre subsistemas y mantiene la separación estricta de responsabilidades en la arquitectura monolítica modular. 

- **Consecuencias:** La clase Facade debe mantenerse cohesionada para no volverse una clase con exceso de responsabilidad (God Object). 

### **ADR-16: Patrón Factory Method para el Módulo de Generación y Exportación de Reportes Analíticos** 

- **Decisión:** Aplicar el patrón Factory Method para la creación dinámica de generadores de documentos en el módulo de administración. 

- **Contexto:** El proceso de negocio tres hace que administradores y vendedores puedan exportar los reportes en formatos estructurados. 

- **Alternativas:** Estructuras condicionales dentro de los servicios web para formatear manualmente las respuestas en cada formato. 

- **Justificación:** Centraliza la lógica de instanciación del exportador correctamente según el parámetro solicitado en la petición de la API. Además, cumple ciertos principios SOLID facilitando la incorporación futura de nuevos formatos sin alterar los controladores de reporte. 

- **Consecuencias:** Requiere definir una interfaz común que estandarice la construcción del archivo descargable. 



<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **3. DECISIONES DE ARQUITECTURA (ADRs)** 

### **ADR-17: Patrón Interceptor / Chain of Responsability para la Seguridad y Manejo de Peticiones** 

- **Decisión:** Implementar el patrón Chain of Responsability mediante la cadena de filtros de Spring Security en el backend y HTTP Interceptors en Angular. 

- **Contexto:** Cada petición recibida por la API debe ser interceptada para validar el token JWT, verificar los permisos del rol y sanitizar las entradas contra inyecciones SQL. En el frontend, cada solicitud saliente debe adjuntar automáticamente el encabezado. 

- **Alternativas:** Validar manualmente la autenticación y permisos dentro de cada método de cada controlador REST. 

- **Justificación:** Permite procesar de forma transversal e independiente los aspectos de seguridad antes de que la petición llegue a la lógica de negocio. 

- **Consecuencias:** Requiere una configuración rigurosa en Spring Security para evitar bloqueos no deseados en endpoints públicos. 





<!-- Start of picture text -->
< RenZgl7 ZF<br>” =a<br>\ ii) www.uniquindio.edu.co<br><!-- End of picture text -->

**LittleStyle Software Architecture Description – C4 Model** _Curso: Ingeniería de software III_ 





## **4. STACK TECNOLÓGICO DEL SISTEMA** 

En esta sección se describen las tecnologías utilizadas para la implementación del sistema LittleStyle, organizadas por capa: cliente móvil (frontend), backend y base de datos. Esta descripción es coherente con la arquitectura presentada en los diagramas C4 y corresponde al alcance definido para el rol del cliente. 



<!-- Start of picture text -->
FrontEnd BackEnd<br>I I I<br>f TTT Ty rN<br>I - HTML (diseño de  !II<br>I layouts y vistas)<br>-TypeScript (lógica de  ! I a - Java I<br>interfaz, eventos,<br>S| ! I I<br>= ! an </> 3 !<br>I navegación) e—>) I<br>-  CSS( apariencia visual,<br>I I I<br>colores, letras)<br>Frameworks / Tools <—-- Frameworks ]<br>I ram l . !<br>I TAL I 1 II Orvctspring |;<br>I —_—_—_J ! ->i I<br>wee eee --_7 | Noe ee —_~__7<br>t | 7 |<br>a|<br>|<br>/ CI/CD Nt \<br>7 S I Base de datos I<br>! Commit -> Build -> Test \‘11<br>- SQL<br>I Artifact -> Deploy -> Monitoring  1! 1<br>(Relacional)<br>| i! I<br>|<br>7 Tools ,;<br>I 1<br>LP 7 | Frameworks |<br>N ,II<br>LON N x @ @/c 7 |I :I<br>l 1<br>Noe eZ<br>Cloud Service Provider<br>ee<br><!-- End of picture text -->



<!-- Start of picture text -->
A  =~)—~S8 SS yi}eX i | SZ a en conexionUNIQUINDLO~ www.uniquindio.edu.coterritorial<br><!-- End of picture text -->

i 



<!-- Start of picture text -->
SS 4 3 y “a<br>| '<br><!-- End of picture text -->

UNIQUIND{O, en conexidn territorial Carrera 15 Calle 12 Norte Tel: (606) 7 35 93 00 Armenia - Quindio - Colombia 

www.uniquindio.edu.co 

