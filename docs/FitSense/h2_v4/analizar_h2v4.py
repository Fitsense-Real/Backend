#!/usr/bin/env python3
"""H2-v4 con gpt-5-mini real: analisis de la evaluacion pareada PR-1.7 (A) frente a PR-2.2 (B).

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
Fijado en docs/FitSense/h2_v4/04_PROTOCOLO_Y_PRESUPUESTO.md antes de ejecutar predicciones reales. Criterios IGUALES a
los de H2-v3 (P1, P2, K1-K6). Solo se anaden salidas informativas que no deciden: S1 frente a PR-2.0 y PR-2.1
historicos, y la latencia pareada (mediana de B - A y de B / A por pareja).
"""
import csv, sys, statistics, math
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
    out.append(f"- S1 (informativo): B con V15 de la mitad o V26 en {B['casos_v15_mitad_o_v26']} casos (historico: PR-2.0 2, PR-2.1 1)")
    pt_par = [(pt[parejas[k]['A']['prediction_id']], pt[parejas[k]['B']['prediction_id']]) for k in parejas
              if parejas[k]['A']['prediction_id'] in pt and parejas[k]['B']['prediction_id'] in pt]
    if pt_par:
        out.append(f"- Latencia pareada (informativa, no decide): mediana B - A {statistics.median(b - a for a, b in pt_par):.1f} s; "
                   f"mediana B / A {statistics.median(b / a for a, b in pt_par):.3f}; B mas lento en "
                   f"{sum(b > a for a, b in pt_par)} de {len(pt_par)} parejas")
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


if __name__ == '__main__' and sys.argv[1] in ('etapa1', 'etapa2'):
    {'etapa1': etapa1, 'etapa2': etapa2}[sys.argv[1]](sys.argv[2], sys.argv[3])


# =====================================================================================================================
# OPCION C · DOS REPLICAS POR PERFIL. Reglas FIJADAS antes de ejecutar (04_PROTOCOLO_Y_PRESUPUESTO.md §7):
#   Unidad para P1, K1, K5 y potencia: el PERFIL (K1: las mismas categorias que etapa1; V15 de grupos solo se informa). Un perfil esta afectado en un brazo si ALGUNA de sus replicas en ese
#     brazo tiene la categoria (regla conservadora: basta una). Umbrales sin cambios: P1 B < A y B <= 1 perfil;
#     K1 ninguna otra categoria sube mas de 1 perfil; K5 perfiles con V24 o V25 B <= A; potencia: A >= 3 perfiles.
#   P2: validas a la primera sobre las parejas completas (hasta 52): B >= A.
#   K2: mediana de predict_time de TODAS las predicciones con respuesta de cada brazo (hasta 52): B <= A x 1,10.
#   K6: errores del proveedor (ERROR_PROVEEDOR) de cada brazo sobre sus 52: B <= A + 1.
#   INCONCLUSA si la potencia no llega o si alguna de las 104 no se ejecuto por presupuesto.
#   Estadistica por PERFIL (las dos replicas no son independientes): McNemar exacta sobre perfiles afectados;
#     prueba de signos exacta sobre d = validas B - validas A por perfil (0..2); prueba de signos de la latencia sobre
#     la media por perfil de log(B/A). Ademas, las mismas medidas por replica, por separado (informativo).
# =====================================================================================================================

def signos(pos, neg):
    """p bilateral exacta de la prueba de signos (como McNemar)."""
    return mcnemar(pos, neg)


