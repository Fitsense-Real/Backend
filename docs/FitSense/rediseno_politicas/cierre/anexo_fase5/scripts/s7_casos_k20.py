"""5.2/5.3 Casos J03 (REV2_D_J) y L14 (REV2_D_L): plan de la semana S1 (2026-11-02) ejercicio a ejercicio, con
segundos estimados (estimador real), % de M, series por grupo, minutos aerobicos y traza 'dosis' de
planning_policy_trace. Uso: python3 -I s7_casos_k20.py J03|L14 ...   Salida: csv/5_2_caso_<id>.csv"""
import sys, csv, json, collections
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

csv.field_size_limit(10 ** 9)
cat = leer_catalogo()
for caso in sys.argv[1:]:
    d = SCR + '/ext/' + ('REV2_D_J' if caso.startswith('J') else 'REV2_D_L') + '/tablas'
    u = [r for r in csv.DictReader(open(d + '/user_profiles.csv')) if r['goal_text'].endswith(' ' + caso)]
    assert len(u) == 1, u
    u = u[0]
    print('==', caso, {k: u[k] for k in ('user_id', 'fitness_level', 'goal_type', 'training_location', 'days_per_week',
                                          'available_days', 'session_minutes', 'equipment_codes', 'allow_high_impact')})
    planes = [r for r in csv.DictReader(open(d + '/weekly_training_plans.csv'))
              if r['user_id'] == u['user_id'] and r['week_start_date'] == '2026-11-02']
    for pl in planes:
        print('plan', pl['plan_id'], 'v', pl['plan_version'], pl['status'], pl['generation_source'], pl['planning_policy'])
        tr = json.loads(pl['planning_policy_trace']) if pl['planning_policy_trace'] else {}
        print('  traza.dosis:', json.dumps(tr.get('dosis'), ensure_ascii=False))
        print('  traza.sesiones:', [(s['fecha'], s['segundos_estimados'], s['pct_de_m']) for s in tr.get('sesiones', [])])
    pl = sorted(planes, key=lambda r: int(r['plan_version']))[0]
    pw = [r for r in csv.DictReader(open(d + '/planned_workouts.csv')) if r['plan_id'] == pl['plan_id']]
    pwe = collections.defaultdict(list)
    for r in csv.DictReader(open(d + '/planned_workout_exercises.csv')):
        pwe[r['planned_workout_id']].append(r)
    M = int(u['session_minutes']) * 60
    out = []
    sem = collections.Counter(); aer = 0
    for w in sorted(pw, key=lambda r: r['scheduled_date']):
        ex = sorted(pwe[w['planned_workout_id']], key=lambda r: int(r['exercise_order']))
        tot = 300 + 60 * (len(ex) - 1)
        for e in ex:
            s = int(e['planned_sets'] or 1)
            work = s * int(e['planned_duration_seconds'] or 0) if e['prescription_type'] == 'DURATION' \
                else s * int(e['planned_reps'] or 0) * 3
            rest = max(0, s - 1) * int(e['rest_seconds'] or 60)
            tot += work + rest
            c = cat[int(e['exercise_id'])]
            if c['clase'] in ('ESTRUCTURAL', 'RESISTIDO'):
                sem[c['grupo']] += s
            if c['clase'] == 'CARDIO' and e['planned_duration_seconds']:
                aer += s * int(e['planned_duration_seconds'])
            out.append(dict(caso=caso, fecha=w['scheduled_date'], enfoque=w['focus_code'], orden=e['exercise_order'],
                            exercise_id=c['id'], source_code=c['source_code'], name_en=c['name_en'], name_es=c['name_es'],
                            grupo=c['grupo'], target=c['target_muscle'], patron=c['movement_pattern'], clase=c['clase'],
                            aislamiento=c['is_isolation'], dificultad=c['difficulty_level'], high_impact=c['high_impact'],
                            tipo=e['prescription_type'], series=e['planned_sets'], reps=e['planned_reps'],
                            segundos=e['planned_duration_seconds'], descanso=e['rest_seconds'], estado=w['status']))
        print(f"  {w['scheduled_date']} {w['focus_code']:11s} {w['status']:10s} est={tot}s ({100*tot/M:.0f}% de M) "
              f"ej={len(ex)} | " + '; '.join(f"{e['exercise_id']} {cat[int(e['exercise_id'])]['name_es']} "
                                              f"{e['planned_sets']}x{e['planned_reps'] or e['planned_duration_seconds'] + 's'}/{e['rest_seconds']}"
                                              for e in ex))
    print('  series por grupo (semana):', dict(sem), 'aerobico min:', aer // 60)
    with open(SCR + f'/csv/5_2_caso_{caso}.csv', 'w', newline='') as fh:
        wr = csv.DictWriter(fh, fieldnames=list(out[0]))
        wr.writeheader(); wr.writerows(out)
