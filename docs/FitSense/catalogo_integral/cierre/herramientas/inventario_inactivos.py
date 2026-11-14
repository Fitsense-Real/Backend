"""Inventario de los ejercicios inactivos: version que los dejo inactivos, motivo documentado en la migracion y estado de
sus atributos. Se calcula SOLO a partir de la linea base (catalogo V50) y de la procedencia por migracion.
Uso: python3 -I -B inventario_inactivos.py <catalogo_V50.tsv> <procedencia_cambios.tsv.gz> <salida.tsv>"""
import csv, gzip, sys, collections
csv.field_size_limit(10**8)
cat, proc, out = sys.argv[1:4]
C = {r['id']: r for r in csv.DictReader(open(cat, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
hist = collections.defaultdict(list)  # (id, atributo) -> [(version, anterior, nuevo)]
with gzip.open(proc, 'rt', encoding='utf-8') as fh:
    for r in csv.DictReader(fh, delimiter='\t', quoting=csv.QUOTE_NONE):
        hist[(r['id'], r['atributo'])].append((float(r['version']), r['anterior'], r['nuevo']))
def valor_en(i, atr, v):
    val = ''
    for ver, _, nuevo in sorted(hist[(i, atr)]):
        if ver <= v: val = nuevo
    return val
V11_EQUIPOS = {'body weight', 'dumbbell', 'band', 'kettlebell', 'stability ball', 'medicine ball', 'barbell', 'cable',
               'ez barbell', 'leverage machine', 'smith machine'}
MOTIVO_EXPLICITO = {
    '24': 'V24: exige un implemento sin codigo de equipamiento (revision humana del catalogo)',
    '36': 'V36: habilidad gimnastica con inversion (Stalder press); fuera del catalogo elegible, fila conservada',
    '37': 'V37: elevator en anillas; colgarse e invertirse con dificultad real 3 (revision de seguridad, sin aprobacion de reactivacion)',
}
filas = []
for i, c in sorted(C.items(), key=lambda x: int(x[0])):
    if c['active'] == 't': continue
    cambios = sorted(hist[(i, 'is_active')])
    ultima = cambios[-1] if cambios else None
    ver = ultima[0] if ultima else None
    vtxt = ('%g' % ver) if ver is not None else '-'
    if c['source'].startswith('MVP-'):
        motivo = 'V11 §3: semilla minima de V8 retirada (desactivada, no borrada, por la FK de planes historicos)'
        clase = 'SEMILLA_V8'
    elif vtxt in MOTIVO_EXPLICITO:
        motivo, clase = MOTIVO_EXPLICITO[vtxt], 'DESACTIVADO_REVISION'
    else:
        eq11 = valor_en(i, 'equipment_code', 11)
        faltan = [n for n, a in (('gif', 'gif_path'), ('instrucciones_es', 'instructions_es'), ('nombre_es', 'name_es'))
                  if not valor_en(i, a, 11)]
        partes = []
        if eq11 not in V11_EQUIPOS: partes.append(f'equipamiento «{eq11}» fuera de la lista de V11')
        if faltan: partes.append('sin ' + ', '.join(faltan) + ' en V11')
        motivo = 'V11 §2: no cumple el criterio de activacion (' + ('; '.join(partes) or 'criterio no reproducido') + ')'
        clase = 'NUNCA_ACTIVADO_V11'
    filas.append([i, c['source'], c['name_en'], c['name_es'], c['bp'], c['equip'], c['reqs'] or '-', c['dif'], vtxt, clase, motivo])
with open(out, 'w', encoding='utf-8', newline='') as fh:
    w = csv.writer(fh, delimiter='\t', quoting=csv.QUOTE_NONE, escapechar='\\')
    w.writerow(['id', 'source_code', 'name_en', 'name_es', 'zona', 'equipo_V50', 'requisitos_V50', 'dificultad', 'version_inactivacion',
                'clase', 'motivo'])
    w.writerows(filas)
print(len(filas), collections.Counter(f[9] for f in filas))
print(collections.Counter(f[10] for f in filas if f[9] == 'NUNCA_ACTIVADO_V11').most_common())
