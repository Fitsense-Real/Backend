"""Compara variantes C4/C6 con el control V39.

Uso: python3 -I comparar_c4c6.py control=<dir> nombre=<dir> ...
Cada <dir> es la salida de simv38.sh (referencias, barrido, robustez, 192 restricciones y
continuidad). Imprime una tabla resumen y, por variante, los perfiles de referencia que cambian
con los ejercicios que salen y entran.
"""
import collections
import re
import sys

IDS = {'1225', '439', '959', '962', '927', '1223', '674', '162'}
V = [a.split('=', 1) for a in sys.argv[1:]]


def filas(d, f):
    L = open(f'{d}/{f}').read().splitlines()
    h = L[0].split('\t')
    return [dict(zip(h, l.split('\t'))) for l in L[1:] if l and not l.startswith('#')]


def refs(d):
    return {l.split(' | ', 1)[0]: l.strip() for l in open(f'{d}/generacion_referencia.txt')}


def ids(linea):
    return re.findall(r' (\d+):', linea)


def continuidad(d):
    try:
        x = open(f'{d}/' + next(f for f in __import__('os').listdir(d) if 'Continuidad' in f)).read()
        return 'OK' if 'failures="0"' in x and 'errors="0"' in x else 'FALLA'
    except StopIteration:
        return '?'


base = refs(V[0][1])
print('| Variante | Refs validas | Refs que cambian | Barrido valido | Barrido COMPLETO | Sesiones con aviso de mitad (V15) | Robustez | 192 publicados | 192 con implemento ausente / violaciones | Continuidad | Uso de los 8 (refs) |')
print('|---|---|---|---|---|---|---|---|---|---|---|')
detalle = {}
for n, d in V:
    R = refs(d)
    bp = filas(d, 'barrido_4_6_dias_planes.tsv')
    bs = filas(d, 'barrido_4_6_dias_sesiones.tsv')
    rb = [x for x in filas(d, 'robustez_semillas_4_6_dias.tsv') if x.get('resultado')]
    im = filas(d, 'impacto_restricciones.tsv')
    cambian = [k for k in R if R[k] != base.get(k)]
    uso = collections.Counter(e for l in R.values() for e in ids(l) if e in IDS)
    mitad = sum('MAS_DE_LA_MITAD' in s.get('banderas', '') for s in bs)
    print(f"| {n} | {sum(x.endswith('VALIDA') for x in R.values())}/72 | {len(cambian)} | "
          f"{sum(p['validacion'].startswith('VALIDA') for p in bp)}/{len(bp)} | "
          f"{sum(p['cobertura'].startswith('COMPLETO') for p in bp)}/{len(bp)} | {mitad} | "
          f"{sum(x['resultado'].startswith('VALIDA') for x in rb)}/{len(rb)} | "
          f"{sum(x['plan'] == 'PUBLICADO' for x in im)}/{len(im)} | "
          f"{sum(x['equipo_ausente_segun_gif'] not in ('[]', '') for x in im)} / "
          f"{sum(x['violaciones_plan'] not in ('[]', '') for x in im)} | {continuidad(d)} | "
          f"{dict(sorted(uso.items()))} |")
    detalle[n] = cambian, R
for n, (cambian, R) in detalle.items():
    if n == V[0][0] or not cambian:
        continue
    print(f'\n### {n}: {len(cambian)} perfiles cambian\n')
    entran = collections.Counter()
    salen = collections.Counter()
    for k in cambian:
        a, b = collections.Counter(ids(base[k])), collections.Counter(ids(R[k]))
        salen.update(a - b)
        entran.update(b - a)
    print('- Salen: ' + ', '.join(f'{e} x{c}' for e, c in salen.most_common(12)))
    print('- Entran: ' + ', '.join(f'{e} x{c}' for e, c in entran.most_common(12)))
