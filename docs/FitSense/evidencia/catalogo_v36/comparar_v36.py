"""Comparacion antes / despues de V36. Uso, desde la raiz del repo:
   python3 -I docs/FitSense/evidencia/catalogo_v36/comparar_v36.py docs/FitSense/evidencia/catalogo_v36 .
"""
import csv, re, sys, collections, statistics, os

SP, REPO = sys.argv[1], sys.argv[2]
COMPL = {'LOCOMOTION_CARDIO', 'MOBILITY', 'BALANCE_CONTROL'}
NUEVE = {527, 609, 725, 760, 872, 1059, 1267, 1296, 1333}
MAXDIF = {'BEGINNER': 1, 'INTERMEDIATE': 2, 'ADVANCED': 3}


def catalogo(f):
    c = {}
    for r in csv.reader(open(f), delimiter='\t'):
        c[int(r[0])] = dict(bp=r[1], pat=r[2], dif=int(r[3]), hi=r[4] == 't', fl=r[5] == 't', ov=r[6] == 't',
                            inv=r[7] == 't', act=r[8] == 't', eq=r[9], pres=r[10])
    return c


ANTES, V36 = catalogo(f'{SP}/catalogo_antes_v36.tsv'), catalogo(f'{SP}/catalogo_con_v36.tsv')


def refs(path):
    out = {}
    for line in open(path):
        line = line.rstrip('\n')
        if not line: continue
        perfil, resto = line.split(' ', 1)
        cuerpo, valid = resto.rsplit(' || ', 1)
        ses = []
        for s in cuerpo.split('|')[1:]:
            s = s.strip()
            dia, foco, mins, *ej = s.split(' ')
            ses.append((dia, foco, int(mins[:-3]), [(int(e.split(':')[0]), e.split(':')[1]) for e in ej]))
        out[perfil] = (ses, valid)
    return out


def tsv(d, f):
    return list(csv.DictReader(open(f'{d}/{f}.tsv'), delimiter='\t'))


L = []
p = L.append

# ---------------------------------------------------------------- 72 referencias
A = refs(f'{REPO}/docs/FitSense/evidencia/progresion/generacion_referencia.txt')
B = refs(f'{REPO}/docs/FitSense/evidencia/catalogo_v36/generacion_referencia_candidata_v36.txt')
assert A.keys() == B.keys() and len(A) == 72
tot = cambiadas = directas = estructura = 0
minutos_cambiados = []
perfiles_cambiados = set()
dosis_ant, dosis_nue = collections.Counter(), collections.Counter()
pat_ant, pat_nue = collections.Counter(), collections.Counter()
nej_ant = nej_nue = 0
filas = []
for k in A:
    sa, va = A[k]
    sb, vb = B[k]
    nivel = k.split('/')[1]
    for (da, fa, ma, ea), (db, fb, mb, eb) in zip(sa, sb):
        tot += 1
        if (da, fa) != (db, fb): estructura += 1
        if ma != mb: minutos_cambiados.append(mb - ma)
        nej_ant += len(ea); nej_nue += len(eb)
        for i, d in ea: dosis_ant[d] += 1; pat_ant[ANTES[i]['pat']] += 1
        for i, d in eb: dosis_nue[d] += 1; pat_nue[V36[i]['pat']] += 1
        if ea != eb:
            cambiadas += 1
            perfiles_cambiados.add(k)
            dir_ = bool(({i for i, _ in ea} | {i for i, _ in eb}) & NUEVE)
            directas += dir_
            filas.append((k, da, fa, 'DIRECTO' if dir_ else 'INDIRECTO', ' '.join(f'{i}:{d}' for i, d in ea),
                          ' '.join(f'{i}:{d}' for i, d in eb)))
    assert len(sa) == len(sb)
validas_a = sum(v == 'VALIDA' for _, v in A.values())
validas_b = sum(v == 'VALIDA' for _, v in B.values())
# Pool: 1267 era elegible para los 72 (peso corporal, nivel 1, sin restriccion declarada).
with open(f'{REPO}/docs/FitSense/evidencia/catalogo_v36/referencias_sesiones_cambiadas.tsv', 'w') as f:
    f.write('perfil\tdia\tenfoque\ttipo\tantes\tdespues\n')
    for r in filas: f.write('\t'.join(r) + '\n')


def seguridad_real(R, cat, etiqueta):
    """Exposiciones que la clasificacion CORRECTA (V36) no admite, por nivel."""
    c = collections.Counter()
    for k, (ses, _) in R.items():
        nivel = k.split('/')[1]
        for _, foco, _, ej in ses:
            for i, _ in ej:
                e = V36[i]
                if not e['act']: c['inactivo_tras_V36(1267)'] += 1
                if e['dif'] > MAXDIF[nivel]: c[f'dificultad>{nivel}'] += 1
                if e['hi'] and nivel == 'BEGINNER': c['alto_impacto_en_principiante'] += 1
                if foco == 'PULL' and e['bp'] not in ('back', 'upper arms') and e['pat'] not in COMPL: c['trabajo_fuera_de_grupo_en_PULL'] += 1
    return c


