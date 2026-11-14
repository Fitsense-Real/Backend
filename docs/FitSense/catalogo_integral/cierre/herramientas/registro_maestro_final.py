"""Registro maestro FINAL del catalogo (1.376 filas) tras el cierre (V51-V53).
Parte de la linea base V50 congelada, aplica V51 (valores congelados), V52 (decisiones_v52.tsv) y V53, y une por id:
revision 1 (registro_maestro.tsv), segunda revision, verificacion independiente, adjudicacion, revision profesional,
inactivos y procedencia por migracion. La exactitud del catalogo reconstruido la garantiza la huella de CatalogoCalidadIT,
con los mismos ejercicios cambiados; los textos se contrastan ademas con la firma md5 de la huella.
Uso: python3 -I -B registro_maestro_final.py <dir catalogo_integral> <salida.xlsx> <salida.tsv>"""
import csv, gzip, json, os, re, sys, collections
csv.field_size_limit(10**8)
base, out_x, out_t = sys.argv[1:4]
ci = lambda *p: os.path.join(base, *p)
rd = lambda f: list(csv.DictReader(open(f, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE))
mig = os.path.join(base, '../../../src/main/resources/db/migration/')
C = {r['id']: dict(r) for r in rd(ci('cierre/linea_base/catalogo_pre_cierre_V50.tsv'))}
ANTES = {k: dict(v) for k, v in C.items()}
for m in re.finditer(r"^\((\d+), '(\w+)', (TRUE|FALSE)\)", open(mig + 'V51__catalogo_impacto_y_estiramiento_explicitos.sql', encoding='utf-8').read(), re.M):
    C[m.group(1)]['impact'] = m.group(2); C[m.group(1)]['stretch'] = 't' if m.group(3) == 'TRUE' else 'f'
COL = {'name_es': 'name_es', 'instructions_es': 'instr_es', 'movement_pattern': 'pattern', 'default_prescription': 'presc',
       'difficulty_level': 'dif', 'impact_level': 'impact', 'is_stretch': 'stretch', 'high_impact': 'hi', 'requires_floor': 'floor',
       'axial_load': 'axial', 'requires_kneeling': 'kneel', 'requires_hanging': 'hang', 'requires_overhead': 'overhead',
       'requires_inversion': 'inv'}
# Texto final exacto tal como lo escribe V52 (pasos normalizados por gen_v52.py), no la propuesta cruda.
V52_TXT = {}
for bloque in open(mig + 'V52__catalogo_cierre_correcciones_verificadas.sql', encoding='utf-8').read().split('END $v52$;'):
    ids = re.findall(r"WHERE exercise_id = (\d+) AND", bloque)
    t = re.search(r"UPDATE exercises SET (?:[^\n]*?, )?instructions_es = '((?:[^']|'')*)'", bloque)
    if ids and t: V52_TXT[ids[0]] = t.group(1).replace("''", "'")
cambios = collections.defaultdict(list)
for d in rd(ci('cierre/adjudicacion/decisiones_v52.tsv')):
    c = C[d['id']]; cambios[d['id']].append(d['atributo'])
    if d['atributo'] in COL:
        v = d['despues']
        if d['atributo'] == 'instructions_es': v = V52_TXT[d['id']]
        c[COL[d['atributo']]] = v
    elif d['atributo'] == 'zona': c['bp'], c['target'] = d['despues'].split('/', 1)
    elif d['atributo'] == 'equipo':
        p, r = [x.strip() for x in d['despues'].split(';', 1)]; c['equip'] = p; c['reqs'] = r
v53 = {r['id']: r['clase'] for r in rd(ci('cierre/adjudicacion/apoyo_suspendido_v53.tsv'))}
v53_marcados = set(re.findall(r"^\s+\((\d+), '", open(mig + 'V53__catalogo_apoyo_suspendido.sql', encoding='utf-8').read(), re.M))
R1 = {r['ID']: r for r in rd(ci('registro_maestro.tsv'))}
R2 = {r['id']: r for r in rd(ci('cierre/adjudicacion/revision2_consolidada.tsv'))}
T = collections.defaultdict(list)
for t in rd(ci('cierre/adjudicacion/adjudicacion_traza.tsv')): T[t['id']].append(t)
PRO = collections.defaultdict(set)
# Registro unico de decisiones (orden de desbloqueo, 14/11/2026): cuenta lo que sigue abierto o contenido.
for r in rd(ci('cierre/REGISTRO_DECISIONES_PENDIENTES.tsv')):
    if r['id'] and r['estado'] != 'RESUELTA_TECNICA': PRO[r['id']].add(r['origen'])
INA = {r['id']: r for r in rd(ci('cierre/inactivos/INVENTARIO_INACTIVOS.tsv'))}
proc = {}
with gzip.open(ci('cierre/linea_base/procedencia_cambios_V1_V50.tsv.gz'), 'rt', encoding='utf-8') as fh:
    for r in csv.DictReader(fh, delimiter='\t', quoting=csv.QUOTE_NONE): proc[(r['id'], r['atributo'])] = 'V' + r['version']
def origen(i, atr, attr52):
    if attr52 in cambios[i]: return 'V52'
    return proc.get((i, atr), '-')
B = [('hi', 'HI'), ('floor', 'FL'), ('axial', 'AX'), ('kneel', 'KN'), ('hang', 'HG'), ('overhead', 'OH'), ('inv', 'INV')]
CAB = ['id', 'source_code', 'activo', 'nombre_es', 'nombre_en_fuente', 'zona', 'musculo_objetivo', 'sinergistas', 'secundarios',
       'patron', 'aislamiento', 'prescripcion', 'dificultad', 'equipo_principal', 'requisitos', 'banderas', 'impacto', 'estiramiento',
       'apoyo_suspendido', 'apoyo_suspendido_clase_observada', 'gif', 'estado_auditoria_integral', 'r2_identidad', 'r2_confianza',
       'r2_movimiento_observado', 'r2_propuestas', 'v3_decisiones', 'cambios_V52', 'pendiente_profesional', 'estado_cierre',
       'origen_nombre', 'origen_instrucciones', 'origen_banderas', 'origen_dificultad', 'origen_patron', 'origen_equipo',
       'inactivo_clase', 'inactivo_motivo', 'instrucciones_es',
       'nombre_anterior_V50', 'instrucciones_anteriores_V50', 'equipo_anterior_V50', 'zona_objetivo_anterior_V50',
       'patron_anterior_V50', 'dificultad_anterior_V50', 'banderas_anteriores_V50', 'capacidades', 'problema_encontrado',
       'evidencia_correccion', 'migracion_responsable', 'prueba_asociada', 'contencion_vigente', 'efecto_elegibilidad_168',
       'revision_visual', 'revision_humana', 'validacion_profesional', 'fecha_revision', 'revisor_responsable']
import csv as _csv
CMP = {}
try:
    A_ = {r['clave']: set(r['elegibles'].split(',')) for r in _csv.DictReader(open(ci('cierre/impacto/antes_V50.csv')), delimiter=';')}
    B_ = {r['clave']: set(r['elegibles'].split(',')) for r in _csv.DictReader(open(ci('cierre/impacto/despues_V53.csv')), delimiter=';')}
    for k in A_:
        for x in A_[k] - B_[k]: CMP.setdefault(x, [0, 0])[0] += 1
        for x in B_[k] - A_[k]: CMP.setdefault(x, [0, 0])[1] += 1
except FileNotFoundError: pass
C39 = {'1153', '1301', '1303', '1304', '1306', '131', '400', '448'}
RET = {'813', '858', '861', '873', '885', '1192', '1298'}
V28 = {'122', '776', '872', '878'}
def bandas(c): return ' '.join(tag for col, tag in B if c[col] == 't') or '-'
filas = []
for i, c in sorted(C.items(), key=lambda x: int(x[0])):
    act = c['active'] == 't'; t = T.get(i, []); r2 = R2.get(i, {})
    pro = sorted(PRO.get(i, set()))
    if not act: estado = 'INACTIVO'
    elif pro: estado = 'BLOQUEO_PROFESIONAL'
    elif cambios[i] or i in v53_marcados: estado = 'CORREGIDO_CIERRE'
    else: estado = 'VERIFICADO_CIERRE'
    banderas = ' '.join(tag for col, tag in B if c[col] == 't') or '-'
    flag_orig = max((origen(i, a, a52) for a, a52 in [('high_impact', 'high_impact'), ('requires_floor', 'requires_floor'), ('axial_load', 'axial_load'),
                     ('requires_kneeling', 'requires_kneeling'), ('requires_hanging', 'requires_hanging'), ('requires_overhead', 'requires_overhead'),
                     ('requires_inversion', 'requires_inversion')] if origen(i, a, a52) != '-'), key=lambda v: float(v[1:]), default='-')
    filas.append([i, c['source'], 'SI' if act else 'NO', c['name_es'], c['name_en'], c['bp'], c['target'], c['synergist'], c['secondary'],
                  c['pattern'], c['iso'], c['presc'], c['dif'], c['equip'], c['reqs'] or '-', banderas, c.get('impact', ''),
                  'SI' if c.get('stretch') == 't' else 'NO', 'SI' if i in v53_marcados else 'NO', v53.get(i, '-'), c['gif'],
                  R1.get(i, {}).get('Estado final', '-'), r2.get('identidad', '-'), r2.get('confianza', '-'), r2.get('movimiento_observado', '-'),
                  '; '.join(sorted({x['tipo'] for x in t})) or '-',
                  '; '.join(f"{x['tipo']}:{x['v3_decision']}" for x in t if x['v3_decision'] not in ('SIN_VERIFICAR',)) or '-',
                  ', '.join(sorted(set(cambios[i]))) or '-', '; '.join(pro) or '-', estado,
                  origen(i, 'name_es', 'name_es'), origen(i, 'instructions_es', 'instructions_es'), flag_orig,
                  origen(i, 'difficulty_level', 'difficulty_level'), origen(i, 'movement_pattern', 'movement_pattern'),
                  'V52' if 'equipo' in cambios[i] else proc.get((i, 'requisitos'), '-'),
                  INA.get(i, {}).get('clase', '-'), INA.get(i, {}).get('motivo', '-'), c['instr_es']]
                 + [ANTES[i]['name_es'], ANTES[i]['instr_es'], f"{ANTES[i]['equip']}; {ANTES[i]['reqs'] or '-'}",
                    f"{ANTES[i]['bp']}/{ANTES[i]['target']}", ANTES[i]['pattern'], ANTES[i]['dif'], bandas(ANTES[i]),
                    'derivada en ejecucion (CapabilityPrescriptionPolicy) de patron, zona y prescripcion; no es atributo del catalogo',
                    ('; '.join(f"{x['tipo']}: {x['actual'][:60]} -> {x['propuesto'][:80]}" for x in t if x['resultado'] in ('APLICADO', 'V53'))
                     or ('sin problema aplicable' if act else '-')),
                    ('; '.join((x['v3_evidencia'] if x['v3_evidencia'] != '-' else x['motivo'])[:160] for x in t if x['resultado'] in ('APLICADO', 'V53') and (x['v3_evidencia'] != '-' or x['tipo'] == 'FRASE_FINAL'))[:600] or '-'),
                    ('V52' if cambios[i] else '') + (' V53' if i in v53_marcados else '') or 'sin cambio en el cierre (ver origen_*)',
                    'CatalogoCalidadIT.huella; CatalogoAtributosExplicitosIT' + ('; ContencionTextoBanderaIT' if i in C39 else '') + ('; Plan78ContencionReducidaIT' if i in ('1303', '910', '916') else ''),
                    ('INC-CAT-C39' if i in C39 else '') + ('INC-4C-03' if i in RET else '') + ('INC-CAT-1374' if i == '1374' else '') or '-',
                    (f"sale en {CMP[i][0]} y entra en {CMP[i][1]} de 168 perfiles" if i in CMP else ('sin cambio en la rejilla de 168' if act else 'inactivo')),
                    ('IA: R2 ' + r2.get('lote', '-') + ' (animacion completa)' + ('; V3 ' + ', '.join(sorted({x['v3_lote'] for x in t if x['v3_lote'] != '-'})) if any(x['v3_lote'] != '-' for x in t) else '')) if act else 'no (inactivo)',
                    ('SI: aprobacion de dificultad V28 (Juan y Daniel, 10/10/2026)' if i in V28 else 'NO en el cierre (V24-V26 historicas sin trazabilidad por fila)'),
                    'NO: sin acceso a profesional' + ('; pendiente: ' + '; '.join(pro) if pro else ''),
                    '2026-11-13', 'revisores y verificadores de IA + coordinador (IA); sin firma humana'])
with open(out_t, 'w', encoding='utf-8', newline='') as fh:
    w = csv.writer(fh, delimiter='\t', quoting=csv.QUOTE_NONE, escapechar='\\'); w.writerow(CAB); w.writerows(filas)
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill
from openpyxl.utils import get_column_letter
wb = Workbook(); ws = wb.active; ws.title = 'Registro maestro'
ws.append(CAB)
for f in filas: ws.append(f)
for c in ws[1]: c.font = Font(bold=True, color='FFFFFF'); c.fill = PatternFill('solid', fgColor='305496')
ws.freeze_panes = 'B2'; ws.auto_filter.ref = ws.dimensions
for k, wdt in enumerate([6, 9, 6, 38, 30, 11, 16, 14, 14, 18, 8, 10, 6, 16, 26, 14, 9, 8, 8, 16, 26, 22, 9, 8, 50, 30, 40, 30, 30, 20, 8, 8, 8, 8, 8, 8, 18, 40, 80], 1):
    ws.column_dimensions[get_column_letter(k)].width = wdt
res = wb.create_sheet('Resumen')
cnt = collections.Counter(f[29] for f in filas)
res.append(['Concepto', 'Valor'])
for k, v in [('Filas', len(filas)), ('Activos', sum(f[2] == 'SI' for f in filas)), ('Inactivos', sum(f[2] == 'NO' for f in filas))] + sorted(cnt.items()):
    res.append([k, v])
res.append(['Ejercicios corregidos por V52', sum(1 for i in C if cambios[i])])
res.append(['Marcados con apoyo suspendido (V53)', len(v53_marcados)])
res.append(['Leyenda de estado_cierre', 'VERIFICADO_CIERRE: R2 sin propuestas aplicables o rechazadas por V3; CORREGIDO_CIERRE: corregido en V52/V53; '
            'BLOQUEO_PROFESIONAL: alguna decision exige profesional (ver REVISION_PROFESIONAL.md); INACTIVO: ver inactivo_motivo'])
for c in res[1]: c.font = Font(bold=True)
res.column_dimensions['A'].width = 40; res.column_dimensions['B'].width = 120
wb.save(out_x)
# Contraste con la huella real de la base (CatalogoCalidadIT): dificultad y firma md5 de nombre + instrucciones.
import hashlib
hue = {r['id']: r for r in rd(os.path.join(base, '../../../src/test/resources/catalogo/huella_catalogo.tsv'))}
mal = []
for i, c in C.items():
    if i not in hue: continue
    firma = hashlib.md5((c['name_es'] + '|' + re.sub(r'[\t\r\n]+', ' ', c['instr_es'])).encode('utf-8')).hexdigest()[:12]
    if firma != hue[i]['textos'] or str(c['dif']) != hue[i]['dificultad']: mal.append(i)
if mal: sys.exit(f"registro distinto de la huella real en {len(mal)} ejercicios: {mal[:20]}")
print('contraste con la huella:', len(hue), 'activos, 0 discrepancias de textos y dificultad')
print(len(filas), dict(cnt), 'V53', len(v53_marcados))
