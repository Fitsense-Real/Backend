"""5.4 Calidad por objetivo (REV2_final frente a HIST_final; solo planes VALIDOS).
GM : series directas/grupo grande/semana (mediana por nivel, % de grupos en la banda de 02 §1 y bajo el minimo),
     frecuencia (grupos entrenados en >=2 sesiones), grupo en dias consecutivos, maximo por sesion.
IS : cobertura de slots con dosis de fuerza (reps <= 8 y descanso 120/150), series de fuerza por slot y semana,
     descansos de las colocaciones de fuerza.
LW : minutos aerobicos (frente a 45/150), dias con fuerza, series/grupo, impacto (ImpactoPolicy reproducida),
     proporcion de tiempo aerobico/fuerza.
GF : idem + movilidad.
Salida: csv/5_4_calidad_por_objetivo.csv. Uso: python3 -I s11_calidad_objetivo.py"""
import sys, collections, csv, statistics, re, datetime
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

SLOTS = {'LOWER_KNEE': {'KNEE_DOMINANT'}, 'LOWER_HIP': {'HIP_HINGE', 'HIP_EXTENSION'},
         'UPPER_PUSH': {'HORIZONTAL_PUSH', 'VERTICAL_PUSH'}, 'UPPER_PULL': {'HORIZONTAL_PULL', 'VERTICAL_PULL'}}
BANDA = {'BEGINNER': (6, 10), 'INTERMEDIATE': (10, 16), 'ADVANCED': (12, 20)}
MIN_GM = {'BEGINNER': 4, 'INTERMEDIATE': 6, 'ADVANCED': 8}
MOD = re.compile(r"\b(trote|trotar|trotando|correr|carrera|corriendo|skipping|rodillas altas|jumping jacks?|piernas separadas|comba|cuerda|saltar la cuerda|tijeras|escaladores? r[aá]pidos?)\b")
ALTO = re.compile(r"\b(salto|saltos|saltando|pliom[eé]trico|burpee|burpees|sprint|esprint|caja|rebote|zancada con salto|a una pierna con salto|patinador)\b")
cat = leer_catalogo()


def impacto(c):
    nombre = (c['name_es'] or '').lower()
    m = bool(MOD.search(nombre))
    if c['high_impact'] == 'true':
        return 'MODERADO' if m and not ALTO.search(nombre.replace('saltos con las piernas separadas', '')) else 'ALTO'
    if c['high_impact'] == '':
        return 'DESCONOCIDO'
    return 'MODERADO' if m else 'BAJO'


def slot_disp(e):
    return {s for s, pats in SLOTS.items() if any(c['is_active'] == 'true' and equipable(c, e)
                                                 and (c['movement_pattern'] or '') in pats for c in cat.values())}


SD = {e: slot_disp(e) for e in EQUIPOS}
res = []
for pol in ('REV2_final', 'HIST_final'):
    b = leer_barrido(pol); p = leer_planes(pol)
    for f in b:
        if f['resultado'] != 'VALIDO':
            continue
        k = clave(f); o, n, e = f['objetivo'], f['nivel'], f['equipo']
        ses = collections.OrderedDict()
        for x in p[k]:
            ses.setdefault(x['fecha'], []).append(x)
        sem = collections.Counter(); frec = collections.Counter(); maxses = 0
        fuerza_slot = collections.Counter(); slot_any = collections.Counter(); desc_fuerza = []
        aer = 0; seg_aer = 0; seg_res = 0; mov = 0; imp = collections.Counter(); dias_fuerza = 0
        grupos_por_dia = []
        for fe, ex in ses.items():
            g_dia = collections.Counter(); fuerza_hoy = False
            for x in ex:
                c = cat[int(x['ejercicio'])]; s = num(x['series']) or 0
                imp[impacto(c)] += 1
                dur = s * (num(x['segundos']) or 0) if x['tipo'] == 'DURATION' else s * (num(x['reps']) or 0) * 3
                if c['clase'] == 'CARDIO':
                    aer += dur; seg_aer += dur
                elif c['clase'] == 'MOVILIDAD':
                    mov += 1
                elif c['clase'] in ('ESTRUCTURAL', 'RESISTIDO'):
                    seg_res += dur; fuerza_hoy = True
                    if c['grupo']:
                        sem[c['grupo']] += s; g_dia[c['grupo']] += s
                    for sl, pp in SLOTS.items():
                        if (c['movement_pattern'] or '') in pp:
                            slot_any[sl] += s
                            if num(x['reps']) is not None and num(x['reps']) <= 8 and num(x['descanso']) in (120, 150):
                                fuerza_slot[sl] += s; desc_fuerza.append(num(x['descanso']))
            for g in g_dia:
                frec[g] += 1
            if g_dia:
                maxses = max(maxses, max(g_dia[g] for g in g_dia if g in GRUPOS_GRANDES) if any(g in GRUPOS_GRANDES for g in g_dia) else 0)
            dias_fuerza += fuerza_hoy
            grupos_por_dia.append((datetime.date.fromisoformat(fe), {g for g in g_dia if g in GRUPOS_GRANDES}))
        consec = sum(1 for (d1, g1), (d2, g2) in zip(grupos_por_dia, grupos_por_dia[1:])
                     if (d2 - d1).days == 1 and g1 & g2)
        gm = [g for g in GRUPOS_GRANDES if sem.get(g, 0) > 0 or any(c['grupo'] == g for c in [])]
        trabajados = [g for g in GRUPOS_GRANDES if sem.get(g, 0) > 0]
        r = dict(politica=pol, perfil=k, objetivo=o, nivel=n, equipo=e, dias=int(f['dias']), minutos=int(f['minutos']),
                 sesiones=len(ses), pct_tiempo=float(f['pct_tiempo_medio']),
                 grupos_trabajados=len(trabajados),
                 series_med_grupo_trab=statistics.median([sem[g] for g in trabajados]) if trabajados else 0,
                 grupos_en_banda_gm=sum(BANDA[n][0] <= sem[g] <= BANDA[n][1] for g in trabajados),
                 grupos_bajo_min_gm=sum(sem[g] < MIN_GM[n] for g in trabajados),
                 grupos_sobre_banda_gm=sum(sem[g] > BANDA[n][1] for g in trabajados),
                 grupos_frec2=sum(frec[g] >= 2 for g in trabajados), max_series_sesion_grupo=maxses,
                 sesiones_consecutivas_mismo_grupo=consec,
                 slots_disponibles=len(SD[e]), slots_con_fuerza=sum(fuerza_slot[s] > 0 for s in SD[e]),
                 slots_cubiertos=sum(slot_any[s] > 0 for s in SD[e]),
                 series_fuerza_min_slot=min(fuerza_slot[s] for s in SD[e]),
                 descanso_fuerza_med=statistics.median(desc_fuerza) if desc_fuerza else '',
                 aerobico_min=aer // 60, dias_con_fuerza=dias_fuerza, movilidad=mov,
                 pct_trabajo_aerobico=round(100 * seg_aer / max(1, seg_aer + seg_res), 1),
                 imp_bajo=imp['BAJO'], imp_moderado=imp['MODERADO'], imp_alto=imp['ALTO'], imp_desconocido=imp['DESCONOCIDO'])
        res.append(r)
