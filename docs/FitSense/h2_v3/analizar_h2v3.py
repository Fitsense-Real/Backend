#!/usr/bin/env python3
"""H2-v3 con gpt-5-mini real: analisis de la evaluacion pareada PR-1.7 (A) frente a PR-2.1 (B).

Uso:
  python3 -I analizar_h2v2.py etapa1 <primera_propuesta.csv> <predict_time.csv>
  python3 -I analizar_h2v2.py etapa2 <pipeline_completo_final.csv> <predict_time_e2.csv>

Solo lee. Aplica los criterios FIJADOS DE ANTEMANO en 05_DISENO_EVALUACION_Y_PRESUPUESTO.md §4, sin cambiarlos:
  P1 (primario): casos con algun rechazo V15 de la mitad o V26 en la primera propuesta: B < A y B <= 1.
  P2 (primario): validas al primer intento: B >= A.
  K1 (bloqueante): ninguna otra categoria (V1, V4, V23, V24, V25...) sube mas de 1 caso en B.
  K2 (bloqueante): mediana de predict_time: B <= A x 1,10.
  K3 (bloqueante, etapa 2): 0 publicados invalidos; respaldos + NO_FACTIBLE en B <= A; predicciones B <= A x 1,10.
  K4 (bloqueante): ningun validador ni politica cambia (diff de la rama; se comprueba aparte).
  K5 (bloqueante, H2-v3): casos con algun rechazo V24 o V25 en la primera propuesta: B <= A (sin margen).
  K6 (bloqueante, H2-v3): errores del proveedor (cancelacion o tiempo) en B <= A + 1.
  Inconclusa si A tiene menos de 3 casos con V15 de la mitad o V26.
Fijado en docs/FitSense/h2_v3/04_PROTOCOLO_Y_PRESUPUESTO.md antes de ejecutar predicciones reales.
"""
import csv, sys, statistics
from collections import Counter, defaultdict
from math import comb


def mcnemar(b, c):
    n = b + c
    if n == 0:
        return 1.0
    return min(1.0, 2 * sum(comb(n, i) for i in range(0, min(b, c) + 1)) / 2 ** n)


def otros(texto):
    c = Counter()
    t = texto.strip().strip('{}').strip()
    if not t:
        return c
    for par in t.split(','):
        k, v = par.strip().split('=')
        c[k.strip()] += int(v)
    return c


def predict_times(ruta):
    d = {}
    for r in csv.DictReader(open(ruta), delimiter=';'):
        try:
            d[r['prediction_id']] = float(r['predict_time_s'])
        except ValueError:
            pass
    return d


