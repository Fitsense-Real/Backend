"""5.1 Clasificacion de los planes DOSIS_LIMITADA de REV2_final (solo planes VALIDOS).

(a) por debajo del objetivo pero >= minimo razonable  /  (b) por debajo del minimo, y causa dominante por
contrafactual dentro de la propia rejilla del barrido.

Minimos (definidos en el informe, a partir de 02_EVIDENCIA_DOSIS.md):
  ESTRICTO  GM: tabla §1 'minimo eficaz' B4/I6/A8; no-GM: 4 series/grupo grande/semana (F24, Iversen);
            aerobico: LW-B 45 (arranque), LW-I/A 75 (equivalente vigoroso de 150 moderados, F3 [SP]),
            GF-B 30 y GF-I/A 60 (sin minimo inferior publicado: el objetivo [HP] hace de minimo).
  LAXO (sensibilidad) GM igual; IS-B/I 3 (tabla §1, 1x3 dias); LW/GF 2 (F3: >=2 dias x 1 serie); IS-A 4;
            aerobico LW-I/A 45, GF-I/A 30.
Causas (por componente en deficit, en orden de precedencia):
  tiempo        : se cumple con los mismos dias y MAS minutos (algun m' > m del barrido)
  dias          : se cumple con los mismos minutos y MAS dias (algun d' > d)
  equipamiento  : se cumple con el mismo dias/minutos en un equipamiento mas completo
  reglas_combin.: se cumple en alguna otra combinacion (dias, minutos) del mismo objetivo/nivel/equipo
  estructural   : no se cumple en NINGUN perfil del mismo objetivo/nivel/equipo -> catalogo o reglas (se separa
                  con el recuento de ejercicios elegibles del grupo)
Salida: csv/5_1_dosis_limitada_planes.csv, csv/5_1_componentes.csv y resumen por pantalla.
Uso: python3 -I s2_dosis_limitada.py"""
import sys, collections, csv, os
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

OUT = SCR + '/csv'
os.makedirs(OUT, exist_ok=True)
cat = leer_catalogo()
b = leer_barrido('REV2_final')
p = leer_planes('REV2_final')
MINUTOS = [30, 35, 40, 45, 50, 53, 55, 59, 60, 61, 65, 68, 69, 70, 75]
MEJOR_EQ = {'CASA_PESO_CORPORAL': ['CASA_MANCUERNAS_BANDAS', 'GIMNASIO'], 'CASA_MANCUERNAS_BANDAS': ['GIMNASIO'],
            'GIMNASIO': []}


def minimo(modo, obj, nivel, comp):
    if comp == 'aerobico':
        if obj == 'LOSE_WEIGHT':
            return 45 if nivel == 'BEGINNER' else (75 if modo == 'ESTRICTO' else 45)
        if obj == 'GENERAL_FITNESS':
            return 30 if nivel == 'BEGINNER' else (60 if modo == 'ESTRICTO' else 30)
        return 0
    if obj == 'GAIN_MUSCLE':
        return {'BEGINNER': 4, 'INTERMEDIATE': 6, 'ADVANCED': 8}[nivel]
    if modo == 'ESTRICTO':
        return 4
    if obj == 'INCREASE_STRENGTH':
        return 4 if nivel == 'ADVANCED' else 3
    return 2


def objetivo(obj, nivel, comp):
    return objetivo_aerobico(obj, nivel) if comp == 'aerobico' else objetivo_series(obj, nivel, comp)


# valores por perfil
val = {}
filas = {}
for f in b:
    filas[clave(f)] = f
    if f['resultado'] != 'VALIDO':
        continue
    sem, aer, _ = dosis_plan(p[clave(f)], cat)
    v = {g: sem.get(g, 0) for g in GRUPOS_GRANDES}
    v['aerobico'] = aer
    val[clave(f)] = v
disp = {e: {g for g in GRUPOS_GRANDES if any(c['grupo'] == g and c['is_active'] == 'true' and equipable(c, e)
                                              for c in cat.values())} for e in EQUIPOS}
elig = {e: collections.Counter(c['grupo'] for c in cat.values() if c['is_active'] == 'true' and equipable(c, e)
                               and c['clase'] in ('ESTRUCTURAL', 'RESISTIDO')) for e in EQUIPOS}


def cumple(k, comp, obj, nivel):
    v = val.get(k)
    return v is not None and v[comp] >= objetivo(obj, nivel, comp)


def causa(f, comp):
    o, n, e, d, m = f['objetivo'], f['nivel'], f['equipo'], int(f['dias']), int(f['minutos'])
    k = lambda e2, d2, m2: '|'.join([o, n, e2, str(d2), str(m2)])
    c = []
    if any(cumple(k(e, d, m2), comp, o, n) for m2 in MINUTOS if m2 > m):
        c.append('tiempo')
    if any(cumple(k(e, d2, m), comp, o, n) for d2 in range(d + 1, 8)):
        c.append('dias')
    if any(cumple(k(e2, d, m), comp, o, n) for e2 in MEJOR_EQ[e]):
        c.append('equipamiento')
    if not c:
        if any(cumple(k(e, d2, m2), comp, o, n) for d2 in range(1, 8) for m2 in MINUTOS):
            c.append('reglas_combinacion')
        else:
            c.append('estructural')
    return c


