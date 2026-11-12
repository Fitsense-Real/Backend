#!/usr/bin/env python3
"""Clasificacion de los resultados del oraculo SIN modificar el oraculo ni sus salidas (solo lectura).
Uso: python3 -I clasificar_resultados.py <corrida> <subcarpeta_oraculo> <perfiles.csv> [<salida.csv>]

Cada perfil queda en una sola clase:
  PASS               el oraculo da PASS y, si el perfil tiene `esperado`, las sesiones programadas de S1 coinciden.
  ESPERADO_SIN_PLAN  el oraculo da FAIL SOLO en A_generacion por ausencia de plan, la columna `esperado` (fijada ANTES de
                     ejecutar) prevé «sin plan (CODIGO)» para la politica de la corrida, y incidencias.log registra ese
                     mismo CODIGO para ese perfil. Es un resultado correcto previsto; se informa aparte, no como PASS.
  DEFECTO            cualquier otro FAIL; o un PASS cuyo numero de sesiones no es el esperado; o un plan generado
                     donde se esperaba «sin plan». Un perfil sin columna `esperado` nunca puede ser ESPERADO_SIN_PLAN.
El estado PASS/FAIL del oraculo se copia tal cual en la columna `oraculo`."""
import csv, json, os, re, sys
sys.dont_write_bytecode = True
run, sub, perfiles = sys.argv[1], sys.argv[2], sys.argv[3]
salida = sys.argv[4] if len(sys.argv) > 4 else None
cfg = open(os.path.join(run, '00_configuracion.txt'), encoding='utf-8').read()
pol = re.search(r'politica_por_defecto: (\S+)', cfg).group(1)
clave_pol = 'REV2' if 'REV' in pol else 'HIST'
inc = open(os.path.join(run, 'incidencias.log'), encoding='utf-8').read() if os.path.exists(os.path.join(run, 'incidencias.log')) else ''
matriz = {r['id']: r for r in csv.DictReader(open(os.path.join(run, sub, 'matriz_20.csv')))}
detalle = {d['id']: d for d in json.load(open(os.path.join(run, sub, 'detalle.json')))}
esperado = {r['id']: r.get('esperado', '') for r in csv.DictReader(open(perfiles, encoding='utf-8'))}
t = os.path.join(run, 'tablas')
u_id = {r['user_id']: r['last_name'] for r in csv.DictReader(open(f'{t}/users.csv'))}
s1 = {}
for r in csv.DictReader(open(f'{t}/weekly_training_plans.csv')):
    if r['week_number'] == '1' and r['plan_version'] == '1':
        s1[r['user_id']] = int(r['planned_days_count'])
ses_por_id = {u_id[uid]: n for uid, n in s1.items() if uid in u_id}


def previsto(texto):
    """(sin_plan_codigo | None, sesiones | None) para la politica de la corrida."""
    seg = texto
    partes = dict((p.split(':', 1)[0].strip(), p.split(':', 1)[1]) for p in texto.split(';') if ':' in p and p.split(':', 1)[0].strip() in ('REV2', 'HIST'))
    if partes: seg = partes.get(clave_pol, '')
    sp = re.search(r'sin plan \((\w+)\)', seg)
    ns = re.search(r'(\d+) sesiones programadas', seg)
    return (sp.group(1) if sp else None), (int(ns.group(1)) if ns else None)


filas, cuenta = [], {}
for i, m in matriz.items():
    codigo, ses = previsto(esperado.get(i, ''))
    estado = m['estado']; comp = detalle.get(i, {}).get('comprobaciones', {})
    motivo = ''
    if estado == 'PASS':
        if codigo: clase, motivo = 'DEFECTO', f'se esperaba sin plan ({codigo}) y hay plan'
        elif ses is not None and ses_por_id.get(i) != ses: clase, motivo = 'DEFECTO', f'sesiones S1 {ses_por_id.get(i)} != esperadas {ses}'
        else: clase = 'PASS'
    else:
        solo_gen = set(comp) == {'A_generacion'} and all('0 planes' in x for x in comp['A_generacion'])
        registrado = codigo is not None and re.search(rf'^{i} GENERACION sin plan: .*{codigo}', inc, re.M)
        if codigo and solo_gen and registrado: clase, motivo = 'ESPERADO_SIN_PLAN', codigo
        else: clase, motivo = 'DEFECTO', '; '.join(f'{k}: {v}' for k, v in comp.items())[:300]
    cuenta[clase] = cuenta.get(clase, 0) + 1
    filas.append([i, estado, clase, esperado.get(i, ''), ses_por_id.get(i, ''), motivo])
if salida:
    with open(salida, 'w', newline='') as f:
        w = csv.writer(f); w.writerow(['id', 'oraculo', 'clase', 'esperado', 'sesiones_S1', 'motivo']); w.writerows(filas)
print(json.dumps({'corrida': os.path.basename(run.rstrip('/')), 'politica': pol, 'oraculo': sub, 'clases': cuenta}, ensure_ascii=False))
