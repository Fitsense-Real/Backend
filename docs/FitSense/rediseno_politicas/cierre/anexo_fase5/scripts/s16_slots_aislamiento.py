"""5.4 IS/GF: planes cuyo slot (con dosis de fuerza) solo lo acreditan ejercicios con is_isolation=true (p. ej. 1195/1196
curl femoral como KNEE_DOMINANT) y ejercicios KNEE_DOMINANT/HIP_* de aislamiento activos. Uso: python3 -I s16_slots_aislamiento.py"""
import sys, collections
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *
SLOTS = {'LOWER_KNEE': {'KNEE_DOMINANT'}, 'LOWER_HIP': {'HIP_HINGE', 'HIP_EXTENSION'},
         'UPPER_PUSH': {'HORIZONTAL_PUSH', 'VERTICAL_PUSH'}, 'UPPER_PULL': {'HORIZONTAL_PULL', 'VERTICAL_PULL'}}
cat = leer_catalogo()
iso = [(c['id'], c['name_en'], c['movement_pattern'], c['equipo']) for c in cat.values() if c['is_active'] == 'true'
       and c['is_isolation'] == 'true' and c['movement_pattern'] in ('KNEE_DOMINANT', 'HIP_HINGE', 'HIP_EXTENSION', 'HORIZONTAL_PUSH', 'VERTICAL_PUSH', 'HORIZONTAL_PULL', 'VERTICAL_PULL')]
print('activos de aislamiento con patron de slot:', len(iso), collections.Counter(x[2] for x in iso))
for pol in ('REV2_final', 'HIST_final'):
    b = leer_barrido(pol); p = leer_planes(pol)
    t = collections.Counter()
    for f in b:
        if f['resultado'] != 'VALIDO' or f['objetivo'] not in ('INCREASE_STRENGTH', 'GENERAL_FITNESS'): continue
        cred = collections.defaultdict(list)
        for e in p[clave(f)]:
            c = cat[int(e['ejercicio'])]
            for sl, pp in SLOTS.items():
                if (c['movement_pattern'] or '') in pp and num(e['reps']) is not None and num(e['reps']) <= 8 and num(e['descanso']) in (120, 150):
                    cred[sl].append(c['is_isolation'] == 'true')
        for sl, v in cred.items():
            if v and all(v): t[(f['objetivo'], f['nivel'], sl)] += 1
    print(pol, 'planes con slot de fuerza acreditado SOLO por aislamiento:', dict(t), 'total', sum(t.values()))