comps_rows = []
plan_rows = []
for f in b:
    if f['resultado'] != 'VALIDO' or f['estado_dosis'] != 'DOSIS_LIMITADA':
        continue
    o, n, e = f['objetivo'], f['nivel'], f['equipo']
    v = val[clave(f)]
    defs = [g for g in GRUPOS_GRANDES if g in disp[e] and v[g] < objetivo(o, n, g)]
    if objetivo_aerobico(o, n) and v['aerobico'] < objetivo_aerobico(o, n):
        defs.append('aerobico')
    bajo_min = {m: [c for c in defs if v[c] < minimo(m, o, n, c)] for m in ('ESTRICTO', 'LAXO')}
    causas = {}
    for c in defs:
        cs = causa(f, c)
        causas[c] = cs
        comps_rows.append(dict(perfil=clave(f), objetivo=o, nivel=n, equipo=e, dias=f['dias'], minutos=f['minutos'],
                               componente=c, valor=v[c], objetivo_valor=objetivo(o, n, c),
                               minimo_estricto=minimo('ESTRICTO', o, n, c), minimo_laxo=minimo('LAXO', o, n, c),
                               bajo_minimo_estricto=int(v[c] < minimo('ESTRICTO', o, n, c)),
                               bajo_minimo_laxo=int(v[c] < minimo('LAXO', o, n, c)),
                               causas='+'.join(cs), causa_dominante=cs[0],
                               elegibles_grupo=(elig[e][c] if c != 'aerobico' else '')))
    # causa dominante del plan: la del componente mas lejos de su minimo (relativo)
    peor = min(defs, key=lambda c: v[c] / max(1, objetivo(o, n, c)))
    plan_rows.append(dict(perfil=clave(f), objetivo=o, nivel=n, equipo=e, dias=f['dias'], minutos=f['minutos'],
                          componentes_deficit=';'.join(defs),
                          clase_estricto='b_bajo_minimo' if bajo_min['ESTRICTO'] else 'a_sobre_minimo',
                          clase_laxo='b_bajo_minimo' if bajo_min['LAXO'] else 'a_sobre_minimo',
                          componentes_bajo_minimo_estricto=';'.join(bajo_min['ESTRICTO']),
                          causa_dominante=causas[peor][0], componente_peor=peor,
                          causas_todas=' '.join(sorted({x for cs in causas.values() for x in cs}))))

with open(OUT + '/5_1_componentes.csv', 'w', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=list(comps_rows[0]))
    w.writeheader(); w.writerows(comps_rows)
with open(OUT + '/5_1_dosis_limitada_planes.csv', 'w', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=list(plan_rows[0]))
    w.writeheader(); w.writerows(plan_rows)

N = len(plan_rows)
print('planes DOSIS_LIMITADA validos:', N)
for modo in ('clase_estricto', 'clase_laxo'):
    print(modo, collections.Counter(r[modo] for r in plan_rows))
print('causa dominante (plan):', collections.Counter(r['causa_dominante'] for r in plan_rows))


def tabla(campo, modo='clase_estricto'):
    t = collections.defaultdict(collections.Counter)
    for r in plan_rows:
        t[r[campo]][r[modo]] += 1
        t[r[campo]]['c_' + r['causa_dominante']] += 1
    tot = collections.Counter()
    for f in b:
        if f['resultado'] == 'VALIDO':
            tot[f[campo]] += 1
    print('\n==', campo, modo)
    for k in sorted(t):
        x = t[k]
        n = sum(x[c] for c in ('a_sobre_minimo', 'b_bajo_minimo'))
        print(f"{k:24s} validos={tot[k]:4d} limitada={n:4d} ({100*n/tot[k]:.0f}%) a={x['a_sobre_minimo']} "
              f"b={x['b_bajo_minimo']} | " + ' '.join(f"{c[2:]}={x[c]}" for c in sorted(x) if c.startswith('c_')))


for campo in ('objetivo', 'nivel', 'equipo', 'dias'):
    tabla(campo)
tabla('objetivo', 'clase_laxo')
# objetivo x nivel
t = collections.defaultdict(collections.Counter)
for r in plan_rows:
    t[(r['objetivo'], r['nivel'])][r['clase_estricto']] += 1
    t[(r['objetivo'], r['nivel'])]['L' + r['clase_laxo']] += 1
print('\n== objetivo x nivel (estricto a/b | laxo a/b)')
for k in sorted(t):
    x = t[k]
    print(k, x['a_sobre_minimo'], x['b_bajo_minimo'], '|', x['La_sobre_minimo'], x['Lb_bajo_minimo'])
# componentes
print('\n== componentes en deficit: objetivo x componente -> n, bajo_min_estricto, causas dominantes')
t = collections.defaultdict(collections.Counter)
for r in comps_rows:
    t[(r['objetivo'], r['componente'])]['n'] += 1
    t[(r['objetivo'], r['componente'])]['bajo'] += r['bajo_minimo_estricto']
    t[(r['objetivo'], r['componente'])][r['causa_dominante']] += 1
for k in sorted(t):
    print(k, dict(t[k]))
print('\n== componente x equipo (causa dominante)')
t = collections.defaultdict(collections.Counter)
for r in comps_rows:
    t[(r['componente'], r['equipo'])][r['causa_dominante']] += 1
for k in sorted(t):
    print(k, dict(t[k]), 'elegibles', elig[k[1]][k[0]] if k[0] != 'aerobico' else '-')
print('\nvalores de deficit (componente, valor): ',
      sorted(collections.Counter((r['objetivo'][:2] + r['nivel'][:1], r['componente'], r['valor']) for r in comps_rows
                                 ).items())[:400])