def etapa1(ruta, ruta_pt):
    pt = predict_times(ruta_pt)
    por = defaultdict(dict)
    todas = list(csv.DictReader(open(ruta), delimiter=';'))
    for r in todas:
        por[r['caso']][r['brazo']] = r
    errores = {b: sum(r['brazo'] == b and r['estado'] == 'ERROR_PROVEEDOR' for r in todas) for b in 'AB'}
    version_b = next((r['prompt_version'] for r in todas if r['brazo'] == 'B'), 'B')
    parejas = {k: v for k, v in por.items() if all(b in v and v[b]['estado'] == 'RESPUESTA' for b in 'AB')}
    excluidas = {k: {b: v[b]['estado'] for b in v} for k, v in por.items() if k not in parejas}

    def v26(r):
        return otros(r['otros']).get('V26', 0)

    def problema(r):  # V15 de la mitad o V26
        return int(r['rechazos_v15_mitad']) > 0 or v26(r) > 0

    def dosis(r):  # V24 o V25 (la regresion de PR-2.0)
        o = otros(r['otros'])
        return o.get('V24', 0) > 0 or o.get('V25', 0) > 0

    def resto(r):  # categorias distintas de V15 y V26
        o = otros(r['otros'])
        o.pop('V26', None)
        return o

    out = [f"Parejas completas: {len(parejas)}; excluidas: {excluidas or 'ninguna'}", ""]
    out.append("| Caso | Objetivo | Nivel | Lugar | Días | A | B | A V15 mitad / cob. | B V15 mitad / cob. | A V26 | B V26 "
               "| A otros | B otros | A predict s | B predict s | A pred. | B pred. |")
    out.append("|---|---|---|---|---:|---|---|---|---|---:|---:|---|---|---:|---:|---|---|")
    for k in sorted(parejas):
        a, b = parejas[k]['A'], parejas[k]['B']
        f = lambda r: f"{pt.get(r['prediction_id'], float('nan')):.1f}"
        cob = lambda r: int(r['rechazos_v15']) - int(r['rechazos_v15_mitad'])
        out.append(f"| {k} | {a['objetivo']} | {a['nivel']} | {a['lugar']} | {a['dias']} | {a['validacion']} | "
                   f"{b['validacion']} | {a['rechazos_v15_mitad']} / {cob(a)} | {b['rechazos_v15_mitad']} / {cob(b)} | "
                   f"{v26(a)} | {v26(b)} | {dict(resto(a)) or '—'} | {dict(resto(b)) or '—'} | {f(a)} | {f(b)} | "
                   f"`{a['prediction_id']}` | `{b['prediction_id']}` |")

    def res(br):
        l = [parejas[k][br] for k in sorted(parejas)]
        cats = Counter()
        for r in l:
            for c in resto(r):
                cats[c] += 1
        tiempos = [pt[r['prediction_id']] for r in l if r['prediction_id'] in pt]
        return {
            'validas_primera': sum(r['validacion'] == 'VALIDA' for r in l),
            'casos_v15_mitad_o_v26': sum(problema(r) for r in l),
            'casos_v15_mitad': sum(int(r['rechazos_v15_mitad']) > 0 for r in l),
            'casos_v15_cualquiera': sum(int(r['rechazos_v15']) > 0 for r in l),
            'casos_v26': sum(v26(r) > 0 for r in l),
            'casos_v24_o_v25': sum(dosis(r) for r in l),
            'rechazos_v15_mitad': sum(int(r['rechazos_v15_mitad']) for r in l),
            'rechazos_v15': sum(int(r['rechazos_v15']) for r in l),
            'rechazos_v26': sum(v26(r) for r in l),
            'casos_por_otra_categoria': dict(sorted(cats.items())),
            'mediana_predict_time_s': round(statistics.median(tiempos), 1) if tiempos else None,
            'predict_time_medidos': len(tiempos),
            'mediana_ms_backend_s': round(statistics.median(int(r['ms']) for r in l) / 1000, 1) if l else None,
            'caracteres_salida_mediana': statistics.median(int(r['caracteres_salida']) for r in l) if l else None,
            'cota_usd': round(sum(float(r['cota_usd']) for r in l), 4),
        }

    A, B = res('A'), res('B')
    out += ["", f"| Métrica (primera propuesta) | A · PR-1.7 | B · {version_b} |", "|---|---:|---:|"]
    for k in A:
        out.append(f"| {k} | {A[k]} | {B[k]} |")

    sa = sum(problema(parejas[k]['A']) and not problema(parejas[k]['B']) for k in parejas)
    sb = sum(not problema(parejas[k]['A']) and problema(parejas[k]['B']) for k in parejas)
    va = sum(parejas[k]['A']['validacion'] == 'VALIDA' and parejas[k]['B']['validacion'] != 'VALIDA' for k in parejas)
    vb = sum(parejas[k]['A']['validacion'] != 'VALIDA' and parejas[k]['B']['validacion'] == 'VALIDA' for k in parejas)
    corrige = sorted(k for k in parejas if problema(parejas[k]['A']) and not problema(parejas[k]['B']))
    introduce = sorted(k for k in parejas if not problema(parejas[k]['A']) and problema(parejas[k]['B']))
    out += ["", f"V15 de la mitad o V26, discordantes: solo A = {sa} {corrige}, solo B = {sb} {introduce}; "
                f"McNemar exacta p = {mcnemar(sa, sb):.4f}",
            f"Válida a la primera, discordantes: solo A = {va}, solo B = {vb}; McNemar exacta p = {mcnemar(va, vb):.4f}",
            "", "Criterios fijados de antemano (05 §4):"]
    pot = A['casos_v15_mitad_o_v26'] >= 3
    p1 = B['casos_v15_mitad_o_v26'] < A['casos_v15_mitad_o_v26'] and B['casos_v15_mitad_o_v26'] <= 1
    p2 = B['validas_primera'] >= A['validas_primera']
    sube = {c: B['casos_por_otra_categoria'].get(c, 0) - A['casos_por_otra_categoria'].get(c, 0)
            for c in set(A['casos_por_otra_categoria']) | set(B['casos_por_otra_categoria'])}
    k1 = all(v <= 1 for v in sube.values())
    k2 = (A['mediana_predict_time_s'] is not None and B['mediana_predict_time_s'] is not None
          and B['mediana_predict_time_s'] <= A['mediana_predict_time_s'] * 1.10)
    out.append(f"- Potencia: A con V15 de la mitad o V26 en {A['casos_v15_mitad_o_v26']} casos -> "
               f"{'>= 3, hay potencia' if pot else '< 3: INCONCLUSA por construccion'}")
    out.append(f"- P1: B < A y B <= 1: {B['casos_v15_mitad_o_v26']} vs {A['casos_v15_mitad_o_v26']} -> "
               f"{'CUMPLE' if p1 else 'NO CUMPLE'}")
    out.append(f"- P2: válidas a la primera B >= A: {B['validas_primera']} >= {A['validas_primera']} -> "
               f"{'CUMPLE' if p2 else 'NO CUMPLE'}")
    out.append(f"- K1: ninguna otra categoría sube más de 1 caso: {dict(sorted(sube.items()))} -> "
               f"{'CUMPLE' if k1 else 'NO CUMPLE'}")
    out.append(f"- K2: mediana predict_time B <= A x 1,10: {B['mediana_predict_time_s']} <= "
               f"{None if A['mediana_predict_time_s'] is None else round(A['mediana_predict_time_s'] * 1.1, 1)} -> "
               f"{'CUMPLE' if k2 else 'NO CUMPLE'}")
    k5 = B['casos_v24_o_v25'] <= A['casos_v24_o_v25']
    k6 = errores['B'] <= errores['A'] + 1
    out.append(f"- K5: casos con V24 o V25 B <= A: {B['casos_v24_o_v25']} <= {A['casos_v24_o_v25']} -> "
               f"{'CUMPLE' if k5 else 'NO CUMPLE'}")
    out.append(f"- K6: errores del proveedor B <= A + 1: {errores['B']} <= {errores['A'] + 1} -> "
               f"{'CUMPLE' if k6 else 'NO CUMPLE'}")
    out.append(f"- S1 (informativo): B con V15 de la mitad o V26 en {B['casos_v15_mitad_o_v26']} casos (PR-2.0 historico: 2)")
    if not pot:
        veredicto = 'INCONCLUSA'
    elif k1 and k2 and k5 and k6 and p1 and p2:
        veredicto = 'FAVORABLE (etapa 1): pasa a la etapa 2'
    else:
        veredicto = 'DESFAVORABLE (etapa 1)'
    out.append(f"- Veredicto de la etapa 1 según la regla previa: **{veredicto}**")
    print("\n".join(out))


