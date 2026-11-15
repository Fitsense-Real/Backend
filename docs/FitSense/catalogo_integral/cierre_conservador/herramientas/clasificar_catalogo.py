"""Clasificacion operacional del catalogo para FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1 (orden de 15/11/2026).

Solo lee: el registro maestro final, el registro de decisiones pendientes y las listas de contencion del codigo
(ContencionConservadora.java, ExternalCatalogService.java, ContencionTextoBandera.java). No revisa ningun ejercicio de
nuevo ni cambia ningun dato. Uso:
  python3 -I -B herramientas/clasificar_catalogo.py <raiz_repo> <carpeta_salida>
Estados:
  A  ELEGIBLE                      sin restriccion adicional del cierre conservador
  B  ELEGIBLE_CON_RESTRICCIONES    B1 restriccion conservadora (valor vigente mas restrictivo que la propuesta de IA)
                                   B2 contencion temporal condicionada al perfil (INC-CAT-C39, INC-CAT-1374, INC-CAT-CONS-v1)
  C  EXCLUIDO_TEMPORALMENTE        no se recomienda en planificaciones nuevas a nadie (INC-4C-03, INC-CAT-CONS-v1 identidad)
  D  INACTIVO                      is_active = false
Ninguno de estos estados es una validacion profesional.
"""
import csv, os, re, sys, collections

raiz, salida = sys.argv[1], sys.argv[2]
cat = os.path.join(raiz, 'docs/FitSense/catalogo_integral/cierre')
java = lambda p: open(os.path.join(raiz, p)).read()

def ids_de(texto, nombre):
    m = re.search(nombre + r'\s*=\s*Set\.of\(([^;]*)\);', texto, re.S)
    return {int(x) for x in re.findall(r'(\d+)L', m.group(1))}

cons = java('src/main/java/main/web/services/fitsense/planning/domain/services/ContencionConservadora.java')
IDENT = ids_de(cons, 'IDENTIDAD_NO_DETERMINADA')
APOYO = ids_de(cons, 'APOYO_SUSPENDIDO')
DIFIC = ids_de(cons, 'DIFICULTAD_EN_DUDA')
ecs = java('src/main/java/main/web/services/fitsense/planning/application/internal/outboundservices/acl/ExternalCatalogService.java')
RETEN = {int(x) for x in re.findall(r'(\d+)L', re.search(r'RETENIDOS_INC_4C_03\s*=\s*([^;]*);', ecs, re.S).group(1))}
C1374 = {1374}

maestro = list(csv.DictReader(open(os.path.join(cat, 'registro_maestro_final.tsv')), delimiter='\t'))
# Migraciones posteriores al registro maestro que cambian lo que se muestra (no la seguridad): V56 corrige el nombre del
# 853 segun el GIF completo (REVISION_853.md). Se lee de la propia migracion para no copiarlo a mano.
v56 = java('src/main/resources/db/migration/V56__catalogo_853_texto_segun_gif.sql')
POSTERIORES = {853: {'nombre_es': re.search(r"name_es = '([^']*)'", v56).group(1)}}
for x in maestro:
    x.update(POSTERIORES.get(int(x['id']), {}))
registro = list(csv.DictReader(open(os.path.join(cat, 'REGISTRO_DECISIONES_PENDIENTES.tsv')), delimiter='\t'))
pend = collections.defaultdict(list)
for r in registro:
    if r['id'] and r['estado'] != 'RESUELTA_TECNICA':
        pend[int(r['id'])].append(r)

