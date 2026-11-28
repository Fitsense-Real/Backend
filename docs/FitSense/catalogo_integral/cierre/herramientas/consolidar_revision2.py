"""Consolida la segunda revision visual (20 lotes) contra el catalogo final V50 y la primera revision (registro maestro).
Salida: revision2_consolidada.tsv (una fila por id), propuestas.tsv (un cambio propuesto por fila, para verificar) y
un resumen. Uso: python3 -I -B consolidar_revision2.py <dir lotes> <catalogo.tsv> <registro_maestro.tsv> <dir salida>"""
import csv, glob, os, re, sys, collections
csv.field_size_limit(10**8)
lotes, cat, reg, out = sys.argv[1:5]
V51 = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../../../src/main/resources/db/migration/V51__catalogo_impacto_y_estiramiento_explicitos.sql')
EXPL = {m.group(1): (m.group(2), m.group(3) == 'TRUE') for m in re.finditer(r"^\((\d+), '(\w+)', (TRUE|FALSE)\)", open(V51, encoding='utf-8').read(), re.M)}
COLS = ['id','identidad','movimiento_observado','fotogramas_clave','pies','impacto','cuelga','apoyo_suspendido','brazos_sobre_cabeza','suelo','rodilla_apoyada','invertido','carga_axial','banderas_correctas','banderas_anadir','banderas_quitar','nombre','nombre_propuesto','instrucciones','problema_instrucciones','instrucciones_propuestas','equipo','equipo_propuesto','zona_objetivo','zona_objetivo_propuesto','patron','patron_propuesto','prescripcion','estiramiento','dificultad','exigencias','confianza','observaciones']
C = {r['id']: r for r in csv.DictReader(open(cat, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
R1 = {r['ID']: r for r in csv.DictReader(open(reg, encoding='utf-8'), delimiter='\t')}
R2, origen = {}, {}
for f in sorted(glob.glob(os.path.join(lotes, 'L*/revision2.tsv'))):
    lote = os.path.basename(os.path.dirname(f))
    for r in csv.DictReader(open(f, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE):
        R2[r['id']] = r; origen[r['id']] = lote
activos = sorted((i for i, r in C.items() if r['active'] == 't'), key=int)
faltan = [i for i in activos if i not in R2]
B = {'hi': 'HI', 'floor': 'FL', 'axial': 'AX', 'kneel': 'KN', 'hang': 'HG', 'overhead': 'OH', 'inv': 'INV'}
def banderas(c): return {t for k, t in B.items() if c[k] == 't'}
prop = []
def p(i, tipo, actual, propuesto, motivo):
    prop.append({'id': i, 'lote': origen.get(i, ''), 'tipo': tipo, 'actual': actual, 'propuesto': propuesto, 'motivo': motivo,
                 'confianza': R2[i]['confianza'], 'revision1': R1.get(i, {}).get('Estado final', ''),
                 'nombre': C[i]['name_es']})
for i in activos:
    if i not in R2: continue
    r, c = R2[i], C[i]
    fl = banderas(c)
    for t in r['banderas_anadir'].split():
        if t in B.values() and t not in fl: p(i, 'BANDERA_ANADIR', t, t, r['observaciones'])
    for t in r['banderas_quitar'].split():
        if t in B.values() and t in fl: p(i, 'BANDERA_QUITAR', t, '-', r['observaciones'])
    if r['apoyo_suspendido'] not in ('NO', '', '-'): p(i, 'APOYO_SUSPENDIDO', '-', r['apoyo_suspendido'], r['movimiento_observado'])
    if r['nombre'] == 'NO': p(i, 'NOMBRE', c['name_es'], r['nombre_propuesto'], r['observaciones'])
    if r['instrucciones'] == 'NO': p(i, 'INSTRUCCIONES', c['instr_es'], r['instrucciones_propuestas'], r['problema_instrucciones'])
    if r['equipo'] == 'NO': p(i, 'EQUIPO', f"{c['equip']}; {c['reqs'] or '-'}", r['equipo_propuesto'], r['observaciones'])
    if r['zona_objetivo'] == 'NO': p(i, 'ZONA_OBJETIVO', f"{c['bp']}/{c['target']}", r['zona_objetivo_propuesto'], r['observaciones'])
    if r['patron'] == 'NO': p(i, 'PATRON', c['pattern'], r['patron_propuesto'], r['observaciones'])
    if r['prescripcion'] == 'NO': p(i, 'PRESCRIPCION', c['presc'], 'DURATION' if c['presc'] == 'SETS_REPS' else 'SETS_REPS', r['observaciones'])
    if r['dificultad'] in ('SUBIR', 'BAJAR'): p(i, 'DIFICULTAD_' + r['dificultad'], c['dif'], r['dificultad'], r['exigencias'])
    imp, est = EXPL[i]
    if r['impacto'] != imp: p(i, 'IMPACTO', imp, r['impacto'], f"pies={r['pies']}; {r['movimiento_observado']}")
    if (r['estiramiento'] == 'SI') != est: p(i, 'ESTIRAMIENTO', 'SI' if est else 'NO', r['estiramiento'], r['movimiento_observado'])
    if r['identidad'] != 'CLARA': p(i, 'IDENTIDAD', r['identidad'], '-', r['observaciones'])
os.makedirs(out, exist_ok=True)
with open(os.path.join(out, 'propuestas.tsv'), 'w', encoding='utf-8', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=['id','lote','tipo','actual','propuesto','motivo','confianza','revision1','nombre'], delimiter='\t')
    w.writeheader(); w.writerows(prop)
with open(os.path.join(out, 'revision2_consolidada.tsv'), 'w', encoding='utf-8', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=['lote'] + COLS, delimiter='\t')
    w.writeheader()
    for i in activos:
        if i in R2: w.writerow({'lote': origen[i], **{k: R2[i].get(k, '') for k in COLS}})
print('activos', len(activos), 'revisados', len(R2), 'faltan', len(faltan))
print(collections.Counter(x['tipo'] for x in prop).most_common())
print('identidad', collections.Counter(R2[i]['identidad'] for i in R2), 'confianza', collections.Counter(R2[i]['confianza'] for i in R2))
