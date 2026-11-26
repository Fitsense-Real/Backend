import json,sys,re,collections
pdir, odir = sys.argv[1], sys.argv[2]
def snapshot(caso,b):
    t=open(f"{pdir}/{caso}_{b}_prompt.txt").read()
    i=t.index('{"', t.index('=====')) if False else None
    # localizar el JSON que contiene available_exercises
    k=t.index('"available_exercises"')
    ini=t.rfind('\n{',0,k)+1
    dec=json.JSONDecoder(); obj,_=dec.raw_decode(t[ini:])
    return obj
def tabla(caso,b):
    snap=snapshot(caso,b)
    ex={e['exercise_id']:e for e in snap['available_exercises']}
    out=json.loads(open(f"{odir}/{caso}_{b}.json").read())
    if isinstance(out,str): out=json.loads(out)
    print(f"== {caso} {b}  split={[ (s.get('focus') or s.get('focus_code'), s.get('max_per_body_part_por_n_trabajo')) for s in snap['constraints'].get('suggested_split',[])][:6]}")
    sem=collections.Counter()
    for w in out['workouts']:
        bp=collections.Counter(); filas=[]
        for e in w['exercises']:
            c=ex.get(e['exercise_id'],{})
            filas.append(f"{e['exercise_id']}:{c.get('body_part','?')[:6]}:{c.get('clase_dosis') or '-'}:{e.get('planned_sets')}x{e.get('planned_reps') or e.get('planned_duration_seconds')}/{e.get('rest_seconds')}")
            if e['exercise_id'] not in snap['constraints']['volumen_semanal']['no_cuentan'] if snap['constraints'].get('volumen_semanal') else True:
                bp[c.get('body_part','?')]+=e.get('planned_sets') or 0
        sem+=bp
        print(" ",w['scheduled_date'],w.get('focus_code'),len(w['exercises']),'ej |',' '.join(filas))
        print("    series por grupo:",dict(bp))
    print("  semana:",dict(sem))
for caso,b in [x.split('_') for x in sys.argv[3:]]:
    tabla(caso,b)
