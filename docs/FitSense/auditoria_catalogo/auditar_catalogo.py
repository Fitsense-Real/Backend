"""Auditoria automatica del catalogo de ejercicios (fase 1).

Uso:  python3 -I auditar_catalogo.py catalogo.tsv salida_dir

catalogo.tsv sale de exportar_catalogo.sql. Escribe en salida_dir:
  hallazgos.tsv     una fila por (ejercicio, regla) sospechosa
  por_ejercicio.tsv una fila por ejercicio activo con sus reglas y gravedad maxima
  resumen.md        recuentos por regla, dimension y gravedad

Las reglas detectan SOSPECHAS a partir del nombre de la fuente (name_en), las
instrucciones de la fuente (instructions_en) y la coherencia entre atributos.
No prueban un error: cada sospecha se confirma o descarta contra el GIF (fase 2).

Gravedad:
  ALTA   puede exponer a un participante a algo que su perfil excluye
         (suelo, rodillas, suspension, brazos arriba, inversion, impacto) o
         dar a un principiante una habilidad avanzada.
  MEDIA  clasificacion que cambia lo que el motor y los validadores cuentan
         (grupo muscular, patron, slot 4C, equipamiento exigido).
  BAJA   texto, integridad o restricciones de mas (el ejercicio se excluye
         sin necesidad, lo que no es inseguro).
"""
import collections
import csv
import os
import re
import sys

COLS = ['id', 'source', 'active', 'name_en', 'name_es', 'bp', 'target', 'synergist', 'secondary', 'equip',
        'reqs', 'dif', 'presc', 'pattern', 'iso', 'hi', 'floor', 'axial', 'kneel', 'hang', 'overhead', 'inv',
        'gif', 'instr_en', 'instr_es']


def leer(path):
    out = []
    with open(path, newline='') as f:
        for r in csv.reader(f, delimiter='\t', quoting=csv.QUOTE_NONE):
            d = dict(zip(COLS, r))
            d['id'] = int(d['id'])
            d['dif'] = int(d['dif'])
            for k in ('active', 'hi', 'floor', 'axial', 'kneel', 'hang', 'overhead', 'inv'):
                d[k] = d[k] == 't'
            d['req_set'] = {x.split(':', 1)[1] for x in d['reqs'].split(',') if ':' in x}
            d['n'] = ' ' + re.sub(r'[^a-z0-9]+', ' ', d['name_en'].lower()) + ' '
            d['i'] = ' ' + d['instr_en'].lower() + ' '
            out.append(d)
    return out


def tiene(texto, *patrones):
    return any(re.search(p, texto) for p in patrones)


# ---------------------------------------------------------------- reglas

APOYOS = r' (bench|incline|decline|machine|lever|cable|smith|ball|bosu|wall|chair|box|step|standing|seated|sitting|hanging|captains?) '


