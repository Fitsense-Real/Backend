#!/usr/bin/env python3
"""H2 con gpt-5-mini: analisis pareado de la PRIMERA propuesta (primera_propuesta.csv de H2PrimeraPropuestaIT).

Uso: python3 -I analizar_h2.py primera_propuesta.csv
Solo lee. Criterios: los de CRITERIOS_ACEPTACION_H2.md (PR #69) que son medibles a nivel de primera propuesta
(C1 primario, C5, C6, C7-latencia, C2 no aplica: no se publica nada). Se informan TODAS las parejas.
"""
import csv, sys, statistics
from collections import Counter, defaultdict
from math import comb


def mcnemar_exacta(b, c):
    """p bilateral exacta de McNemar (binomial con p=0,5 sobre las parejas discordantes)."""
    n = b + c
    if n == 0:
        return 1.0
    k = min(b, c)
    p = sum(comb(n, i) for i in range(0, k + 1)) / 2 ** n
    return min(1.0, 2 * p)


def otros(texto):
    """'{V20=1, V23=2}' -> Counter."""
    c = Counter()
    t = texto.strip('{}').strip()
    if not t:
        return c
    for par in t.split(','):
        k, v = par.strip().split('=')
        c[k] += int(v)
    return c


filas = [r for r in csv.DictReader(open(sys.argv[1]), delimiter=';')]
por_caso = defaultdict(dict)
for r in filas:
    por_caso[r['caso']][r['brazo']] = r
parejas = {k: v for k, v in por_caso.items() if all(b in v and v[b]['estado'] == 'RESPUESTA' for b in 'AB')}
excluidas = {k: {b: v[b]['estado'] for b in v} for k, v in por_caso.items() if k not in parejas}

print(f"Parejas completas con respuesta: {len(parejas)} · excluidas: {excluidas or 'ninguna'}\n")
print("| Caso | Objetivo | Nivel | Lugar | Días | Min | A válida | B válida | A V15 (mitad) | B V15 (mitad) | A otros | B otros | A s | B s |")
print("|---|---|---|---|---:|---:|---|---|---|---|---|---|---:|---:|")
for k in sorted(parejas):
    a, b = parejas[k]['A'], parejas[k]['B']
    print(f"| {k} | {a['objetivo']} | {a['nivel']} | {a['lugar']} | {a['dias']} | {a['minutos']} | "
          f"{a['validacion']} | {b['validacion']} | {a['rechazos_v15']} ({a['rechazos_v15_mitad']}) | "
          f"{b['rechazos_v15']} ({b['rechazos_v15_mitad']}) | {a['otros']} | {b['otros']} | "
          f"{(int(a['ms'])/1000 and f'{int(a["ms"])/1000:.1f}') or 'n/d'} | {(int(b['ms'])/1000 and f'{int(b["ms"])/1000:.1f}') or 'n/d'} |")


con_tiempo = [k for k in parejas if all(int(parejas[k][b]['ms']) > 0 for b in 'AB')]


def resumen(brazo):
    l = [parejas[k][brazo] for k in parejas]
    o = Counter()
    for r in l:
        o += otros(r['otros'])
    return {
        'validas_primera': sum(r['validacion'] == 'VALIDA' for r in l),
        'casos_con_v15': sum(int(r['rechazos_v15']) > 0 for r in l),
        'casos_con_v15_mitad': sum(int(r['rechazos_v15_mitad']) > 0 for r in l),
        'rechazos_v15': sum(int(r['rechazos_v15']) for r in l),
        'rechazos_v15_mitad': sum(int(r['rechazos_v15_mitad']) for r in l),
        'otros': dict(sorted(o.items())),
        'casos_con_otros': {k: sum(otros(r['otros']).get(k, 0) > 0 for r in l) for k in sorted(o)},
        # Latencia solo en parejas con las DOS medidas (las recuperadas por GET no tienen tiempo del backend).
        'mediana_s': statistics.median(int(parejas[k][brazo]['ms']) for k in con_tiempo) / 1000 if con_tiempo else 0,
        'max_s': max(int(parejas[k][brazo]['ms']) for k in con_tiempo) / 1000 if con_tiempo else 0,
        'parejas_con_tiempo': len(con_tiempo),
        'cota_usd': sum(float(r['cota_usd']) for r in l),
        'caracteres_salida_mediana': statistics.median(int(r['caracteres_salida']) for r in l) if l else 0,
    }


