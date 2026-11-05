"""5.1 Causa MECANICA de cada componente en deficit (REV2_final), mirando las sesiones que podrian alojarlo:
  regla_enfoque   : ninguna sesion del plan admite el grupo (WorkoutFocus; p. ej. 'upper arms' en FULL_BODY)
  catalogo        : <= 1 ejercicio resistido elegible del grupo con ese equipamiento
  techo_V26_sesion: en todas las sesiones que lo admiten el grupo ya esta en el techo por sesion (6 B / 10 I-A)
  tope_ejercicios : todas esas sesiones estan en el tope de colocaciones (GM 6 = V24/MvpDosePolicy; resto 8)
  tiempo          : todas esas sesiones no admiten un ejercicio mas sin pasar de M (estimador real: 300 s de
                    calentamiento + trabajo + descansos + 60 s por transicion)
  tiempo_o_tope   : mezcla de las dos anteriores
  otras_reglas    : alguna sesion tenia hueco de tiempo y de colocaciones -> lo impidio otra regla (V15 mitad,
                    V18 dias pegados, V22, agotamiento del pool del enfoque, reparto de la dosis por sesion)
Ademas une la causa contrafactual de s2 (csv/5_1_componentes.csv). Salida csv/5_1_componentes_mecanica.csv.
Uso: python3 -I s4_causa_mecanica.py  (requiere haber ejecutado s2)"""
import sys, collections, csv, math
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

FOCUS = {'FULL_BODY': ["chest", "back", "upper legs", "shoulders", "waist"],
         'UPPER_BODY': ["chest", "back", "shoulders", "upper arms"], 'LOWER_BODY': ["upper legs", "lower legs", "waist"],
         'PUSH': ["chest", "shoulders", "upper arms"], 'PULL': ["back", "upper arms"], 'LEGS': ["upper legs", "lower legs"]}
cat = leer_catalogo()
b = {clave(f): f for f in leer_barrido('REV2_final')}
p = leer_planes('REV2_final')
elig = {e: collections.Counter(c['grupo'] for c in cat.values() if c['is_active'] == 'true' and equipable(c, e)
                               and c['clase'] in ('ESTRUCTURAL', 'RESISTIDO')) for e in EQUIPOS}


def seg_ej(e):
    s = num(e['series']) or 1
    w = s * (num(e['segundos']) or 0) if e['tipo'] == 'DURATION' else s * (num(e['reps']) or 0) * 3
    r = num(e['descanso'])
    return w + max(0, s - 1) * (60 if r is None else r)


def seg_sesion(ex):
    return 300 + sum(seg_ej(e) for e in ex) + (len(ex) - 1) * 60 if ex else 0


comps = list(csv.DictReader(open(SCR + '/csv/5_1_componentes.csv')))
out = []
for r in comps:
    f = b[r['perfil']]
    M = int(f['minutos']) * 60
    nivel, obj, e = f['nivel'], f['objetivo'], f['equipo']
    ses = collections.OrderedDict()
    for x in p[r['perfil']]:
        ses.setdefault(x['fecha'], []).append(x)
    g = r['componente']
    tope = 6 if obj == 'GAIN_MUSCLE' else 8
    techo_s = 6 if nivel == 'BEGINNER' else 10
    res = [x for ex in ses.values() for x in ex if cat[int(x['ejercicio'])]['clase'] in ('ESTRUCTURAL', 'RESISTIDO')]
    car = [x for ex in ses.values() for x in ex if cat[int(x['ejercicio'])]['clase'] == 'CARDIO']
    if g == 'aerobico':
        coste = (sorted(seg_ej(x) for x in car)[len(car) // 2] if car else 390) + 60
        cand = list(ses.values())
    else:
        coste = (sorted(seg_ej(x) for x in res)[len(res) // 2] if res else 228) + 60
        cand = [ex for fe, ex in ses.items() if g in FOCUS.get(ex[0]['enfoque'], [])]
    if g != 'aerobico' and not cand:
        mec = 'regla_enfoque'
    elif g != 'aerobico' and elig[e][g] <= 1:
        mec = 'catalogo'
    else:
        bloq = []
        for ex in cand:
            sg = sum(num(x['series']) or 0 for x in ex if cat[int(x['ejercicio'])]['grupo'] == g
                     and cat[int(x['ejercicio'])]['clase'] in ('ESTRUCTURAL', 'RESISTIDO'))
            if g != 'aerobico' and sg + 2 > techo_s:
                bloq.append('techo')
            elif len(ex) >= tope:
                bloq.append('tope')
            elif seg_sesion(ex) + coste > M:
                bloq.append('tiempo')
            else:
                bloq.append('hueco')
        s = set(bloq)
        if 'hueco' in s:
            mec = 'otras_reglas'
        elif s == {'techo'}:
            mec = 'techo_V26_sesion'
        elif s == {'tope'}:
            mec = 'tope_ejercicios'
        elif s == {'tiempo'}:
            mec = 'tiempo'
        else:
            mec = 'tiempo_o_tope' if 'techo' not in s else 'techo_y_' + '_'.join(sorted(s - {'techo'}))
    r2 = dict(r)
    r2['causa_mecanica'] = mec
    out.append(r2)
with open(SCR + '/csv/5_1_componentes_mecanica.csv', 'w', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=list(out[0]))
    w.writeheader(); w.writerows(out)
t = collections.defaultdict(collections.Counter)
for r in out:
    t[(r['objetivo'], r['componente'])][r['causa_mecanica']] += 1
for k in sorted(t):
    print(k, sum(t[k].values()), dict(t[k].most_common()))
print('TOTAL', collections.Counter(r['causa_mecanica'] for r in out))
print('cruce mecanica x contrafactual', sorted(collections.Counter((r['causa_mecanica'], r['causa_dominante'])
                                                                   for r in out).items()))
# verificacion del estimador con seg_min_sesion / seg_max_sesion del barrido
ok = bad = 0
for k, f in b.items():
    if f['resultado'] != 'VALIDO':
        continue
    ses = collections.OrderedDict()
    for x in p[k]:
        ses.setdefault(x['fecha'], []).append(x)
    v = [seg_sesion(ex) for ex in ses.values()]
    if min(v) == int(f['seg_min_sesion']) and max(v) == int(f['seg_max_sesion']):
        ok += 1
    else:
        bad += 1
print('estimador reproducido en', ok, 'perfiles; discrepancias', bad)