def r_seguridad(e):
    n, i = e['n'], e['i']
    # requires_floor (V10): exige bajar al suelo y levantarse. Tumbado en un banco NO es suelo.
    suelo_nombre = tiene(n, r' floor ', r' push ?ups? ', r' pushups? ', r' plank', r' crunch', r' sit ?ups? ',
                         r' situps? ', r' bridge ', r' dead bug ', r' bird dog ', r' mountain climber', r' v ups? ',
                         r' flutter kick', r' scissor', r' supermans? ', r' glute kickback ', r' donkey kick',
                         r' fire hydrant', r' quadruped ', r' lying ', r' supine ', r' prone ', r' on knees ',
                         r' hip raise ', r' leg raise ', r' toe touch', r' bicycle ', r' hollow ', r' l sit ',
                         r' v sit ', r' dip on floor ')
    frases = [f for f in re.split(r'[.;]', i)
              if not re.search(r'\b(bench|ball|machine|chair|pad|box|step|seat|feet flat|feet on|foot on)\b', f)]
    suelo_instr = any(tiene(' ' + f + ' ',
                            r'\b(lie|lying|lay|sit|sitting|kneel|kneeling)\b[^,]{0,40}\b(floor|ground|mat)\b',
                            r'\b(hands|palms|forearms|elbows)\b[^,]{0,25}\bon the (floor|ground|mat)\b',
                            r'\bhands and knees\b', r'\ball fours\b', r'\bplank position\b',
                            r'\bpush-?up position\b', r'\bface (down|up) on (the )?(floor|ground|mat)\b')
                      for f in frases)
    if not e['floor'] and (suelo_instr or tiene(n, r' floor (?!calf)', r' on knees ')
                           or (suelo_nombre and not tiene(n, APOYOS) and not tiene(i, r'\bbench\b'))):
        yield 'S1_SUELO_SIN_BANDERA', 'ALTA', 'nombre/instrucciones indican suelo y requires_floor=false'
    if not e['kneel'] and tiene(n + i, r'\bkneel', r'\bon your knees\b', r'\bhands and knees\b', r'\ball fours\b',
                                r' on knees '):
        yield 'S2_RODILLAS_SIN_BANDERA', 'ALTA', 'indica apoyo de rodillas y requires_kneeling=false'
    # requires_hanging (V25): el cuerpo cuelga del agarre. "let your arms hang" no lo es.
    if not e['hang'] and (tiene(n, r' hanging ', r' pull ?ups? ', r' chin ?ups? ', r' muscle ?up', r' toes to bar')
                          or tiene(i, r'\bhang (from|onto|on) (a|the)\b', r'\bdead hang\b')) \
            and not tiene(n, r' band (assisted )?pull ?ups? ', r' inverted ', r' bench pull', r' high pull '):
        yield 'S3_SUSPENSION_SIN_BANDERA', 'ALTA', 'indica suspension y requires_hanging=false'
    if not e['overhead'] and (e['pattern'] in ('VERTICAL_PUSH', 'VERTICAL_PULL')
                              or tiene(n, r' overhead ', r' shoulder press ', r' military press ', r' push press ',
                                       r' snatch ', r' jerk ')
                              or tiene(i, r'\boverhead\b', r'\babove your head\b')):
        yield 'S4_BRAZOS_ARRIBA_SIN_BANDERA', 'ALTA', 'patron o texto con brazos por encima de la cabeza y requires_overhead=false'
    if not e['inv'] and tiene(n + i, r'handstand', r'headstand', r' inverted (?!row)', r'\bupside down\b',
                              r' pike push', r'\bhead (is )?below (your )?hips\b'):
        yield 'S5_INVERSION_SIN_BANDERA', 'ALTA', 'indica inversion y requires_inversion=false'
    if not e['hi'] and tiene(n + i, r'\bjump', r' hop ', r'\bhops?\b', r' skip ', r'\bskipping\b', r' burpee',
                             r' plyo', r'\bsprint', r' bound', r' jacks? ', r'\bclap', r'\bexplosive(ly)?\b',
                             r'\bleap'):
        yield 'S6_IMPACTO_SIN_BANDERA', 'ALTA', 'indica salto/pliometria/sprint y high_impact=false'
    if not e['axial'] and 'barbell' in e['req_set'] | {e['equip']} and e['pattern'] in (
            'KNEE_DOMINANT', 'HIP_HINGE', 'VERTICAL_PUSH') and not tiene(n, r' bench ', r' lying ', r' seated ',
                                                                          r' hip thrust', r' glute bridge'):
        yield 'S7_CARGA_AXIAL_SIN_BANDERA', 'MEDIA', 'barra en sentadilla/bisagra/press vertical y axial_load=false'
    # Restricciones de mas: excluyen sin necesidad (no es inseguro).
    if e['floor'] and tiene(n, r' standing ', r' seated ') and not (suelo_nombre or suelo_instr):
        yield 'S8_SUELO_DE_MAS', 'BAJA', 'requires_floor=true en un ejercicio de pie o sentado'
    if e['hi'] and not tiene(n + i, r'jump', r'hop', r'skip', r'burpee', r'plyo', r'sprint', r'bound', r'jack',
                             r'clap', r'explosive', r'leap', r' run', r'slam', r'throw', r'kick', r'punch'):
        yield 'S9_IMPACTO_SIN_EVIDENCIA_TEXTUAL', 'BAJA', 'high_impact=true sin indicio en nombre/instrucciones'


