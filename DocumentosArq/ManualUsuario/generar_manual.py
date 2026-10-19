"""Genera el Manual de Usuario de LittleStyle (manual.html) a partir de las capturas reales de la carpeta
capturas/ y de las cajas de marcas.json. Con --pdf exporta además el PDF usando Chrome (Playwright).

    python generar_manual.py --pdf
"""
import html, json, pathlib, sys

AQUI = pathlib.Path(__file__).resolve().parent
MARCAS = json.loads((AQUI / 'capturas' / 'marcas.json').read_text(encoding='utf-8'))
PDF = AQUI.parent / '0.3 Manual de Usuario LittleStyle.pdf'
URL = 'littlestyle.duckdns.org'
PW, PH = 1280, 720
CHROME_BARRA = 26  # alto de la barra de navegador dibujada sobre cada captura

e = html.escape


# ---------------------------------------------------------------- piezas de contenido

def img(src, ruta, crop=None, usar=None, extra=(), pie=None, pos=None):
    """crop=(x0, y0, x1, y1) en px CSS de la captura; usar=lista de marcas automáticas a mostrar;
    extra=[(n, x, y, w, h)] marcas manuales en px CSS."""
    return dict(src=src, ruta=ruta, crop=crop, usar=usar, extra=list(extra), pie=pie, pos=pos or {})


def paso(parte, num, titulo, intro, items, imagenes, nota=None, tipo='consejo', layout='derecha'):
    return dict(k='paso', parte=parte, num=num, titulo=titulo, intro=intro, items=items, imagenes=imagenes,
                nota=nota, tipo=tipo, layout=layout)


PARTES = [
    ('I', 'Registro de usuarios', 'Crea tu cuenta como cliente o como vendedor.', 'vestido', '#2b7d40'),
    ('II', 'Inicio de sesión', 'Ingresa de forma segura y llega directo a tu espacio.', 'camiseta', '#233127'),
    ('III', 'Tu espacio como cliente', 'Conoce el inicio, el menú y tu cuenta.', 'overol', '#3e9c55'),
    ('IV', 'Perfiles infantiles', 'Registra a tus hijos, sus medidas y su talla sugerida.', 'pijama', '#2b7d40'),
    ('V', 'Compra de prendas', 'Del catálogo al pago, paso a paso.', 'conjunto', '#233127'),
    ('VI', 'Seguimiento de pedidos', 'Consulta el estado de cada compra.', 'chaqueta', '#3e9c55'),
    ('VII', 'Gestión del vendedor', 'Productos, inventario, alertas y despachos.', 'camisa', '#2b7d40'),
    ('VIII', 'Panel administrativo', 'Supervisión general de la plataforma.', 'buzo', '#233127'),
]

PAGINAS = []


def divisor(i):
    PAGINAS.append(dict(k='divisor', i=i))


# ---------------- Parte I
divisor(0)
PAGINAS += [
    paso('I', '1', 'Acceder a LittleStyle',
         f'Ingresa a <b>{URL}</b> desde cualquier navegador actualizado (Chrome, Edge, Firefox o Safari). '
         'Si no tienes una sesión activa, la plataforma te mostrará la pantalla de inicio de sesión.',
         [(5, 'En la parte inferior del formulario haz clic en <b>“Crear cuenta”</b> para abrir el registro.'),
          (None, 'Si ya tienes una cuenta, continúa con la <b>Parte II · Inicio de sesión</b>.')],
         [img('01_login', '/auth/login', usar=[5])],
         nota='Los administradores no pueden registrarse por sí mismos: su cuenta la crea el equipo de LittleStyle.'),
    paso('I', '2', 'Completar el formulario de registro',
         'El formulario <b>“Crear cuenta”</b> pide tus datos básicos. Todos los campos son obligatorios, salvo los que se indican como opcionales.',
         [(1, 'Elige el <b>tipo de cuenta</b>: <i>Cliente</i> (padre, madre o acudiente) o <i>Vendedor</i>.'),
          (2, 'Escribe tu <b>nombre</b> y <b>apellido</b> (solo letras).'),
          (3, 'Ingresa un <b>correo electrónico</b> válido: será tu usuario para iniciar sesión.'),
          (4, 'Agrega tu <b>celular</b>: 10 dígitos que empiezan por 3.'),
          (5, 'Crea una <b>contraseña</b> de 8 a 64 caracteres con al menos una letra y un número, y confírmala. Usa <b>“Ver”</b> para revisarla.'),
          (6, 'Marca la casilla para aceptar los <b>términos y condiciones</b> y el tratamiento de datos.'),
          (7, 'Presiona <b>“Crear cuenta”</b>. Si todo está correcto, entrarás de inmediato a tu espacio.')],
         [img('04_registro', '/auth/registro')], layout='izquierda'),
    paso('I', '3', 'Registro como vendedor',
         'Si vendes ropa infantil, selecciona la tarjeta <b>“Vendedor”</b>. El formulario agrega un campo adicional para identificar tu negocio.',
         [(1, 'Con la opción <b>Vendedor</b> seleccionada, la tarjeta se resalta en verde.'),
          (2, 'Escribe el <b>nombre de la tienda</b>. Aparecerá en tu panel y como marca predeterminada de tus prendas.'),
          (None, 'El resto del proceso es igual al de un cliente.')],
         [img('05_registro_vendedor', '/auth/registro')],
         nota='El nombre de la tienda admite hasta 100 caracteres. Si lo dejas vacío, podrás completarlo después desde “Mi cuenta”.'),
    paso('I', '4', 'Términos y condiciones',
         'Antes de crear la cuenta puedes leer los términos completos haciendo clic en el enlace <b>“términos y condiciones”</b> del formulario.',
         [(None, 'Se abre una ventana con la versión vigente: aceptación, objeto de la plataforma, cuentas de usuario, uso adecuado y tratamiento de datos.'),
          (None, 'Presiona <b>“He leído y acepto”</b> para marcar la casilla automáticamente, o <b>“Cerrar”</b> para volver sin aceptar.')],
         [img('06_terminos', '/auth/registro', crop=(300, 60, 1140, 860))],
         nota='LittleStyle trata los datos personales conforme a la Ley 1581 de 2012 (Habeas Data). Los datos de los menores se guardan cifrados.',
         tipo='importante'),
]

