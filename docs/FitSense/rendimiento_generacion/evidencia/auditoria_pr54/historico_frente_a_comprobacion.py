"""Cruza TODAS las generaciones iniciales historicas con IA real con la comprobacion previa de la PR #54.
Uso: python3 -I -B historico_frente_a_comprobacion.py <raiz repo> <conf_gen.csv de experimento/mejora-ia>
     <AUDITORIA_COTA_FRENTE_AL_VALIDADOR.csv> <salida.csv>
Una generacion de 2+ sesiones queda fuera del alcance POR CODIGO (FactibilidadGeneracionIA devuelve vacio si
effectiveDaysPerWeek != 1): siempre se llama a la IA. Las de 1 sesion se cruzan con el resultado del IT."""
import csv, sys, os, collections
raiz, conf, audit, salida = sys.argv[1:5]
dias_o = {r['id']: int(r['dias_por_semana']) for r in csv.DictReader(open(os.path.join(raiz,
          'docs/FitSense/evaluacion_integral_O/cohorte/O_PERFILES.csv'), encoding='utf-8'))}
dias_o['O11F35'] = 1
aud = {r['id']: r for r in csv.DictReader(open(audit, encoding='utf-8')) if r['id'] not in ('R', 'U')}
mapa_aud = {'O10': 'O10', 'O11': 'O11', 'O11F35': 'O11-F35', 'MVP:U04': 'MVP-U04', 'ESTRES:U05': 'ESTRES-U05',
            'ESTRES:U07': 'ESTRES-U07'}
filas = []

def comprobacion(clave, dias):
    if dias != 1:
        return 'FUERA_DE_ALCANCE (varias sesiones)'
    a = aud.get(mapa_aud.get(clave, ''), None)
    if a is None:
        return 'SIN_DATO'
    return {'SE_LLAMA_A_LA_IA': 'SE_LLAMA_A_LA_IA', 'INVIABLE': 'INVIABLE'}.get(a['decision'],
            'SIN_GENERACION_HOY (' + a['nota'][:60] + ')')

def fila(fuente, corrida, pid, clave, dias, llamadas, valida_en, publicado):
    c = comprobacion(clave, dias)
    filas.append({'fuente': fuente, 'corrida': corrida, 'perfil': pid, 'sesiones_semana': dias, 'llamadas_ia': llamadas,
                  'propuesta_valida_en_intento': valida_en, 'publicado_desde': publicado, 'comprobacion_previa_hoy': c,
                  'descartada_por_la_comprobacion': 'SI' if c == 'INVIABLE' else 'NO'})

G = os.path.join(raiz, 'docs/FitSense/evaluacion_integral_O/generacion_ia_real/entregables/RESULTADOS_GENERACION_REPLICATE.csv')
for r in csv.DictReader(open(G, encoding='utf-8')):
    n = int(r['llamadas_ia'] or 0)
    ia = r['plan_publicado_desde'].startswith('IA')
    fila('GRA (PR-1.7)', r['corrida'], r['id'], r['id'], dias_o.get(r['id'], 0), n,
         str(n) if ia else '-', 'IA' if ia else r['plan_publicado_desde'])
for r in csv.DictReader(open(conf, encoding='utf-8')):
    if r['ejecutado'] != 'True':
        continue
    n = int(r['propuestas'] or 0)
    ia = r['fuente_publicado'] == 'AI'
    fila('CONF-IA (' + r['condicion'] + ')', r['ronda'], r['id'], r['id'], dias_o.get(r['id'], 0), n,
         str(n) if ia else '-', 'IA' if ia else r['fuente_publicado'])
# Campana MVP (SIMULACION_MVP_8_USUARIOS_8_SEMANAS.md, tabla 2) y campana de estres (GENERACIONES_REPLICATE.csv).
mvp = {'U01': (2, 3, '-', 'RESPALDO'), 'U02': (2, 3, '-', 'RESPALDO'), 'U03': (4, 3, '3', 'IA'),
       'U04': (1, 3, '-', 'RESPALDO'), 'U05': (6, 1, '1', 'IA'), 'U06': (5, 0, '-', 'SIN_PLAN'),
       'U07': (6, 1, '1', 'IA'), 'U08': (3, 1, '1', 'IA')}
for u, (d, n, v, p) in mvp.items():
    fila('Campana MVP', 'MVP', u, 'MVP:' + u, d, n, v, p)
est_dias = {'U01': 2, 'U02': 5, 'U03': 7, 'U04': 6, 'U05': 1, 'U06': 4, 'U07': 1, 'U08': 3}
est = collections.OrderedDict()
for r in csv.DictReader(open(os.path.join(raiz, 'docs/FitSense/campana_estres/GENERACIONES_REPLICATE.csv'), encoding='utf-8')):
    if not r['operacion'].startswith('GENERACION'):
        continue
    est.setdefault(r['usuario'], []).append(r)
for u, rs in est.items():
    final = rs[-1]['plan_inicial_final']
    ia = final.startswith('AI')
    fila('Campana de estres', 'ESTRES', u, 'ESTRES:' + u, est_dias[u], len(rs), str(len(rs)) if ia else '-',
         'IA' if ia else final)
with open(salida, 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
    w.writeheader()
    w.writerows(filas)
tot = len(filas)
print('generaciones historicas:', tot)
print('perfiles distintos:', len({(f['fuente'].split(' ')[0], f['perfil']) for f in filas}))
print('por comprobacion:', collections.Counter(f['comprobacion_previa_hoy'] for f in filas))
for k in ('1', '2', '3'):
    s = [f for f in filas if f['propuesta_valida_en_intento'] == k]
    print(f'validas en el intento {k}: {len(s)}; descartadas por la comprobacion: {sum(f["descartada_por_la_comprobacion"]=="SI" for f in s)}'
          f'; de una sesion: {sum(f["sesiones_semana"]==1 for f in s)}')
print('respaldo:', sum(f['publicado_desde'] == 'RESPALDO' or f['publicado_desde'].startswith('RULE') for f in filas),
      ' sin plan:', sum('SIN_PLAN' in f['publicado_desde'] for f in filas))
print('descartadas por la comprobacion (total):', sum(f['descartada_por_la_comprobacion'] == 'SI' for f in filas))
print('de una sesion:', [(f['fuente'], f['corrida'], f['perfil'], f['propuesta_valida_en_intento'], f['comprobacion_previa_hoy'][:22]) for f in filas if f['sesiones_semana'] == 1])