HABILIDADES = [r' handstand', r' muscle ?up', r' planche', r' front lever', r' back lever', r' human flag',
               r' pistol', r' one arm (push|pull|chin)', r' single arm (push|pull|chin)', r' dragon flag', r' l ?sit',
               r' v ?sit', r' stalder', r' nordic', r' archer', r' typewriter', r' clap', r' ring dip', r' korean dip',
               r' skin the cat', r' press to handstand', r' tuck planche', r' hollow back', r' iron cross',
               r' 360', r' windshield wiper', r' toes to bar']


def r_dificultad(e):
    if e['dif'] < 3 and tiene(e['n'], *HABILIDADES) and not tiene(e['n'], r' stretch', r' dumbbell ', r' bosu ',
                                                                   r' standing archer', r' swing 360'):
        yield 'D1_HABILIDAD_AVANZADA_NIVEL_BAJO', 'ALTA', 'habilidad avanzada con difficulty_level=%d' % e['dif']
    if e['dif'] == 1 and (e['inv'] or e['hang']):
        yield 'D2_SUSPENSION_O_INVERSION_EN_NIVEL_1', 'MEDIA', 'suspension/inversion con difficulty_level=1'
    if e['dif'] == 1 and e['hi']:
        yield 'D3_IMPACTO_EN_NIVEL_1', 'BAJA', 'alto impacto con nivel 1 (la politica ya lo excluye de principiantes)'


ZONAS_PATRON = {
    'KNEE_DOMINANT': {'upper legs'}, 'HIP_HINGE': {'upper legs'}, 'HIP_EXTENSION': {'upper legs'},
    'HIP_ABDUCTION_ADDUCTION': {'upper legs'}, 'ANKLE_PLANTAR_FLEXION': {'lower legs'},
    'HORIZONTAL_PUSH': {'chest', 'upper arms', 'shoulders'}, 'VERTICAL_PUSH': {'shoulders', 'upper arms', 'chest'},
    'HORIZONTAL_PULL': {'back', 'shoulders'}, 'VERTICAL_PULL': {'back', 'upper arms'},
    'SHOULDER_RAISE': {'shoulders'}, 'HORIZONTAL_ADDUCTION': {'chest', 'shoulders'},
    'SHOULDER_EXTENSION': {'back', 'chest', 'upper arms'}, 'SCAPULAR_ELEVATION': {'back', 'shoulders'},
    'ELBOW_FLEXION': {'upper arms', 'lower arms'}, 'ELBOW_EXTENSION': {'upper arms'},
    'WRIST_FOREARM': {'lower arms'}, 'TRUNK_FLEXION': {'waist'}, 'TRUNK_EXTENSION': {'back', 'waist'},
    'TRUNK_ROTATION_LATERAL_FLEXION': {'waist'}, 'CORE_STABILITY': {'waist'}, 'LOCOMOTION_CARDIO': {'cardio'},
}
MUSCULOS_ZONA = {
    'upper legs': {'quads', 'glutes', 'hamstrings', 'abductors', 'adductors'}, 'lower legs': {'calves'},
    'waist': {'abs', 'obliques'}, 'upper arms': {'biceps', 'triceps'}, 'chest': {'pectorals', 'serratus anterior'},
    'back': {'lats', 'upper back', 'spine', 'traps'}, 'shoulders': {'delts'}, 'lower arms': {'forearms'},
    'cardio': {'cardiovascular system'}, 'neck': {'levator scapulae'},
}
# Palabra del nombre de la fuente -> zona esperada. Ignora las combinaciones (se marcan aparte).
NOMBRE_ZONA = [
    (r' (squat|lunge|leg press|step ?up|split squat|hack squat|leg extension|good morning|romanian|deadlift|'
     r'hip thrust|glute bridge|leg curl|hamstring curl|abduct|adduct|sissy) ', {'upper legs'}),
    (r' (calf|calves|heel raise) ', {'lower legs'}),
    (r' (bicep|biceps|hammer curl|preacher|tricep|triceps|skull ?crusher|kickback|pushdown) ', {'upper arms'}),
    (r' (crunch|sit ?up|situp|oblique|russian twist|plank|leg raise|v up|dead bug) ', {'waist'}),
    (r' (bench press|chest press|push ?up|pushup|chest fly|pec|flye?s?) ', {'chest', 'upper arms', 'shoulders'}),
    (r' (lateral raise|front raise|shoulder press|military press|upright row|rear delt|face pull) ', {'shoulders'}),
    (r' (lat pulldown|pulldown|pull ?up|chin ?up|row) ', {'back', 'shoulders', 'upper arms'}),
    (r' (wrist curl|forearm|finger curl) ', {'lower arms'}),
]
COMBINACIONES = [r' to (press|row|curl|squat|lunge|extension|raise|deadlift|high pull) ',
                 r' (lunge|squat|deadlift|step up) (with|and|to) (curl|press|row|raise|twist|rotation|reach)',
                 r' (curl|press|row|raise) (with|and|to) (lunge|squat|deadlift|step up)',
                 r' squat row ', r' thruster', r' clean and (press|jerk)', r' complex ']