p('## R1. 72 referencias: oficial (antes de V36) frente a candidata (V36)\n')
p(f'| Medida | Oficial | Candidata |\n|---|---:|---:|')
p(f'| Perfiles con plan VALIDA | {validas_a}/72 | {validas_b}/72 |')
p(f'| Sesiones | {tot} | {tot} |')
p(f'| Sesiones con otro dia o enfoque | — | {estructura} |')
p(f'| Sesiones con otros minutos estimados | — | {len(minutos_cambiados)} (de {min(minutos_cambiados):+d} a {max(minutos_cambiados):+d} min) |')
p(f'| Colocaciones (ejercicios) | {nej_ant} | {nej_nue} |')
p(f'| Sesiones con algun ejercicio o dosis distinta | — | {cambiadas} ({len(perfiles_cambiados)} perfiles) |')
p(f'| · directas (contienen alguno de los 9, antes o despues) | — | {directas} |')
p(f'| · indirectas (seleccion determinista) | — | {cambiadas - directas} |')
p('')
p('Distribucion de dosis (series x reps o segundos / descanso), las 10 mas frecuentes:\n')
p('| Dosis | Oficial | Candidata |\n|---|---:|---:|')
for d, _ in (dosis_ant + dosis_nue).most_common(10):
    p(f'| {d} | {dosis_ant[d]} | {dosis_nue[d]} |')
p('')
p('Patrones de movimiento de las colocaciones (cada plan con su catalogo):\n')
p('| Patron | Oficial | Candidata |\n|---|---:|---:|')
for d in sorted(set(pat_ant) | set(pat_nue), key=lambda x: (-(pat_ant[x] + pat_nue[x]), x)):
    p(f'| {d} | {pat_ant[d]} | {pat_nue[d]} |')
p('')
sa_, sb_ = seguridad_real(A, ANTES, 'oficial'), seguridad_real(B, V36, 'candidata')
p('Colocaciones que la clasificacion CORRECTA no admite (se juzgan los dos planes con los datos de V36):\n')
p('| Exposicion | Oficial | Candidata |\n|---|---:|---:|')
for d in sorted(set(sa_) | set(sb_)):
    p(f'| {d} | {sa_[d]} | {sb_[d]} |')
if not (sa_ or sb_): p('| (ninguna) | 0 | 0 |')
p('')


# ---------------------------------------------------------------- barridos
def resumen(d):
    pl = tsv(d, 'barrido_4_6_dias_planes')
    se = tsv(d, 'barrido_4_6_dias_sesiones')
    ro = [r for r in tsv(d, 'robustez_semillas_4_6_dias') if r['resultado'] and not r['perfil'].startswith('#')]
    rv = tsv(d, 'impacto_v20_referencias_sesiones')
    occ = collections.Counter()
    malos = collections.Counter()
    for s in se:
        ids = [int(x) for x in re.findall(r'\d+', s['ejercicios'])]
        for i in ids:
            if i in NUEVE: occ[(i, s['enfoque'])] += 1
        nivel = s['perfil'].split('/')[1]
        for i in ids:
            e = V36[i]
            if not e['act']: malos['1267_inactivo'] += 1
            if e['hi'] and nivel == 'BEGINNER': malos['alto_impacto_principiante'] += 1
            if e['dif'] > MAXDIF[nivel]: malos['dificultad_superior_al_nivel'] += 1
            if s['enfoque'] == 'PULL' and e['bp'] not in ('back', 'upper arms') and e['pat'] not in COMPL: malos['trabajo_fuera_de_grupo_en_PULL'] += 1
    banderas = collections.Counter()
    for s in se:
        for b in re.findall(r'[A-Z_0-9]+(?:\[[^\]]*\]|\([^)]*\))?', s['banderas']):
            banderas['EJERCICIO_EN_REVISION' if b.startswith('EJERCICIO_EN_REVISION') else re.sub(r'\(.*', '', b)] += 1
    bajo70 = sum(float(s['pct_configurado']) < 70 for s in se)
    bajo70r = sum(float(s['pct_configurado']) < 70 for s in rv)
    freq = sum(int(r['sesiones']) == int(r['dias_solicitados']) for r in pl if r['validacion'] == 'VALIDA')
    return dict(validos=sum(r['validacion'] == 'VALIDA' for r in pl), sin=sum(r['validacion'].startswith('SIN_PLAN') for r in pl),
                ses=len(se), freq=freq, rob=collections.Counter('VALIDA' if r['resultado'] == 'VALIDA' else r['resultado'].split(' ')[0] for r in ro),
                rob_total=len(ro), occ=occ, malos=malos, banderas=banderas, bajo70=bajo70, bajo70r=bajo70r, se=se, pl=pl)