filas = []
for x in maestro:
    i = int(x['id'])
    activo = x['activo'] == 'SI'
    contencion = x['contencion_vigente'] if x['contencion_vigente'] != '-' else ''
    decisiones = pend.get(i, [])
    claves = ', '.join(d['clave'] for d in decisiones)
    bloques = sorted({d['bloque'] for d in decisiones})
    restriccion, motivo, sub = '', '', ''
    if not activo:
        estado, motivo = 'D', 'Inactivo en el catalogo (' + (x['inactivo_motivo'] or x['inactivo_clase'] or 'sin motivo registrado') + ')'
    elif i in RETEN:
        estado, sub, motivo = 'C', 'INC-4C-03', 'Retenido: no se prescribe a nadie (identidad ambigua o criterio de salida pendiente de profesional)'
        restriccion = 'Ninguna planificacion nueva'
    elif i in IDENT:
        estado, sub = 'C', 'INC-CAT-CONS-v1:identidad'
        restriccion = 'Ninguna planificacion nueva'
        motivo = 'Identidad del movimiento AMBIGUA en la revision visual (R2): nombre o instrucciones no describen con certeza el GIF'
    elif i in APOYO or i in DIFIC or contencion in ('INC-CAT-C39', 'INC-CAT-1374'):
        estado, sub = 'B', 'B2'
        partes, mot = [], []
        if contencion == 'INC-CAT-C39':
            partes.append('INC-CAT-C39: no para quien declara la restriccion que el texto pide incumplir (brazos arriba o colgarse)')
            mot.append('Texto del catalogo contra bandera (caso 39)')
        if i in C1374:
            partes.append('INC-CAT-1374: solo para perfiles con barra y sin restricciones de colgarse/brazos arriba')
            mot.append('Contencion INC-CAT-1374')
        if i in APOYO:
            partes.append('INC-CAT-CONS-v1: solo con confirmacion explicita del permiso de apoyo suspendido (hoy nadie)')
            mot.append('Apoyo suspendido sin soporte de pies (V53); el permiso propio no se puede confirmar todavia')
        if i in DIFIC:
            partes.append('INC-CAT-CONS-v1: no para principiantes')
            mot.append('Dificultad almacenada 1 (V39) con R2 y V3 en nivel 2: posible subestimacion')
        restriccion, motivo = '; '.join(partes), '; '.join(mot)
    elif decisiones and ('DIFICULTAD' in bloques or 'SEGURIDAD' in bloques):
        estado, sub = 'B', 'B1'
        mot = []
        if 'DIFICULTAD' in bloques:
            mot.append('Se conserva la dificultad vigente, mas alta que la propuesta de la IA (no se baja sin evidencia)')
        if 'SEGURIDAD' in bloques:
            mot.append('Se conserva la bandera de seguridad vigente (conservadora) pendiente de confirmacion profesional')
        restriccion = 'Dificultad y banderas vigentes (las mas restrictivas)'
        motivo = '; '.join(mot)
    elif i in POSTERIORES:
        estado = 'A'
        motivo = ('Identidad determinada con la animacion completa y nombre e instrucciones corregidos (V56, '
                  'REVISION_853.md); sale de INC-CAT-CONS-v1. La decision profesional sigue abierta: '
                  + ', '.join(bloques))
    else:
        estado = 'A'
        motivo = 'Sin incertidumbre material conocida' + (' (decision pendiente no material: ' + ', '.join(bloques) + ')' if bloques else '')
    filas.append({
        'id': i, 'activo': 'SI' if activo else 'NO', 'estado': estado, 'subtipo': sub,
        'estado_descripcion': {'A': 'Elegible', 'B': 'Elegible con restricciones', 'C': 'Excluido temporalmente', 'D': 'Inactivo'}[estado],
        'nombre_es': x['nombre_es'], 'nombre_fuente': x['nombre_en_fuente'], 'zona': x['zona'], 'patron': x['patron'],
        'dificultad': x['dificultad'], 'equipo': x['requisitos'] or x['equipo_principal'], 'banderas': x['banderas'],
        'impacto': x['impacto'], 'apoyo_suspendido': x['apoyo_suspendido'], 'contencion_previa': contencion,
        'restriccion': restriccion, 'motivo': motivo, 'decisiones_pendientes': claves,
        'bloques_pendientes': ', '.join(bloques), 'estado_auditoria': x['estado_cierre'] or x['estado_auditoria_integral'],
        'gif': x['gif'], 'validacion_profesional': 'NINGUNA',
    })

os.makedirs(salida, exist_ok=True)
campos = list(filas[0].keys())
with open(os.path.join(salida, 'catalogo_clasificado.tsv'), 'w', newline='') as f:
    w = csv.DictWriter(f, fieldnames=campos, delimiter='\t'); w.writeheader(); w.writerows(filas)
c = collections.Counter((r['estado'], r['subtipo']) for r in filas)
print({k: v for k, v in sorted(c.items())}, 'activos', sum(1 for r in filas if r['activo'] == 'SI'))

from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.utils import get_column_letter
COLORES = {'A': 'E2EFDA', 'B': 'FFF2CC', 'C': 'F8CBAD', 'D': 'D9D9D9'}
def libro(nombre, hojas, leeme):
    wb = Workbook(); ws0 = wb.active; ws0.title = 'Leeme'
    for linea in leeme: ws0.append([linea])
    ws0.column_dimensions['A'].width = 140
    for titulo, datos in hojas:
        ws = wb.create_sheet(titulo); ws.append(campos)
        for c_ in ws[1]: c_.font = Font(bold=True)
        for r in datos:
            ws.append([r[k] for k in campos])
            ws.cell(ws.max_row, campos.index('estado') + 1).fill = PatternFill('solid', fgColor=COLORES[r['estado']])
        ws.auto_filter.ref = ws.dimensions; ws.freeze_panes = 'B2'
        for j, k in enumerate(campos, 1):
            ws.column_dimensions[get_column_letter(j)].width = 60 if k in ('motivo', 'restriccion') else (34 if k in ('nombre_es', 'nombre_fuente', 'gif') else 14)
        for row in ws.iter_rows(min_row=2):
            for c_ in row: c_.alignment = Alignment(vertical='top', wrap_text=c_.column_letter in ('Q', 'R'))
    wb.save(os.path.join(salida, nombre))
LEEME = ['FITSENSE-CATALOGO-TECNICO-CONSERVADOR-v1 · clasificacion operacional (generada por herramientas/clasificar_catalogo.py).',
         'A = elegible sin restriccion adicional; B = elegible con restricciones (B1 conservadora, B2 contencion temporal por perfil);',
         'C = excluido temporalmente de planificaciones nuevas; D = inactivo. Los IDs y el historial se conservan en todos los casos.',
         'NINGUN estado es una validacion profesional: la revision fue asistida por IA y por controles automaticos.',
         'Use los filtros de la fila de cabecera para filtrar por estado, subtipo, restriccion o motivo.']
activos = [r for r in filas if r['activo'] == 'SI']
libro('CATALOGO_ELEGIBLE_FINAL.xlsx', [('Activos', activos), ('Elegibles_A', [r for r in filas if r['estado'] == 'A']),
                                       ('Inactivos_D', [r for r in filas if r['estado'] == 'D']), ('Todos', filas)], LEEME)
libro('EJERCICIOS_RESTRINGIDOS.xlsx', [('Restringidos_B', [r for r in filas if r['estado'] == 'B']),
                                       ('Excluidos_C', [r for r in filas if r['estado'] == 'C'])], LEEME)