def r_grupo(e):
    if not e['pattern']:
        return
    zonas = ZONAS_PATRON.get(e['pattern'])
    if zonas and e['bp'] not in zonas:
        yield 'G1_PATRON_ZONA_INCOHERENTE', 'MEDIA', '%s con body_part=%s' % (e['pattern'], e['bp'])
    esperados = MUSCULOS_ZONA.get(e['bp'])
    if esperados and e['target'] not in esperados:
        yield 'G2_MUSCULO_ZONA_INCOHERENTE', 'MEDIA', 'target_muscle=%s en body_part=%s' % (e['target'], e['bp'])
    combinado = tiene(e['n'], *COMBINACIONES)
    if combinado:
        yield 'G4_EJERCICIO_COMBINADO', 'MEDIA', 'combina dos movimientos principales: revisar zona y patron'
    elif e['pattern'] not in ('MOBILITY', 'BALANCE_CONTROL', 'LOCOMOTION_CARDIO', 'OTHER'):
        for patron, zonas_n in NOMBRE_ZONA:
            if tiene(e['n'], patron) and e['bp'] not in zonas_n and not tiene(e['n'], r' stretch'):
                yield 'G3_NOMBRE_ZONA_INCOHERENTE', 'MEDIA', 'el nombre sugiere %s y body_part=%s' % (
                    '/'.join(sorted(zonas_n)), e['bp'])
                break
    if e['pattern'] == 'OTHER':
        yield 'G5_PATRON_OTHER', 'BAJA', 'movement_pattern=OTHER: no cuenta para ningun slot ni capacidad'


EQUIPOS = [
    (r' dumbbells? ', 'dumbbell'), (r' (?<!ez )barbell ', 'barbell'), (r' ez (bar|barbell) ', 'ez barbell'),
    (r' cable ', 'cable'), (r' bands? |resistance band', 'band'), (r' kettlebells? ', 'kettlebell'),
    (r' (?<!back )lever (?!bent)| machine ', 'leverage machine'), (r' smith ', 'smith machine'),
    (r' (stability|exercise|swiss) ball ', 'stability ball'), (r' medicine ball ', 'medicine ball'),
    (r' bosu ', 'bosu ball'), (r' (ab )?wheel | roller ', 'ab wheel'), (r' suspension | trx ', 'suspension straps'),
    (r' rings? ', 'gymnastic rings'), (r' (pull ?up|chin ?up) bar ', 'pull-up bar'),
    (r' parallel bars? ', 'parallel bars'), (r' landmine ', 'landmine'), (r' plyo box | box ', 'plyo box'),
    (r' step (?!up|down|to|rear)', 'step platform'), (r' sled ', 'other'), (r' bench ', 'bench'),
    (r' treadmill ', 'treadmill'), (r' stationary bike | (?<!air )bike ', 'stationary bike'),
]
EQUIPOS_INSTR = [(r'\bdumbbells?\b', 'dumbbell'), (r'\bbarbell\b', 'barbell'), (r'\bcable\b', 'cable'),
                 (r'\bresistance band|\bband\b', 'band'), (r'\bkettlebell', 'kettlebell'),
                 (r'\bstability ball|\bexercise ball|\bswiss ball', 'stability ball'), (r'\bmedicine ball', 'medicine ball'),
                 (r'\bab wheel|\bthe wheel\b|\broller\b', 'ab wheel'), (r'\bbench\b', 'bench')]
