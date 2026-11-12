"""Exploracion de DOSIS_LIMITADA en REV2_final: componentes en deficit por objetivo/nivel/equipo/dias, material por
grupo y equipo, y divisiones semanales. Uso: python3 -I s1_explorar.py"""
import sys, collections
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

cat = leer_catalogo()
b = leer_barrido('REV2_final')
p = leer_planes('REV2_final')
print('estado', collections.Counter(f['estado_dosis'] for f in b))
# material por grupo/equipo (activos, por clase resistida)
for e in EQUIPOS:
    m = collections.Counter(c['grupo'] for c in cat.values() if c['is_active'] == 'true' and equipable(c, e)
                            and c['clase'] in ('ESTRUCTURAL', 'RESISTIDO'))
    print(e, {g: m[g] for g in GRUPOS_GRANDES}, 'cardio', sum(1 for c in cat.values() if c['is_active'] == 'true'
                                                                   and equipable(c, e) and c['clase'] == 'CARDIO'))
comp = collections.Counter()
porobj = collections.defaultdict(collections.Counter)
for f in b:
    if f['estado_dosis'] != 'DOSIS_LIMITADA':
        continue
    sem, aer, ses = dosis_plan(p[clave(f)], cat)
    defs = [g for g in GRUPOS_GRANDES if sem.get(g, 0) < objetivo_series(f['objetivo'], f['nivel'], g)]
    oa = objetivo_aerobico(f['objetivo'], f['nivel'])
    if oa and aer < oa:
        defs.append('aerobico')
    for d in defs:
        comp[(f['objetivo'], d)] += 1
    porobj[(f['objetivo'], f['nivel'])][f['dias']] += 1
for k, v in sorted(comp.items()):
    print(k, v)
for k, v in sorted(porobj.items()):
    print(k, dict(sorted(v.items())))
# divisiones
div = collections.defaultdict(collections.Counter)
for f in b:
    if f['resultado'] != 'VALIDO':
        continue
    filas = p[clave(f)]
    en = []
    for e in filas:
        if not en or en[-1][0] != e['fecha']:
            en.append((e['fecha'], e['enfoque']))
    div[(f['objetivo'], f['dias'])]['/'.join(x[1] for x in en)] += 1
for k in sorted(div):
    print(k, dict(div[k]))
