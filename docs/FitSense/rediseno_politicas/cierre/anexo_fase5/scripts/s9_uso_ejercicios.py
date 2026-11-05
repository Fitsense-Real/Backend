"""5.3 Uso en los barridos (REV2_final y HIST_final) de los ejercicios revisados: planes en que aparecen, por objetivo,
nivel, equipo y enfoque, y prescripcion. Tambien: todos los ejercicios 'back' con patron no de traccion usados.
Uso: python3 -I s9_uso_ejercicios.py"""
import sys, collections
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *
cat = leer_catalogo()
IDS = [1262, 775, 1320, 1263, 1261, 1308, 973, 853, 1117, 1093, 1115, 1195, 1196, 509, 1073, 253, 1054]
for nombre in ('REV2_final', 'HIST_final'):
    p = leer_planes(nombre)
    print('==', nombre, 'planes', len(p))
    for i in IDS:
        pl = {k for k, v in p.items() if any(int(e['ejercicio']) == i for e in v)}
        if not pl:
            print(f"  {i} {cat[i]['name_en']}: 0 planes"); continue
        obj = collections.Counter(k.split('|')[0][:2] + k.split('|')[1][:1] for k in pl)
        eq = collections.Counter(k.split('|')[2] for k in pl)
        foc = collections.Counter(e['enfoque'] for k in pl for e in p[k] if int(e['ejercicio']) == i)
        pres = collections.Counter(f"{e['series']}x{e['reps'] if e['tipo']=='SETS_REPS' else e['segundos']+'s'}/{e['descanso']}"
                                   for k in pl for e in p[k] if int(e['ejercicio']) == i)
        print(f"  {i} {cat[i]['name_en']}: {len(pl)} planes; {dict(obj)}; {dict(eq)}; enfoques {dict(foc)}; {dict(pres.most_common(4))}")
    back_np = collections.Counter()
    for k, v in p.items():
        for e in v:
            c = cat[int(e['ejercicio'])]
            if c['grupo'] == 'back' and c['clase'] in ('ESTRUCTURAL', 'RESISTIDO') and c['movement_pattern'] not in ('HORIZONTAL_PULL', 'VERTICAL_PULL'):
                back_np[(c['id'], c['name_en'], c['movement_pattern'])] += 1
    print('  back sin patron de traccion (filas plan-ejercicio):', back_np.most_common(15))