EQUIVALENTES = {'barbell': {'barbell', 'ez barbell', 'olympic barbell', 'smith machine'},
                'pull-up bar': {'pull-up bar', 'gymnastic rings', 'suspension straps'},
                'bench': {'bench'}, 'leverage machine': {'leverage machine', 'smith machine'},
                'stationary bike': {'stationary bike'}}


def r_equipamiento(e):
    exigidos = e['req_set'] or ({'body weight'} if e['equip'] == 'body weight' else {e['equip']})
    for patron, codigo in EQUIPOS:
        if codigo in ('other',):
            continue
        if tiene(e['n'], patron) and not (EQUIVALENTES.get(codigo, {codigo}) & exigidos):
            if codigo == 'step platform' and tiene(e['n'], r' step ?up'):
                continue
            if codigo == 'ab wheel' and tiene(e['n'], r' foam roller'):
                continue
            yield 'E1_NOMBRE_EQUIPO_NO_EXIGIDO', 'MEDIA', 'el nombre menciona %s y no se exige (%s)' % (
                codigo, e['reqs'] or 'peso corporal')
            break
    for patron, codigo in EQUIPOS_INSTR:
        if tiene(e['i'], patron) and not (EQUIVALENTES.get(codigo, {codigo}) & exigidos) \
                and not tiene(e['n'], patron.replace(r'\b', ' ')):
            if codigo == 'bench' and e['pattern'] in ('MOBILITY',):
                continue
            yield 'E2_INSTRUCCIONES_OTRO_EQUIPO', 'BAJA', 'las instrucciones usan %s y no se exige' % codigo
            break


def r_instrucciones(e):
    if e['active'] and not e['instr_es'].strip():
        yield 'I2_SIN_INSTRUCCIONES_ES', 'BAJA', 'activo sin instructions_es'


def r_integridad(e):
    if e['pattern'] == 'MOBILITY' and e['presc'] != 'DURATION':
        yield 'N3_MOVILIDAD_POR_REPETICIONES', 'BAJA', 'MOBILITY con prescripcion SETS_REPS'
    if e['pattern'] == 'LOCOMOTION_CARDIO' and e['presc'] != 'DURATION':
        yield 'N3_CARDIO_POR_REPETICIONES', 'BAJA', 'LOCOMOTION_CARDIO con prescripcion SETS_REPS'
    if e['hang'] and not ({'pull-up bar', 'gymnastic rings', 'suspension straps', 'parallel bars', 'captains chair'}
                          & (e['req_set'] | {e['equip']})):
        yield 'N4_SUSPENSION_SIN_APARATO', 'MEDIA', 'requires_hanging=true sin barra, anillas ni correas exigidas'
    if e['active'] and not e['gif']:
        yield 'N5_SIN_GIF', 'BAJA', 'activo sin gif_path'


def r_duplicados(activos):
    def clave(e):
        n = re.sub(r' v \d+ | version \d+ ', ' ', e['n'])
        return re.sub(r'\s+', ' ', n).strip()
    grupos = collections.defaultdict(list)
    for e in activos:
        grupos[clave(e)].append(e)
    for k, g in grupos.items():
        if len(g) > 1:
            ids = ','.join(str(x['id']) for x in g)
            for e in g:
                yield e, 'N1_NOMBRE_DUPLICADO', 'BAJA', 'mismo nombre de fuente que %s' % ids
    por_texto = collections.defaultdict(list)
    for e in activos:
        t = re.sub(r'\W+', ' ', e['instr_en'].lower()).strip()
        if len(t) > 80:
            por_texto[t].append(e)
    for t, g in por_texto.items():
        if len(g) > 1:
            ids = ','.join(str(x['id']) for x in g)
            for e in g:
                yield e, 'N2_INSTRUCCIONES_DUPLICADAS', 'BAJA', 'instrucciones identicas a %s' % ids


