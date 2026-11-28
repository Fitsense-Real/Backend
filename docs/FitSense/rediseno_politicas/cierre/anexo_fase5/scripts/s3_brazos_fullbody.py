"""5.1 (complemento): efecto de que FULL_BODY no admita 'upper arms' (WorkoutFocus, tabla 20.2) mientras CalidadDosis
lo exige como grupo grande; material de hombro y espalda en CASA_PESO_CORPORAL; aerobico LW/GF por dias/minutos.
Uso: python3 -I s3_brazos_fullbody.py"""
import sys, collections, csv
sys.path.insert(0, '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3/scripts')
from comun import *

cat = leer_catalogo()
b = leer_barrido('REV2_final')
p = leer_planes('REV2_final')
ACEPTA_BRAZOS = {'UPPER_BODY', 'PUSH', 'PULL'}
rows = csv.DictReader(open(SCR + '/csv/5_1_dosis_limitada_planes.csv'))
lim = {r['perfil']: r for r in rows}
c1 = collections.Counter()
c2 = collections.Counter()
for f in b:
    if f['resultado'] != 'VALIDO':
        continue
    k = clave(f)
    filas = p[k]
    enfoques = {e['enfoque'] for e in filas}
    sin_brazos = not (enfoques & ACEPTA_BRAZOS)
    if k not in lim:
        continue
    defs = lim[k]['componentes_deficit'].split(';')
    c1[('dias' + f['dias'], 'sin_enfoque_brazos' if sin_brazos else 'con_enfoque_brazos')] += 1
    if sin_brazos and 'upper arms' in defs:
        resto = [d for d in defs if d != 'upper arms']
        c2['brazos_en_deficit'] += 1
        c2['solo_brazos' if not resto else 'brazos_y_otros'] += 1
        # variante: series indirectas 0,5 de empujes/tracciones multiarticulares (F8)
        ind = 0.0
        for e in filas:
            c = cat.get(int(e['ejercicio']))
            if c and c['movement_pattern'] in ('HORIZONTAL_PUSH', 'VERTICAL_PUSH', 'HORIZONTAL_PULL', 'VERTICAL_PULL') \
                    and c['is_isolation'] != 'true':
                ind += 0.5 * (num(e['series']) or 0)
        obj = objetivo_series(f['objetivo'], f['nivel'], 'upper arms')
        c2['indirectas>=objetivo' if ind >= obj else 'indirectas<objetivo'] += 1
        if not resto:
            c2['solo_brazos_' + f['objetivo']] += 1
print('planes DOSIS_LIMITADA por dias y si algun enfoque admite brazos:', sorted(c1.items()))
print(dict(c2))
for e in EQUIPOS:
    for g in ('shoulders', 'back'):
        l = [(c['id'], c['source_code'], c['name_en'], c['name_es'], c['movement_pattern'], c['difficulty_level'],
              c['target_muscle']) for c in cat.values() if c['is_active'] == 'true' and equipable(c, e)
             and c['grupo'] == g and c['clase'] in ('ESTRUCTURAL', 'RESISTIDO')]
        if len(l) <= 20:
            print(e, g, len(l))
            for x in sorted(l):
                print('   ', x)
# aerobico LW/GF: mediana y maximo por objetivo/nivel/dias
ae = collections.defaultdict(list)
for f in b:
    if f['resultado'] == 'VALIDO' and f['objetivo'] in ('LOSE_WEIGHT', 'GENERAL_FITNESS'):
        ae[(f['objetivo'], f['nivel'], int(f['dias']))].append(int(f['aerobico_trabajo_min']))
import statistics
for k in sorted(ae):
    v = ae[k]
    print(k, 'mediana', statistics.median(v), 'max', max(v), 'min', min(v),
          '>=objetivo', sum(x >= objetivo_aerobico(k[0], k[1]) for x in v), '/', len(v))
