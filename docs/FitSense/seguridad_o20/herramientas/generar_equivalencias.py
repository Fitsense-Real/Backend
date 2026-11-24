"""
O20 · AJUSTE FINAL. Propone la lista EXPLICITA de equivalencias de nivel 2 (mismo gesto con otro nombre, material o
postura) y el inventario de diferencias PENDIENTES, a partir de una exportacion CSV de la tabla `exercises`.

La salida es una PROPUESTA que se revisa y se versiona a mano: el backend no ejecuta este script, lee solo
src/main/resources/catalog/equivalencias_mismo_gesto.csv. Coincidir en patron y musculo NO basta para confirmar.

Criterio:
  1. Candidatos: ejercicios activos con el mismo nucleo de nombre (sin material, lateralidad, postura ni angulo), el
     mismo patron y el mismo musculo objetivo. Solo PROPONE: no confirma nada.
  2. Se CONFIRMA un grupo solo si sus miembros difieren unicamente en material (dumbbell, barbell, cable, band,
     lever, smith, kettlebell...), lateralidad (one arm, alternate...) o apoyo sentado/de pie, y el nucleo describe
     un gesto concreto.
  3. Un cambio de ANGULO o de apoyo del cuerpo (incline, decline, lying, kneeling) no se da por equivalente: el
     grupo se parte por angulo; cada subgrupo con el mismo angulo puede confirmarse, y la relacion entre angulos
     queda PENDIENTE.
  4. Un nucleo GENERICO (curl, row, press, fly, extension...) queda PENDIENTE: agrupa gestos distintos. Los nucleos
     de una palabra revisados uno a uno como gesto concreto estan en REVISADOS_COMO_GESTO.
  5. Revision manual: FUSIONES (mismo gesto registrado con patrones distintos) y PENDIENTES_MANUALES.
Uso: python3 -I generar_equivalencias.py <exercises.csv> <salida_confirmadas.csv> <salida_pendientes.csv>
"""
import csv, re, sys

ent, sal_conf, sal_pend = sys.argv[1:4]
ex = [r for r in csv.DictReader(open(ent)) if r['is_active'] == 'true']

def clave(n):
    n = re.sub(r'\s+', ' ', n.strip().lower())
    return re.sub(r'\s+v\.\s*\d+\s*$', '', n).strip()

MATERIAL = ['ez barbell', 'ez-bar', 'ez bar', 'smith machine', 'leverage machine', 'resistance band', 'body weight',
            'lever', 'barbell', 'dumbbells', 'dumbbell', 'kettlebell', 'cable', 'band', 'weighted', 'bodyweight',
            'machine', 'smith']
LATERAL = ['single arm', 'one arm', 'two arm', 'alternate', 'alternating', 'single', 'one']
APOYO = ['seated', 'standing']
ANGULO = ['incline', 'decline', 'lying', 'kneeling']
TODOS = MATERIAL + LATERAL + APOYO + ANGULO
GENERICOS = {'press', 'extension', 'curl', 'row', 'fly', 'raise', 'twist', 'crunch', 'pull', 'push', 'squat',
             'lunge', 'kickback', 'pullover', 'dip', 'bridge', 'plank', 'stretch', 'rotation', 'hold'}
# Nucleos de una palabra revisados como gesto concreto (con su patron y musculo).
REVISADOS_COMO_GESTO = {
    'shrug': 'encogimiento de hombros', 'deadlift': 'peso muerto', 'lunge': 'zancada', 'step up': 'subida al cajon',
    'thruster': 'sentadilla con empuje por encima de la cabeza', 'clean': 'cargada', 'snatch': 'arrancada',
    'burpee': 'burpee', 'chin up': 'dominada supina', 'kickback': 'patada de triceps', 'pulldown': 'jalon',
    'v up': 'v-up', 'pullover': 'pullover', 'push up': 'flexion de brazos',
}
# Variantes ORTOGRAFICAS del mismo nucleo (no sinonimos): se unifican antes de agrupar.
ALIAS_NUCLEO = {'tricep kickback': 'kickback'}
ORTOGRAFIA = [(r'-', ' '), (r'\bbiceps\b', 'bicep'), (r'\btriceps\b', 'tricep')]
# Posibles SINONIMOS con palabras distintas: no se deciden aqui (no es un criterio clinico); quedan PENDIENTES.
SINONIMOS_POSIBLES = [
    ('bench press ~ chest press', 'HORIZONTAL_PUSH', 'pectorals', 'empuje horizontal tumbado en banco frente a sentado o de pie en maquina'),
    ('shoulder press ~ military press ~ overhead press', 'VERTICAL_PUSH', 'delts', 'posibles nombres del mismo press vertical; revisar por un profesional'),
    ('stiff leg deadlift ~ straight leg deadlift ~ romanian deadlift', 'HIP_HINGE', 'glutes/hamstrings', 'posibles nombres de la misma bisagra con piernas casi rectas; revisar por un profesional'),
]
# El mismo gesto registrado con dos patrones (nivel 1 ya los une por nombre): un solo grupo.
FUSIONES = {'upright row': 'remo al menton (el catalogo lo registra como HORIZONTAL_PULL y como SHOULDER_RAISE)'}
# Revisados y NO confirmados pese a cumplir el criterio automatico.
PENDIENTES_MANUALES = {
    'hack squat': 'barbell hack squat sujeta la barra por detras de las piernas; smith hack squat carga los hombros',
    'dip': 'lever seated dip (maquina sentado) y one arm dip no son el mismo gesto',
    'tricep extension': 'el nombre no dice si el brazo trabaja por encima de la cabeza, a la altura del pecho o junto al cuerpo',
}

