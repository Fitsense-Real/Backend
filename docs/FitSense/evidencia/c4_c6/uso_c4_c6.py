"""Uso de los 8 ejercicios C4/C6 en una simulacion (referencias, barrido y 192 restricciones).

Uso: python3 -I uso_c4_c6.py <dir_simulacion> [<referencia_v37.txt>]
Cuenta apariciones por objetivo, nivel, lugar y enfoque del dia en que aparecen.
"""
import collections
import csv
import re
import sys

IDS = ['1225', '439', '959', '962', '927', '1223', '674', '162']
d = sys.argv[1]


def ref(path):
    uso = {i: collections.Counter() for i in IDS}
    for linea in open(path):
        partes = linea.rstrip('\n').split(' | ')
        perfil = partes[0]
        for sesion in partes[1:]:
            m = re.match(r'\s*(\w+) (\w+) \d+min (.*)', sesion)
            if not m:
                continue
            for e in re.findall(r'(\d+):', m.group(3)):
                if e in uso:
                    obj, niv, _, lugar = perfil.split('/')
                    uso[e][f'{obj}/{niv}/{lugar}/{m.group(2)}'] += 1
    return uso


def barrido(path):
    uso = {i: collections.Counter() for i in IDS}
    for r in csv.DictReader(open(path), delimiter='\t'):
        for e in re.findall(r'\d+', r['ejercicios']):
            if e in uso:
                obj, niv, dias, _, lugar = r['perfil'].split('/')
                uso[e][f'{obj}/{niv}/{dias}/{lugar}/{r["enfoque"]}'] += 1
    return uso


def restricciones(path):
    uso = collections.Counter()
    for r in csv.DictReader(open(path), delimiter='\t'):
        for e in set(re.findall(r'\d+', r['ejercicios_plan'])):
            if e in IDS:
                uso[e] += 1
    return uso


r39 = ref(d + '/generacion_referencia.txt')
r37 = ref(sys.argv[2]) if len(sys.argv) > 2 else None
b = barrido(d + '/barrido_4_6_dias_sesiones.tsv')
rs = restricciones(d + '/impacto_restricciones.tsv')
print('| Ej. | Referencias V37 | Referencias V39 | Barrido 4-6 dias (sesiones) | 192 restricciones (perfiles) |')
print('|---|---|---|---|---|')
for i in IDS:
    print(f'| {i} | {sum(r37[i].values()) if r37 else "-"} | {sum(r39[i].values())} | {sum(b[i].values())} | {rs[i]} |')
for i in IDS:
    print(f'\n**{i}** referencias V39: ' + ', '.join(f'{k} x{v}' for k, v in sorted(r39[i].items())))
    print(f'  barrido: ' + ', '.join(f'{k} x{v}' for k, v in sorted(b[i].items())))