def cambios(a, b):
    ka = {(s['perfil'], s['fecha']): s for s in a['se']}
    n = estr = 0
    for s in b['se']:
        o = ka.get((s['perfil'], s['fecha']))
        if o is None: continue
        if o['ejercicios'] != s['ejercicios']: n += 1
        if o['enfoque'] != s['enfoque']: estr += 1
    return n, estr


R = {k: resumen(f'{SP}/{k}') for k in ['base_antes', 'base_v36', 'pr3_antes', 'pr3_v36']}
p('## R2. Barrido de 72 perfiles de 4-6 dias y robustez (288 generaciones)\n')
p('| Medida | Base sin V36 | Base + V36 | PR #3 sin V36 | PR #3 + V36 |\n|---|---:|---:|---:|---:|')
row = lambda t, f: p(f'| {t} | ' + ' | '.join(str(f(R[k])) for k in R) + ' |')
row('Perfiles con plan VALIDA', lambda r: f"{r['validos']}/72")
row('Perfiles sin plan', lambda r: r['sin'])
row('Planes con tantas sesiones como dias pedidos', lambda r: f"{r['freq']}/{r['validos']}")
row('Sesiones', lambda r: r['ses'])
row('Sesiones por debajo del 70 % exacto (barrido)', lambda r: r['bajo70'])
row('Sesiones por debajo del 70 % exacto (72 referencias)', lambda r: r['bajo70r'])
row('Robustez: generaciones', lambda r: r['rob_total'])
row('Robustez: VALIDA', lambda r: r['rob']['VALIDA'])
row('Robustez: sin plan', lambda r: r['rob_total'] - r['rob']['VALIDA'])
for b in sorted(set().union(*[r['banderas'] for r in R.values()])):
    row(f'Bandera {b}', lambda r, b=b: r['banderas'][b])
for m in ['1267_inactivo', 'alto_impacto_principiante', 'dificultad_superior_al_nivel', 'trabajo_fuera_de_grupo_en_PULL']:
    row(f'Colocaciones que V36 no admite: {m}', lambda r, m=m: r['malos'][m])
p('')
n1, e1 = cambios(R['base_antes'], R['base_v36'])
n2, e2 = cambios(R['pr3_antes'], R['pr3_v36'])
p(f'Sesiones del barrido con otros ejercicios: base {n1}/{R["base_v36"]["ses"]}, PR #3 {n2}/{R["pr3_v36"]["ses"]}; '
  f'con otro enfoque: base {e1}, PR #3 {e2}.\n')
p('Apariciones de los nueve en el barrido, por enfoque (PR #3 sin V36 -> PR #3 + V36):\n')
p('| Ejercicio | Antes | Despues |\n|---|---|---|')
for i in sorted(NUEVE):
    f = lambda r: ', '.join(f'{fo} {n}' for (j, fo), n in sorted(r['occ'].items()) if j == i) or '0'
    p(f'| {i} | {f(R["pr3_antes"])} | {f(R["pr3_v36"])} |')
p('')
sinplan = [(r['perfil'], r['validacion'][:160]) for r in R['base_v36']['pl'] if not r['validacion'] == 'VALIDA']
sinplan0 = {r['perfil'] for r in R['base_antes']['pl'] if r['validacion'] != 'VALIDA'}
p(f'Base: perfiles sin plan antes {sorted(sinplan0) == sorted(x for x, _ in sinplan) and "= despues" or "!= despues"}: '
  + ', '.join(sorted(x for x, _ in sinplan)) + '\n')
print('\n'.join(L))

p('## R3. Cobertura de capacidades (CapabilityCoveragePolicy)\n')
p('| Conjunto | ' + ' | '.join(['Base sin V36', 'Base + V36', 'PR #3 sin V36', 'PR #3 + V36']) + ' |\n|---|---|---|---|---|')
for f, t in [('impacto_v20_referencias_planes', '72 referencias'), ('barrido_4_6_dias_planes', 'barrido 4-6 dias')]:
    celdas = []
    for k in ['base_antes', 'base_v36', 'pr3_antes', 'pr3_v36']:
        c = collections.Counter(r['cobertura'].split(' ')[0] for r in tsv(f'{SP}/{k}', f))
        celdas.append(', '.join(f'{a} {n}' for a, n in sorted(c.items())))
    p(f'| {t} | ' + ' | '.join(celdas) + ' |')
cambios_cob = 0
for f in ['impacto_v20_referencias_planes', 'barrido_4_6_dias_planes']:
    a = {r['perfil']: r['cobertura'] for r in tsv(f'{SP}/pr3_antes', f)}
    for r in tsv(f'{SP}/pr3_v36', f):
        if a.get(r['perfil']) != r['cobertura']: cambios_cob += 1
p(f'\nPerfiles cuyo detalle de cobertura (colocaciones por capacidad) cambia con V36 (PR #3, referencias + barrido): {cambios_cob} de 144.\n')
