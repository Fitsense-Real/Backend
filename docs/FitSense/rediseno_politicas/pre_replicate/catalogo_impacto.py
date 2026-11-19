#!/usr/bin/env python3
"""Impacto en la seleccion REAL de los ejercicios dudosos del catalogo (solo lectura; no modifica nada).
Uso: python3 -I catalogo_impacto.py <tablas_catalogo> <dir_barridos> <salida.csv> [<corrida_k20> ...]
  <tablas_catalogo>: tablas/ exportadas por cualquier corrida K20 (exercises, body_parts, equipment_types, ...)
  <dir_barridos>:    evidencia/barridos (usa REV2_cierre y HIST_cierre)
Por ejercicio: planes y colocaciones; si es el UNICO credito de su familia de patron en la semana (sin el, la familia
quedaria sin cubrir); si es el UNICO ejercicio resistido de su grupo corporal (toda la dosis del grupo viene de el) y
las series que aporta. Ademas: planes cuyo patron de rodilla/cadera solo lo cubren ejercicios con is_isolation=true."""
import csv, gzip, collections, sys, os, json
sys.dont_write_bytecode = True
TAB, BARR, SAL = sys.argv[1], sys.argv[2], sys.argv[3]
K20 = sys.argv[4:]
FAM = {'HORIZONTAL_PUSH': 'PUSH', 'VERTICAL_PUSH': 'PUSH', 'HORIZONTAL_PULL': 'PULL', 'VERTICAL_PULL': 'PULL',
       'KNEE_DOMINANT': 'KNEE', 'HIP_HINGE': 'HIP', 'HIP_EXTENSION': 'HIP'}
NO_RESISTIDO = {'', 'OTHER', 'LOCOMOTION_CARDIO', 'MOBILITY', 'BALANCE_CONTROL'}
FOCO = [775, 580, 973, 1320]
OTROS = [1261, 1308, 1263, 853, 1093, 1262, 1115, 1195, 1196]

bp = {r['body_part_id']: r['code'] for r in csv.DictReader(open(f'{TAB}/body_parts.csv'))}
cat = {}
for r in csv.DictReader(open(f'{TAB}/exercises.csv')):
    r['grupo'] = bp[r['body_part_id']]; r['pat'] = (r['movement_pattern'] or '').upper(); cat[int(r['exercise_id'])] = r
AISLADOS_SLOT = sorted(i for i, c in cat.items() if c['is_active'] == 't' or c['is_active'] == 'true'
                       if c['is_isolation'] in ('t', 'true') and c['pat'] in FAM)


def planes(nombre):
    p = collections.defaultdict(list)
    with gzip.open(f'{BARR}/{nombre}/planes.csv.gz', 'rt') as f:
        for r in csv.DictReader(f): p[r['perfil']].append(r)
    validos = {'|'.join([f['objetivo'], f['nivel'], f['equipo'], f['dias'], f['minutos']])
               for f in csv.DictReader(open(f'{BARR}/{nombre}/barrido_generacion.csv')) if f['resultado'] == 'VALIDO'}
    return {k: v for k, v in p.items() if k in validos}


def analizar(p):
    res = {}
    for i in FOCO + OTROS + AISLADOS_SLOT:
        c = cat[i]; fam = FAM.get(c['pat'])
        con = {k: v for k, v in p.items() if any(int(e['ejercicio']) == i for e in v)}
        unico_fam = unico_grupo = series = 0
        obj = collections.Counter(k.split('|')[0] for k in con)
        for k, v in con.items():
            if fam and all(int(e['ejercicio']) == i for e in v if FAM.get(cat[int(e['ejercicio'])]['pat']) == fam):
                unico_fam += 1
            if c['pat'] not in NO_RESISTIDO:
                del_grupo = [e for e in v if cat[int(e['ejercicio'])]['grupo'] == c['grupo'] and cat[int(e['ejercicio'])]['pat'] not in NO_RESISTIDO]
                if all(int(e['ejercicio']) == i for e in del_grupo): unico_grupo += 1
                series += sum(int(e['series'] or 0) for e in v if int(e['ejercicio']) == i)
        res[i] = dict(planes=len(con), colocaciones=sum(1 for v in con.values() for e in v if int(e['ejercicio']) == i),
                      unico_familia=unico_fam, unico_grupo=unico_grupo, series=series, objetivos=dict(obj))
    solo_iso = collections.Counter()
    for k, v in p.items():
        for fam in ('KNEE', 'HIP', 'PUSH', 'PULL'):
            cred = [cat[int(e['ejercicio'])]['is_isolation'] in ('t', 'true') for e in v if FAM.get(cat[int(e['ejercicio'])]['pat']) == fam]
            if cred and all(cred): solo_iso[(fam, k.split('|')[0])] += 1
    return res, solo_iso


def k20(carpeta):
    t = carpeta + '/tablas'
    plan_user = {r['plan_id']: r['user_id'] for r in csv.DictReader(open(f'{t}/weekly_training_plans.csv'))}
    wk_plan = {r['planned_workout_id']: r['plan_id'] for r in csv.DictReader(open(f'{t}/planned_workouts.csv'))}
    us = collections.defaultdict(set)
    for r in csv.DictReader(open(f'{t}/planned_workout_exercises.csv')):
        us[int(r['exercise_id'])].add(plan_user[wk_plan[r['planned_workout_id']]])
    n = len({r['user_id'] for r in csv.DictReader(open(f'{t}/users.csv'))})
    return {i: len(us.get(i, ())) for i in FOCO + OTROS + AISLADOS_SLOT}, n


salida = {}
filas = []
for pol in ('REV2_cierre', 'HIST_cierre'):
    res, solo = analizar(planes(pol))
    salida[pol] = {'solo_aislados': {f'{a}|{b}': n for (a, b), n in sorted(solo.items())}}
    for i, m in res.items():
        filas.append([pol, i, cat[i]['name_en'], cat[i]['name_es'], cat[i]['grupo'], cat[i]['pat'], cat[i]['is_isolation'],
                      cat[i]['difficulty_level'], m['planes'], m['colocaciones'], m['unico_familia'], m['unico_grupo'], m['series'],
                      json.dumps(m['objetivos'])])
exp = {}
for c in K20:
    e, n = k20(c); exp[os.path.basename(c.rstrip('/'))] = (n, {i: v for i, v in e.items() if v})
with open(SAL, 'w', newline='') as f:
    w = csv.writer(f)
    w.writerow(['barrido', 'exercise_id', 'name_en', 'name_es', 'grupo', 'patron', 'is_isolation', 'dificultad', 'planes',
                'colocaciones', 'unico_credito_familia', 'unico_del_grupo', 'series_aportadas', 'planes_por_objetivo'])
    w.writerows(filas)
salida['aislados_con_patron_de_slot'] = AISLADOS_SLOT
salida['k20_usuarios_expuestos'] = exp
print(json.dumps(salida, ensure_ascii=False))