# ---------------- Parte II
divisor(1)
PAGINAS += [
    paso('II', '1', 'Ingresar con tu correo y contraseña',
         'En la pantalla <b>“Bienvenido de nuevo”</b> escribe las credenciales con las que te registraste.',
         [(1, 'Escribe tu <b>correo electrónico</b>.'),
          (2, 'Escribe tu <b>contraseña</b>. El botón <b>“Ver”</b> la muestra u oculta.'),
          (4, 'Presiona <b>“Iniciar sesión”</b>.'),
          (None, 'La plataforma te lleva automáticamente a tu inicio según tu rol: <b>cliente</b>, <b>vendedor</b> o <b>administrador</b>.')],
         [img('01_login', '/auth/login', usar=[1, 2, 4])],
         nota='Tu sesión permanece activa durante 8 horas. Pasado ese tiempo deberás iniciar sesión de nuevo.'),
    paso('II', '2', 'Mensajes de validación',
         'Si un campo está vacío o tiene un formato incorrecto, el formulario lo resalta en rojo y explica qué corregir antes de enviar.',
         [(None, '<b>“El correo es obligatorio”</b> o <b>“El correo no tiene un formato válido”</b>.'),
          (None, '<b>“La contraseña es obligatoria”</b>.'),
          (None, 'Si el correo o la contraseña no coinciden, verás un aviso general de credenciales incorrectas.')],
         [img('02_login_error', '/auth/login')],
         nota='Por seguridad, después de 5 intentos fallidos en 15 minutos el acceso desde ese correo se bloquea temporalmente durante 15 minutos.',
         tipo='importante', layout='izquierda'),
    paso('II', '3', '¿Olvidaste tu contraseña?',
         'Junto al campo de contraseña encontrarás el enlace <b>“¿Olvidaste tu contraseña?”</b>.',
         [(None, 'Al presionarlo, la plataforma informa que la recuperación de contraseña estará disponible próximamente.'),
          (None, 'Mientras tanto, si ya iniciaste sesión, puedes cambiar tu contraseña desde <b>Mi cuenta → Seguridad</b> (Parte VI).'),
          (None, 'Si no tienes cuenta, usa el enlace <b>“¿No tienes una cuenta? Crear cuenta”</b>.')],
         [img('03_login_recuperar', '/auth/login')],
         nota='Esta función se encuentra en desarrollo y se habilitará en una próxima versión.', tipo='importante'),
]

# ---------------- Parte III
divisor(2)
PAGINAS += [
    paso('III', '1', 'Pantalla de inicio del cliente',
         'Después de iniciar sesión verás un saludo con tu nombre, un carrusel de prendas destacadas y accesos rápidos.',
         [(1, '<b>Menú principal</b>: abre el panel lateral de navegación.'),
          (2, '<b>Carrito</b>: muestra la vista rápida de tu compra; el número indica cuántas prendas llevas.'),
          (3, '<b>Menú de cuenta</b>: tu inicial abre las opciones de cuenta.'),
          (4, '<b>“Explorar catálogo”</b> y <b>“Ver tallas sugeridas”</b> te llevan directo a comprar.')],
         [img('10_inicio_cliente', '/cliente', pos={2: 'abajo', 3: 'abajo'})], layout='ancha'),
    paso('III', '2', 'El perfil activo',
         'Debajo del carrusel aparece el <b>perfil activo</b>: el hijo para el que estás comprando. La talla sugerida, los filtros y las advertencias de alergias se calculan con sus datos.',
         [(None, 'Muestra el nombre, la edad y la estatura más reciente.'),
          (None, '<b>“Cambiar perfil”</b> te permite elegir otro hijo.'),
          (None, '<b>“Ver tallas sugeridas”</b> abre las recomendaciones para ese perfil.'),
          (None, 'Más abajo están las <b>novedades</b> del catálogo y accesos a <i>Mis hijos</i>, <i>Catálogo</i>, <i>Mis pedidos</i> y <i>Carrito</i>.')],
         [img('11_inicio_perfil', '/cliente'),
          img('10_inicio_cliente', '/cliente', crop=(0, 0, 1440, 560), usar=[])],
         nota='Si aún no has registrado hijos, el inicio te invitará a crear el primer perfil.', layout='doble'),
    paso('III', '3', 'Menú lateral de navegación',
         'Presiona el ícono <b>☰</b> de la esquina superior izquierda para desplegar el menú.',
         [(None, '<b>Catálogo</b>: todas las prendas. Despliega la flecha para filtrar por <b>Niños</b> o <b>Niñas</b>.'),
          (None, '<b>Categorías</b>: vestidos, pantalones, shorts, faldas, camisas, camisetas, buzos, chaquetas, conjuntos, pijamas y más.'),
          (None, 'Cierra el menú con <b>✕</b>, la tecla <b>Esc</b> o haciendo clic fuera de él.')],
         [img('12_menu_cliente', '/cliente', crop=(0, 0, 1000, 720))], layout='izquierda'),
    paso('III', '4', 'Menú de cuenta',
         'Haz clic en el círculo con tu inicial, en la esquina superior derecha.',
         [(1, 'Se despliegan las opciones: <b>Mi cuenta</b>, <b>Mis hijos</b>, <b>Mis pedidos</b> y <b>Cerrar sesión</b>.'),
          (None, '<b>Cerrar sesión</b> termina tu sesión en ese navegador.')],
         [img('13_menu_cuenta', '/cliente', crop=(700, 0, 1440, 420))],
         nota='Si usas un computador compartido, cierra siempre la sesión al terminar.'),
]

