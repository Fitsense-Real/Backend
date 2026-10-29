"""Compara la simulacion V36 / V37 / V37+V37.1. Uso, desde la raiz del repo:
   python3 -I docs/FitSense/auditoria_catalogo/v37/comparar_v37.py <carpeta_sim>

<carpeta_sim> tiene v36/, v37/ y v37b/ con la salida de las pruebas de integracion
(GeneracionReferenciaCandidataIT, EvaluacionFrecuenciaIT, ImpactoRestriccionesIT,
ContinuidadSemanalIT y Usuario31ReproduccionIT). Escribe resultados_simulacion.md
en esta carpeta.
"""
import collections
import csv
import os
import re
import sys

SIM = sys.argv[1]
AQUI = os.path.dirname(os.path.abspath(__file__))
CAT = ['v36', 'v37', 'v37b']
NOM = {'v36': 'V36 (vigente)', 'v37': 'V37 (bloque A)', 'v37b': 'V37 + V37.1'}
OFICIAL = 'docs/FitSense/evidencia/progresion/generacion_referencia.txt'


def lineas(p):
    return [l.rstrip('\n') for l in open(p) if l.strip()] if os.path.exists(p) else []


def sesiones(linea):
    perfil, resto = linea.split(' ', 1)
    cuerpo, valid = resto.rsplit(' || ', 1) if ' || ' in resto else (resto, resto)
    return perfil, [s.strip() for s in cuerpo.split('|')[1:]], valid


def tsv(p):
    if not os.path.exists(p):
        return []
    return [r for r in csv.DictReader((l for l in open(p) if l.strip() and not l.startswith('#')), delimiter='\t')]


out = []
P = out.append
P('# Simulación V36 / V37 / V37 + V37.1\n')
P('Bases PostgreSQL 16 aisladas (Testcontainers). V37 y V37.1 se cargan con `spring.flyway.locations`, sin estar en `db/migration`. Motor de reglas, sin IA ni Replicate.\n')

# 1. Referencias
P('## 1. Generación de referencia (72 perfiles)\n')
ref = {c: [sesiones(l) for l in lineas(f'{SIM}/{c}/generacion_referencia.txt')] for c in CAT}
of = [sesiones(l) for l in lineas(OFICIAL)]
P(f"- V36 simulado frente a la referencia oficial: **{'idéntico' if ref['v36'] == of else 'DISTINTO'}** ({len(ref['v36'])} perfiles).")
P('\n| Catálogo | Perfiles | Válidos | Sin plan | Perfiles distintos de V36 | Sesiones distintas de V36 |\n|---|---:|---:|---:|---:|---:|')
for c in CAT:
    r = ref[c]
    val = sum(1 for _, _, v in r if v == 'VALIDA')
    sin = sum(1 for _, s, v in r if 'SIN_PLAN' in v or not s)
    dp = sum(1 for a, b in zip(ref['v36'], r) if a != b)
    ds = sum(sum(1 for x, y in zip(a[1], b[1]) if x != y) + abs(len(a[1]) - len(b[1])) for a, b in zip(ref['v36'], r))
    P(f'| {NOM[c]} | {len(r)} | {val} | {sin} | {dp} | {ds} |')
noval = {c: [(p, v) for p, _, v in ref[c] if v != 'VALIDA'] for c in CAT}
for c in CAT:
    if noval[c]:
        P(f'\nNo válidos con {NOM[c]}:')
        for p, v in noval[c]:
            P(f'- {p}: {v[:300]}')

# 2. Barrido y robustez
P('\n## 2. Barrido de 4 a 6 días (72 perfiles) y robustez (288 generaciones)\n')
P('| Catálogo | Barrido: válidos | Barrido: no válidos | Robustez: válidas | Robustez: sin plan |\n|---|---:|---:|---:|---:|')
rob = {}
for c in CAT:
    b = tsv(f'{SIM}/{c}/barrido_4_6_dias_planes.tsv')
    rb = tsv(f'{SIM}/{c}/robustez_semillas_4_6_dias.tsv')
    rob[c] = rb
    P(f"| {NOM[c]} | {sum(1 for r in b if r['validacion'] == 'VALIDA')} | {sum(1 for r in b if r['validacion'] != 'VALIDA')} | "
      f"{sum(1 for r in rb if r['resultado'] == 'VALIDA')} | {sum(1 for r in rb if r['resultado'] != 'VALIDA')} |")
for c in CAT:
    sin = collections.Counter(r['perfil'] for r in rob[c] if r['resultado'] != 'VALIDA')
    if sin:
        P(f'\nSin plan en robustez con {NOM[c]} (perfil: semillas de 4): ' + ', '.join(f'{k}={v}' for k, v in sorted(sin.items())))

