"""Adjudicacion del cierre del catalogo: une las propuestas de la segunda revision (R2), la verificacion independiente
(V3) y las decisiones del coordinador, y produce las entradas de V52 y V53 y la lista de lo que NO se aplica.

Reglas (PROTOCOLO_VERIFICACION_3 y orden de cierre):
  * BANDERA_ANADIR, IMPACTO al alza, APOYO_SUSPENDIDO, NOMBRE, INSTRUCCIONES, PATRON, PRESCRIPCION, ESTIRAMIENTO,
    DIFICULTAD_SUBIR: se aplican si V3 ACEPTA (o MEJORA un texto). El coordinador puede vetar (NO_APLICAR) con motivo.
  * BANDERA_QUITAR e IMPACTO a la baja (relajan una restriccion): exigen R2 + V3 ACEPTAR + coordinador APLICAR.
  * EQUIPO y ZONA_OBJETIVO: exigen V3 ACEPTAR y el valor exacto escrito por el coordinador (formato de V52).
  * DIFICULTAD_BAJAR: nunca se aplica (V27: aprobacion humana); se lista para la revision profesional.
  * IDENTIDAD y NO_DECIDIBLE: no cambian nada; se listan para la revision profesional salvo resolucion del coordinador.
  * APOYO_SUSPENDIDO de clase MAQUINA_PARCIAL: se documenta, no se marca (la maquina reduce la carga).
Uso: python3 -I -B adjudicar.py <propuestas.tsv> <dir verif> <coordinador.tsv> <catalogo_V50.tsv> <V51.sql> <dir salida>"""
import csv, glob, json, os, re, sys, collections
csv.field_size_limit(10**8)
prop_f, verif_d, coord_f, cat_f, v51_f, out = sys.argv[1:7]
rd = lambda f: list(csv.DictReader(open(f, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE))
C = {r['id']: r for r in rd(cat_f)}
EXPL = {m.group(1): [m.group(2), 't' if m.group(3) == 'TRUE' else 'f']
        for m in re.finditer(r"^\((\d+), '(\w+)', (TRUE|FALSE)\)", open(v51_f, encoding='utf-8').read(), re.M)}
P = rd(prop_f)
V = {}
for d in sorted(glob.glob(os.path.join(verif_d, '*'))):
    pf, vf = os.path.join(d, 'propuestas.tsv'), os.path.join(d, 'verificacion3.tsv')
    if not os.path.exists(vf): continue
    ps, vs = rd(pf), rd(vf)
    assert len(ps) == len(vs), d
    for p, v in zip(ps, vs):
        assert p['id'] == v['id'] and p['tipo'] == v['tipo'], (d, p['id'], v['id'])
        V[(p['id'], p['tipo'], p['actual'])] = {**v, 'lote_v': os.path.basename(d)}
K = {}
if os.path.exists(coord_f):
    for r in rd(coord_f): K[(r['id'], r['tipo'], r.get('actual', ''))] = r
FLAG = {'HI': ('high_impact', 'hi'), 'FL': ('requires_floor', 'floor'), 'AX': ('axial_load', 'axial'), 'KN': ('requires_kneeling', 'kneel'),
        'HG': ('requires_hanging', 'hang'), 'OH': ('requires_overhead', 'overhead'), 'INV': ('requires_inversion', 'inv')}
NIV = {'BAJO': 0, 'MODERADO': 1, 'ALTO': 2}
dec, v53, pend, traza = [], [], [], []
estado = {i: {'hi': C[i]['hi'], 'imp': EXPL[i][0]} for i in C}
def add(i, atr, antes, despues, fund): dec.append({'id': i, 'name_en': C[i]['name_en'], 'atributo': atr, 'antes': antes, 'despues': despues, 'fundamento': fund})
def coord(p):
    return K.get((p['id'], p['tipo'], p['actual'])) or K.get((p['id'], p['tipo'], ''))
for p in P:
    i, t = p['id'], p['tipo']
    v = V.get((i, t, p['actual']))
    k = coord(p)
    vd = v['decision'] if v else 'SIN_VERIFICAR'
    kd = k['decision'] if k else ''
    res, motivo = 'NO_APLICADO', ''
    fund = f"R2 {p['lote']} ({p['confianza']}): {p['motivo'][:160]} / V3 {v['lote_v'] if v else '-'} {vd}: {v['evidencia'][:160] if v else '-'}"
    if t == 'DIFICULTAD_BAJAR':
        res, motivo = 'PROFESIONAL', 'bajar una dificultad exige aprobacion humana (V27)'
    elif kd == 'NO_APLICAR':
        motivo = 'vetado por el coordinador: ' + k['fundamento']
    elif t == 'IDENTIDAD' or vd == 'NO_DECIDIBLE':
        res, motivo = ('RESUELTO', k['fundamento']) if kd == 'RESUELTO' else ('PROFESIONAL', 'identidad o evidencia no decidible con el GIF')
    elif vd not in ('ACEPTAR', 'MEJORAR'):
        motivo = f'V3 {vd}'
    elif t == 'BANDERA_ANADIR':
        col, c = FLAG[p['actual']]
        add(i, col, 'f', 't', fund); res = 'APLICADO'
        if p['actual'] == 'HI':
            estado[i]['hi'] = 't'
            if estado[i]['imp'] == 'BAJO': add(i, 'impact_level', 'BAJO', 'ALTO', 'HI anadido: el impacto explicito no puede ser BAJO'); estado[i]['imp'] = 'ALTO'
    elif t == 'BANDERA_QUITAR':
        if kd == 'APLICAR':
            col, c = FLAG[p['actual']]; add(i, col, 't', 'f', fund + ' / coordinador: ' + k['fundamento']); res = 'APLICADO'
            if p['actual'] == 'HI': estado[i]['hi'] = 'f'
        else: res, motivo = 'PENDIENTE_COORDINADOR', 'quitar una bandera exige la adjudicacion del coordinador'
    elif t == 'IMPACTO':
        nuevo = v['valor_final'] if v['valor_final'] in NIV else p['propuesto']
        sube = NIV[nuevo] > NIV[estado[i]['imp']]
        if nuevo == estado[i]['imp']: res, motivo = 'SIN_CAMBIO', 'ya en ese nivel'
        elif sube or kd == 'APLICAR':
            add(i, 'impact_level', estado[i]['imp'], nuevo, fund + (' / coordinador: ' + k['fundamento'] if k else '')); estado[i]['imp'] = nuevo; res = 'APLICADO'
        else: res, motivo = 'PENDIENTE_COORDINADOR', 'bajar el impacto exige la adjudicacion del coordinador'
    elif t == 'APOYO_SUSPENDIDO':
        clase = v['valor_final'] if v['valor_final'] not in ('', '-', 'NO') else p['propuesto']
        if clase == 'MAQUINA_PARCIAL': res, motivo = 'DOCUMENTADO', 'maquina asistida: reduce la carga; no se marca'
        else: v53.append({'id': i, 'name_en': C[i]['name_en'], 'clase': clase, 'fundamento': fund}); res = 'V53'
    elif t in ('NOMBRE', 'INSTRUCCIONES'):
        val = (k['valor'] if kd == 'APLICAR' and k.get('valor') else v['valor_final'] if vd == 'MEJORAR' else p['propuesto'])
        add(i, 'name_es' if t == 'NOMBRE' else 'instructions_es', C[i]['name_es'] if t == 'NOMBRE' else C[i]['instr_es'], val, fund); res = 'APLICADO'
    elif t == 'PATRON':
        add(i, 'movement_pattern', C[i]['pattern'], v['valor_final'] if vd == 'ACEPTAR' and v['valor_final'] else p['propuesto'], fund); res = 'APLICADO'
    elif t == 'PRESCRIPCION':
        add(i, 'default_prescription', C[i]['presc'], p['propuesto'], fund); res = 'APLICADO'
    elif t == 'ESTIRAMIENTO':
        add(i, 'is_stretch', EXPL[i][1], 't' if p['propuesto'] == 'SI' else 'f', fund); res = 'APLICADO'
    elif t == 'DIFICULTAD_SUBIR':
        nuevo = int(k['valor']) if kd == 'APLICAR' and k.get('valor') else min(3, int(C[i]['dif']) + 1)
        if nuevo > int(C[i]['dif']): add(i, 'difficulty_level', C[i]['dif'], str(nuevo), fund); res = 'APLICADO'
        else: res, motivo = 'SIN_CAMBIO', 'ya en el maximo'
    elif t in ('EQUIPO', 'ZONA_OBJETIVO'):
        if kd == 'APLICAR' and k.get('valor'):
            add(i, 'equipo' if t == 'EQUIPO' else 'zona', k['antes'] if k.get('antes') else p['actual'], k['valor'], fund); res = 'APLICADO'
        else: res, motivo = 'PENDIENTE_COORDINADOR', 'falta el valor exacto del coordinador'
    traza.append({'id': i, 'tipo': t, 'actual': p['actual'], 'propuesto': p['propuesto'][:200], 'r2_lote': p['lote'], 'r2_confianza': p['confianza'],
                  'v3_lote': v['lote_v'] if v else '-', 'v3_decision': vd, 'v3_confianza': v['confianza'] if v else '-',
                  'v3_evidencia': v['evidencia'] if v else '-', 'coordinador': kd or '-', 'resultado': res, 'motivo': motivo})
# coherencia de la prescripcion con la frase final de las instrucciones que no se reescriben
textos = {d['id'] for d in dec if d['atributo'] == 'instructions_es'}
for d in list(dec):
    if d['atributo'] == 'default_prescription' and d['id'] not in textos:
        i = d['id']; pasos = json.loads(C[i]['steps_es']) if C[i]['steps_es'] else []
        fin = {'DURATION': 'Mantén la duración indicada.', 'SETS_REPS': 'Repite el número de repeticiones indicado.'}
        if pasos and pasos[-1] == fin[d['antes']]:
            add(i, 'instructions_es', C[i]['instr_es'], ' || '.join(pasos[:-1] + [fin[d['despues']]]), 'frase final coherente con la nueva prescripcion')
        else: pend.append({'id': i, 'tipo': 'PRESCRIPCION_TEXTO', 'motivo': 'la frase final no es la reglamentaria; revisar a mano'})
# un ejercicio con HI no puede quedar en BAJO
for i, e in estado.items():
    if e['hi'] == 't' and e['imp'] == 'BAJO': pend.append({'id': i, 'tipo': 'HI_CON_BAJO', 'motivo': 'contradice ck_exercises_high_impact_not_bajo'})
os.makedirs(out, exist_ok=True)
def w(nombre, filas, campos):
    with open(os.path.join(out, nombre), 'w', encoding='utf-8', newline='') as fh:
        x = csv.DictWriter(fh, fieldnames=campos, delimiter='\t', quoting=csv.QUOTE_NONE, escapechar='\\'); x.writeheader(); x.writerows(filas)
w('decisiones_v52.tsv', dec, ['id', 'name_en', 'atributo', 'antes', 'despues', 'fundamento'])
w('apoyo_suspendido_v53.tsv', v53, ['id', 'name_en', 'clase', 'fundamento'])
w('adjudicacion_traza.tsv', traza, list(traza[0].keys()))
w('pendientes_adjudicacion.tsv', pend, ['id', 'tipo', 'motivo'])
print('decisiones', len(dec), collections.Counter(d['atributo'] for d in dec))
print('v53', len(v53), collections.Counter(x['clase'] for x in v53))
print('resultado', collections.Counter((t['tipo'], t['resultado']) for t in traza))
print('pendientes', len(pend))