# ---------------- Parte IV
divisor(3)
PAGINAS += [
    paso('IV', '1', 'Mis hijos',
         'Desde el menú de cuenta entra a <b>“Mis hijos”</b>. Cada tarjeta resume un perfil infantil.',
         [(1, '<b>“+ Agregar hijo”</b> crea un perfil nuevo.'),
          (2, '<b>“Ver talla sugerida”</b> abre las recomendaciones de ese hijo.'),
          (3, '<b>“Ver perfil”</b> muestra toda su información y el historial de medidas.'),
          (4, '<b>“Editar perfil”</b> actualiza sus datos.'),
          (None, 'La etiqueta <b>Completo</b> indica que el perfil tiene todo lo necesario para recomendar tallas; las alergias se destacan en rojo.')],
         [img('26_mis_hijos', '/cliente/hijos', crop=(180, 80, 1260, 790))], layout='izquierda'),
    paso('IV', '2', 'Agregar un hijo · Datos y alergias',
         'El formulario <b>“Agregar hijo”</b> está organizado en tarjetas. Las dos primeras definen quién es y qué debe evitar.',
         [(None, '<b>Datos básicos</b>: nombre, fecha de nacimiento, contextura (delgada, media o robusta) y holgura preferida (ajustada, regular u holgada).'),
          (None, '<b>Alergias textiles</b>: marca las que apliquen (fibras sintéticas, lana, tintes, broches metálicos, algodón, látex, níquel, cuero, formaldehído) o selecciona <b>“Sin alergias”</b>. Puedes describir otra alergia.')],
         [img('27_nuevo_hijo', '/cliente/hijos/nuevo', crop=(190, 90, 1250, 905))],
         nota='Las alergias graves bloquean agregar al carrito prendas incompatibles. Declararlas con cuidado protege a tu hijo.',
         tipo='importante'),
    paso('IV', '3', 'Agregar un hijo · Preferencias y medidas',
         'Las últimas tarjetas personalizan las recomendaciones y registran la primera medición.',
         [(None, '<b>Preferencias</b>: colores y estampados favoritos (hasta 10 de cada uno). Las prendas afines suben en las recomendaciones.'),
          (None, '<b>Medidas iniciales</b>: fecha de medición, estatura (40–200 cm) y peso (2–120 kg).'),
          (None, 'Presiona <b>“Crear perfil”</b> para guardar.')],
         [img('27_nuevo_hijo', '/cliente/hijos/nuevo', crop=(190, 895, 1250, 1800))],
         nota='Puedes registrar hasta 10 perfiles infantiles por cuenta.', layout='izquierda'),
    paso('IV', '4', 'Detalle del perfil',
         'Al entrar a <b>“Ver perfil”</b> encontrarás toda la información del hijo organizada por secciones.',
         [(None, '<b>Información</b>, <b>Medidas actuales</b>, <b>Preferencias</b> y <b>Restricciones textiles</b>.'),
          (1, '<b>“Recomendaciones”</b> abre las tallas sugeridas; el lápiz edita el perfil.'),
          (2, '<b>“+ Nueva medición”</b> agrega estatura y peso al historial de crecimiento.'),
          (3, '<b>“Eliminar perfil”</b> borra el perfil y su historial.')],
         [img('28_perfil', '/cliente/hijos/1', crop=(180, 80, 1260, 1180))], layout='ancha-lado'),
    paso('IV', '5', 'Registrar una nueva medición',
         'Los niños crecen rápido: mantener las medidas al día hace que la talla sugerida sea precisa.',
         [(None, 'En <b>Historial de crecimiento</b> presiona <b>“+ Nueva medición”</b>.'),
          (None, 'Ingresa la <b>fecha</b>, la <b>estatura</b> y el <b>peso</b>, y presiona <b>“Guardar medición”</b>.'),
          (None, 'Usa <b>“Corregir”</b> en una fila del historial si te equivocaste en algún dato.')],
         [img('29_nueva_medicion', '/cliente/hijos/1', crop=(200, 860, 1240, 1310))],
         nota='Las medidas deben corresponder a la edad del perfil. Las recomendaciones usan medidas de los últimos 6 meses.'),
    paso('IV', '6', 'Talla sugerida',
         'La página <b>“Talla sugerida”</b> lista las prendas del catálogo con la talla calculada a partir de las medidas del perfil activo, no solo de su edad.',
         [(2, 'Cada prenda indica la <b>talla sugerida</b> para tu hijo.'),
          (3, 'La barra de <b>compatibilidad</b> muestra qué tan bien se ajustan sus medidas a esa talla; la <i>afinidad de estilo</i> suma por colores y estampados favoritos.'),
          (None, 'Si la talla sugerida está agotada, la tarjeta lo indica con “Sin stock en esa talla”.'),
          (None, '<b>“Cambiar perfil”</b> recalcula todo para otro hijo.')],
         [img('30_recomendaciones', '/cliente/recomendaciones', usar=[2, 3])], layout='ancha'),
]

