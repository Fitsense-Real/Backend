"""5.3/5.1 Sensibilidad: estado de dosis de REV2_final si NO cuentan como series directas de su grupo los ejercicios de
clasificacion dudosa: 775 (back, HORIZONTAL_PUSH), 1261 y 1308 (back, TRUNK_FLEXION, objetivo 'spine'), 1259 (back,
TRUNK_ROTATION), 973 (shoulders, gancho de boxeo). Uso: python3 -I s10_dosis_sin_dudosos.py"""
import sys, collections
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *
DUD = {775, 1261, 1308, 1259, 973}
cat = leer_catalogo()
b = leer_barrido('REV2_final'); p = leer_planes('REV2_final')
disp = {e: {g for g in GRUPOS_GRANDES if any(c['grupo'] == g and c['is_active'] == 'true' and equipable(c, e) for c in cat.values())} for e in EQUIPOS}
cambio = collections.Counter(); dep = collections.Counter(); grupos = collections.Counter()
for f in b:
    if f['resultado'] != 'VALIDO':
        continue
    filas = p[clave(f)]
    sem, aer, _ = dosis_plan(filas, cat)
    sem2, _, _ = dosis_plan([e for e in filas if int(e['ejercicio']) not in DUD], cat)
    o, n, e = f['objetivo'], f['nivel'], f['equipo']
    usa = any(int(x['ejercicio']) in DUD for x in filas)
    dep[(e, 'usa_dudoso' if usa else 'no')] += 1
    if f['estado_dosis'] == 'DOSIS_OBJETIVO':
        bajo2 = [g for g in GRUPOS_GRANDES if g in disp[e] and sem2.get(g, 0) < objetivo_series(o, n, g)]
        cambio[(e, 'pasa_a_LIMITADA' if bajo2 else 'sigue_OBJETIVO')] += 1
        for g in bajo2: grupos[(e, g)] += 1
print('planes que usan alguno de', sorted(DUD), dict(dep))
print('DOSIS_OBJETIVO sin contar los dudosos:', dict(cambio))
print('grupos que caen:', dict(grupos))
