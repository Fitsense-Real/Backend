"""5.4 GM: mediana de series/semana por grupo grande y nivel (planes validos; grupos con >0), descansos y repeticiones
de las colocaciones resistidas, y LW/GF: series/grupo/semana. REV2_final frente a HIST_final.
Uso: python3 -I s15_gm_distribucion.py"""
import sys, collections, statistics
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *
cat = leer_catalogo()
for pol in ('HIST_final', 'REV2_final'):
    b = leer_barrido(pol); p = leer_planes(pol)
    por = collections.defaultdict(list); desc = collections.defaultdict(collections.Counter)
    for f in b:
        if f['resultado'] != 'VALIDO': continue
        sem, aer, _ = dosis_plan(p[clave(f)], cat)
        for g in GRUPOS_GRANDES + ['waist', 'lower legs']:
            if sem.get(g, 0) > 0: por[(f['objetivo'], f['nivel'], g)].append(sem[g])
        if f['objetivo'] == 'GAIN_MUSCLE':
            for e in p[clave(f)]:
                c = cat[int(e['ejercicio'])]
                if c['clase'] in ('ESTRUCTURAL', 'RESISTIDO'):
                    desc[f['nivel']][f"{e['series']}x{e['reps'] if e['tipo']=='SETS_REPS' else e['segundos']+'s'}/{e['descanso']}"] += 1
    print('==', pol)
    for o in ('GAIN_MUSCLE', 'LOSE_WEIGHT', 'GENERAL_FITNESS', 'INCREASE_STRENGTH'):
        for n in ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'):
            print(' ', o[:4], n[:3], {g: (statistics.median(por[(o, n, g)]), max(por[(o, n, g)]), len(por[(o, n, g)])) for g in GRUPOS_GRANDES + ['waist', 'lower legs'] if por[(o, n, g)]})
    print('  GM prescripciones resistidas mas frecuentes:', {n: v.most_common(5) for n, v in desc.items()})