REGLAS = [r_seguridad, r_dificultad, r_grupo, r_equipamiento, r_instrucciones, r_integridad]
DIMENSION = {'S': 'Seguridad', 'D': 'Dificultad', 'G': 'Grupo y patron', 'E': 'Equipamiento',
             'I': 'Nombre e instrucciones', 'N': 'Integridad'}
ORDEN = {'ALTA': 0, 'MEDIA': 1, 'BAJA': 2}


def main():
    entrada, salida = sys.argv[1], sys.argv[2]
    os.makedirs(salida, exist_ok=True)
    todos = leer(entrada)
    activos = [e for e in todos if e['active']]
    hallazgos = []
    for e in activos:
        for regla in REGLAS:
            for codigo, gravedad, detalle in regla(e):
                hallazgos.append((e, codigo, gravedad, detalle))
    for e, codigo, gravedad, detalle in r_duplicados(activos):
        hallazgos.append((e, codigo, gravedad, detalle))
    hallazgos.sort(key=lambda h: (ORDEN[h[2]], h[1], h[0]['id']))

    with open(os.path.join(salida, 'hallazgos.tsv'), 'w') as f:
        f.write('exercise_id\tregla\tgravedad\tdetalle\tname_en\tname_es\tbody_part\ttarget\tpattern\tdif\tequipo\t'
                'requisitos\tbanderas\tgif\n')
        for e, codigo, gravedad, detalle in hallazgos:
            banderas = ' '.join(k for k in ('hi', 'floor', 'axial', 'kneel', 'hang', 'overhead', 'inv') if e[k])
            f.write('\t'.join(map(str, [e['id'], codigo, gravedad, detalle, e['name_en'], e['name_es'], e['bp'],
                                        e['target'], e['pattern'], e['dif'], e['equip'], e['reqs'], banderas,
                                        e['gif']])) + '\n')

    por = collections.defaultdict(list)
    for e, codigo, gravedad, detalle in hallazgos:
        por[e['id']].append((codigo, gravedad))
    with open(os.path.join(salida, 'por_ejercicio.tsv'), 'w') as f:
        f.write('exercise_id\tgravedad_max\treglas\tname_en\tname_es\tbody_part\tpattern\tdif\tgif\n')
        for e in activos:
            if e['id'] not in por:
                continue
            g = min(por[e['id']], key=lambda x: ORDEN[x[1]])[1]
            f.write('\t'.join(map(str, [e['id'], g, ','.join(sorted({c for c, _ in por[e['id']]})), e['name_en'],
                                        e['name_es'], e['bp'], e['pattern'], e['dif'], e['gif']])) + '\n')

    por_regla = collections.Counter((h[1], h[2]) for h in hallazgos)
    ej_por_grav = collections.Counter(min(v, key=lambda x: ORDEN[x[1]])[1] for v in por.values())
    with open(os.path.join(salida, 'resumen.md'), 'w') as f:
        f.write('| Medida | Valor |\n|---|---:|\n')
        f.write('| Ejercicios en el catalogo | %d |\n| Activos auditados | %d |\n' % (len(todos), len(activos)))
        f.write('| Activos con alguna sospecha | %d |\n' % len(por))
        for g in ('ALTA', 'MEDIA', 'BAJA'):
            f.write('| · con gravedad maxima %s | %d |\n' % (g, ej_por_grav[g]))
        f.write('| Activos sin sospechas | %d |\n\n' % (len(activos) - len(por)))
        f.write('| Regla | Dimension | Gravedad | Ejercicios |\n|---|---|---|---:|\n')
        for (codigo, gravedad), n in sorted(por_regla.items(), key=lambda x: (ORDEN[x[0][1]], x[0][0])):
            f.write('| %s | %s | %s | %d |\n' % (codigo, DIMENSION[codigo[0]], gravedad, n))
    print(open(os.path.join(salida, 'resumen.md')).read())


if __name__ == '__main__':
    main()
