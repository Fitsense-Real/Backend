"""Matriz 26 (PR #55 original) frente a 8 (reducida), con el catalogo final V50. Solo lectura.
Uso: python3 -I -B matriz_26_vs_8.py <catalogo_final_V50.tsv> <salida.tsv>"""
import csv, re, sys
csv.field_size_limit(10**8)
CAT = {r['id']: r for r in csv.DictReader(open(sys.argv[1], encoding='utf-8'), delimiter='\t')}
COLG = re.compile(r"\bcu[eé]lgate\b|\bcolg[aá]ndote\b|\bcolgad[oa]\b|dominad", re.I)
CAB = re.compile(r"por encima de (la|tu) cabeza|sobre la cabeza", re.I)
SUE = re.compile(r"(t[uú]mbate|acu[eé]state|si[eé]ntate|tumbad[oa]|sentad[oa]) (boca (arriba|abajo) |de lado |de espaldas )?"
                 r"(en|sobre) (el suelo|el piso|una esterilla|la esterilla|una colchoneta)|posici[oó]n de (flexi[oó]n|plancha)", re.I)
ORIG = {316: 'COLG CAB', 1301: 'COLG CAB', 1306: 'COLG', 1153: 'COLG', 1303: 'COLG SUE', 1304: 'COLG SUE', 1152: 'COLG',
        131: 'CAB', 400: 'CAB', 448: 'CAB', 1272: 'CAB', 1309: 'CAB', 1322: 'CAB', 55: 'SUE', 224: 'SUE', 637: 'SUE',
        741: 'SUE', 875: 'SUE', 1062: 'SUE', 1063: 'SUE', 1093: 'SUE', 1095: 'SUE', 1156: 'SUE', 1181: 'SUE',
        1193: 'SUE', 1311: 'SUE'}
RED = {1153: 'COLG', 1301: 'COLG', 1303: 'COLG', 1304: 'COLG', 1306: 'COLG', 131: 'CAB', 400: 'CAB', 448: 'CAB'}
MOTIVO_MANTIENE = {
    'COLG': 'apoyo suspendido (cuerpo sostenido por brazos/antebrazos, pies sin apoyo); requires_hanging no lo cubre; decision profesional',
    'CAB': 'curl alto: brazo a la altura del hombro y manos hacia la cabeza; limite de la definicion de requires_overhead',
}
flag = {'COLG': 'hang', 'CAB': 'overhead', 'SUE': 'floor'}
pat = {'COLG': COLG, 'CAB': CAB, 'SUE': SUE}
cols = ['id', 'nombre_final_V50', 'equipo', 'contencion_26', 'contencion_8', 'liberado', 'banderas_HG_OH_FL',
        'texto_pide_lo_liberado', 'bandera_declara_lo_liberado', 'justificacion', 'resultado']
rows = []
for i in sorted(ORIG):
    c = CAT[str(i)]
    o, m = set(ORIG[i].split()), set(RED.get(i, '').split())
    lib = o - m
    texto = c['name_es'] + ' ' + c['instr_es']
    pide = sorted(r for r in lib if pat[r].search(texto))
    declara = sorted(r for r in lib if c[flag[r]] == 't')
    if not lib:
        just = MOTIVO_MANTIENE[next(iter(m))]
        res = 'SE MANTIENE (8)'
    elif m:
        just = (f"liberado {' '.join(sorted(lib))}: el texto V49 ya no lo pide; se mantiene {' '.join(sorted(m))}: "
                + MOTIVO_MANTIENE[next(iter(m))])
        res = 'LIBERADO EN PARTE'
    else:
        just = 'texto V49 coherente con el GIF y con la bandera'
        res = 'LIBERADO (18)'
    if set(pide) - set(declara):
        res += ' ¡INJUSTIFICADO!'
    rows.append({'id': i, 'nombre_final_V50': c['name_es'], 'equipo': c['equip'], 'contencion_26': ' '.join(sorted(o)),
                 'contencion_8': ' '.join(sorted(m)) or '-', 'liberado': ' '.join(sorted(lib)) or '-',
                 'banderas_HG_OH_FL': ''.join(x for x, k in (('HG ', 'hang'), ('OH ', 'overhead'), ('FL ', 'floor')) if c[k] == 't').strip() or '-',
                 'texto_pide_lo_liberado': ' '.join(pide) or 'no', 'bandera_declara_lo_liberado': ' '.join(declara) or '-',
                 'justificacion': just, 'resultado': res})
with open(sys.argv[2], 'w', encoding='utf-8', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=cols, delimiter='\t'); w.writeheader(); w.writerows(rows)
for r in rows:
    print(r['id'], r['resultado'], '|', r['contencion_26'], '->', r['contencion_8'], '| pide:', r['texto_pide_lo_liberado'], '| declara:', r['bandera_declara_lo_liberado'], '|', r['banderas_HG_OH_FL'])
