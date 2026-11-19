"""Paso 0: comprueba que la reconstruccion (catalogo + planes.csv.gz) reproduce las columnas del barrido
(series_grupo_max, aerobico_trabajo_min, grupos_bajo_objetivo, estado_dosis). Uso: python3 -I s0_verificar.py"""
import sys, collections
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

cat = leer_catalogo()
for nombre in ['REV2_final', 'HIST_final']:
    b = leer_barrido(nombre)
    p = leer_planes(nombre)
    ok = collections.Counter()
    malos = []
    for f in b:
        if f['resultado'] != 'VALIDO':
            ok['sin_plan_o_invalido'] += 1
            continue
        sem, aer, _ = dosis_plan(p[clave(f)], cat)
        smax = max(sem.values()) if sem else 0
        disp = {g for g in GRUPOS_GRANDES if any(c['grupo'] == g and c['is_active'] == 'true' and equipable(c, f['equipo'])
                                                for c in cat.values())}
        bajo = [g for g in GRUPOS_GRANDES if g in disp and sem.get(g, 0) < objetivo_series(f['objetivo'], f['nivel'], g)]
        oa = objetivo_aerobico(f['objetivo'], f['nivel'])
        estado = 'DOSIS_LIMITADA' if bajo or (oa > 0 and aer < oa) else 'DOSIS_OBJETIVO'
        ok['smax_ok' if smax == int(f['series_grupo_max']) else 'smax_mal'] += 1
        ok['aer_ok' if aer == int(f['aerobico_trabajo_min']) else 'aer_mal'] += 1
        ok['bajo_ok' if len(bajo) == int(f['grupos_bajo_objetivo']) else 'bajo_mal'] += 1
        ok['estado_ok' if estado == f['estado_dosis'] else 'estado_mal'] += 1
        if len(bajo) != int(f['grupos_bajo_objetivo']) and len(malos) < 8:
            malos.append((clave(f), dict(sem), sorted(disp), f['grupos_bajo_objetivo']))
    print(nombre, len(b), dict(ok))
    for m in malos:
        print('  ', m)