with open(SCR + '/csv/5_4_calidad_por_objetivo.csv', 'w', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=list(res[0]))
    w.writeheader(); w.writerows(res)

med = lambda v: statistics.median(v) if v else '-'
for o in ('GAIN_MUSCLE', 'INCREASE_STRENGTH', 'LOSE_WEIGHT', 'GENERAL_FITNESS'):
    print('\n######', o)
    for n in ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'):
        for pol in ('HIST_final', 'REV2_final'):
            v = [r for r in res if r['politica'] == pol and r['objetivo'] == o and r['nivel'] == n]
            N = len(v)
            tg = sum(r['grupos_trabajados'] for r in v)
            line = (f"{n[:3]} {pol[:4]} N={N} t%={med([r['pct_tiempo'] for r in v])} serie_med={med([r['series_med_grupo_trab'] for r in v])} "
                    f"grupos_en_banda={100*sum(r['grupos_en_banda_gm'] for r in v)/max(1,tg):.0f}% bajo_min={100*sum(r['grupos_bajo_min_gm'] for r in v)/max(1,tg):.0f}% "
                    f"sobre_banda={100*sum(r['grupos_sobre_banda_gm'] for r in v)/max(1,tg):.0f}% frec2={100*sum(r['grupos_frec2'] for r in v)/max(1,tg):.0f}% "
                    f"(dias>=2: {100*sum(r['grupos_frec2'] for r in v if r['dias']>=2)/max(1,sum(r['grupos_trabajados'] for r in v if r['dias']>=2)):.0f}%) "
                    f"consec>0={sum(r['sesiones_consecutivas_mismo_grupo']>0 for r in v)} max_ses={max([r['max_series_sesion_grupo'] for r in v] or [0])}")
            if o in ('INCREASE_STRENGTH', 'GENERAL_FITNESS'):
                line += (f" slots_fuerza_todos={sum(r['slots_con_fuerza']==r['slots_disponibles'] for r in v)}/{N} "
                         f"fuerza_min_slot_med={med([r['series_fuerza_min_slot'] for r in v])} "
                         f"desc_fuerza_med={med([r['descanso_fuerza_med'] for r in v if r['descanso_fuerza_med']!=''])}")
            if o in ('LOSE_WEIGHT', 'GENERAL_FITNESS'):
                line += (f" aer_med={med([r['aerobico_min'] for r in v])} aer>=150={sum(r['aerobico_min']>=150 for r in v)} "
                         f"dias_fuerza>=2={sum(r['dias_con_fuerza']>=2 for r in v if r['dias']>=2)}/{sum(1 for r in v if r['dias']>=2)} "
                         f"slots_cub_todos={sum(r['slots_cubiertos']==r['slots_disponibles'] for r in v)} "
                         f"pct_aer_med={med([r['pct_trabajo_aerobico'] for r in v])} "
                         f"planes_mod={sum(r['imp_moderado']>0 for r in v)} planes_alto={sum(r['imp_alto']>0 for r in v)} "
                         f"mov_med={med([r['movilidad'] for r in v])}")
            if o == 'GAIN_MUSCLE':
                line += f" planes_alto={sum(r['imp_alto']>0 for r in v)}"
            print(line)
