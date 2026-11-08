"""Toma las capturas del panel administrativo (Parte VIII del manual) y actualiza capturas/marcas.json.

Requiere la aplicación corriendo en local (docker compose ... up) con datos de demostración y un
administrador configurado en .env (APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD). Las cuentas de vendedor de
demostración usan la contraseña DEMO_PASSWORD.

    python capturar_admin.py [--base http://localhost]
"""
import json, pathlib, sys
from playwright.sync_api import sync_playwright

AQUI = pathlib.Path(__file__).resolve().parent
REPO = AQUI.parent.parent
CAPTURAS = AQUI / 'capturas'
BASE = sys.argv[sys.argv.index('--base') + 1] if '--base' in sys.argv else 'http://localhost'
VENDEDORA_DEMO = ('camila.torres@example.com', 'Demo12345')
W, H = 1440, 900

env = dict(l.split('=', 1) for l in (REPO / '.env').read_text(encoding='utf-8').splitlines()
           if '=' in l and l[:1].isupper())
marcas = json.loads((CAPTURAS / 'marcas.json').read_text(encoding='utf-8'))


def caja(pg, loc):
    """Rectángulo del elemento en px CSS de la página completa."""
    return loc.first.evaluate(
        'e => { const r = e.getBoundingClientRect(); return [r.x + scrollX, r.y + scrollY, r.width, r.height]; }')


def capturar(pg, nombre, marcadores, completa=True):
    pg.wait_for_timeout(700)
    alto = pg.evaluate('document.documentElement.scrollHeight') if completa else H
    lista = []
    for n, loc in marcadores:
        x, y, w, h = caja(pg, loc)
        if not completa:
            y -= pg.evaluate('scrollY')
        pad = 4
        x, y, w, h = x - pad, y - pad, w + 2 * pad, h + 2 * pad
        lista.append(dict(n=n, x=round(x / W * 100, 2), y=round(y / alto * 100, 2),
                          w=round(w / W * 100, 2), h=round(h / alto * 100, 2)))
    pg.screenshot(path=str(CAPTURAS / f'{nombre}.png'), full_page=completa)
    marcas[nombre] = dict(w=W, h=alto, marcas=lista)
    print(nombre, alto, len(lista))


def ir(pg, ruta):
    pg.goto(BASE + ruta)
    pg.wait_for_load_state('networkidle')
    pg.wait_for_timeout(900)


def login(ctx, email, password, destino):
    pg = ctx.new_page()
    ir(pg, '/auth/login')
    pg.fill('input[type=email]', email)
    pg.fill('input[type=password]', password)
    pg.click('button[type=submit]')
    pg.wait_for_url('**' + destino)
    pg.wait_for_load_state('networkidle')
    return pg


