"""Compara sin presupuesto / 360 / 420 / 480 s sobre las generaciones REALES (GRA y campana de estres), con el sondeo
de 1 s en todas (para aislar el efecto del presupuesto). Solo aritmetica sobre trazas: no llama a nada.
Uso: python3 -I -B presupuestos_sobre_trazas.py <raiz repo> <salida.csv>"""
import csv, math, os, sys
raiz, salida = sys.argv[1], sys.argv[2]
POST = 20.5
MINIMO = 40.0

def retraso(p, i):
    return 0.0 if p <= POST else math.ceil((p - POST) / i) * i - (p - POST)

def pct(v, q):
    v = sorted(v); k = (len(v) - 1) * q; f = int(k); c = min(f + 1, len(v) - 1)
    return v[f] + (v[c] - v[f]) * (k - f)

G = os.path.join(raiz, 'docs/FitSense/evaluacion_integral_O/generacion_ia_real/entregables/')
res = {(r['corrida'], r['id']): r for r in csv.DictReader(open(G + 'RESULTADOS_GENERACION_REPLICATE.csv', encoding='utf-8'))}
gra = {}
for r in csv.DictReader(open(G + 'PREDICCIONES_Y_COSTOS_REPLICATE.csv', encoding='utf-8')):
    if r['tipo'] == 'GENERACION':
        gra.setdefault((r['corrida'], r['etiqueta'].split('/')[0]), []).append(
            {'predict': float(r['predict_time_s']), 'total': int(r['ms']) / 1000})
datos = {'GRA (PR-1.7, configuracion evaluada)': [
    (f'{c}/{i}', v, res[(c, i)]['plan_publicado_desde'].startswith('IA')) for (c, i), v in gra.items()]}
est = {}
for r in csv.DictReader(open(os.path.join(raiz, 'docs/FitSense/campana_estres/GENERACIONES_REPLICATE.csv'), encoding='utf-8')):
    if r['operacion'].startswith('GENERACION'):
        est.setdefault(r['usuario'], []).append({'predict': float(r['predict_time_s']), 'total': int(r['total_ms']) / 1000,
                                                 'final': r['plan_inicial_final']})
datos['Campana de estres (prompt anterior)'] = [(u, v, v[-1]['final'].startswith('AI')) for u, v in est.items()]

filas = []
for conjunto, gens in datos.items():
    for nombre, presupuesto in (('sin presupuesto', None), ('360 s', 360), ('420 s', 420), ('480 s', 480)):
        tiempos, ia, respaldo, cortadas, no_empezados, intentos, perdidas = [], 0, 0, 0, 0, 0, []
        for gid, ats, ia_hist in gens:
            t, desenlace = 0.0, ('IA' if ia_hist else 'RESPALDO')
            for a in ats:
                if presupuesto is not None and presupuesto - t < MINIMO:
                    desenlace = 'RESPALDO'; no_empezados += 1; break
                d = a['total'] - retraso(a['predict'], 3) + retraso(a['predict'], 1)
                if presupuesto is not None and t + d > presupuesto:
                    t = presupuesto; intentos += 1; cortadas += 1; desenlace = 'RESPALDO'; break
                t += d; intentos += 1
            tiempos.append(t)
            ia += desenlace == 'IA'; respaldo += desenlace != 'IA'
            if ia_hist and desenlace != 'IA': perdidas.append(f'{gid} ({sum(a["total"] for a in ats):.0f} s)')
        filas.append({'conjunto': conjunto, 'configuracion': nombre, 'generaciones': len(gens),
                      'planes_de_ia': ia, 'respaldos': respaldo, 'planes_de_ia_perdidos': len(perdidas),
                      'cuales': '; '.join(perdidas), 'predicciones_cortadas_y_canceladas': cortadas,
                      'intentos_no_empezados': no_empezados, 'intentos_completados_o_cortados': intentos,
                      'p50_s': round(pct(tiempos, .5), 1), 'p95_s': round(pct(tiempos, .95), 1), 'max_s': round(max(tiempos), 1)})
with open(salida, 'w', newline='', encoding='utf-8') as f:
    w = csv.DictWriter(f, fieldnames=list(filas[0].keys())); w.writeheader(); w.writerows(filas)
for f in filas:
    print(f"{f['conjunto'][:28]:28} {f['configuracion']:16} IA={f['planes_de_ia']:2} respaldo={f['respaldos']:2} perdidos={f['planes_de_ia_perdidos']} "
          f"cortadas={f['predicciones_cortadas_y_canceladas']} no_empezados={f['intentos_no_empezados']} intentos={f['intentos_completados_o_cortados']} "
          f"p50={f['p50_s']} p95={f['p95_s']} max={f['max_s']} {f['cuales']}")
