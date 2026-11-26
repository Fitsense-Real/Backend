#!/usr/bin/env python3
"""H2-v4: que explica la latencia de PR-2.1 (K2), con los datos ya medidos (sin coste).

Uso: python3 -I analisis_latencia.py   (desde la raiz del repositorio)
Lee primera_propuesta.csv y predict_time.csv de las evaluaciones de H2-v2 (25/11) y H2-v3 (26/11).
"""
import csv, math, statistics

def corr(x, y):
    mx, my = statistics.mean(x), statistics.mean(y)
    return sum((a - mx) * (b - my) for a, b in zip(x, y)) / math.sqrt(sum((a - mx) ** 2 for a in x) * sum((b - my) ** 2 for b in y))

def ols(x, y):
    mx, my = statistics.mean(x), statistics.mean(y)
    b = sum((a - mx) * (c - my) for a, c in zip(x, y)) / sum((a - mx) ** 2 for a in x)
    return b

for nombre, d in [('H2-v2 25/11 · PR-1.7 frente a PR-2.0', 'docs/FitSense/h2_v2/evaluacion_real/etapa1'),
                  ('H2-v3 26/11 · PR-1.7 frente a PR-2.1', 'docs/FitSense/h2_v3/evaluacion_real/etapa1')]:
    pt = {r['prediction_id']: float(r['predict_time_s']) for r in csv.DictReader(open(d + '/predict_time.csv'), delimiter=';') if r.get('predict_time_s')}
    rows = [r for r in csv.DictReader(open(d + '/primera_propuesta.csv'), delimiter=';') if r['prediction_id'] in pt]
    print('==', nombre, '· predicciones con tiempo:', len(rows))
    for b in 'AB':
        rr = [r for r in rows if r['brazo'] == b]
        ent = [int(r['caracteres_entrada']) for r in rr]; sal = [int(r['caracteres_salida']) for r in rr]; t = [pt[r['prediction_id']] for r in rr]
        print(f'  {b}: mediana t {statistics.median(t):.1f} s · entrada {statistics.median(ent):.0f} · salida {statistics.median(sal):.0f} · '
              f'corr(t, entrada) {corr(ent, t):.2f} · corr(t, salida) {corr(sal, t):.2f}')
    A = [r for r in rows if r['brazo'] == 'A']
    pend = ols([int(r['caracteres_entrada']) for r in A], [pt[r['prediction_id']] for r in A])
    por = {}
    for r in rows: por.setdefault(r['caso'], {})[r['brazo']] = r
    pares = [v for v in por.values() if 'A' in v and 'B' in v]
    din = [int(v['B']['caracteres_entrada']) - int(v['A']['caracteres_entrada']) for v in pares]
    dt = [pt[v['B']['prediction_id']] - pt[v['A']['prediction_id']] for v in pares]
    res = [x - pend * y for x, y in zip(dt, din)]
    print(f'  pendiente de A: {pend * 1e4:.2f} s por 10.000 caracteres de entrada (cota alta: confunde tamano con perfil)')
    print(f'  parejas: entrada B-A mediana {statistics.median(din):.0f} car. -> explicaria {pend * statistics.median(din):.1f} s; '
          f't B-A mediana {statistics.median(dt):.1f} s; sin el efecto de la entrada {statistics.median(res):.1f} s')
