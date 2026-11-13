"""Lotes de la verificacion independiente (tercera mirada) a partir de propuestas.tsv de la segunda revision.
Excluye DIFICULTAD_BAJAR (nunca se aplica sin aprobacion humana, V27). Agrupa por ejercicio (todas las propuestas de un
id van al mismo verificador) y reparte en lotes de ~TAM propuestas. Solo incluye los lotes de origen indicados.
Uso: python3 -I -B lotes_verificacion3.py <propuestas.tsv> <catalogo.tsv> <dir salida> <prefijo> <TAM> L01,L02,..."""
import csv, os, sys, collections
csv.field_size_limit(10**8)
prop, cat, out, pref, tam, origenes = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], int(sys.argv[5]), set(sys.argv[6].split(','))
C = {r['id']: r for r in csv.DictReader(open(cat, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
P = [r for r in csv.DictReader(open(prop, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)
     if r['lote'] in origenes and r['tipo'] != 'DIFICULTAD_BAJAR']
por_id = collections.OrderedDict()
for r in sorted(P, key=lambda r: int(r['id'])): por_id.setdefault(r['id'], []).append(r)
B = [('hi', 'HI'), ('floor', 'FL'), ('axial', 'AX'), ('kneel', 'KN'), ('hang', 'HG'), ('overhead', 'OH'), ('inv', 'INV')]
lotes, act = [], []
for i, rs in por_id.items():
    if act and sum(len(x) for x in act) + len(rs) > tam: lotes.append(act); act = []
    act.append(rs)
if act: lotes.append(act)
CAMPOS = ['id', 'tipo', 'actual', 'propuesto', 'motivo', 'nombre_actual', 'nombre_fuente_en', 'zona', 'patron', 'prescripcion',
          'dificultad', 'equipo', 'banderas', 'instrucciones_actuales']
for k, lote in enumerate(lotes, 1):
    d = os.path.join(out, f'{pref}{k:02d}'); os.makedirs(d, exist_ok=True)
    with open(os.path.join(d, 'propuestas.tsv'), 'w', encoding='utf-8', newline='') as fh:
        w = csv.writer(fh, delimiter='\t', quoting=csv.QUOTE_NONE, escapechar='\\')
        w.writerow(CAMPOS)
        for rs in lote:
            for r in rs:
                c = C[r['id']]
                w.writerow([r['id'], r['tipo'], r['actual'], r['propuesto'], r['motivo'], c['name_es'], c['name_en'],
                            f"{c['bp']}/{c['target']}", c['pattern'], c['presc'], c['dif'], f"{c['equip']}; {c['reqs'] or '-'}",
                            ' '.join(t for col, t in B if c[col] == 't') or '-', c['instr_es']])
print(len(P), 'propuestas en', len(lotes), 'lotes', [sum(len(x) for x in l) for l in lotes])