# ---------------- Parte V
divisor(4)
PAGINAS += [
    paso('V', '1', 'Catálogo de prendas',
         'En <b>“Explora prendas”</b> se muestran todas las prendas activas de los vendedores, con foto, nombre y precio.',
         [(1, '<b>Buscador</b>: escribe el nombre de la prenda.'),
          (2, '<b>Filtros</b>: categoría, para quién (niño, niña), material, talla, edad y rango de precio.'),
          (3, '<b>“Solo mi talla sugerida”</b> filtra todo el catálogo según el perfil activo; <b>“Solo disponibles”</b> oculta lo agotado.'),
          (4, '<b>“Limpiar filtros”</b> restablece la búsqueda.'),
          (5, '<b>“Ver”</b> abre el detalle de la prenda.')],
         [img('14_catalogo', '/cliente/catalogo', crop=(130, 60, 1300, 1150))], layout='ancha'),
    paso('V', '2', 'Detalle de una prenda',
         'El detalle reúne todo lo que necesitas para decidir.',
         [(1, '<b>Galería</b> de fotos con miniaturas y botón para ampliar.'),
          (2, 'Selecciona la <b>talla</b>; se muestra cuántas unidades quedan.'),
          (3, 'Ajusta la <b>cantidad</b> con − y +.'),
          (4, 'Presiona <b>“Agregar al carrito”</b>.')],
         [img('16_detalle', '/cliente/catalogo/1', crop=(160, 70, 1280, 830))],
         nota='Si tienes alergias textiles, revisa la composición y consulta tu talla sugerida con el perfil activo antes de comprar.'),
    paso('V', '3', 'Ficha técnica y tabla de medidas',
         'Más abajo en el detalle encontrarás la información textil y los rangos de cada talla.',
         [(None, '<b>Ficha técnica textil</b>: composición, colores, estampado, instrucciones de cuidado y descripción.'),
          (None, '<b>Tabla de medidas</b>: estatura, peso y edad que cubre cada talla, unidades disponibles y estado.'),
          (None, 'La fila de la talla recomendada para tu hijo se marca como <b>“Tu talla”</b>.')],
         [img('17_detalle_ficha', '/cliente/catalogo/1')], layout='ancha'),
    paso('V', '4', 'Vista rápida del carrito',
         'Al agregar una prenda se abre automáticamente el panel lateral <b>“Carrito”</b>. También puedes abrirlo con el ícono del carrito en la barra superior.',
         [(1, 'Revisa cada prenda con su talla, cambia la cantidad o elimínala con el ícono de la papelera.'),
          (None, 'Abajo se muestra el <b>subtotal</b>.'),
          (None, '<b>“Ver carrito”</b> abre la página completa del carrito.')],
         [img('19_carrito_panel', '/cliente/catalogo/2', crop=(720, 0, 1440, 900))],
         nota='Las prendas del carrito no se reservan: el stock se confirma al momento del pago.', layout='izquierda'),
    paso('V', '5', 'Carrito de compras',
         'En <b>“Carrito de compras”</b> ajustas tu selección antes de pagar.',
         [(1, 'Cada prenda muestra foto, nombre, talla y precio unitario.'),
          (2, 'Modifica la <b>cantidad</b> con − y +, o quítala con <b>“Eliminar”</b>.'),
          (4, 'El <b>resumen</b> muestra subtotal y envío (se calcula con tu dirección).'),
          (5, 'Presiona <b>“Continuar compra”</b>. <b>“Vaciar carrito”</b> elimina todo.')],
         [img('20_carrito', '/cliente/carrito', crop=(150, 70, 1290, 620), usar=[1, 2, 4, 5])], layout='ancha'),
    paso('V', '6', 'Información de envío',
         'El checkout tiene tres pasos: <b>Información de envío → Resumen → Pago</b>. Primero indica a dónde enviar el pedido.',
         [(1, 'La barra superior muestra en qué paso estás.'),
          (2, 'Escribe el <b>destinatario</b>, la dirección, complemento opcional y código postal (6 dígitos).'),
          (3, 'Selecciona <b>departamento</b> y <b>municipio</b>, y un celular de contacto.'),
          (4, 'Presiona <b>“Guardar dirección y continuar”</b>. La próxima vez podrás reutilizarla.')],
         [img('21_checkout_info', '/cliente/checkout', crop=(0, 70, 1440, 1060))],
         nota='Costo de envío: Armenia $7.000 · resto del Quindío $10.000 · otras ciudades $15.000.', layout='izquierda'),
    paso('V', '7', 'Resumen y pago',
         'Revisa las prendas, la dirección de entrega y los totales antes de pagar.',
         [(1, 'Los <b>totales</b> incluyen subtotal, envío y total a pagar.'),
          (2, 'Presiona <b>“Pagar ahora”</b>. Se abre la pasarela segura <b>Wompi</b>, donde eliges el medio de pago (tarjeta, PSE, Nequi u otros disponibles) y confirmas.'),
          (None, '<b>“Editar información de envío”</b> te devuelve al paso anterior.')],
         [img('22_checkout_resumen', '/cliente/checkout', crop=(0, 70, 1440, 760))],
         nota='LittleStyle no almacena los datos de tu tarjeta o cuenta: el pago se procesa directamente en Wompi. Tienes 30 minutos para completarlo.',
         tipo='importante', layout='ancha'),
    paso('V', '8', 'Resultado del pago',
         'Al terminar en Wompi regresas a LittleStyle, que confirma automáticamente el estado de la transacción.',
         [(None, '<b>¡Pago aprobado!</b>: el pedido queda registrado y verás la referencia, el total pagado y el medio de pago.'),
          (None, 'Recibirás la confirmación de la compra en tu correo electrónico.'),
          (None, 'Usa <b>“Volver al inicio”</b> o <b>“Seguir comprando”</b> para regresar al catálogo; el pedido aparece en <b>Mis pedidos</b>.'),
          (None, 'Si el pago sigue en proceso o fue rechazado, la página lo indica: un pago rechazado no genera cobro ni pedido y puedes volver al checkout.')],
         [img('23_pago_resultado', '/cliente/pago/resultado', crop=(330, 80, 1110, 500))]),
]

# ---------------- Parte VI
divisor(5)
PAGINAS += [
    paso('VI', '1', 'Mis pedidos',
         'Desde el menú de cuenta entra a <b>“Mis pedidos”</b>. Cada tarjeta muestra el número de pedido, la cantidad de prendas, el total, la fecha y su estado.',
         [(1, 'Haz clic en un pedido para ver su detalle.'),
          (None, 'Estados posibles: <b>Pendiente de pago</b>, <b>Pagado</b>, <b>Registrado</b>, <b>En preparación</b>, <b>Parcialmente enviado</b>, <b>Enviado</b> y <b>Cancelado</b>.')],
         [img('24_mis_pedidos', '/cliente/pedidos', crop=(150, 70, 1290, 520))], layout='ancha'),
    paso('VI', '2', 'Detalle de un pedido',
         'El detalle muestra el avance del pedido y lo que envía cada vendedor.',
         [(None, 'La <b>línea de estados</b> indica en qué etapa está tu compra; <b>“Actualizar”</b> consulta el estado más reciente.'),
          (None, '<b>Tu pedido por vendedor</b>: si compraste a varias tiendas, cada una despacha su paquete con su propio estado y <b>número de guía</b>.'),
          (None, 'Al final están los productos, los totales y el <b>historial</b> con fecha y hora de cada cambio.')],
         [img('25_pedido_detalle', '/cliente/pedidos/1', crop=(150, 80, 1290, 1180))],
         nota='Con el número de guía puedes rastrear el paquete en la página de la transportadora.', layout='ancha-lado'),
    paso('VI', '3', 'Mi cuenta',
         'En <b>“Mi cuenta”</b> administras tus datos personales y la seguridad de tu acceso.',
         [(None, '<b>Información personal</b>: nombre, apellido, correo y celular. Presiona <b>“Editar”</b> para actualizarlos.'),
          (None, '<b>Dirección de envío</b>: la dirección guardada para tus compras.'),
          (None, '<b>Seguridad</b>: presiona <b>“Cambiar”</b>, escribe tu contraseña actual y la nueva dos veces.'),
          (None, '<b>Eliminar cuenta</b>: pide confirmación antes de proceder.')],
         [img('31_cuenta', '/cuenta', crop=(320, 80, 1120, 1120))],
         nota='Al confirmar la eliminación se borran permanentemente tus datos personales y los perfiles infantiles que hayas creado. No se puede deshacer.',
         tipo='importante', layout='izquierda'),
]

