"""Genera CORRECCIONES_POR_EJERCICIO.md y correcciones_texto_detalle.tsv.
Uso: python3 -I -B correcciones_por_ejercicio.py <linea_base.tsv> <final.tsv> <dir revision_visual> <salida_dir>"""
import csv, glob, sys
def tsv(p): return {r['id']: r for r in csv.DictReader(open(p, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
b, f, RV, D = tsv(sys.argv[1]), tsv(sys.argv[2]), sys.argv[3], sys.argv[4]
rev = {}
for p in sorted(glob.glob(f'{RV}/revision_L*.tsv')):
    for r in csv.DictReader(open(p, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE): rev[r['id']] = r
FL = [('hi','HI'),('floor','FL'),('axial','AX'),('kneel','KN'),('hang','HG'),('overhead','OH'),('inv','INV')]
ban = lambda r: ' '.join(t for k, t in FL if r[k] == 't') or '-'
act = [i for i in sorted(b, key=int) if b[i]['active'] == 't']
L = ['# Correcciones por ejercicio', '',
     'Generado con `herramientas/correcciones_por_ejercicio.py` desde la línea base (V1–V46) y el catálogo final (V47–V50).',
     '- **Evidencia de cada fila:** el GIF original de la fuente (8 fotogramas + imagen fija) y el veredicto del lote en `revision_visual/revision_LNN.tsv`.',
     '- **Textos completos de V49:** en `revision_visual/correcciones_texto_consolidadas.json` y en la propia migración.', '',
     '## V47 · Banderas de seguridad que el GIF exige (26 ejercicios, 28 banderas)', '',
     '| ID | Ejercicio | Banderas antes | Banderas después | Movimiento verificado |', '|---|---|---|---|---|']
for i in act:
    if ban(b[i]) != ban(f[i]): L.append(f"| {i} | {f[i]['name_es']} | {ban(b[i])} | {ban(f[i])} | {rev[i]['movimiento_gif'][:140]} |")
L += ['', '## V48 · Contención conservadora de dificultad (12 ejercicios; pendiente de confirmación profesional)', '',
      '| ID | Ejercicio | Antes | Después | Motivo |', '|---|---|---|---|---|']
for i in act:
    if b[i]['dif'] != f[i]['dif']: L.append(f"| {i} | {f[i]['name_es']} | d{b[i]['dif']} | d{f[i]['dif']} | {rev[i]['notas'][:140]} |")
L += ['', '## V50 · Clasificación (9 ejercicios)', '', '| ID | Ejercicio | Antes | Después |', '|---|---|---|---|']
for i in act:
    k = lambda r: (r['bp'], r['target'], r['pattern'], r['presc'])
    if k(b[i]) != k(f[i]): L.append(f"| {i} | {f[i]['name_es']} | {'/'.join(k(b[i]))} | {'/'.join(k(f[i]))} |")
L += ['', '## V50 · Equipamiento (17 ejercicios)', '', '| ID | Ejercicio | Requisitos antes | Requisitos después | Principal |', '|---|---|---|---|---|']
for i in act:
    if (b[i]['reqs'], b[i]['equip']) != (f[i]['reqs'], f[i]['equip']):
        L.append(f"| {i} | {f[i]['name_es']} | {b[i]['reqs'] or '-'} | {f[i]['reqs'] or '-'} | {b[i]['equip']} → {f[i]['equip']} |")
nn = [i for i in act if b[i]['name_es'] != f[i]['name_es']]
ni = [i for i in act if b[i]['instr_es'] != f[i]['instr_es']]
L += ['', f'## V49 · Nombres ({len(nn)}) e instrucciones ({len(ni)})', '', 'Nombres cambiados (el 1254 cambia en V50, junto con su clasificación):', '',
      '| ID | Antes | Después | Fuente (en) |', '|---|---|---|---|']
for i in nn: L.append(f"| {i} | {b[i]['name_es']} | {f[i]['name_es']} | {b[i]['name_en']} |")
L += ['', 'Instrucciones reescritas: el detalle (antes / después / frase que contradecía al GIF) está en `correcciones_texto_detalle.tsv`.']
open(f'{D}/CORRECCIONES_POR_EJERCICIO.md', 'w', encoding='utf-8').write('\n'.join(L) + '\n')
with open(f'{D}/correcciones_texto_detalle.tsv', 'w', encoding='utf-8') as o:
    o.write('id\tnombre_antes\tnombre_despues\tproblema_detectado\tinstrucciones_antes\tinstrucciones_despues\n')
    for i in sorted(set(nn) | set(ni), key=int):
        o.write('\t'.join([i, b[i]['name_es'], f[i]['name_es'], rev[i]['problema_instr'], b[i]['instr_es'], f[i]['instr_es']]).replace('\n', ' ') + '\n')
print(len(nn), len(ni))