def etapa1r(ruta, ruta_pt):
    pt = predict_times(ruta_pt)
    todas = list(csv.DictReader(open(ruta), delimiter=';'))
    filas = defaultdict(dict)  # (caso, replica) -> brazo -> fila
    for r in todas:
        filas[(r['caso'], int(r['replica']))][r['brazo']] = r
    perfiles = sorted({c for c, _ in filas})
    respuesta = lambda r: r is not None and r['estado'] == 'RESPUESTA'
    no_ejecutadas = sum(r['estado'] == 'NO_EJECUTADO_PRESUPUESTO' for r in todas)
    errores = {b: sum(r['brazo'] == b and r['estado'] == 'ERROR_PROVEEDOR' for r in todas) for b in 'AB'}
    version_b = next((r['prompt_version'] for r in todas if r['brazo'] == 'B'), 'B')

    def cat(r):
        o = otros(r['otros'])
        mitad_v26 = int(r['rechazos_v15_mitad']) > 0 or o.get('V26', 0) > 0
        dosis = o.get('V24', 0) > 0 or o.get('V25', 0) > 0
        resto = {k for k in o if k != 'V26'}  # las mismas categorias de K1 que etapa1
        if int(r['rechazos_v15']) - int(r['rechazos_v15_mitad']) > 0:
            resto.add('V15_GRUPOS (informativo)')
        return mitad_v26, dosis, resto

    def afectados(brazo):
        m26, dos, otras = set(), set(), defaultdict(set)
        for (c, rep), v in filas.items():
            r = v.get(brazo)
            if not respuesta(r):
                continue
            a, d, o = cat(r)
            if a: m26.add(c)
            if d: dos.add(c)
            for k in o: otras[k].add(c)
        return m26, dos, otras

    mA, dA, oA = afectados('A')
    mB, dB, oB = afectados('B')
    pares = [(k, v) for k, v in filas.items() if respuesta(v.get('A')) and respuesta(v.get('B'))]
    valA = sum(v['A']['validacion'] == 'VALIDA' for _, v in pares)
    valB = sum(v['B']['validacion'] == 'VALIDA' for _, v in pares)
    tA = [pt[r['prediction_id']] for r in todas if r['brazo'] == 'A' and respuesta(r) and r['prediction_id'] in pt]
    tB = [pt[r['prediction_id']] for r in todas if r['brazo'] == 'B' and respuesta(r) and r['prediction_id'] in pt]
    medA, medB = round(statistics.median(tA), 1), round(statistics.median(tB), 1)  # como etapa1

    out = [f"Perfiles: {len(perfiles)} · parejas completas: {len(pares)} de {len(filas)} · no ejecutadas por presupuesto: "
           f"{no_ejecutadas} · errores del proveedor A {errores['A']}, B {errores['B']}", ""]
    out.append("| Caso | Rep. | A | B | A V15 mitad/grupos | B V15 mitad/grupos | A V26 | B V26 | A otros | B otros | A s | B s |")
    out.append("|---|---:|---|---|---|---|---:|---:|---|---|---:|---:|")
    for (c, rep) in sorted(filas):
        v = filas[(c, rep)]
        celda = lambda b, f: (f(v[b]) if respuesta(v.get(b)) else v.get(b, {}).get('estado', '—'))
        out.append("| %s | %d | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |" % (
            c, rep, celda('A', lambda r: r['validacion']), celda('B', lambda r: r['validacion']),
            celda('A', lambda r: f"{r['rechazos_v15_mitad']}/{int(r['rechazos_v15']) - int(r['rechazos_v15_mitad'])}"),
            celda('B', lambda r: f"{r['rechazos_v15_mitad']}/{int(r['rechazos_v15']) - int(r['rechazos_v15_mitad'])}"),
            celda('A', lambda r: otros(r['otros']).get('V26', 0)), celda('B', lambda r: otros(r['otros']).get('V26', 0)),
            celda('A', lambda r: {k: n for k, n in otros(r['otros']).items() if k != 'V26'} or '—'),
            celda('B', lambda r: {k: n for k, n in otros(r['otros']).items() if k != 'V26'} or '—'),
            celda('A', lambda r: f"{pt.get(r['prediction_id'], float('nan')):.1f}"),
            celda('B', lambda r: f"{pt.get(r['prediction_id'], float('nan')):.1f}")))

    out += ["", f"| Métrica | A · PR-1.7 | B · {version_b} |", "|---|---:|---:|",
            f"| Válidas a la primera (parejas completas) | {valA} | {valB} |",
            f"| Perfiles con V15 de la mitad o V26 (alguna réplica) | {len(mA)} {sorted(mA)} | {len(mB)} {sorted(mB)} |",
            f"| Perfiles con V24 o V25 (alguna réplica) | {len(dA)} {sorted(dA)} | {len(dB)} {sorted(dB)} |"]
    for k in sorted(set(oA) | set(oB)):
        out.append(f"| Perfiles con {k} (alguna réplica) | {len(oA.get(k, ()))} {sorted(oA.get(k, ()))} | "
                   f"{len(oB.get(k, ()))} {sorted(oB.get(k, ()))} |")
    out += [f"| Mediana de predict_time (todas las respuestas) | {medA:.1f} s (n={len(tA)}) | {medB:.1f} s (n={len(tB)}) |",
            f"| Errores del proveedor | {errores['A']} | {errores['B']} |"]

    # Estadistica por perfil.
    soloA = len(mA - mB); soloB = len(mB - mA)
    d = defaultdict(int)
    for (c, rep), v in pares:
        d[c] += (v['B']['validacion'] == 'VALIDA') - (v['A']['validacion'] == 'VALIDA')
    pos = sum(x > 0 for x in d.values()); neg = sum(x < 0 for x in d.values())
    lr = defaultdict(list)
    for (c, rep), v in pares:
        a, b = pt.get(v['A']['prediction_id']), pt.get(v['B']['prediction_id'])
        if a and b: lr[c].append(math.log(b / a))
    media = {c: sum(x) / len(x) for c, x in lr.items()}
    lpos = sum(x > 0 for x in media.values()); lneg = sum(x < 0 for x in media.values())
    out += ["", "Estadística por PERFIL (las dos réplicas no son independientes):",
            f"- V15 de la mitad o V26: perfiles solo en A = {soloA}, solo en B = {soloB}; McNemar exacta p = {mcnemar(soloA, soloB):.4f}",
            f"- Validez: perfiles con B mejor = {pos}, con A mejor = {neg}; prueba de signos p = {signos(pos, neg):.4f}",
            f"- Latencia: perfiles con B más lento (media de log(B/A)) = {lpos}, más rápido = {lneg}; prueba de signos p = "
            f"{signos(lpos, lneg):.4f}; mediana por perfil de B/A = {math.exp(statistics.median(media.values())):.3f}" if media else "- Latencia: sin datos"]

    # Variabilidad de la latencia.
    porPerfil = {b: defaultdict(list) for b in 'AB'}
    for (c, rep), v in filas.items():
        for b in 'AB':
            r = v.get(b)
            if respuesta(r) and r['prediction_id'] in pt: porPerfil[b][c].append(pt[r['prediction_id']])
    for b in 'AB':
        meds = sorted(statistics.median(x) for x in porPerfil[b].values())
        difs = [abs(x[0] - x[1]) for x in porPerfil[b].values() if len(x) == 2]
        if meds:
            q = statistics.quantiles(meds, n=4) if len(meds) >= 4 else [meds[0], statistics.median(meds), meds[-1]]
            out.append(f"- Variabilidad {b}: mediana por perfil de {meds[0]:.1f} a {meds[-1]:.1f} s (IQR {q[0]:.1f}–{q[2]:.1f}); "
                       f"diferencia entre réplicas del mismo perfil: mediana {statistics.median(difs):.1f} s" if difs else "")

    # Por replica, por separado (informativo).
    out += ["", "Por réplica (informativo, no decide):"]
    for rep in sorted({r for _, r in filas}):
        pr = [(k, v) for k, v in pares if k[1] == rep]
        va = sum(v['A']['validacion'] == 'VALIDA' for _, v in pr); vb = sum(v['B']['validacion'] == 'VALIDA' for _, v in pr)
        ma = sum(cat(v['A'])[0] for _, v in pr); mb = sum(cat(v['B'])[0] for _, v in pr)
        ta = [pt[v['A']['prediction_id']] for _, v in pr if v['A']['prediction_id'] in pt]
        tb = [pt[v['B']['prediction_id']] for _, v in pr if v['B']['prediction_id'] in pt]
        out.append(f"- Réplica {rep}: parejas {len(pr)}; válidas A {va}, B {vb}; con V15 mitad o V26 A {ma}, B {mb}; "
                   f"mediana predict_time A {statistics.median(ta):.1f} s, B {statistics.median(tb):.1f} s" if ta and tb else f"- Réplica {rep}: sin datos")

    # Criterios.
    pot = len(mA) >= 3
    p1 = len(mB) < len(mA) and len(mB) <= 1
    p2 = valB >= valA
    sube = {k: len(oB.get(k, ())) - len(oA.get(k, ())) for k in set(oA) | set(oB) if 'informativo' not in k}
    k1 = all(x <= 1 for x in sube.values())
    k2 = medB <= medA * 1.10
    k5 = len(dB) <= len(dA)
    k6 = errores['B'] <= errores['A'] + 1
    out += ["", "Criterios fijados de antemano (04 §3 y §7):",
            f"- Potencia: A con V15 de la mitad o V26 en {len(mA)} perfiles -> {'hay potencia' if pot else '< 3: INCONCLUSA'}",
            f"- P1: perfiles B < A y B <= 1: {len(mB)} vs {len(mA)} -> {'CUMPLE' if p1 else 'NO CUMPLE'}",
            f"- P2: válidas B >= A: {valB} >= {valA} -> {'CUMPLE' if p2 else 'NO CUMPLE'}",
            f"- K1: ninguna otra categoría sube más de 1 perfil: {dict(sorted(sube.items()))} -> {'CUMPLE' if k1 else 'NO CUMPLE'}",
            f"- K2: mediana B <= A x 1,10: {medB} <= {round(medA * 1.1, 1)} -> {'CUMPLE' if k2 else 'NO CUMPLE'}",
            f"- K5: perfiles con V24 o V25 B <= A: {len(dB)} <= {len(dA)} -> {'CUMPLE' if k5 else 'NO CUMPLE'}",
            f"- K6: errores del proveedor B <= A + 1: {errores['B']} <= {errores['A'] + 1} -> {'CUMPLE' if k6 else 'NO CUMPLE'}"]
    if not pot or no_ejecutadas:
        veredicto = 'INCONCLUSA'
    elif all([p1, p2, k1, k2, k5, k6]):
        veredicto = 'FAVORABLE (etapa 1): pasa a la etapa 2'
    else:
        veredicto = 'DESFAVORABLE (etapa 1)'
    out.append(f"- Veredicto de la etapa 1 según la regla previa: **{veredicto}**")
    print("\n".join(out))


if __name__ == '__main__' and len(sys.argv) > 1 and sys.argv[1] == 'etapa1r':
    etapa1r(sys.argv[2], sys.argv[3])