# ---------------- Parte VII
divisor(6)
PAGINAS += [
    paso('VII', '1', 'Panel del vendedor',
         'Al iniciar sesión como vendedor llegas al <b>Panel del vendedor</b>, con el nombre de tu tienda.',
         [(1, '<b>Indicadores</b>: productos activos y prendas con stock bajo.'),
          (2, '<b>Accesos</b> a Mis productos, Inventario y Pedidos (Reportes estará disponible próximamente).'),
          (3, '<b>Alertas de inventario</b>: las tallas que requieren reabastecimiento. “Ver todas →” abre el listado completo.')],
         [img('40_panel_vendedor', '/vendedor')], layout='ancha'),
    paso('VII', '2', 'Menú del vendedor',
         'El ícono <b>☰</b> abre el menú lateral con las secciones de gestión.',
         [(1, '<b>Mis productos</b>: catálogo de tu tienda.'),
          (None, '<b>Inventario</b>: existencias por talla.'),
          (None, '<b>Alertas</b>: tallas en stock bajo o agotadas.'),
          (None, '<b>Pedidos</b>: paquetes por preparar y despachar.')],
         [img('41_menu_vendedor', '/vendedor', crop=(0, 0, 1000, 900))], layout='izquierda'),
    paso('VII', '3', 'Mis productos',
         'La tabla lista todas tus prendas con su categoría, precio, stock total y estado.',
         [(1, '<b>“+ Crear producto”</b> publica una prenda nueva.'),
          (2, '<b>Buscador</b> por nombre.'),
          (3, 'El <b>stock</b> se acompaña de una etiqueta: Disponible, Stock bajo o Agotado.'),
          (4, 'El <b>lápiz</b> edita la prenda; el <b>ojo</b> la oculta o la publica en el catálogo.')],
         [img('42_productos', '/vendedor/productos', crop=(150, 70, 1290, 900))], layout='ancha-lado'),
    paso('VII', '4', 'Crear producto · Información',
         'El formulario <b>“Crear producto”</b> se divide en tarjetas. La primera describe la prenda.',
         [(None, '<b>Nombre</b> (3 a 120 caracteres), <b>categoría</b> y <b>para quién es</b> (niño, niña o unisex).'),
          (None, '<b>Marca</b> opcional: por defecto se usa el nombre de tu tienda.'),
          (None, '<b>Descripción</b> e <b>instrucciones de cuidado</b> (obligatorias según la Resolución Andina 2109).'),
          (None, '<b>Precio</b> en pesos colombianos.')],
         [img('43_crear_producto', '/vendedor/productos/nuevo', crop=(350, 90, 1090, 1000))], layout='izquierda'),
    paso('VII', '5', 'Crear producto · Características y composición',
         'Estos datos alimentan el filtro de alergias y las recomendaciones de estilo.',
         [(None, '<b>Colores</b> (hasta 10) y <b>estampado</b>.'),
          (None, '<b>Información para alergias</b>: marca si la prenda tiene tintes sintéticos, broches metálicos, níquel o tratamiento con formaldehído.'),
          (None, '<b>Composición textil</b>: material y porcentaje. Con <b>“+ Agregar material”</b> sumas hasta 6; el total debe ser 100%.')],
         [img('44_editar_producto', '/vendedor/productos/1', crop=(350, 990, 1090, 1800))],
         nota='Declarar la composición y los componentes con precisión evita que la prenda llegue a niños con alergias incompatibles.',
         tipo='importante'),
    paso('VII', '6', 'Crear producto · Tallas, inventario y fotos',
         'Las últimas tarjetas definen para quién sirve cada talla y cuántas unidades tienes.',
         [(None, '<b>Tabla de tallas</b>: para cada talla indica estatura y peso mínimos y máximos y, opcionalmente, la edad en meses. Así el sistema sugiere la talla correcta (hasta 15 tallas).'),
          (None, '<b>Inventario</b>: stock inicial y mínimo de alerta por talla.'),
          (None, '<b>Fotos</b>: JPG, PNG o WEBP de máximo 5 MB, hasta 8 por prenda; la primera es la principal.'),
          (None, 'Presiona <b>“Crear producto”</b> (o <b>“Guardar cambios”</b> al editar).')],
         [img('43_crear_producto', '/vendedor/productos/nuevo', crop=(350, 1790, 1090, 2730))], layout='izquierda'),
    paso('VII', '7', 'Inventario',
         'En <b>“Inventario”</b> controlas las existencias de todas tus prendas.',
         [(None, 'Cada fila muestra el <b>stock actual</b> (en naranja si está bajo, en rojo si alguna talla se agotó), los mínimos por talla y el estado.'),
          (1, 'El botón <b>“Alertas”</b> indica cuántas tallas requieren atención.'),
          (2, '<b>“Actualizar stock”</b> abre la edición de esa prenda.')],
         [img('45_inventario', '/vendedor/inventario', crop=(150, 70, 1290, 900))], layout='ancha-lado'),
    paso('VII', '8', 'Actualizar stock',
         'Al presionar <b>“Actualizar stock”</b> la fila se expande con un campo por talla.',
         [(None, 'Escribe la <b>cantidad actual</b> de cada talla (valores absolutos, no sumas).'),
          (None, 'Ajusta el <b>mínimo de alerta</b>; vacío significa que la talla no genera alertas.'),
          (None, 'Presiona <b>“Guardar”</b> o <b>✕</b> para cancelar.')],
         [img('46_actualizar_stock', '/vendedor/inventario', crop=(150, 180, 1290, 560))],
         nota='Cuando una talla llega a su mínimo, aparece automáticamente en tus alertas y en el panel.', layout='ancha'),
    paso('VII', '9', 'Alertas de inventario',
         'Cada tarjeta corresponde a una talla que llegó a su mínimo o se agotó.',
         [(None, 'Muestra la talla, el stock actual, el mínimo y la <b>diferencia</b> (cuántas unidades faltan).'),
          (None, 'El borde y la etiqueta indican la gravedad: <b>Stock bajo</b> (amarillo) o <b>Agotado</b> (rojo).'),
          (None, '<b>“Actualizar stock →”</b> te lleva directo a reponer esa prenda.')],
         [img('47_alertas', '/vendedor/inventario/alertas', crop=(150, 80, 1290, 900))], layout='ancha-lado'),
    paso('VII', '10', 'Mis despachos',
         'En <b>“Pedidos”</b> ves los paquetes que te corresponden: solo los productos de tu tienda dentro de cada pedido.',
         [(None, 'Cada tarjeta muestra el número de pedido, las unidades, tu subtotal, la fecha y el estado.'),
          (None, 'Si ya lo enviaste, aparece el <b>número de guía</b>.'),
          (None, 'Haz clic en un paquete para gestionarlo.')],
         [img('48_despachos', '/vendedor/pedidos', crop=(150, 70, 1290, 500))], layout='ancha'),
    paso('VII', '11', 'Preparar y enviar un paquete',
         'En el detalle del paquete avanzas su estado a medida que lo alistas.',
         [(None, 'Los indicadores resumen el pedido, el estado de tu paquete, tus productos y tu subtotal.'),
          (1, 'Al despachar, escribe el <b>número de guía</b> de la transportadora.'),
          (2, 'Presiona <b>“Marcar como enviado”</b>. El cliente verá el cambio y la guía en su pedido.'),
          (None, 'Abajo se listan tus productos con talla, cantidad y precio.')],
         [img('49_despacho_detalle', '/vendedor/pedidos/2', crop=(150, 80, 1290, 800))],
         nota='El flujo es Registrado → En preparación → Enviado. Un estado no puede devolverse.', layout='izquierda'),
]

