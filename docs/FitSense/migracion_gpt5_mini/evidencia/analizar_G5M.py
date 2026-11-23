#!/usr/bin/env python3
"""Compara corridas K20 integrales (cohorte O, generacion + adaptacion con IA real).

Uso: python3 -I analizar_G5M.py SERIE=dir1,dir2 [SERIE=dir...]
Cada dir es la salida de EvaluacionK20IT (generacion_ia.csv, predicciones_metricas.csv, intentos_ia.csv,
trazas_ia.jsonl, resultados_20.csv, 99_resumen.txt). Solo lee; no cambia perfiles, reglas ni oraculo.

Coste:
  - usd_medido: el del arnes (GRA: por tokens; G5M con COTA_POR_TOPE: la COTA superior).
  - usd_estimado (G5M): tokens de entrada = caracteres / RATIO (calibrado con GRA: mismos prompts, tokens medidos)
    + tokens de salida VISIBLES = caracteres de salida / RATIO. NO incluye el razonamiento, que gpt-5-mini cobra y
    Replicate no informa: es un MINIMO, no el gasto real.
"""
import csv, json, re, statistics, sys, glob, os
from collections import Counter

TE, TS = 0.25, 2.00


def leer(path):
    if not os.path.exists(path):
        return []
    with open(path, newline='') as f:
        return list(csv.DictReader(f))


def ratio_gra(dirs):
    pares = []
    for d in dirs:
        trazas = {}
        for l in open(os.path.join(d, 'trazas_ia.jsonl')):
            t = json.loads(l)
            trazas[(t.get('etiqueta'), str(t.get('intento')))] = t
        for m in leer(os.path.join(d, 'predicciones_metricas.csv')):
            t = trazas.get((m['etiqueta'], m['intento']))
            if t and t.get('prompt_chars') and int(m['tokens_entrada']) > 0:
                pares.append((t['prompt_chars'], int(m['tokens_entrada'])))
    return sum(c for c, _ in pares) / sum(t for _, t in pares) if pares else None


