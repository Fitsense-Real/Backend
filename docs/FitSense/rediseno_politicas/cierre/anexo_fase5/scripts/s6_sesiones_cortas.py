"""5.2 Planes REV2_final con alguna sesion CORTA_RAZONABLE (< 50 % de M y >= 3 ejercicios de trabajo y >= 6 series
efectivas, criterio de CalidadDosis). Para cada plan: cobertura semanal de los 4 slots (sobre los que tienen material
con ese equipamiento), frecuencia por grupo grande (sesiones que lo entrenan con series directas), dosis (clase a/b de
s2), y comprobaciones de 02_EVIDENCIA_DOSIS §2 en cada sesion corta.
Salida: csv/5_2_planes_cortos.csv, csv/5_2_sesiones_cortas.csv. Uso: python3 -I s6_sesiones_cortas.py (tras s2)"""
import sys, collections, csv, statistics
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

SLOTS = {'LOWER_KNEE': {'KNEE_DOMINANT'}, 'LOWER_HIP': {'HIP_HINGE', 'HIP_EXTENSION'},
         'UPPER_PUSH': {'HORIZONTAL_PUSH', 'VERTICAL_PUSH'}, 'UPPER_PULL': {'HORIZONTAL_PULL', 'VERTICAL_PULL'}}
cat = leer_catalogo()
b = leer_barrido('REV2_final')
p = leer_planes('REV2_final')
lim = {r['perfil']: r for r in csv.DictReader(open(SCR + '/csv/5_1_dosis_limitada_planes.csv'))}


def seg_ej(e):
    s = num(e['series']) or 1
    w = s * (num(e['segundos']) or 0) if e['tipo'] == 'DURATION' else s * (num(e['reps']) or 0) * 3
    r = num(e['descanso'])
    return w + max(0, s - 1) * (60 if r is None else r)


def seg_sesion(ex):
    return 300 + sum(seg_ej(e) for e in ex) + (len(ex) - 1) * 60 if ex else 0


slot_disp = {e: {s for s, pats in SLOTS.items() if any(c['is_active'] == 'true' and equipable(c, e)
                                                      and (c['movement_pattern'] or '') in pats for c in cat.values())}
             for e in EQUIPOS}
print('slots con material por equipo:', {e: sorted(v) for e, v in slot_disp.items()})
pl_rows, se_rows = [], []
for f in b:
    if f['resultado'] != 'VALIDO':
        continue
    k = clave(f)
    o, n, e, d, m = f['objetivo'], f['nivel'], f['equipo'], int(f['dias']), int(f['minutos'])
    ses = collections.OrderedDict()
    for x in p[k]:
        ses.setdefault(x['fecha'], []).append(x)
    cortas = []
    slots_sem = collections.Counter()
    fuerza_por_slot = collections.Counter()
    frec = collections.Counter()
    for fe, ex in ses.items():
        trabajo = efect = 0
        grupos_ses = set()
        pats = set()
        multi = 0
        for x in ex:
            c = cat[int(x['ejercicio'])]
            s = num(x['series']) or 0
            if c['clase'] == 'CARDIO':
                trabajo += 1; efect += s
            elif c['clase'] in ('ESTRUCTURAL', 'RESISTIDO'):
                trabajo += 1; efect += s
                if c['grupo']:
                    grupos_ses.add(c['grupo'])
                for sl, pp in SLOTS.items():
                    if (c['movement_pattern'] or '') in pp:
                        slots_sem[sl] += 1; pats.add(sl)
                        # dosis de fuerza: principiante 2x8/120, resto 3x5/150 (CapabilityPrescriptionPolicy)
                        if (num(x['reps']) is not None and num(x['reps']) <= 8 and num(x['descanso']) in (120, 150)):
                            fuerza_por_slot[sl] += s
                if c['clase'] == 'ESTRUCTURAL' and c['is_isolation'] != 'true':
                    multi += 1
        for g in grupos_ses:
            frec[g] += 1
        sg = seg_sesion(ex)
        corta = trabajo >= 3 and efect >= 6 and sg * 2 < m * 60
        if corta:
            cortas.append(fe)
            se_rows.append(dict(perfil=k, objetivo=o, nivel=n, equipo=e, dias=d, minutos=m, fecha=fe,
                                enfoque=ex[0]['enfoque'], segundos=sg, pct_M=round(100 * sg / (m * 60), 1),
                                ejercicios=len(ex), ejercicios_trabajo=trabajo, series_efectivas=efect,
                                multiarticulares=multi, patrones_sesion=len(pats),
                                cardio=sum(1 for x in ex if cat[int(x['ejercicio'])]['clase'] == 'CARDIO')))
    if not cortas:
        continue
    sem, aer, _ = dosis_plan(p[k], cat)
    grupos_mat = [g for g in GRUPOS_GRANDES if any(c['grupo'] == g and c['is_active'] == 'true' and equipable(c, e)
                                                    and c['clase'] in ('ESTRUCTURAL', 'RESISTIDO') for c in cat.values())]
    FOC = {'FULL_BODY': {"chest", "back", "upper legs", "shoulders"}, 'UPPER_BODY': {"chest", "back", "shoulders", "upper arms"},
           'LOWER_BODY': {"upper legs"}, 'PUSH': {"chest", "shoulders", "upper arms"}, 'PULL': {"back", "upper arms"},
           'LEGS': {"upper legs"}}
    admit = set().union(*[FOC.get(ex[0]['enfoque'], set()) for ex in ses.values()])
    grupos_mat = [g for g in grupos_mat if g in admit]
    cubiertos = [s for s in slot_disp[e] if slots_sem[s] > 0]
    pl_rows.append(dict(perfil=k, objetivo=o, nivel=n, equipo=e, dias=d, minutos=m, sesiones=len(ses),
                        sesiones_cortas=len(cortas), estado_dosis=f['estado_dosis'],
                        clase_dosis=lim[k]['clase_estricto'] if k in lim else 'objetivo',
                        slots_disponibles=len(slot_disp[e]), slots_cubiertos=len(cubiertos),
                        slots_fuerza_cubiertos=sum(1 for s in slot_disp[e] if fuerza_por_slot[s] > 0),
                        series_fuerza_min_por_slot=min([fuerza_por_slot[s] for s in slot_disp[e]] or [0]),
                        grupos_grandes_frec2=sum(1 for g in grupos_mat if frec[g] >= 2),
                        grupos_grandes_con_material=len(grupos_mat),
                        slots_faltan=';'.join(sorted(set(slot_disp[e]) - set(cubiertos))),
                        series_grupo_min=min(sem.get(g, 0) for g in grupos_mat), series_grupo_max=max(sem.values() or [0]),
                        aerobico_min=aer, pct_tiempo_medio=f['pct_tiempo_medio']))