# ---------------- Parte VIII
divisor(7)
PAGINAS += [
    paso('VIII', '1', 'Panel administrativo',
         'La cuenta de administrador accede al <b>Panel administrativo</b>, punto de partida para la supervisión general de LittleStyle.',
         [(None, '<b>Usuarios</b>: clientes y vendedores.'),
          (None, '<b>Moderación</b>: publicaciones del catálogo.'),
          (None, '<b>Categorías</b>: categorías generales.'),
          (None, '<b>Reportes</b>: dashboards y exportación en PDF.')],
         [img('50_admin', '/admin', crop=(150, 60, 1290, 560))],
         nota='Estos módulos se habilitarán progresivamente en las próximas versiones de la plataforma.', layout='ancha'),
]

PAGINAS.append(dict(k='faq'))
PAGINAS.append(dict(k='cierre'))


# ---------------------------------------------------------------- render

def marcas_de(im):
    """Devuelve las marcas en % del área recortada."""
    d = MARCAS[im['src']]
    W, H = d['w'], d['h']
    x0, y0, x1, y1 = im['crop'] or (0, 0, W, H)
    cw, ch = x1 - x0, y1 - y0
    res = []
    for m in d['marcas']:
        if im['usar'] is not None and m['n'] not in im['usar']:
            continue
        x, y, w, h = m['x'] * W / 100, m['y'] * H / 100, m['w'] * W / 100, m['h'] * H / 100
        res.append((m['n'], x, y, w, h))
    res += im['extra']
    out = []
    for n, x, y, w, h in res:
        cx, cy = (x + w / 2 - x0) / cw * 100, (y + h / 2 - y0) / ch * 100
        if not (0 <= cx <= 100 and 0 <= cy <= 100):
            continue
        out.append((n, (x - x0) / cw * 100, (y - y0) / ch * 100, w / cw * 100, h / ch * 100))
    return out, cw, ch


def figura(im, ancho_max, alto_max):
    marcas, cw, ch = marcas_de(im)
    ratio = cw / ch
    w = min(ancho_max, (alto_max - CHROME_BARRA - 12) * ratio)
    d = MARCAS[im['src']]
    x0, y0, _, _ = im['crop'] or (0, 0, d['w'], d['h'])
    estilo_img = (f"width:{d['w'] / cw * 100:.3f}%;left:{-x0 / cw * 100:.3f}%;top:{-y0 / ch * 100:.3f}%")
    pins = ''.join(
        f'<span class="marca {im["pos"].get(n, "")}" style="left:{x:.2f}%;top:{y:.2f}%;width:{mw:.2f}%;height:{mh:.2f}%">'
        f'<b class="boton">{n}</b></span>' for n, x, y, mw, mh in marcas)
    pie = f'<figcaption>{im["pie"]}</figcaption>' if im['pie'] else ''
    return (f'<figure class="pieza" style="width:{w + 12:.0f}px">'
            f'<div class="barra"><i></i><i></i><i></i><span>{URL}{e(im["ruta"])}</span></div>'
            f'<div class="lienzo" style="aspect-ratio:{cw:.1f}/{ch:.1f}"><div class="ventana">'
            f'<img src="capturas/{im["src"]}.png" style="{estilo_img}" alt=""></div>{pins}</div>{pie}</figure>')


def cinta(num):
    marcas = ''.join(f'<span style="top:{y}px">{i}</span>' for i, y in enumerate(range(48, PH, 48), 1))
    return f'<div class="cinta">{marcas}<em>{num:02d}</em></div>'


def marco(contenido, num, clase=''):
    return (f'<section class="pagina {clase}">{cinta(num)}{contenido}'
            f'<footer><span class="puntada"></span><span>LittleStyle · Manual de usuario</span><b>{num:02d}</b></footer></section>')


ICONOS = {
    'consejo': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3Z"/></svg>',
    'importante': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"><path d="M12 3 2 20h20L12 3Z"/><path d="M12 10v4M12 17v.01" stroke-linecap="round"/></svg>',
}


def nota_html(p):
    if not p['nota']:
        return ''
    titulo = 'Consejo' if p['tipo'] == 'consejo' else 'Importante'
    return (f'<aside class="cuidado {p["tipo"]}"><div class="cuidado-ico">{ICONOS[p["tipo"]]}</div>'
            f'<div><small>Etiqueta de cuidado · {titulo}</small><p>{p["nota"]}</p></div></aside>')


def items_html(items):
    lis = []
    for n, t in items:
        b = f'<b class="boton">{n}</b>' if n else '<b class="boton vacio"></b>'
        lis.append(f'<li>{b}<span>{t}</span></li>')
    return f'<ol class="pasos">{"".join(lis)}</ol>'


def render_paso(p, num):
    cab = (f'<header class="cab"><span class="tag">Parte {p["parte"]}</span>'
           f'<span class="paso-num">Paso {p["num"]}</span></header>'
           f'<h2 class="titulo"><span>{p["titulo"]}</span></h2>')
    texto = f'<p class="intro">{p["intro"]}</p>{items_html(p["items"])}{nota_html(p)}'
    lay = p['layout']
    ims = p['imagenes']
    if lay in ('derecha', 'izquierda'):
        figs = figura(ims[0], 720, 520)
        cuerpo = (f'<div class="fila {lay}"><div class="col-texto">{texto}</div>'
                  f'<div class="col-img">{figs}</div></div>')
    elif lay == 'ancha-lado':
        figs = figura(ims[0], 640, 535)
        cuerpo = (f'<div class="fila derecha ancha-lado"><div class="col-texto">{texto}</div>'
                  f'<div class="col-img">{figs}</div></div>')
    elif lay == 'ancha':
        figs = figura(ims[0], 760, 535)
        cuerpo = (f'<div class="fila ancha"><div class="col-img">{figs}</div>'
                  f'<div class="col-texto">{texto}</div></div>')
    else:  # doble
        f1 = figura(ims[0], 600, 190)
        f2 = figura(ims[1], 600, 330)
        cuerpo = (f'<div class="fila doble"><div class="col-texto">{texto}</div>'
                  f'<div class="col-img apiladas">{f1}{f2}</div></div>')
    return marco(cab + cuerpo, num, 'p-paso')


