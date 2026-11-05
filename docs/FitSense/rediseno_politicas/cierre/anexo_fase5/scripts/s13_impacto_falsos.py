"""5.4 Impacto: ejercicios activos que ImpactoPolicy (reproducida) clasifica MODERADO solo por el nombre (high_impact
=false) y por que palabra; planes REV2 de LW/GF principiante con algun no-BAJO y si se explican solo por esos.
Uso: python3 -I s13_impacto_falsos.py"""
import sys, collections, re
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *
MOD = re.compile(r"\b(trote|trotar|trotando|correr|carrera|corriendo|skipping|rodillas altas|jumping jacks?|piernas separadas|comba|cuerda|saltar la cuerda|tijeras|escaladores? r[aá]pidos?)\b")
cat = leer_catalogo()
pal = collections.Counter(); porNombre = {}
for c in cat.values():
    if c['is_active'] != 'true' or c['high_impact'] != 'false': continue
    m = MOD.search((c['name_es'] or '').lower())
    if m:
        porNombre[c['id']] = m.group(1); pal[(m.group(1), c['equipo'], c['clase'])] += 1
print('activos high_impact=false que pasan a MODERADO por nombre:', len(porNombre))
for k, v in sorted(pal.items(), key=lambda x: -x[1]): print('  ', k, v)
b = leer_barrido('REV2_final'); p = leer_planes('REV2_final')
t = collections.Counter()
for f in b:
    if f['resultado'] != 'VALIDO' or f['nivel'] != 'BEGINNER' or f['objetivo'] not in ('LOSE_WEIGHT', 'GENERAL_FITNESS'): continue
    if int(f['moderado_impacto']) + int(f['alto_impacto']) == 0: continue
    ids = {int(e['ejercicio']) for e in p[clave(f)]}
    solo = int(f['alto_impacto']) == 0 and all(i in porNombre for i in ids if i in porNombre or False) 
    mods = [i for i in ids if i in porNombre]
    t[(f['objetivo'], 'solo_por_nombre' if int(f['alto_impacto']) == 0 and len(mods) > 0 else 'otro')] += 1
print('planes LW/GF principiante con no-BAJO:', dict(t))
for i in sorted(porNombre):
    c = cat[i]
    if c['clase'] == 'CARDIO' or 'cuerda' == porNombre[i]:
        print('   ', i, c['name_en'], '|', c['name_es'], '|', c['equipo'], '|', c['clase'], '| palabra:', porNombre[i])
