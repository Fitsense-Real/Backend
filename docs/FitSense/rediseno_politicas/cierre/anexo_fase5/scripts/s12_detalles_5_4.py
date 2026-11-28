"""5.4 detalles: (1) series de fuerza por slot y semana en IS (REV2/HIST); (2) grupo repetido en dias consecutivos (GM);
(3) slots que faltan en LW; (4) ejercicios de impacto MODERADO/ALTO en LW-B y GF-B (REV2); (5) proporcion de sesiones
LW con fuerza en cada sesion. Uso: python3 -I s12_detalles_5_4.py"""
import sys, collections, datetime, statistics
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *
import importlib.util
spec = importlib.util.spec_from_file_location('s11', SCR + '/scripts/s11_calidad_objetivo.py')
SLOTS = {'LOWER_KNEE': {'KNEE_DOMINANT'}, 'LOWER_HIP': {'HIP_HINGE', 'HIP_EXTENSION'},
         'UPPER_PUSH': {'HORIZONTAL_PUSH', 'VERTICAL_PUSH'}, 'UPPER_PULL': {'HORIZONTAL_PULL', 'VERTICAL_PULL'}}
import re
MOD = re.compile(r"\b(trote|trotar|trotando|correr|carrera|corriendo|skipping|rodillas altas|jumping jacks?|piernas separadas|comba|cuerda|saltar la cuerda|tijeras|escaladores? r[aá]pidos?)\b")
ALTO = re.compile(r"\b(salto|saltos|saltando|pliom[eé]trico|burpee|burpees|sprint|esprint|caja|rebote|zancada con salto|a una pierna con salto|patinador)\b")
def impacto(c):
    nombre = (c['name_es'] or '').lower(); m = bool(MOD.search(nombre))
    if c['high_impact'] == 'true':
        return 'MODERADO' if m and not ALTO.search(nombre.replace('saltos con las piernas separadas', '')) else 'ALTO'
    return 'MODERADO' if m else 'BAJO'
cat = leer_catalogo()
for pol in ('REV2_final', 'HIST_final'):
    b = leer_barrido(pol); p = leer_planes(pol)
    dist = collections.defaultdict(collections.Counter); consec = collections.Counter(); falta = collections.Counter()
    impx = collections.Counter()
    for f in b:
        if f['resultado'] != 'VALIDO': continue
        k = clave(f); o, n, e = f['objetivo'], f['nivel'], f['equipo']
        ses = collections.OrderedDict()
        for x in p[k]: ses.setdefault(x['fecha'], []).append(x)
        fs = collections.Counter(); anys = collections.Counter(); gd = []
        for fe, ex in ses.items():
            g = set()
            for x in ex:
                c = cat[int(x['ejercicio'])]
                if c['clase'] in ('ESTRUCTURAL', 'RESISTIDO'):
                    if c['grupo'] in GRUPOS_GRANDES: g.add(c['grupo'])
                    for sl, pp in SLOTS.items():
                        if (c['movement_pattern'] or '') in pp:
                            anys[sl] += 1
                            if num(x['reps']) is not None and num(x['reps']) <= 8 and num(x['descanso']) in (120, 150):
                                fs[sl] += num(x['series'])
                if pol == 'REV2_final' and o in ('LOSE_WEIGHT', 'GENERAL_FITNESS') and n == 'BEGINNER' and impacto(c) != 'BAJO':
                    impx[(o, c['id'], c['name_en'], c['name_es'], impacto(c), c['high_impact'])] += 1
            gd.append((datetime.date.fromisoformat(fe), g, ex[0]['enfoque']))
        for (d1, g1, f1), (d2, g2, f2) in zip(gd, gd[1:]):
            if (d2 - d1).days == 1 and g1 & g2:
                consec[(o, f1 + '>' + f2, ';'.join(sorted(g1 & g2)))] += 1
        if o == 'INCREASE_STRENGTH':
            disp = [s for s in SLOTS if any(c['is_active'] == 'true' and equipable(c, e) and (c['movement_pattern'] or '') in SLOTS[s] for c in cat.values())]
            for s in disp:
                v = fs[s]
                dist[n]['<3' if v < 3 else '3' if v == 3 else '4-6' if v <= 6 else '7-9' if v <= 9 else '>9'] += 1
        if o == 'LOSE_WEIGHT':
            disp = [s for s in SLOTS if any(c['is_active'] == 'true' and equipable(c, e) and (c['movement_pattern'] or '') in SLOTS[s] for c in cat.values())]
            for s in disp:
                if anys[s] == 0: falta[(n, s)] += 1
    print('==', pol)
    print(' IS series de fuerza por slot y semana (recuento slot-plan):', {n: dict(v) for n, v in dist.items()})
    print(' grupo grande en dias consecutivos (objetivo, enfoques, grupos):', consec.most_common(12))
    print(' LW slots sin ningun ejercicio en la semana (nivel, slot):', dict(falta))
    if impx:
        print(' LW/GF principiante: ejercicios no BAJO (filas):', impx.most_common(10))