def gancho(prenda, color):
    return (f'<div class="colgante"><svg class="gancho" viewBox="0 0 200 120"><path d="M100 10c-12 0-18 10-14 18 3 6 14 8 14 20v6L20 104h160L100 54" '
            f'fill="none" stroke="{color}" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
            f'<img src="prendas/{prenda}.png" alt=""></div>')


def render_divisor(i, num, pasos_parte):
    rom, titulo, desc, prenda, color = PARTES[i]
    lista = ''.join(f'<li><span>Paso {p["num"]}</span>{p["titulo"]}</li>' for p in pasos_parte)
    c = (f'<div class="div-num">{i + 1:02d}</div>'
         f'<div class="div-texto"><span class="tag claro">Parte {rom}</span><h1>{titulo}</h1><p>{desc}</p>'
         f'<ul class="ticket">{lista}</ul></div>{gancho(prenda, "#f2ecad")}')
    return marco(c, num, 'p-divisor').replace('class="pagina p-divisor"', f'class="pagina p-divisor" style="--fondo:{color}"')


def render_portada():
    prendas = ['vestido', 'camiseta', 'overol', 'falda', 'short']
    pinzas = ''.join(
        f'<div class="tendida" style="--r:{r}deg;--d:{d}px"><i class="pinza"></i><img src="prendas/{p}.png" alt=""></div>'
        for p, r, d in zip(prendas, [-6, 4, -3, 6, -4], [0, 18, 6, 22, 4]))
    return f'''<section class="pagina p-portada">
  <svg class="cuerda" viewBox="0 0 1280 120" preserveAspectRatio="none"><path d="M-10 30 Q640 120 1290 30" fill="none" stroke="#f2ecad" stroke-width="3"/></svg>
  <div class="tendedero">{pinzas}</div>
  <div class="portada-texto">
    <span class="tag claro">Manual de usuario · Versión 1.0</span>
    <h1>Little<b>Style</b></h1>
    <p class="lema">La talla perfecta para cada etapa.</p>
    <p class="sub">Regístrate, crea los perfiles de tus hijos, compra con talla sugerida y, si eres vendedor, gestiona tu tienda. Todo desde nuestra página web.</p>
  </div>
  <div class="etiqueta-autores">
    <i class="ojal"></i>
    <small>Realizado por</small>
    <p>Oscar Tomás Aristizabal Velásquez<br>Valentina González Díaz<br>Valentina Rodríguez Castro</p>
    <small>Ingeniería de Software III · Universidad del Quindío · Octubre 2026</small>
  </div>
</section>'''


def render_bienvenida(num):
    total_pasos = sum(1 for p in PAGINAS if p['k'] == 'paso')
    c = f'''<header class="cab"><span class="tag">Bienvenida</span></header>
<h2 class="titulo"><span>Bienvenido al manual de LittleStyle</span></h2>
<div class="bienvenida">
  <div>
    <p class="intro grande">LittleStyle es una plataforma de moda infantil que crece con tus hijos: recomienda la talla correcta
    según sus medidas reales, filtra las prendas que no son seguras para sus alergias textiles y te permite comprar con pagos seguros.</p>
    <p class="intro">Esta guía te acompaña en cada paso, ya seas <b>cliente</b> (padre, madre o acudiente) o <b>vendedor</b> de ropa infantil.
    Cada pantalla es una captura real de la plataforma con marcadores numerados que corresponden a las instrucciones.</p>
    <h3 class="sub-h">Aquí aprenderás a:</h3>
    <ul class="checks">
      <li>Crear tu cuenta e iniciar sesión de forma segura</li>
      <li>Registrar los perfiles de tus hijos con sus medidas y alergias</li>
      <li>Encontrar prendas con la talla sugerida y comprarlas</li>
      <li>Hacer seguimiento a tus pedidos</li>
      <li>Publicar productos, controlar inventario y despachar pedidos como vendedor</li>
    </ul>
  </div>

  <div class="como-leer">
    <h3>Cómo leer este manual</h3>
    <div class="leyenda"><b class="boton">1</b><p>Los <b>botones numerados</b> sobre la captura señalan el elemento que se describe en el paso con el mismo número.</p></div>
    <div class="leyenda"><span class="mini-marca"></span><p>La <b>puntada verde</b> rodea el área exacta de la pantalla.</p></div>
    <div class="leyenda"><span class="mini-barra">{URL}/ruta</span><p>La <b>barra superior</b> indica la dirección de la página.</p></div>
    <div class="leyenda"><span class="mini-cuidado"></span><p>Las <b>etiquetas de cuidado</b> contienen consejos y advertencias importantes.</p></div>
  </div>
</div>
<div class="medidas">
  <div><b>8</b><span>partes</span></div><div><b>{total_pasos}</b><span>pasos guiados</span></div>
  <div><b>35</b><span>capturas reales</span></div><div><b>3</b><span>roles de usuario</span></div>
  <p>Versión documentada:<br>LittleStyle a octubre de 2026</p>
</div>'''
    return marco(c, num)


def render_intro(num):
    roles = [
        ('Cliente', 'Padre, madre o acudiente', 'Registra perfiles infantiles, consulta la talla sugerida, compra y sigue sus pedidos.', 'pijama'),
        ('Vendedor', 'Tienda de ropa infantil', 'Publica prendas con su ficha textil y tabla de tallas, controla inventario y despacha pedidos.', 'camisa'),
        ('Administrador', 'Equipo LittleStyle', 'Supervisa usuarios, publicaciones, categorías y reportes de la plataforma.', 'buzo'),
    ]
    tarjetas = ''.join(
        f'<article class="rol"><img src="prendas/{im}.png" alt=""><h3>{t}</h3><small>{s}</small><p>{d}</p></article>'
        for t, s, d, im in roles)
    c = f'''<header class="cab"><span class="tag">Introducción</span></header>
<h2 class="titulo"><span>Introducción y objetivo</span></h2>
<div class="intro-grid">
  <div>
    <p class="intro">Este manual está dirigido a los usuarios de LittleStyle y tiene como propósito brindar una guía clara, detallada y
    funcional sobre el uso de la plataforma en su versión actual.</p>
    <p class="intro">Para acceder, cada persona se registra y luego inicia sesión con su correo y contraseña. El sistema identifica su
    rol y habilita únicamente las funciones correspondientes.</p>
    <div class="objetivo"><small>Objetivo</small><p>Ofrecer una guía precisa sobre los módulos de LittleStyle, orientada a mejorar la
    comprensión de la interfaz y facilitar las tareas de compra, gestión de perfiles infantiles y administración de la tienda.</p></div>
    <div class="requisitos"><small>Requisitos</small><p>Conexión a internet · navegador actualizado · correo electrónico activo · dirección: <b>{URL}</b></p></div>
  </div>
  <div class="roles">{tarjetas}</div>
</div>'''
    return marco(c, num)