A, B = resumen('A'), resumen('B')
print("\n| Métrica (primera propuesta) | A · PR-1.7 | B · PR-1.9 |\n|---|---:|---:|")
for k in A:
    print(f"| {k} | {A[k]} | {B[k]} |")

# Primario (C1): casos con al menos un rechazo V15 de la regla de la mitad, pareado.
b_ = sum(int(parejas[k]['A']['rechazos_v15_mitad']) > 0 and int(parejas[k]['B']['rechazos_v15_mitad']) == 0 for k in parejas)
c_ = sum(int(parejas[k]['A']['rechazos_v15_mitad']) == 0 and int(parejas[k]['B']['rechazos_v15_mitad']) > 0 for k in parejas)
print(f"\nC1 (mitad V15) discordantes: solo A = {b_}, solo B = {c_}; McNemar exacta p = {mcnemar_exacta(b_, c_):.4f}")
b2 = sum(int(parejas[k]['A']['rechazos_v15']) > 0 and int(parejas[k]['B']['rechazos_v15']) == 0 for k in parejas)
c2 = sum(int(parejas[k]['A']['rechazos_v15']) == 0 and int(parejas[k]['B']['rechazos_v15']) > 0 for k in parejas)
print(f"V15 (cualquiera) discordantes: solo A = {b2}, solo B = {c2}; McNemar exacta p = {mcnemar_exacta(b2, c2):.4f}")
v_a = sum(parejas[k]['A']['validacion'] == 'VALIDA' and parejas[k]['B']['validacion'] != 'VALIDA' for k in parejas)
v_b = sum(parejas[k]['A']['validacion'] != 'VALIDA' and parejas[k]['B']['validacion'] == 'VALIDA' for k in parejas)
print(f"Validez a la primera discordante: solo A = {v_a}, solo B = {v_b}; McNemar exacta p = {mcnemar_exacta(v_a, v_b):.4f}")

print("\nCriterios previos (CRITERIOS_ACEPTACION_H2.md) medibles en la primera propuesta:")
pot = A['casos_con_v15_mitad'] >= 3
print(f"- Potencia de C1: A con rechazo V15 de la mitad en {A['casos_con_v15_mitad']} casos "
      f"({'>= 3: hay potencia' if pot else '< 3: INCONCLUSO por construccion'})")
print(f"- C1: B < A en casos con V15 de la mitad: {B['casos_con_v15_mitad']} < {A['casos_con_v15_mitad']} -> "
      f"{'CUMPLE' if B['casos_con_v15_mitad'] < A['casos_con_v15_mitad'] else 'NO CUMPLE'}")
print(f"- C5: validas a la primera B >= A - 1: {B['validas_primera']} >= {A['validas_primera'] - 1} -> "
      f"{'CUMPLE' if B['validas_primera'] >= A['validas_primera'] - 1 else 'NO CUMPLE'}")
sube = {k: B['casos_con_otros'].get(k, 0) - A['casos_con_otros'].get(k, 0)
        for k in set(A['casos_con_otros']) | set(B['casos_con_otros'])}
print(f"- C6: ninguna otra categoria sube mas de 1 caso en B: {dict(sorted(sube.items()))} -> "
      f"{'CUMPLE' if all(v <= 1 for v in sube.values()) else 'NO CUMPLE'}")
print(f"- C7 (latencia por prediccion): mediana B <= A x 1,10: {B['mediana_s']:.1f} <= {A['mediana_s'] * 1.1:.1f} -> "
      f"{'CUMPLE' if B['mediana_s'] <= A['mediana_s'] * 1.1 else 'NO CUMPLE'}")
print("- C2, C3, C4: NO MEDIBLES en la primera propuesta (no se publica; no hay reintentos ni respaldo).")
print("- C8: el arbol de la evaluacion no cambia V15, V24, V8C, D8, P-03 ni el catalogo (diff de #69 + #83).")
