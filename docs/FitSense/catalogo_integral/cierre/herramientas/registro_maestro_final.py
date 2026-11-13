"""Registro maestro FINAL del catalogo (1.376 filas) tras el cierre (V51-V53).
Parte de la linea base V50 congelada, aplica V51 (valores congelados), V52 (decisiones_v52.tsv) y V53, y une por id:
revision 1 (registro_maestro.tsv), segunda revision, verificacion independiente, adjudicacion, revision profesional,
inactivos y procedencia por migracion. La exactitud del catalogo reconstruido la garantiza la huella de CatalogoCalidadIT,
con los mismos 438 ejercicios cambiados.
Uso: python3 -I -B registro_maestro_final.py <dir catalogo_integral> <salida.xlsx> <salida.tsv>"""
import csv, gzip, json, os, re, sys, collections
csv.field_size_limit(10**8)
base, out_x, out_t = sys.argv[1:4]
ci = lambda *p: os.path.join(base, *p)
rd = lambda f: list(csv.DictReader(open(f, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE))
mig = os.path.join(base, '../../../src/main/resources/db/migration/')
C = {r['id']: dict(r) for r in rd(ci('cierre/linea_base/catalogo_pre_cierre_V50.tsv'))}
for m in re.finditer(r"^\((\d+), '(\w+)', (TRUE|FALSE)\)", open(mig + 'V51__catalogo_impacto_y_estiramiento_explicitos.sql', encoding='utf-8').read(), re.M):
    C[m.group(1)]['impact'] = m.group(2); C[m.group(1)]['stretch'] = 't' if m.group(3) == 'TRUE' else 'f'
COL = {'name_es': 'name_es', 'instructions_es': 'instr_es', 'movement_pattern': 'pattern', 'default_prescription': 'presc',
       'difficulty_level': 'dif', 'impact_level': 'impact', 'is_stretch': 'stretch', 'high_impact': 'hi', 'requires_floor': 'floor',
       'axial_load': 'axial', 'requires_kneeling': 'kneel', 'requires_hanging': 'hang', 'requires_overhead': 'overhead',
       'requires_inversion': 'inv'}
cambios = collections.defaultdict(list)
for d in rd(ci('cierre/adjudicacion/decisiones_v52.tsv')):
    c = C[d['id']]; cambios[d['id']].append(d['atributo'])
    if d['atributo'] in COL:
        v = d['despues']
        if d['atributo'] == 'instructions_es': v = ' '.join(p.strip() for p in v.split('||') if p.strip())
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
for r in rd(ci('cierre/REVISION_PROFESIONAL_LISTA.tsv')): PRO[r['id']].add(r['categoria'])
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
       'inactivo_clase', 'inactivo_motivo', 'instrucciones_es']
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
                  INA.get(i, {}).get('clase', '-'), INA.get(i, {}).get('motivo', '-'), c['instr_es']])
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
print(len(filas), dict(cnt), 'V53', len(v53_marcados))