def render_indice(num, entradas):
    filas = ''.join(
        f'<tr><td class="t-talla">{rom}</td><td>{t}<small>{d}</small></td><td class="t-pasos">{n}</td><td class="t-pag">{pag:02d}</td></tr>'
        for rom, t, d, n, pag in entradas)
    c = f'''<header class="cab"><span class="tag">Índice</span></header>
<h2 class="titulo"><span>Tabla de contenido</span></h2>
<p class="intro">Como una buena tabla de tallas: encuentra rápido la parte que necesitas.</p>
<table class="indice"><thead><tr><th>Parte</th><th>Contenido</th><th>Alcance</th><th>Página</th></tr></thead><tbody>{filas}</tbody></table>'''
    return marco(c, num)


def render_faq(num):
    preguntas = [
        ('No puedo iniciar sesión', 'Verifica el correo y la contraseña. Tras 5 intentos fallidos el acceso se bloquea 15 minutos; espera y vuelve a intentarlo.'),
        ('La talla sugerida no aparece', 'Revisa que el perfil activo tenga una medición de los últimos 6 meses y que esté completo. Puedes cambiar el perfil desde el inicio.'),
        ('No me deja agregar una prenda al carrito', 'La prenda puede estar agotada en esa talla o ser incompatible con una alergia grave del perfil activo.'),
        ('Pagué pero el pedido sigue pendiente', 'La confirmación de Wompi puede tardar unos minutos. Abre el pedido y presiona “Actualizar”. Si el pago se aprobó tarde sobre un pedido anulado, se reembolsa automáticamente.'),
        ('¿Cómo sé cuándo llega mi pedido?', 'En “Mis pedidos” verás el estado de cada paquete y el número de guía cuando el vendedor lo envíe.'),
        ('Soy vendedor y no veo una prenda en el catálogo', 'Revisa que esté publicada (estado Activo; el ícono del ojo la oculta o publica) y que tenga stock en al menos una talla.'),
    ]
    tarjetas = ''.join(f'<article class="faq"><h3><b class="boton">?</b>{q}</h3><p>{a}</p></article>' for q, a in preguntas)
    c = f'''<header class="cab"><span class="tag">Ayuda</span></header>
<h2 class="titulo"><span>Preguntas frecuentes</span></h2>
<div class="faqs">{tarjetas}</div>'''
    return marco(c, num)


def render_cierre(num):
    c = f'''<div class="cierre">
  <div class="cierre-texto">
    <span class="tag claro">Gracias</span>
    <h1>¡Gracias por ser parte de Little<b>Style</b>!</h1>
    <p>Esperamos que este manual te ayude a encontrar la talla perfecta para cada etapa de tus hijos y a gestionar tu tienda con tranquilidad.</p>
    <div class="contacto"><div><small>Correo</small><p>contacto@littlestyle.com</p></div>
    <div><small>Horario</small><p>Lunes a viernes, 8:00 a 18:00</p></div>
    <div><small>Sitio web</small><p>{URL}</p></div></div>
  </div>
  {gancho('overol', '#f2ecad')}
</div>'''
    return marco(c, num, 'p-divisor p-cierre')


def construir():
    # numeración: 1 portada, 2 bienvenida, 3 introducción, 4 índice, luego el contenido
    num = 5
    paginas_html = []
    entradas = []
    for i, pg in enumerate(PAGINAS):
        if pg['k'] == 'divisor':
            pasos_parte = []
            for q in PAGINAS[i + 1:]:
                if q['k'] != 'paso':
                    break
                pasos_parte.append(q)
            rom, titulo, desc, _, _ = PARTES[pg['i']]
            entradas.append((rom, titulo, desc, f'{len(pasos_parte)} paso' + ('s' if len(pasos_parte) != 1 else ''), num))
            paginas_html.append(render_divisor(pg['i'], num, pasos_parte))
        elif pg['k'] == 'paso':
            paginas_html.append(render_paso(pg, num))
        elif pg['k'] == 'faq':
            entradas.append(('?', 'Preguntas frecuentes', 'Solución a los problemas más comunes.', '6 preguntas', num))
            paginas_html.append(render_faq(num))
        else:
            paginas_html.append(render_cierre(num))
        num += 1
    cuerpo = render_portada() + render_bienvenida(2) + render_intro(3) + render_indice(4, entradas) + ''.join(paginas_html)
    css = (AQUI / 'manual.css').read_text(encoding='utf-8')
    doc = f'''<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>Manual de Usuario LittleStyle</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Nunito:ital,wght@0,400;0,600;0,700;0,800;0,900;1,700&display=swap" rel="stylesheet">
<style>{css}</style></head><body>{cuerpo}</body></html>'''
    (AQUI / 'manual.html').write_text(doc, encoding='utf-8')
    print('manual.html', num - 1, 'páginas')


def exportar_pdf():
    from playwright.sync_api import sync_playwright
    with sync_playwright() as p:
        b = p.chromium.launch(channel='chrome')
        pg = b.new_page(viewport={'width': PW, 'height': PH})
        pg.goto((AQUI / 'manual.html').as_uri())
        pg.wait_for_load_state('networkidle')
        pg.evaluate('document.fonts.ready')
        pg.wait_for_timeout(800)
        pg.pdf(path=str(PDF), width=f'{PW}px', height=f'{PH}px', print_background=True,
               margin={'top': '0', 'right': '0', 'bottom': '0', 'left': '0'})
        b.close()
    print('PDF:', PDF)


if __name__ == '__main__':
    construir()
    if '--pdf' in sys.argv:
        exportar_pdf()