for nombre, rows in (('5_2_planes_cortos', pl_rows), ('5_2_sesiones_cortas', se_rows)):
    with open(SCR + f'/csv/{nombre}.csv', 'w', newline='') as fh:
        w = csv.DictWriter(fh, fieldnames=list(rows[0]))
        w.writeheader(); w.writerows(rows)
print('planes con sesion corta:', len(pl_rows), 'sesiones cortas:', len(se_rows))
tot = collections.Counter((f['objetivo'], f['nivel']) for f in b if f['resultado'] == 'VALIDO')
t = collections.defaultdict(list)
for r in pl_rows:
    t[(r['objetivo'], r['nivel'])].append(r)
print('\nobjetivo/nivel | planes con corta (% validos) | dias(min-max) | clase dosis | slots cubiertos/disp todos | '
      'fuerza en todos los slots | grupos frec>=2 (todos) | med series_grupo_min')
for k in sorted(tot):
    v = t.get(k, [])
    if not v:
        print(k, 0); continue
    print(k, len(v), f"({100*len(v)/tot[k]:.0f}%)", 'dias', collections.Counter(r['dias'] for r in v).most_common(),
          collections.Counter(r['clase_dosis'] for r in v), 'slots_todos', sum(r['slots_cubiertos'] == r['slots_disponibles'] for r in v),
          'fuerza_todos', sum(r['slots_fuerza_cubiertos'] == r['slots_disponibles'] for r in v),
          'frec2_todos', sum(r['grupos_grandes_frec2'] == r['grupos_grandes_con_material'] for r in v),
          'med_sgmin', statistics.median(r['series_grupo_min'] for r in v))
print('slots que faltan (plan con corta):', collections.Counter((r['objetivo'], r['nivel'], r['equipo'], r['slots_faltan']) for r in pl_rows if r['slots_faltan']))
for dim in ('equipo', 'dias'):
    print(dim, sorted(collections.Counter(r[dim] for r in pl_rows).items()))
print('\nsesiones cortas por objetivo/nivel: mediana pct_M, seg, ej_trabajo, series_efectivas, multiart., patrones; '
      'cumple 3-5 ej / 6-12 series')
t = collections.defaultdict(list)
for r in se_rows:
    t[(r['objetivo'], r['nivel'])].append(r)
for k in sorted(t):
    v = t[k]
    med = lambda c: statistics.median(r[c] for r in v)
    print(k, len(v), 'pct', med('pct_M'), 'seg', med('segundos'), 'ejT', med('ejercicios_trabajo'),
          'sef', med('series_efectivas'), 'multi', med('multiarticulares'), 'pat', med('patrones_sesion'),
          '3-5ej', sum(3 <= r['ejercicios_trabajo'] <= 5 for r in v), '6-12s', sum(6 <= r['series_efectivas'] <= 12 for r in v),
          'enfoques', dict(collections.Counter(r['enfoque'] for r in v)))
print('\nsesiones cortas por enfoque y patrones en la sesion:',
      sorted(collections.Counter((r['enfoque'], r['patrones_sesion']) for r in se_rows).items()))
print('min seg sesion corta', min(r['segundos'] for r in se_rows), 'min pct', min(r['pct_M'] for r in se_rows))
