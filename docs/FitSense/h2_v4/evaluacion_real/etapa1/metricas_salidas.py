#!/usr/bin/env python3
"""H2-v4 opcion C · metricas por prediccion, SIN coste, sobre las salidas guardadas (no decide: los criterios estan en
analizar_h2v4.py). Usa el catalogo ofrecido a cada caso (available_exercises del prompt PR-2.2 volcado en modo PROMPTS:
mismos ejercicios y mismo orden que PR-1.7, `H2v4RegresionesIT`).
Uso: metricas_salidas.py <dir salidas> <dir prompts volcados> <primera_propuesta.csv>  > metricas_por_prediccion.csv
Por prediccion: series de trabajo/semana, dosis (series x reps o segundos) distintas, reparto maximo por grupo en una
sesion (ejercicios de trabajo), patrones cubiertos, duracion prevista total y maxima, ejercicios fuera de lo ofrecido."""
import csv, json, re, sys
from collections import Counter
from pathlib import Path

def bloque(texto, clave):
    i = texto.index('"' + clave + '":') + len(clave) + 3
    return json.JSONDecoder().raw_decode(texto[i:])[0]

sal, pro, pp = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3]
cat = {}
filas = list(csv.DictReader(open(pp), delimiter=';'))
w = csv.writer(sys.stdout, delimiter=';')
w.writerow(['caso', 'replica', 'brazo', 'validacion', 'sesiones', 'ejercicios', 'ejercicios_trabajo', 'series_trabajo_semana',
            'dosis_distintas', 'reparto_max_grupo_sesion', 'patrones_cubiertos', 'duracion_total_min', 'duracion_max_min',
            'fuera_de_lo_ofrecido'])
for r in sorted(filas, key=lambda r: (r['caso'], r['replica'], r['brazo'])):
    c = r['caso']
    if c not in cat:
        t = (pro / f'{c}_B_prompt.txt').read_text()
        cat[c] = {e['exercise_id']: e for e in bloque(t, 'available_exercises')}
    f = sal / f"{c}_r{r['replica']}_{r['brazo']}.json"
    if r['estado'] != 'RESPUESTA' or not f.exists():
        w.writerow([c, r['replica'], r['brazo'], r['estado']] + ['-'] * 10); continue
    d = json.loads(f.read_text())
    ofrec = cat[c]
    ej = trab = series = fuera = 0; dosis = Counter(); patrones = set(); dur = []; rep_max = '-'; peor = 0
    for wo in d.get('workouts', []):
        dur.append(wo.get('expected_duration_minutes') or 0)
        grupos = Counter(); n = 0
        for x in wo.get('exercises', []):
            ej += 1
            e = ofrec.get(x.get('exercise_id'))
            if e is None: fuera += 1; continue
            if e.get('cuenta') in ('complemento', 'sin_series'): continue
            trab += 1; n += 1; series += x.get('planned_sets') or 0
            grupos[e['body_part']] += 1
            patrones.add(e.get('movement_pattern'))
            dosis[(x.get('planned_sets'), x.get('planned_reps'), x.get('planned_duration_seconds'))] += 1
        if n:
            g, k = grupos.most_common(1)[0]
            if k / n > peor: peor, rep_max = k / n, f'{wo.get("focus_code")}:{g} {k}/{n}'
    w.writerow([c, r['replica'], r['brazo'], r['validacion'], len(d.get('workouts', [])), ej, trab, series, len(dosis),
                rep_max, len(patrones - {None}), sum(dur), max(dur or [0]), fuera])
