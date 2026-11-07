"""5.2 Criterios de 02_EVIDENCIA_DOSIS §2 sobre las sesiones cortas (csv/5_2_sesiones_cortas.csv) y semana (csv/5_2_planes_cortos.csv):
 - 3-5 ejercicios de trabajo; >= 3 multiarticulares (para objetivos de fuerza: IS); 6-12 series efectivas;
 - FULL_BODY: >= 3 patrones en 20-25 min y >= 4 en 30-35 min (con slot de traccion disponible o no);
 - semana: todos los slots disponibles cubiertos; minimo semanal (clase a/b de s2) y aviso al usuario (no existe en la API).
Uso: python3 -I s14_criterios_cortas.py"""
import sys, csv, collections
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *
S = list(csv.DictReader(open(SCR + '/csv/5_2_sesiones_cortas.csv')))
P = list(csv.DictReader(open(SCR + '/csv/5_2_planes_cortos.csv')))
t = collections.defaultdict(collections.Counter)
for r in S:
    k = (r['objetivo'], r['nivel'])
    t[k]['n'] += 1
    t[k]['ej3_5'] += 3 <= int(r['ejercicios_trabajo']) <= 5
    t[k]['multi>=3'] += int(r['multiarticulares']) >= 3
    t[k]['series6_12'] += 6 <= int(r['series_efectivas']) <= 12
    if r['enfoque'] == 'FULL_BODY':
        t[k]['fb'] += 1
        mins = int(r['segundos']) / 60
        need = 4 if mins >= 30 else 3
        t[k]['fb_patrones_ok'] += int(r['patrones_sesion']) >= need
    t[k]['min<20'] += int(r['segundos']) < 1200
for k in sorted(t):
    x = t[k]
    print(k, dict(x))
w = collections.defaultdict(collections.Counter)
for r in P:
    k = (r['objetivo'], r['nivel'])
    w[k]['planes'] += 1
    w[k]['slots_todos'] += r['slots_cubiertos'] == r['slots_disponibles']
    w[k]['dosis_objetivo'] += r['clase_dosis'] == 'objetivo'
    w[k]['a_sobre_min'] += r['clase_dosis'] == 'a_sobre_minimo'
    w[k]['b_bajo_min'] += r['clase_dosis'] == 'b_bajo_minimo'
    w[k]['frec2_todos'] += r['grupos_grandes_frec2'] == r['grupos_grandes_con_material']
print()
for k in sorted(w): print(k, dict(w[k]))
print('total', sum(w[k]['planes'] for k in w), 'b_bajo_min', sum(w[k]['b_bajo_min'] for k in w), 'slots_todos', sum(w[k]['slots_todos'] for k in w))
