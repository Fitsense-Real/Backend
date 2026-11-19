"""5.1 Resumen por plan: clase (a)/(b) y causa dominante final. Requiere s2 y s4.
Causa dominante del plan = la del componente en deficit mas alejado de su objetivo (valor/objetivo minimo), sin
contar 'upper arms' cuando ninguna sesion lo admite (eso se informa aparte: 'solo_brazos_fullbody').
Mapeo a las categorias pedidas:
  catalogo       : causa mecanica 'catalogo' (<=1 ejercicio del grupo con ese equipamiento)
  tiempo         : contrafactual 'tiempo' (mismos dias, mas minutos lo resuelven)
  dias           : contrafactual 'dias'
  equipamiento   : contrafactual 'equipamiento'
  reglas         : regla_enfoque, techo_V26_sesion, 'reglas_combinacion'/'estructural' sin catalogo
Salida: csv/5_1_resumen_por_dimension.csv. Uso: python3 -I s5_resumen_5_1.py"""
import sys, collections, csv
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

planes = {r['perfil']: r for r in csv.DictReader(open(SCR + '/csv/5_1_dosis_limitada_planes.csv'))}
comps = collections.defaultdict(list)
for r in csv.DictReader(open(SCR + '/csv/5_1_componentes_mecanica.csv')):
    comps[r['perfil']].append(r)
b = leer_barrido('REV2_final')
validos = [f for f in b if f['resultado'] == 'VALIDO']


def final(c):
    if c['causa_mecanica'] == 'catalogo':
        return 'catalogo'
    if c['causa_mecanica'] in ('regla_enfoque', 'techo_V26_sesion'):
        return 'reglas'
    return {'tiempo': 'tiempo', 'dias': 'dias', 'equipamiento': 'equipamiento'}.get(c['causa_dominante'], 'reglas')


filas = []
for k, pl in planes.items():
    cs = comps[k]
    resto = [c for c in cs if c['causa_mecanica'] != 'regla_enfoque']
    if not resto:
        dom, mec, comp = 'solo_brazos_fullbody', 'regla_enfoque', 'upper arms'
    else:
        w = min(resto, key=lambda c: int(c['valor']) / max(1, int(c['objetivo_valor'])))
        dom, mec, comp = final(w), w['causa_mecanica'], w['componente']
    # (a)/(b) sin contar brazos-FULL_BODY (sensibilidad)
    b_sin_brazos = any(int(c['bajo_minimo_estricto']) for c in resto)
    filas.append(dict(pl, causa_final=dom, mecanismo_final=mec, componente_final=comp,
                      clase_estricto_sin_brazos_fb=('sin_deficit' if not resto else
                                                    'b_bajo_minimo' if b_sin_brazos else 'a_sobre_minimo')))
print('N', len(filas))
print('clase estricto', collections.Counter(f['clase_estricto'] for f in filas))
print('clase estricto ignorando brazos en FULL_BODY', collections.Counter(f['clase_estricto_sin_brazos_fb'] for f in filas))
print('causa final', collections.Counter(f['causa_final'] for f in filas).most_common())
print('mecanismo final', collections.Counter(f['mecanismo_final'] for f in filas).most_common())
out = []
for dim in ('objetivo', 'nivel', 'equipo', 'dias'):
    tot = collections.Counter(f[dim] for f in validos)
    t = collections.defaultdict(collections.Counter)
    for f in filas:
        t[f[dim]]['lim'] += 1
        t[f[dim]][f['clase_estricto']] += 1
        t[f[dim]]['sb_' + f['clase_estricto_sin_brazos_fb']] += 1
        t[f[dim]]['c_' + f['causa_final']] += 1
    for v in sorted(t):
        x = t[v]
        row = dict(dimension=dim, valor=v, validos=tot[v], dosis_limitada=x['lim'],
                   pct_limitada=round(100 * x['lim'] / tot[v], 1),
                   a_sobre_minimo=x['a_sobre_minimo'], b_bajo_minimo=x['b_bajo_minimo'],
                   sin_brazos_fb_sin_deficit=x['sb_sin_deficit'], sin_brazos_fb_a=x['sb_a_sobre_minimo'],
                   sin_brazos_fb_b=x['sb_b_bajo_minimo'])
        for c in ('solo_brazos_fullbody', 'dias', 'tiempo', 'equipamiento', 'catalogo', 'reglas'):
            row['causa_' + c] = x['c_' + c]
        out.append(row)
# objetivo x nivel
tot = collections.Counter((f['objetivo'], f['nivel']) for f in validos)
t = collections.defaultdict(collections.Counter)
for f in filas:
    k = (f['objetivo'], f['nivel'])
    t[k]['lim'] += 1; t[k][f['clase_estricto']] += 1; t[k]['sb_' + f['clase_estricto_sin_brazos_fb']] += 1
    t[k]['c_' + f['causa_final']] += 1
for k in sorted(t):
    x = t[k]
    row = dict(dimension='objetivo_nivel', valor='/'.join(k), validos=tot[k], dosis_limitada=x['lim'],
               pct_limitada=round(100 * x['lim'] / tot[k], 1), a_sobre_minimo=x['a_sobre_minimo'],
               b_bajo_minimo=x['b_bajo_minimo'], sin_brazos_fb_sin_deficit=x['sb_sin_deficit'],
               sin_brazos_fb_a=x['sb_a_sobre_minimo'], sin_brazos_fb_b=x['sb_b_bajo_minimo'])
    for c in ('solo_brazos_fullbody', 'dias', 'tiempo', 'equipamiento', 'catalogo', 'reglas'):
        row['causa_' + c] = x['c_' + c]
    out.append(row)
with open(SCR + '/csv/5_1_resumen_por_dimension.csv', 'w', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=list(out[0]))
    w.writeheader(); w.writerows(out)
for r in out:
    print(' | '.join(str(r[c]) for c in r))
with open(SCR + '/csv/5_1_dosis_limitada_planes_final.csv', 'w', newline='') as fh:
    w = csv.DictWriter(fh, fieldnames=list(filas[0]))
    w.writeheader(); w.writerows(filas)
