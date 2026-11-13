"""Genera V49 (textos) desde correcciones_texto_consolidadas.json y la linea base. Uso: gen_v49.py <dir_trabajo> <salida.sql>"""
import csv, json, hashlib, sys
W, OUT = sys.argv[1], sys.argv[2]
cat = {r['id']: r for r in csv.DictReader(open(f'{W}/base/catalogo.tsv', encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
corr = json.load(open(f'{W}/correcciones_texto_consolidadas.json', encoding='utf-8'))
def q(s): return "'" + s.replace("'", "''") + "'"
out = ['-- V49 - Catalogo integral (auditoria visual 2026-11): NOMBRES E INSTRUCCIONES que no describian el movimiento del GIF.',
'-- SOLO cambia name_es, instructions_es e instruction_steps_es. No cambia banderas, patron, grupo, musculos,',
'-- dificultad, equipamiento, prescripcion ni actividad. instructions_en conserva el texto de la fuente (trazabilidad).',
'-- Evidencia: GIF original (8 fotogramas + imagen) revisado por el revisor del lote; textos redactados describiendo solo',
'-- lo que el GIF muestra (protocolo en docs/FitSense/catalogo_integral/). Se conservan las advertencias de seguridad e',
'-- indicaciones tecnicas del texto anterior (V39: toalla y anclaje en 308 y 1072). Ningun nombre nuevo cambia el',
'-- resultado de las reglas por nombre (ImpactoPolicy, exclusion por «estiramiento») salvo el 1093, documentado: pierde',
'-- «correr» y su impacto por nombre pasa de MODERADO a BAJO, que es lo que muestra el GIF (sin fase de vuelo).',
'-- Cada fila exige el texto anterior exacto (name_es, o md5 de instructions_es con los saltos de linea normalizados):',
'-- si no coincide, no cambia nada.', '']
for i in sorted(corr, key=int):
    r = corr[i]; c = cat[i]; nom = r['nombre_nuevo']; ins = r['instrucciones_nuevas']
    sets, conds = [], [f'exercise_id = {i}']
    if nom not in ('=', 'PENDIENTE', ''):
        sets.append(f'name_es = {q(nom)}'); conds.append(f"name_es = {q(c['name_es'])}")
    if ins not in ('=', 'PENDIENTE', ''):
        pasos = [p.strip() for p in ins.split(' || ')]
        sets.append(f"instructions_es = {q(' '.join(pasos))}")
        sets.append(f"instruction_steps_es = {q(json.dumps(pasos, ensure_ascii=False))}::jsonb")
        conds.append("md5(regexp_replace(instructions_es, '[\\t\\r\\n]+', ' ', 'g')) = '" + hashlib.md5(c['instr_es'].encode()).hexdigest() + "'")
    if not sets: continue
    out.append(f"-- {i} {c['name_en']}")
    out.append(f"UPDATE exercises SET {', '.join(sets)}\n WHERE {' AND '.join(conds)};")
open(OUT, 'w', encoding='utf-8').write('\n'.join(out) + '\n')
