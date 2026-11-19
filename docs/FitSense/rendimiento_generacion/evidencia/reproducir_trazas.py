"""Reproduce las generaciones REALES medidas con Replicate (GRA y campana de estres) con la configuracion anterior y
con la nueva (sondeo cada 1 s y presupuesto total de 360 s / 40 s minimo por intento). Solo aritmetica sobre las
trazas: no llama a nada. Uso: python3 -I -B reproducir_trazas.py <raiz del repo> <carpeta de salida>."""
import csv, math, sys, os, statistics as st

RAIZ, SALIDA = sys.argv[1], sys.argv[2]
POST_RETENIDO = 20.5          # s: lo que Replicate retuvo el POST en las 17 generaciones de la campana de estres
PRESUPUESTO, MINIMO = 360.0, 40.0


def retraso(predict, intervalo):
    """Cuanto tarda el sondeo en enterarse de que la prediccion termino (sin red)."""
    if predict <= POST_RETENIDO:
        return 0.0
    resto = predict - POST_RETENIDO
    return math.ceil(resto / intervalo) * intervalo - resto


def pct(v, p):
    v = sorted(v)
    k = (len(v) - 1) * p
    f = int(k)
    c = min(f + 1, len(v) - 1)
    return v[f] + (v[c] - v[f]) * (k - f)


def reproducir(generaciones, nombre):
    filas = []
    for g in generaciones:
        antes = sum(a['total'] for a in g['intentos'])
        t = 0.0
        desenlace = g['desenlace']
        usados = 0
        for i, a in enumerate(g['intentos']):
            if PRESUPUESTO - t < MINIMO:
                desenlace = 'RESPALDO (presupuesto: intento no empezado)'
                break
            dur = a['total'] - retraso(a['predict'], 3) + retraso(a['predict'], 1)
            if t + dur > PRESUPUESTO:
                t = PRESUPUESTO
                usados += 1
                desenlace = 'RESPALDO (presupuesto: prediccion cancelada)'
                break
            t += dur
            usados += 1
        filas.append({'conjunto': nombre, 'generacion': g['id'], 'intentos_antes': len(g['intentos']),
                      'intentos_despues': usados, 'segundos_antes': round(antes, 1), 'segundos_despues': round(t, 1),
                      'desenlace_antes': g['desenlace'], 'desenlace_despues': desenlace,
                      'cambia_desenlace': 'SI' if desenlace != g['desenlace'] else 'NO'})
    return filas


# GRA (gpt-5-mini, PR-1.7: la configuracion activada).
G = os.path.join(RAIZ, 'docs/FitSense/evaluacion_integral_O/generacion_ia_real/entregables/')
pred = [r for r in csv.DictReader(open(G + 'PREDICCIONES_Y_COSTOS_REPLICATE.csv', encoding='utf-8'))
        if r['tipo'] == 'GENERACION']
res = {(r['corrida'], r['id']): r for r in csv.DictReader(open(G + 'RESULTADOS_GENERACION_REPLICATE.csv',
                                                               encoding='utf-8'))}
gra = {}
for r in pred:
    clave = (r['corrida'], r['etiqueta'].split('/')[0])
    gra.setdefault(clave, []).append({'predict': float(r['predict_time_s']), 'total': int(r['ms']) / 1000})
generaciones_gra = [{'id': f'{c}/{i}', 'intentos': v, 'desenlace': 'IA' if res[(c, i)]['plan_publicado_desde']
                     .startswith('IA') else 'RESPALDO'} for (c, i), v in gra.items()]

# Campana de estres (gpt-5-mini, prompt anterior a PR-1.7; solo generacion inicial).
E = os.path.join(RAIZ, 'docs/FitSense/campana_estres/GENERACIONES_REPLICATE.csv')
est = {}
for r in csv.DictReader(open(E, encoding='utf-8')):
    if not r['operacion'].startswith('GENERACION'):
        continue
    est.setdefault(r['usuario'], []).append({'predict': float(r['predict_time_s']), 'total': int(r['total_ms']) / 1000})
generaciones_est = [{'id': u, 'intentos': v, 'desenlace': 'IA'} for u, v in est.items()]

filas = reproducir(generaciones_gra, 'GRA (PR-1.7)') + reproducir(generaciones_est, 'ESTRES (prompt anterior)')
os.makedirs(SALIDA, exist_ok=True)
with open(os.path.join(SALIDA, 'REPRODUCCION_TRAZAS.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0].keys()))
    w.writeheader()
    w.writerows(filas)

for conjunto in ('GRA (PR-1.7)', 'ESTRES (prompt anterior)'):
    s = [f for f in filas if f['conjunto'] == conjunto]
    a = [f['segundos_antes'] for f in s]
    d = [f['segundos_despues'] for f in s]
    uno = [f for f in s if f['intentos_antes'] == 1]
    print(f"{conjunto}: generaciones={len(s)} p50 {pct(a, .5):.1f}->{pct(d, .5):.1f} s, p95 {pct(a, .95):.1f}->"
          f"{pct(d, .95):.1f} s, max {max(a):.1f}->{max(d):.1f} s; validas al 1er intento p50 "
          f"{pct([f['segundos_antes'] for f in uno], .5):.1f}->{pct([f['segundos_despues'] for f in uno], .5):.1f} s; "
          f"cambian de desenlace={sum(1 for f in s if f['cambia_desenlace'] == 'SI')}; intentos "
          f"{sum(f['intentos_antes'] for f in s)}->{sum(f['intentos_despues'] for f in s)}")
    for f in s:
        if f['cambia_desenlace'] == 'SI':
            print('   cambia:', f)

# Barrido: cuantos desenlaces cambiarian con otros presupuestos (minimo por intento 40 s).
print('BARRIDO presupuesto_s | GRA cambian | ESTRES cambian | GRA max_s | ESTRES max_s')
barrido = []
for p in (240, 300, 360, 420, 480):
    PRESUPUESTO = p
    fg = reproducir(generaciones_gra, 'GRA')
    fe = reproducir(generaciones_est, 'ESTRES')
    fila = (p, sum(f['cambia_desenlace'] == 'SI' for f in fg), sum(f['cambia_desenlace'] == 'SI' for f in fe),
            max(f['segundos_despues'] for f in fg), max(f['segundos_despues'] for f in fe))
    barrido.append(fila)
    print('BARRIDO', *fila)
with open(os.path.join(SALIDA, 'BARRIDO_PRESUPUESTO.csv'), 'w', newline='', encoding='utf-8') as f:
    w = csv.writer(f)
    w.writerow(['presupuesto_s', 'gra_desenlaces_que_cambian_de_40', 'estres_desenlaces_que_cambian_de_8',
                'gra_espera_maxima_s', 'estres_espera_maxima_s'])
    w.writerows(barrido)