def serie(nombre, dirs, ratio):
    """dirs: 'dir' o 'dir@O01+O02' (solo esos perfiles de esa corrida; el coste se prorratea por sus llamadas)."""
    filtros = {}
    limpios = []
    for d in dirs:
        d, _, f = d.partition('@')
        limpios.append(d)
        filtros[d] = set(f.split('+')) if f else None
    dirs = limpios

    def ok(d, perfil):
        return filtros[d] is None or perfil in filtros[d]

    gen = [r for d in dirs for r in leer(os.path.join(d, 'generacion_ia.csv')) if ok(d, r['id'])]
    met = [r for d in dirs for r in leer(os.path.join(d, 'predicciones_metricas.csv'))
           if ok(d, r['etiqueta'].split('/')[0])]
    intentos = [r for d in dirs for r in leer(os.path.join(d, 'intentos_ia.csv')) if ok(d, r['etiqueta'].split('/')[0])]
    usuarios = {}
    for d in dirs:
        for r in leer(os.path.join(d, 'usuarios.csv')):
            usuarios[(d, r['user_id'])] = r['id']
    consumo = [l for d in dirs if os.path.exists(os.path.join(d, 'consumo_replicate.log'))
               for l in open(os.path.join(d, 'consumo_replicate.log'))
               if ok(d, usuarios.get((d, re.search(r'user=(\S+)', l).group(1)), '?'))]
    resumen = {}
    for d in dirs:
        t = open(os.path.join(d, '99_resumen.txt')).read()
        resumen[d] = (int(re.search(r'llamadas_proveedor=(\d+)', t).group(1)),
                      float(re.search(r'usd_medido=([\d.]+)', t).group(1)),
                      re.search(r'bloqueo_presupuesto=(.*)', t).group(1),
                      int(re.search(r'incidencias=(\d+)', t).group(1)))

    perfiles = sorted({r['id'] for r in gen})
    # Por perfil: ultima fila (estado final de la generacion)
    final = {}
    for r in gen:
        final[r['id']] = r
    con_llamada = [p for p in perfiles if any(r['id'] == p and r['resultado_proveedor'] not in ('', 'SIN_LLAMADA')
                                              for r in gen)]
    primera = [p for p in perfiles if any(r['id'] == p and r['intento'] == '1' and r['rechazada_por_el_backend'] == 'NO'
                                          and r['resultado_proveedor'] == 'RESPUESTA' for r in gen)]
    rechazadas = [r for r in gen if r['rechazada_por_el_backend'] == 'SI']
    motivos = Counter()
    for r in rechazadas:
        for v in set(re.findall(r'\b(V\d+[A-Z]?)\b', r['motivos'])):
            motivos[v] += 1
        if not re.findall(r'\bV\d+', r['motivos']):
            motivos['contrato/formato'] += 1
    fuentes = Counter(final[p]['generation_source'] for p in perfiles)
    errores_prov = [r for r in gen if r['resultado_proveedor'] not in ('RESPUESTA', '', 'SIN_LLAMADA')]
    intentos_por_perfil = Counter(int(final[p]['generation_attempts'] or 0) for p in perfiles
                                  if final[p]['generation_source'] == 'AI')
    ms_gen = [int(m['ms']) for m in met if m['tipo'] == 'GENERACION']
    ms_ada = [int(m['ms']) for m in met if m['tipo'] == 'ADAPTACION']
    # Tiempo de generacion por perfil (suma de sus intentos)
    por_perfil = Counter()
    for m in met:
        if m['tipo'] == 'GENERACION':
            por_perfil[m['etiqueta'].split('/')[0]] += int(m['ms'])
    ada_ia = [r for r in intentos if r['intento'] not in ('0', '')]
    ada_resultado = Counter((r['motivo'], r['source']) for r in ada_ia)
    llamadas = len(met)
    usd = sum(float(m['usd']) for m in met)
    # Estimacion minima G5M
    est = None
    if consumo and ratio:
        tot = 0.0
        for l in consumo:
            ce = int(re.search(r'caracteres_entrada=(-?\d+)', l).group(1))
            cs = int(re.search(r'caracteres_salida=(-?\d+)', l).group(1))
            if 'origen=reanudada' in l or ce < 0:
                continue
            tot += ce / ratio / 1e6 * TE + max(cs, 0) / ratio / 1e6 * TS
        est = tot
    def med(x):
        return f"{statistics.median(x)/1000:.1f} s (max {max(x)/1000:.1f})" if x else '-'
    out = {
        'serie': nombre,
        'perfiles': len(perfiles),
        'perfiles_con_llamada_generacion': len(con_llamada),
        'llamadas_generacion': len(ms_gen),
        'validas_a_la_primera': f"{len(primera)}/{len(con_llamada)}",
        'intentos_hasta_valida(1/2/3)': '/'.join(str(intentos_por_perfil.get(i, 0)) for i in (1, 2, 3)),
        'propuestas_rechazadas': len(rechazadas),
        'motivos_rechazo': dict(motivos.most_common()),
        'errores_proveedor_generacion': len(errores_prov),
        'planes_por_origen': {k or 'SIN_PLAN': v for k, v in fuentes.items()},
        'latencia_llamada_generacion': med(ms_gen),
        'latencia_generacion_por_perfil': med(list(por_perfil.values())),
        'llamadas_adaptacion': len(ms_ada),
        'latencia_llamada_adaptacion': med(ms_ada),
        'adaptaciones_con_ia(motivo,source)': {f"{k[0]}/{k[1]}": v for k, v in ada_resultado.items()},
        'llamadas_totales': llamadas,
        'usd_arnes': round(usd, 4),
        'usd_estimado_minimo_sin_razonamiento': None if est is None else round(est, 4),
        'bloqueos_presupuesto': [v[2] for v in resumen.values() if v[2] != 'ninguno'],
        'incidencias': sum(v[3] for v in resumen.values()),
        'perfiles_sin_plan': [p for p in perfiles if final[p]['plan_publicado'] in ('', 'null', 'None')],
    }
    return out


def main():
    series = [a.split('=', 1) for a in sys.argv[1:]]
    gra_dirs = [d for n, ds in series if n.startswith('GRA') for d in ds.split(',')]
    ratio = ratio_gra(gra_dirs) if gra_dirs else None
    print(json.dumps({'ratio_caracteres_por_token_entrada (GRA)': None if ratio is None else round(ratio, 3)}))
    for n, ds in series:
        print(json.dumps(serie(n, ds.split(','), ratio), ensure_ascii=False))


if __name__ == '__main__':
    main()