# 3. Continuidad
P('\n## 3. Continuidad semanal (8 LOSE_WEIGHT de 5-6 días y 96 perfiles)\n')
P('| Catálogo | Pruebas | Fallos | Detalle |\n|---|---:|---:|---|')
for c in CAT:
    xs = [f for f in os.listdir(f'{SIM}/{c}') if f.endswith('ContinuidadSemanalIT.xml')] if os.path.isdir(f'{SIM}/{c}') else []
    if not xs:
        P(f'| {NOM[c]} | — | — | sin informe |')
        continue
    x = open(f'{SIM}/{c}/{xs[0]}').read()
    m = re.search(r'tests="(\d+)".*?failures="(\d+)"', x) or re.search(r'failures="(\d+)".*?tests="(\d+)"', x)
    tests = re.search(r'tests="(\d+)"', x).group(1)
    fails = int(re.search(r'failures="(\d+)"', x).group(1)) + int(re.search(r'errors="(\d+)"', x).group(1))
    det = re.findall(r'<failure message="([^"]{0,400})', x)
    P(f"| {NOM[c]} | {tests} | {fails} | {'; '.join(d.replace('|', '/') for d in det)[:600] or '—'} |")

# 4. Restricciones
P('\n## 4. Perfiles con restricciones (192: 4 objetivos x 3 niveles x gimnasio/casa x 8 restricciones)\n')
imp = {c: tsv(f'{SIM}/{c}/impacto_restricciones.tsv') for c in CAT}
P('| Catálogo | Perfiles con violaciones entre sus elegibles | Violaciones elegibles | Planes con un ejercicio prohibido | Sin plan | 4C NO_FACTIBLE | 4C FRAGIL |\n|---|---:|---:|---:|---:|---:|---:|')
for c in CAT:
    r = imp[c]
    ve = [x for x in r if x['violaciones_elegibles'] != '[]']
    nve = sum(len(x['violaciones_elegibles'].strip('[]').split(', ')) for x in ve)
    vp = sum(1 for x in r if x['violaciones_plan'] != '[]')
    sp = sum(1 for x in r if x['plan'].startswith('SIN_PLAN'))
    nf = sum(1 for x in r if 'veredicto=NO_FACTIBLE' in x['cobertura_4c'])
    fr = sum(1 for x in r if 'veredicto=FRAGIL' in x['cobertura_4c'])
    P(f'| {NOM[c]} | {len(ve)} | {nve} | {vp} | {sp} | {nf} | {fr} |')
P('\n**Por restricción** (planes con un ejercicio prohibido / sin plan / media de elegibles de LOWER_HIP):\n')
restr = sorted({x['perfil'].split('/')[3] for x in imp['v36']}) if imp['v36'] else []
P('| Restricción | ' + ' | '.join(NOM[c] for c in CAT) + ' |\n|---|' + '---|' * len(CAT))
for rr in restr:
    cel = []
    for c in CAT:
        xs = [x for x in imp[c] if x['perfil'].split('/')[3] == rr]
        hip = [len([y for y in x['lower_hip'].strip('[]').split(', ') if y]) for x in xs]
        cel.append(f"{sum(1 for x in xs if x['violaciones_plan'] != '[]')} / {sum(1 for x in xs if x['plan'].startswith('SIN_PLAN'))}"
                   f" / {sum(hip) / max(1, len(hip)):.1f}")
    P(f'| {rr} | ' + ' | '.join(cel) + ' |')
P('\n**LOWER_HIP en perfiles sin suelo o sin suspensión** (elegibles mínimo / medio, perfiles con 0, 1 o 2):\n')
P('| Catálogo | Perfiles | Mínimo | Media | Con 0 | Con 1-2 (FRAGIL) |\n|---|---:|---:|---:|---:|---:|')
for c in CAT:
    xs = [x for x in imp[c] if x['perfil'].split('/')[3] in ('SIN_SUELO', 'SIN_SUSPENSION', 'COMO_USUARIO_31', 'TODAS')]
    hip = [len([y for y in x['lower_hip'].strip('[]').split(', ') if y]) for x in xs]
    if hip:
        P(f'| {NOM[c]} | {len(hip)} | {min(hip)} | {sum(hip) / len(hip):.1f} | {sum(1 for h in hip if h == 0)} | {sum(1 for h in hip if 1 <= h <= 2)} |')
cambios = []
for a, b in zip(imp['v36'], imp['v37']):
    if a['plan'] != b['plan'] or a['cobertura_4c'].split('veredicto=')[-1][:12] != b['cobertura_4c'].split('veredicto=')[-1][:12]:
        cambios.append((a['perfil'], a['plan'], b['plan'], a['cobertura_4c'][-40:], b['cobertura_4c'][-40:]))
if cambios:
    P('\nPerfiles cuyo plan o veredicto 4C cambia de V36 a V37:\n')
    P('| Perfil | Plan V36 | Plan V37 | 4C V36 | 4C V37 |\n|---|---|---|---|---|')
    for x in cambios:
        P('| ' + ' | '.join(x) + ' |')

# 5. Usuario 31
P('\n## 5. Usuario 31 (respuestas guardadas de Replicate y plan 63)\n')
for c in CAT:
    lg = f'{SIM}/{c}/u31.log'
    ls = [l for l in lineas(lg) if re.match(r'^(actual|corregido|v37b?) ', l)]
    P(f'**{NOM[c]}**\n')
    for l in ls:
        P(f'- `{l[:400]}`')
    P('')

open(os.path.join(AQUI, 'resultados_simulacion.md'), 'w').write('\n'.join(out) + '\n')
print('\n'.join(out))