def etapa2(ruta, ruta_pt):
    pt = predict_times(ruta_pt)
    por = defaultdict(dict)
    for r in csv.DictReader(open(ruta), delimiter=';'):
        por[r['caso']][r['brazo']] = r
    casos = {k: v for k, v in por.items() if all(b in v and v[b]['estado_operacion'] not in
                                                  ('NO_EJECUTADO_PRESUPUESTO', 'EXCEPCION') for b in 'AB')}
    out = [f"Casos completos: {len(casos)}; incompletos: "
           f"{ {k: {b: v[b]['estado_operacion'] for b in v} for k, v in por.items() if k not in casos} or 'ninguno'}", ""]
    out.append("| Caso | A origen | B origen | A pred. | B pred. | A 1.ª válida | B 1.ª válida | A s | B s | A V15m/V26 | "
               "B V15m/V26 | A otros por intento | B otros por intento | A revalidado | B revalidado |")
    out.append("|---|---|---|---:|---:|---|---|---:|---:|---|---|---|---|---|---|")
    for k in [c for c in por if c in casos]:
        a, b = casos[k]['A'], casos[k]['B']
        out.append(f"| {k} | {a['origen']} | {b['origen']} | {a['predicciones']} | {b['predicciones']} | "
                   f"{a['primera_valida']} | {b['primera_valida']} | {int(a['ms_total'])/1000:.0f} | "
                   f"{int(b['ms_total'])/1000:.0f} | {a['rechazos_v15_mitad']}/{a['rechazos_v26']} | "
                   f"{b['rechazos_v15_mitad']}/{b['rechazos_v26']} | {a['otros_por_intento'] or '—'} | "
                   f"{b['otros_por_intento'] or '—'} | {a['revalidacion_publicado'][:12]} | {b['revalidacion_publicado'][:12]} |")

    def res(br):
        l = [casos[k][br] for k in casos]
        pts = [pt[x] for r in l for x in r['prediction_ids'].split() if x in pt]
        return {
            'publicados_desde_ia': sum(r['origen'] == 'AI' for r in l),
            'publicados_rule_engine': sum(r['origen'] == 'RULE_ENGINE' for r in l),
            'sin_plan': sum(r['origen'] == 'SIN_PLAN' for r in l),
            'no_factible_o_inviable': sum(r['motivo_respaldo'] in ('INVIABLE_PARA_IA',) for r in l),
            'validas_al_primer_intento': sum(r['primera_valida'] == 'true' for r in l),
            'predicciones': sum(int(r['predicciones']) for r in l),
            'reintentos': sum(max(0, int(r['predicciones']) - 1) for r in l),
            'casos_con_v15_mitad': sum(int(r['rechazos_v15_mitad']) > 0 for r in l),
            'casos_con_v26': sum(int(r['rechazos_v26']) > 0 for r in l),
            'rechazos_v15_mitad': sum(int(r['rechazos_v15_mitad']) for r in l),
            'rechazos_v26': sum(int(r['rechazos_v26']) for r in l),
            'publicados_invalidos_revalidados': sum(not r['revalidacion_publicado'].startswith('VALIDA') for r in l),
            'publicados_fuera_de_available': sum(r['fuera_de_available'] != '0' for r in l),
            'mediana_s_hasta_plan': round(statistics.median(int(r['ms_total']) for r in l) / 1000, 1) if l else None,
            'max_s_hasta_plan': round(max(int(r['ms_total']) for r in l) / 1000, 1) if l else None,
            'mediana_predict_time_s': round(statistics.median(pts), 1) if pts else None,
        }

    A, B = res('A'), res('B')
    out += ["", "| Métrica (pipeline completo) | A · PR-1.7 | B · tratamiento |", "|---|---:|---:|"]
    for k in A:
        out.append(f"| {k} | {A[k]} | {B[k]} |")
    k3a = A['publicados_invalidos_revalidados'] == 0 and B['publicados_invalidos_revalidados'] == 0
    k3b = (B['publicados_rule_engine'] + B['sin_plan']) <= (A['publicados_rule_engine'] + A['sin_plan'])
    k3c = B['predicciones'] <= A['predicciones'] * 1.10
    ia_a = sum(casos[k]['A']['origen'] == 'AI' and casos[k]['B']['origen'] != 'AI' for k in casos)
    ia_b = sum(casos[k]['A']['origen'] != 'AI' and casos[k]['B']['origen'] == 'AI' for k in casos)
    out += ["", f"Publicado desde la IA, discordantes: solo A = {ia_a}, solo B = {ia_b}; McNemar exacta p = "
                f"{mcnemar(ia_a, ia_b):.4f}", "", "K3 (05 §4):",
            f"- 0 publicados inválidos (revalidación): {k3a}",
            f"- respaldos + sin plan B <= A: {B['publicados_rule_engine'] + B['sin_plan']} <= "
            f"{A['publicados_rule_engine'] + A['sin_plan']} -> {k3b}",
            f"- predicciones B <= A x 1,10: {B['predicciones']} <= {A['predicciones'] * 1.1:.1f} -> {k3c}",
            f"- K3: **{'CUMPLE' if k3a and k3b and k3c else 'NO CUMPLE'}**"]
    print("\n".join(out))


if __name__ == '__main__':
    {'etapa1': etapa1, 'etapa2': etapa2}[sys.argv[1]](sys.argv[2], sys.argv[3])
