"""Utilidades comunes de la auditoria fase 5 (solo lectura). Uso: importado por los demas scripts.

Reproduce, con el catalogo exportado (REV2_D_J/tablas) y los planes del barrido, el recuento de series directas por
grupo y de minutos de trabajo aerobico de CalidadDosis / BarridoGeneracionInicialIT.
"""
import csv, gzip, os, collections

REPO = '/home/user/wt_aud'
BARR = REPO + '/docs/FitSense/rediseno_politicas/evidencia/barridos'
SCR = '/tmp/claude-0/-home-user/d726e240-bf6c-5000-9fec-bb22a3143d76/scratchpad/aud3'
TAB = SCR + '/ext/REV2_D_J/tablas'

GRUPOS_GRANDES = ["chest", "back", "shoulders", "upper legs", "upper arms"]
ZONAS_PEQUENAS = {"waist", "lower legs"}
EN_REVISION = {59, 93, 298, 876, 902, 903, 1117, 1374, 55, 312, 465, 809, 1192, 1194, 1257, 1310, 54, 853, 1174, 1294, 1295}
REVISADOS_CARDIO = {853: "LOCOMOTION_CARDIO", 1117: "LOCOMOTION_CARDIO"}
SLOT_PATTERNS = None  # se rellena abajo

EQUIPOS = {
    "GIMNASIO": ["body weight", "dumbbell", "barbell", "cable", "leverage machine", "bench", "band", "stationary bike",
                 "treadmill", "kettlebell", "ez barbell", "smith machine"],
    "CASA_MANCUERNAS_BANDAS": ["body weight", "dumbbell", "band"],
    "CASA_PESO_CORPORAL": ["body weight"],
}


def leer_catalogo():
    bp = {r['body_part_id']: r['code'] for r in csv.DictReader(open(TAB + '/body_parts.csv'))}
    eq = {r['equipment_id']: r['code'] for r in csv.DictReader(open(TAB + '/equipment_types.csv'))}
    req = collections.defaultdict(lambda: collections.defaultdict(set))
    for r in csv.DictReader(open(TAB + '/exercise_equipment_requirements.csv')):
        req[int(r['exercise_id'])][r['group_no']].add(eq[r['equipment_id']])
    cat = {}
    for r in csv.DictReader(open(TAB + '/exercises.csv')):
        i = int(r['exercise_id'])
        r = dict(r)
        r['id'] = i
        r['grupo'] = bp.get(r['body_part_id'])
        r['equipo'] = eq.get(r['equipment_id'])
        r['req'] = {k: set(v) for k, v in req.get(i, {}).items()}
        r['clase'] = clase_de(r)
        cat[i] = r
    return cat


def en_revision(r):
    i = r['id']
    if i not in EN_REVISION:
        return False
    rev = REVISADOS_CARDIO.get(i)
    return rev is None or rev != (r['movement_pattern'] or '') or r['default_prescription'] != 'DURATION'


SLOT_PATS = {"HORIZONTAL_PUSH", "VERTICAL_PUSH", "HORIZONTAL_PULL", "VERTICAL_PULL", "KNEE_DOMINANT", "HIP_HINGE", "HIP_EXTENSION"}


def clase_de(r):
    if en_revision(r):
        return 'EN_REVISION'
    p = (r['movement_pattern'] or '').strip().upper()
    if not p or p == 'OTHER':
        return 'SIN_CAPACIDAD'
    if p == 'LOCOMOTION_CARDIO':
        return 'CARDIO'
    if p == 'MOBILITY':
        return 'MOVILIDAD'
    if p == 'BALANCE_CONTROL':
        return 'EQUILIBRIO'
    return 'ESTRUCTURAL' if p in SLOT_PATS else 'RESISTIDO'


def equipable(r, equipo):
    disp = set(EQUIPOS[equipo])
    if not r['req']:
        return r['equipo'] in disp
    return all(opts & disp for opts in r['req'].values())


def objetivo_series(obj, nivel, grupo):
    peq = grupo in ZONAS_PEQUENAS
    if obj == 'GAIN_MUSCLE':
        base = {'BEGINNER': 6, 'ADVANCED': 12}.get(nivel, 10)
        return base // 2 if peq else base
    return 0 if peq else 4


def objetivo_aerobico(obj, nivel):
    if obj == 'LOSE_WEIGHT':
        return 45 if nivel == 'BEGINNER' else 150
    if obj == 'GENERAL_FITNESS':
        return 30 if nivel == 'BEGINNER' else 60
    return 0


def leer_barrido(nombre):
    return list(csv.DictReader(open(f'{BARR}/{nombre}/barrido_generacion.csv')))


def leer_planes(nombre):
    planes = collections.defaultdict(list)
    with gzip.open(f'{BARR}/{nombre}/planes.csv.gz', 'rt') as f:
        for r in csv.DictReader(f):
            planes[r['perfil']].append(r)
    return planes


def clave(f):
    return '|'.join([f['objetivo'], f['nivel'], f['equipo'], f['dias'], f['minutos']])


def num(x):
    return None if x in (None, '', 'null') else int(x)


def dosis_plan(filas, cat):
    """Series directas por grupo (resistido: ESTRUCTURAL/RESISTIDO) y minutos de trabajo aerobico (CARDIO)."""
    semana = collections.Counter()
    aer = 0
    sesiones = collections.OrderedDict()
    for e in filas:
        c = cat.get(int(e['ejercicio']))
        sesiones.setdefault(e['fecha'], []).append(e)
        if c is None:
            continue
        s = num(e['series']) or 0
        if c['clase'] == 'CARDIO':
            if num(e['segundos']) is not None:
                aer += s * num(e['segundos'])
        elif c['clase'] in ('ESTRUCTURAL', 'RESISTIDO') and c['grupo']:
            semana[c['grupo']] += s
    return semana, aer // 60, sesiones