def mods(n):
    n = ' ' + clave(n).replace('(', ' ').replace(')', ' ') + ' '
    found = set()
    for m in sorted(TODOS, key=len, reverse=True):
        p = r'(?<=\s)' + re.escape(m) + r'(?=\s)'
        if re.search(p, n):
            found.add(m); n = re.sub(p, ' ', n)
    n = re.sub(r'\s+', ' ', n).strip()
    for a, b in ORTOGRAFIA:
        n = re.sub(a, b, n)
    n = re.sub(r'\s+', ' ', n).strip()
    return ALIAS_NUCLEO.get(n, n), found

grupos = {}
for r in ex:
    core, m = mods(r['name_en'])
    if core:
        grupos.setdefault((core, r['movement_pattern'], r['target_muscle']), []).append((r, m))

def que_cambia(difs):
    return ', '.join(x for x, s in (('material', MATERIAL), ('lateralidad', LATERAL), ('apoyo sentado/de pie', APOYO))
                     if difs & set(s)) or 'solo el nombre'

confirmados, pendientes = [], []
fusion = {}
for (core, pat, mus), v in sorted(grupos.items()):
    if len(v) < 2 or len({clave(r['name_en']) for r, _ in v}) == 1:
        continue  # un solo ejercicio, o solo variantes del mismo nombre (nivel 1)
    lista = lambda xs: ' | '.join('%s %s' % (r['exercise_id'], r['name_en']) for r, _ in xs)
    generico = (core in GENERICOS or len(core.split()) == 1) and core not in REVISADOS_COMO_GESTO
    if core in PENDIENTES_MANUALES:
        pendientes.append((core, pat, mus, 'REVISADO: ' + PENDIENTES_MANUALES[core], lista(v))); continue
    if generico:
        pendientes.append((core, pat, mus, 'nucleo generico: puede agrupar gestos distintos', lista(v))); continue
    por_angulo = {}
    for r, m in v:
        por_angulo.setdefault(tuple(sorted(m & set(ANGULO))), []).append((r, m))
    if len(por_angulo) > 1:
        pendientes.append((core, pat, mus, 'entre angulos o apoyos del cuerpo distintos (%s): no se da por equivalente'
                           % ' / '.join(','.join(a) or 'sin angulo' for a in sorted(por_angulo)), lista(v)))
    for ang, sub in sorted(por_angulo.items()):
        claves = sorted({clave(r['name_en']) for r, _ in sub})
        if len(claves) < 2:
            continue
        difs = set().union(*[m for _, m in sub]) - set(ANGULO)
        nombre = (' '.join(ang) + ' ' + core).strip()
        just = 'mismo gesto (%s)%s, cambia %s' % (REVISADOS_COMO_GESTO.get(core, core),
                                                  ' con el mismo angulo: ' + ','.join(ang) if ang else '', que_cambia(difs))
        if core in FUSIONES:
            g = fusion.setdefault(core, [core, set(), set(), 'mismo gesto (%s), cambia %s' % (FUSIONES[core], 'material, lateralidad'), set()])
            g[1].add(pat); g[2].add(mus); g[4].update(claves)
            continue
        confirmados.append([nombre, pat, mus, claves, just])
for core, (nombre, pats, muss, just, claves) in fusion.items():
    confirmados.append([nombre, '+'.join(sorted(pats)), '+'.join(sorted(muss)), sorted(claves), just])
confirmados.sort()

with open(sal_conf, 'w') as f:
    f.write('# O20 · equivalencias CONFIRMADAS de nivel 2 (mismo gesto con otro nombre, material o postura).\n')
    f.write('# Fuente unica del backend para el nivel 2. Revisada y versionada a mano; propuesta inicial generada con\n')
    f.write('# docs/FitSense/seguridad_o20/herramientas/generar_equivalencias.py. Miembros: nombre en ingles sin\n')
    f.write('# mayusculas ni sufijo v. N (clave de nivel 1, que arrastra sus variantes). Lo que no esta aqui NO es\n')
    f.write('# equivalente de nivel 2; lo dudoso esta en docs/FitSense/seguridad_o20/evidencia/equivalencias_pendientes.csv.\n')
    f.write('grupo;patron;musculo;miembros;justificacion\n')
    for i, (nombre, pat, mus, claves, just) in enumerate(confirmados, 1):
        assert ';' not in just and all(';' not in c and '|' not in c for c in claves), nombre
        f.write('G2-%03d %s;%s;%s;%s;%s\n' % (i, nombre, pat, mus, '|'.join(claves), just))
with open(sal_pend, 'w') as f:
    w = csv.writer(f)
    w.writerow(['nucleo', 'patron', 'musculo', 'motivo_pendiente', 'ejercicios_activos'])
    for p in pendientes:
        w.writerow(p)
    for nombre, pat, mus, motivo in SINONIMOS_POSIBLES:
        w.writerow([nombre, pat, mus, 'posible sinonimo con otro nombre: ' + motivo, 'ver los grupos confirmados de cada nombre'])
print('confirmados', len(confirmados), 'claves', sum(len(c[3]) for c in confirmados), 'pendientes', len(pendientes))
