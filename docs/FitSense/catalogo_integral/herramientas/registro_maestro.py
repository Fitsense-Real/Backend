"""Construye el registro maestro de calidad del catalogo (una fila por ejercicio).

Uso: python3 -I -B registro_maestro.py <dir_trabajo> <salida_dir>
<dir_trabajo> contiene: base/catalogo.tsv (linea base, V1-V46), final/catalogo.tsv (tras V47-V50),
lotes/LNN/revision.tsv (revision visual), correcciones_texto_consolidadas.json (textos V49).
Escribe REGISTRO_MAESTRO_CATALOGO.xlsx y registro_maestro.tsv. Reglas de estado: ver INFORME_CATALOGO_INTEGRAL.md §8.
"""
import csv, glob, json, os, sys, collections
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.utils import get_column_letter

W, OUT = sys.argv[1], sys.argv[2]
def tsv(p): return {r['id']: r for r in csv.DictReader(open(p, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
base, final = tsv(f'{W}/base/catalogo.tsv'), tsv(f'{W}/final/catalogo.tsv')
rev, lote = {}, {}
for f in sorted(glob.glob(f'{W}/lotes/L*/revision.tsv')):
    for r in csv.DictReader(open(f, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE):
        rev[r['id']] = r; lote[r['id']] = f.split('/')[-2]
corr = json.load(open(f'{W}/correcciones_texto_consolidadas.json', encoding='utf-8'))

FL = [('hi', 'HI'), ('floor', 'FL'), ('axial', 'AX'), ('kneel', 'KN'), ('hang', 'HG'), ('overhead', 'OH'), ('inv', 'INV')]
def banderas(r): return ' '.join(t for k, t in FL if r[k] == 't') or '-'
V47 = {'66','206','474','497','503','594','1300','149','220','227','276','819','889','999','1000','1213','1214','275','685','805','895','460','496','696','995','1005'}
V48 = {'464','1108','1191','862','874','868','880','915','916','1192','821','1201'}
V50_CLAS = {'300','322','353','382','407','574','875','1070','1254'}
V50_EQ = {'316','1306','1307','912','212','496','820','1195','1196','1201','61','62','871','368','532','997','908'}
C39_ORIGINAL = {'316','1301','1306','1153','1303','1304','1152','131','400','448','1272','1309','1322','55','224','637','741','875','1062','1063','1093','1095','1156','1181','1193','1311'}
C39_RESTANTES = {'1301','1306','1153','1303','1304','131','400','448'}
RETENIDOS = {'813','858','861','873','885','1192','1298'}
DIF_BAJADA_PROPUESTA = set()  # se rellena: dificultad_ok = NO y no subida por V48

def pend_texto(i):
    c = corr.get(i)
    return bool(c) and ('PENDIENTE' in (c['nombre_nuevo'] + c['instrucciones_nuevas']))

cols = ['ID', 'Activo', 'Nombre (línea base)', 'Nombre (final)', 'Nombre de la fuente (en)', 'Movimiento verificado (GIF)',
        'Revisión automatizada', 'Revisión visual', 'Validación profesional', 'Lote de revisión', 'GIF revisado',
        'Nombre correcto', 'Instrucciones correctas', 'Equipamiento correcto', 'Grupo y patrón correctos',
        'Dificultad adecuada', 'Seguridad correcta', 'Apoyo suspendido', 'Banderas (base)', 'Banderas (final)',
        'Banderas en duda', 'Dificultad (base→final)', 'Gravedad', 'Problema encontrado', 'Corrección propuesta',
        'Corrección aplicada', 'Migraciones', 'Contención necesaria', 'Evidencia', 'Estado final', 'Motivo del estado']
filas = []
for i in sorted(base, key=int):
    b, f = base[i], final.get(i, base[i])
    if b['active'] != 't':
        filas.append([int(i), 'No', b['name_es'], f['name_es'], b['name_en'], '', 'No', 'No', 'No', '', 'No'] + [''] * 6 +
                     ['', banderas(b), banderas(f), '', f"d{b['dif']}", '', '', '', 'No', '', 'No', '', 'INACTIVO', 'Fuera de alcance: no elegible'])
        continue
    r = rev[i]; c = corr.get(i, {})
    migr, aplicada, problemas, propuestas = [], False, [], []
    if i in V47: migr.append('V47'); aplicada = True; problemas.append('bandera de seguridad ausente'); propuestas.append(f"banderas {banderas(b)} → {banderas(f)}")
    if i in V48: migr.append('V48'); aplicada = True; problemas.append('habilidad avanzada en d1'); propuestas.append(f"dificultad d{b['dif']} → d{f['dif']} (contención, a confirmar)")
    nom_new = c.get('nombre_nuevo', '='); ins_new = c.get('instrucciones_nuevas', '=')
    txt_aplicado = (nom_new not in ('=', 'PENDIENTE', '')) or (ins_new not in ('=', 'PENDIENTE', ''))
    if txt_aplicado or b['name_es'] != f['name_es'] or b['instr_es'] != f['instr_es']:
        if txt_aplicado: migr.append('V49')
        aplicada = True
    if i in V50_CLAS or i in V50_EQ: migr.append('V50'); aplicada = True
    def estado_campo(ok, corregido):
        if ok == 'SI': return 'Sí'
        if ok == 'NO': return 'Sí (corregido)' if corregido else 'No'
        return 'Pendiente'
    nombre_c = estado_campo(r['nombre_ok'], b['name_es'] != f['name_es'])
    instr_c = estado_campo(r['instr_ok'], b['instr_es'] != f['instr_es'])
    eq_fix = i in V50_EQ
    banco_sentado = r['equipo_ok'] in ('NO', 'DUDA') and 'bench' in r['equipo_gif'] and not eq_fix and 'sentad' in r['movimiento_gif'].lower()
    if banco_sentado: equipo_c = 'Sí (asiento cualquiera; decisión documentada)'
    else: equipo_c = estado_campo(r['equipo_ok'], eq_fix)
    clas_c = estado_campo(r['clasif_ok'], i in V50_CLAS)
    dif_c = estado_campo(r['dificultad_ok'], i in V48)
    gif_fl = set(r['banderas_gif'].split()) - {'-'}; duda = set(r['banderas_duda'].split()) - {'-'}
    faltan = [t for k, t in FL if t in gif_fl and f[k] != 't']
    sobran = [t for k, t in FL if f[k] == 't' and t not in gif_fl and t not in duda]
    dudas_sin = [t for k, t in FL if t in duda and f[k] != 't']
    seg_c = 'Sí (corregido)' if i in V47 and not faltan else ('No' if faltan else ('Pendiente' if (sobran or dudas_sin or r['apoyo_suspendido'] != 'NO') else 'Sí'))
    if r['nombre_ok'] == 'NO': problemas.append('nombre no describe el GIF')
    if r['instr_ok'] == 'NO': problemas.append('instrucciones no describen el GIF')
    if r['equipo_ok'] == 'NO' and not banco_sentado: problemas.append('equipamiento: ' + r['equipo_gif'])
    if r['clasif_ok'] == 'NO': problemas.append('clasificación: ' + r['clasif_propuesta'])
    if r['dificultad_ok'] == 'NO' and i not in V48: problemas.append('dificultad inadecuada (propuesta de cambio no aplicada)')
    if sobran: problemas.append('banderas de más (no relajadas): ' + ' '.join(sobran))
    if dudas_sin: problemas.append('banderas en duda sin marcar: ' + ' '.join(dudas_sin))
    if r['apoyo_suspendido'] != 'NO': problemas.append('apoyo suspendido: ' + r['apoyo_suspendido'])
    if txt_aplicado: propuestas.append('texto reescrito según el GIF' + (f" (nombre: {f['name_es']})" if b['name_es'] != f['name_es'] else ''))
    if i in V50_CLAS: propuestas.append(f"clasificación {b['bp']}/{b['pattern']} → {f['bp']}/{f['pattern']}")
    if eq_fix: propuestas.append(f"requisitos {b['reqs'] or '-'} → {f['reqs'] or '-'}")
    contencion = 'Sí (INC-CAT-C39, PR #55)' if i in C39_RESTANTES else ('Sí (INC-4C-03)' if i in RETENIDOS else ('Sí (INC-CAT-1374)' if i == '1374' else 'No'))
    # Estado final (prioridad: el pendiente no se oculta tras una correccion parcial)
    pend_prof = (i in V48 or (r['dificultad_ok'] == 'NO' and i not in V48) or sobran or dudas_sin
                 or r['apoyo_suspendido'] != 'NO' or r['clasif_ok'] == 'DUDA' or r['dificultad_ok'] == 'DUDA'
                 or (r['clasif_ok'] == 'NO' and i not in V50_CLAS))
    pend_vis = pend_texto(i) or r['nombre_ok'] == 'DUDA' or r['instr_ok'] == 'DUDA' or r['gif_legible'] != 'SI'
    pend_tec = (r['equipo_ok'] == 'NO' and not eq_fix and not banco_sentado) or (r['equipo_ok'] == 'DUDA' and not banco_sentado and not eq_fix)
    if i in C39_RESTANTES or i in RETENIDOS or i == '1374': estado, motivo = 'CONTENIDO', 'restricción preventiva vigente por seguridad'
    elif pend_vis: estado, motivo = 'PENDIENTE_VISUAL', 'el GIF no permite verificar ' + ('el texto' if pend_texto(i) else 'nombre/instrucciones')
    elif pend_prof: estado, motivo = 'PENDIENTE_PROFESIONAL', '; '.join(p for p in problemas if any(k in p for k in ('bandera', 'apoyo', 'dificultad', 'clasificación'))) or 'clasificación o dificultad en duda'
    elif pend_tec: estado, motivo = 'PENDIENTE_TECNICO', 'equipamiento a resolver: ' + r['equipo_gif']
    elif aplicada: estado, motivo = 'CORREGIDO', 'error identificado, corrección aplicada y validada por pruebas'
    else: estado, motivo = 'VERIFICADO', 'coherente con el GIF en todos los atributos revisados'
    evidencia = f"GIF {b['gif']} (8 fotogramas + imagen {b['image']}); revisión {lote[i]}/revision.tsv"
    filas.append([int(i), 'Sí', b['name_es'], f['name_es'], b['name_en'], r['movimiento_gif'], 'Sí', 'Sí (revisor IA, fotogramas del GIF)',
                  'No', lote[i], 'Sí', nombre_c, instr_c, equipo_c, clas_c, dif_c, seg_c, r['apoyo_suspendido'],
                  banderas(b), banderas(f), ' '.join(sorted(duda)) or '-', f"d{b['dif']}→d{f['dif']}", r['gravedad'],
                  '; '.join(problemas) or '-', '; '.join(propuestas) or '-', 'Sí' if aplicada else 'No', ' '.join(migr) or '-',
                  contencion, evidencia, estado, motivo])

os.makedirs(OUT, exist_ok=True)
with open(f'{OUT}/registro_maestro.tsv', 'w', encoding='utf-8') as o:
    o.write('\t'.join(cols) + '\n')
    for fl in filas: o.write('\t'.join(str(x).replace('\t', ' ') for x in fl) + '\n')
wb = Workbook(); ws = wb.active; ws.title = 'Registro maestro'
ws.append(cols)
for fl in filas: ws.append(fl)
hdr = PatternFill('solid', fgColor='1F4E78')
for cell in ws[1]: cell.font = Font(bold=True, color='FFFFFF'); cell.fill = hdr; cell.alignment = Alignment(wrap_text=True, vertical='top')
COLOR = {'VERIFICADO': 'C6EFCE', 'CORREGIDO': 'DDEBF7', 'CONTENIDO': 'F8CBAD', 'PENDIENTE_VISUAL': 'FFEB9C',
         'PENDIENTE_PROFESIONAL': 'FCE4D6', 'PENDIENTE_TECNICO': 'EDEDED', 'INACTIVO': 'F2F2F2'}
ie = cols.index('Estado final') + 1
for row in ws.iter_rows(min_row=2):
    row[ie - 1].fill = PatternFill('solid', fgColor=COLOR.get(row[ie - 1].value, 'FFFFFF'))
anchos = [7, 7, 34, 34, 34, 48, 10, 16, 12, 8, 8, 14, 14, 18, 14, 12, 14, 14, 12, 12, 10, 10, 9, 48, 48, 9, 12, 18, 40, 20, 40]
for k, w in enumerate(anchos, 1): ws.column_dimensions[get_column_letter(k)].width = w
ws.freeze_panes = 'C2'; ws.auto_filter.ref = ws.dimensions
# Resumen
rs = wb.create_sheet('Resumen')
cnt = collections.Counter(fl[ie - 1] for fl in filas)
rs.append(['Estado final', 'Ejercicios']); [rs.append([k, v]) for k, v in sorted(cnt.items())]
rs.append([]); rs.append(['Total filas', len(filas)]); rs.append(['Activos', sum(1 for fl in filas if fl[1] == 'Sí')])
rs.append(['Con corrección aplicada', sum(1 for fl in filas if fl[cols.index('Corrección aplicada')] == 'Sí')])
rs.append(['Revisión automatizada', sum(1 for fl in filas if fl[6] == 'Sí')])
rs.append(['Revisión visual (IA)', sum(1 for fl in filas if fl[7].startswith('Sí'))])
rs.append(['Validación profesional', 0])
for cell in rs[1]: cell.font = Font(bold=True)
rs.column_dimensions['A'].width = 28
leyenda = wb.create_sheet('Leyenda')
for l in ['VERIFICADO: coherente con el GIF en todos los atributos revisados (revisión visual por IA; sin validación profesional).',
          'CORREGIDO: error identificado y corrección aplicada (V47-V50) y validada por pruebas automáticas.',
          'CONTENIDO: restringido temporalmente por seguridad (INC-CAT-C39 restante, INC-4C-03, INC-CAT-1374).',
          'PENDIENTE_VISUAL: el GIF no permite verificar el movimiento o el texto; no se inventó información.',
          'PENDIENTE_PROFESIONAL: requiere decisión especializada (banderas de más o en duda, apoyo suspendido, dificultad, clasificación dudosa, subidas de dificultad V48 a confirmar).',
          'PENDIENTE_TECNICO: inconsistencia comprobada sin resolver todavía (equipamiento).',
          'Prioridad del estado: CONTENIDO > PENDIENTE_VISUAL > PENDIENTE_PROFESIONAL > PENDIENTE_TECNICO > CORREGIDO > VERIFICADO.',
          '«Revisión visual» la hicieron revisores IA sobre 8 fotogramas + imagen de cada GIF original; NO es validación profesional.']:
    leyenda.append([l])
leyenda.column_dimensions['A'].width = 160
wb.save(f'{OUT}/REGISTRO_MAESTRO_CATALOGO.xlsx')
print(dict(cnt), 'corregidos', sum(1 for fl in filas if fl[cols.index('Corrección aplicada')] == 'Sí'))