with sync_playwright() as p:
    nav = p.chromium.launch(channel='chrome', args=['--lang=es-CO'])
    opciones = dict(viewport={'width': W, 'height': H}, device_scale_factor=2, locale='es-CO',
                    timezone_id='America/Bogota')
    ctx = nav.new_context(**opciones)
    pg = login(ctx, env['APP_ADMIN_EMAIL'], env['APP_ADMIN_PASSWORD'], '/admin')
    L = pg.locator

    # ---- Panel y menú
    ir(pg, '/admin')
    capturar(pg, '50_admin', [(1, L('.admin-kpis')), (2, L('.btn-refresh')), (3, L('.admin-tiles'))])
    pg.get_by_label('Abrir menú', exact=True).click()
    capturar(pg, '51_menu_admin', [(1, L('nav.menu-lateral'))], completa=False)

    # ---- Usuarios
    ir(pg, '/admin/usuarios')
    fila = L('tbody tr').first
    capturar(pg, '52_usuarios', [(1, L('.admin-filtros')), (2, fila.locator('.state-chip')),
                                 (3, fila.get_by_role('button', name='Cambiar estado')),
                                 (4, fila.get_by_role('button', name='Eliminar cuenta')), (5, L('.admin-paginado'))])
    fila.get_by_role('button', name='Cambiar estado').click()
    dlg = L('.confirm-dialog')
    dlg.locator('[role=group] button').first.click()
    dlg.locator('#motivo-desactivacion').fill('El cliente confirmó por correo que reconoce los pagos realizados.')
    capturar(pg, '53_usuario_estado', [(1, dlg.locator('[role=group]')), (2, dlg.locator('#motivo-desactivacion')),
                                       (3, dlg.locator('.confirm-actions .btn-primary'))], completa=False)
    dlg.get_by_role('button', name='Cancelar').click()
    L('tbody tr').nth(1).get_by_role('button', name='Eliminar cuenta').click()
    dlg = L('.confirm-dialog')
    capturar(pg, '54_usuario_eliminar', [(1, dlg.locator('p[role=alert]')), (2, dlg.locator('#motivo-eliminacion')),
                                         (3, dlg.locator('.confirm-actions .btn-primary'))], completa=False)
    dlg.get_by_role('button', name='Cancelar').click()

    # ---- Moderación
    ir(pg, '/admin/moderacion')
    desact = L('tbody tr', has=L('.chip-DESACTIVADA'))
    capturar(pg, '55_moderacion', [(1, L('.admin-filtros')), (2, L('tbody tr').first.locator('.state-chip')),
                                   (3, desact.locator('td[data-label=Estado]')),
                                   (4, L('tbody tr').first.get_by_role('button', name='Ver detalle')),
                                   (5, L('.admin-paginado'))])
    L('tbody tr', has_text='Overol de Mezclilla').get_by_role('button', name='Ver detalle').click()
    pg.wait_for_selector('.detalle-grid')
    pg.wait_for_timeout(800)
    capturar(pg, '56_moderacion_detalle', [(1, L('.detalle-imagenes')), (2, L('.detalle-tabla-wrap')),
                                           (3, L('.detalle-flags')), (4, L('.detalle-acciones'))])
    L('.accion-desactivar').click()
    dlg = L('.confirm-dialog')
    dlg.locator('#motivo-desactivacion-prenda').fill('La ficha no declara el broche metálico con níquel en la descripción.')
    capturar(pg, '57_moderacion_desactivar', [(1, dlg.locator('#motivo-desactivacion-prenda')),
                                              (2, dlg.locator('.confirm-actions .btn-primary'))], completa=False)
    dlg.get_by_role('button', name='Cancelar').click()

    # ---- Incidencias
    ir(pg, '/admin/incidencias')
    abierta = L('tbody tr', has=L('.chip-ABIERTA')).first
    capturar(pg, '58_incidencias', [(1, L('.admin-filtros')),
                                    (2, L('tbody tr').first.locator('.product-meta', has_text='Referencia')),
                                    (3, abierta.locator('.state-chip')), (4, abierta.locator('select')),
                                    (5, L('.product-meta', has_text='Respuesta'))])
    L('tbody tr', has=L('.chip-EN_REVISION')).first.locator('select').select_option('RESOLVER')
    dlg = L('.confirm-dialog')
    dlg.locator('#respuesta-incidencia').fill('Revisamos la tabla de medidas con la tienda y la corrigió. '
                                              'Puedes solicitar el cambio de talla sin costo.')
    capturar(pg, '59_incidencia_resolver', [(1, dlg.locator('#respuesta-incidencia')),
                                            (2, dlg.locator('.confirm-actions .btn-primary'))], completa=False)
    dlg.get_by_role('button', name='Cancelar').click()

    # ---- Auditoría
    ir(pg, '/admin/auditoria')
    pg.get_by_role('button', name='Verificar integridad').click()
    pg.wait_for_selector('.alert-success, .alert-error')
    capturar(pg, '60_auditoria', [(1, pg.get_by_role('button', name='Verificar integridad')),
                                  (2, L('.alert').first), (3, L('.admin-filtros')),
                                  (4, L('.valor-cambio')), (5, L('tbody tr').first.locator('.state-chip'))])

    # ---- Reportes
    ir(pg, '/dashboard')
    pg.wait_for_timeout(1500)
    capturar(pg, '61_dashboard', [(1, L('.header-filters')), (2, L('.kpi-grid')), (3, L('.summary-grid')),
                                  (4, L('.sales-grid')), (5, L('.export-box'))])
    pg.select_option('#formato-reporte', index=1)
    capturar(pg, '62_exportar', [(1, L('#tipo-reporte')), (2, L('#formato-reporte')),
                                 (3, L('.export-actions .btn-primary')), (4, L('.export-actions .btn-secondary'))])
    ctx.close()

    # ---- Lado del vendedor: apelación
    ctx = nav.new_context(**opciones)
    pg = login(ctx, *VENDEDORA_DEMO, '/vendedor')
    L = pg.locator
    ir(pg, '/vendedor/productos')
    fila = L('tbody tr', has_text='Camiseta Réplica Oficial')
    capturar(pg, '63_apelar', [(1, fila.locator('td').first), (2, fila.get_by_role('link', name='Apelar'))])
    ir(pg, '/mis-incidencias')
    capturar(pg, '64_mis_incidencias', [(1, L('tbody tr').first.locator('td').first),
                                        (2, L('tbody tr').first.locator('.state-chip'))])
    ctx.close()
    nav.close()

(CAPTURAS / 'marcas.json').write_text(json.dumps(marcas, ensure_ascii=False, indent=1), encoding='utf-8')
print('marcas.json actualizado')
