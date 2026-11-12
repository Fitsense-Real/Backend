"""Genera la PROPUESTA V37 a partir de la matriz de revision visual. NO es una migracion aplicada.

Uso, desde la raiz del repo:
  python3 -I docs/FitSense/auditoria_catalogo/v37/generar_v37.py <catalogo.tsv>

catalogo.tsv sale de exportar_catalogo.sql sobre una base V1-V36 (estado de partida).
Escribe en docs/FitSense/auditoria_catalogo/v37/:
  correcciones_v37.tsv            una fila por (ejercicio, columna): antes, despues, bloque, evidencia
  migracion/V37__catalog_safety_corrections.sql      bloque A (recomendado)
  separables/V37_1__catalog_classification_equipment.sql  bloque B (separable)
  no_incluidas_v37.tsv            confirmados que no entran (texto, implemento no modelado)

Solo entran veredictos CONFIRMADO. PROBABLE y PENDIENTE_CRITERIO no se traducen nunca.
Los dos SQL comprueban el estado anterior de cada fila antes de cambiar nada y el
estado final al terminar; si algo no coincide, abortan sin cambios (RAISE EXCEPTION).
"""
import csv
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
MATRIZ = os.path.join(AQUI, '..', 'revision_visual', 'matriz_revision_visual_completa.tsv')
BANDERAS = ['requires_floor', 'requires_kneeling', 'requires_hanging', 'requires_overhead', 'requires_inversion',
            'high_impact', 'axial_load']
COL_TSV = {'high_impact': 15, 'requires_floor': 16, 'axial_load': 17, 'requires_kneeling': 18,
           'requires_hanging': 19, 'requires_overhead': 20, 'requires_inversion': 21}

# A3: aparato de los ejercicios de suspension/gimnasia, coherente con las banderas de A1.
APARATO_A = {776: 'gymnastic rings', 937: 'gymnastic rings', 1068: 'gymnastic rings', 1192: 'gymnastic rings',
             1374: 'pull-up bar', 1294: 'pull-up bar', 913: 'pull-up bar', 1301: 'suspension straps'}
# A4: el resto de la correccion de 1374 (no es cardio ni tiene fase de vuelo).
EXTRA_A = {(1374, 'movement_pattern'): 'TRUNK_FLEXION', (1374, 'high_impact'): 'false'}
# A5: 776 se desactiva (decision del responsable, 29/10/2026). Conserva sus banderas,
# nivel y aparato reales (A1-A3) para que el registro describa el movimiento del GIF.
DESACTIVAR_A = {776: 'Suspension invertida en anillas: habilidad gimnastica, no la bisagra de pie evaluada en V28.'
                     ' Desactivado por decision del responsable (29/10/2026). Ver ANEXO_776_V28.md'}
REPO = os.path.abspath(os.path.join(AQUI, '..', '..', '..', '..'))
OFICIAL = os.path.join(REPO, 'src', 'main', 'resources', 'db', 'migration')
# B2: implemento segun el GIF, solo con codigos que existen en equipment_types.
# 'add': grupo nuevo (AND); los codigos de un grupo son alternativas (OR).
# 'set': requisitos completos nuevos (sustituyen a los actuales).
EQUIPO_B = {
    'bench': ('add', ['bench']),
    'step': ('add', ['step platform']),
    'bench_o_cajon': ('add', ['bench', 'plyo box']),
    'step_o_cajon': ('add', ['step platform', 'plyo box']),
    'bosu': ('add', ['bosu ball']),
    'dumbbell': ('add', ['dumbbell']),
    'smith': ('add', ['smith machine']),
    'treadmill': ('add', ['treadmill']),
    'paralelas': ('add', ['parallel bars']),
    'paralelas_o_silla': ('add', ['parallel bars', 'captains chair']),
    'landmine': ('add', ['landmine']),
}
EQUIPO_B_IDS = {}
for i in (66, 153, 154, 155, 156, 158, 159, 160, 161, 171, 178, 179, 196, 199, 202, 204, 205, 206, 207, 208, 209, 210,
          212, 214, 224, 230, 231, 243, 245, 248, 257, 296, 314, 323, 338, 339, 340, 342, 345, 353, 358, 366, 369, 370,
          377, 380, 443, 468, 471, 495, 560, 561, 606, 636, 637, 667, 677, 690, 696, 699, 700, 701, 718, 741, 753, 797,
          830, 831, 834, 840, 841, 842, 848, 875, 1056, 1062, 1063, 1064, 1078, 1142, 1146, 1220, 1222, 1223, 1242, 1255,
          1284, 1288):
    EQUIPO_B_IDS[i] = ['bench']
for i in (96, 97, 237, 260, 280, 738, 896, 1197, 1214):
    EQUIPO_B_IDS[i] = ['step']
for i in (124, 127, 791, 1110):
    EQUIPO_B_IDS[i] = ['bench_o_cajon']
EQUIPO_B_IDS.update({1182: ['step_o_cajon'], 427: ['bosu'], 816: ['dumbbell'], 817: ['dumbbell'], 818: ['dumbbell'],
                     898: ['smith'], 921: ['smith'], 1321: ['smith'], 1134: ['treadmill'], 777: ['paralelas'],
                     1317: ['paralelas'], 1303: ['paralelas_o_silla'], 1304: ['paralelas_o_silla'],
                     981: ['landmine'], 493: ['bench', 'step'], 895: ['bench', 'step']})
# Sustituciones completas (B2 y B3).
REQ_SET_B = {1019: '1:barbell,2:landmine', 1022: '1:barbell,2:landmine', 295: '1:bench',
             977: '1:captains chair,1:pull-up bar', 786: '1:dumbbell,2:bench,2:plyo box,2:step platform',
             1152: '', 617: '1:dumbbell'}
PRINCIPAL_B = {1019: 'barbell', 1022: 'barbell', 295: 'bench', 898: 'smith machine', 921: 'smith machine',
               1321: 'smith machine', 1134: 'treadmill', 777: 'parallel bars', 1317: 'parallel bars',
               1303: 'parallel bars', 1304: 'parallel bars', 1152: 'body weight', 66: 'bench', 314: 'bench',
               296: 'bench', 1056: 'bench', 1062: 'bench', 1063: 'bench', 1064: 'bench', 1078: 'bench',
               1142: 'bench', 1146: 'bench', 1255: 'bench', 896: 'step platform', 1197: 'step platform'}
TARGET = {'upper legs': 'quads', 'shoulders': 'delts', 'lower arms': 'forearms'}


def leer_catalogo(path):
    cat = {}
    for r in csv.reader(open(path, newline=''), delimiter='\t', quoting=csv.QUOTE_NONE):
        cat[int(r[0])] = r
    return cat


def reqs_de(r):
    return ','.join(sorted(x for x in r[10].split(',') if x))


def anade(reqs, codigos):
    grupos = {}
    for x in [x for x in reqs.split(',') if x]:
        g, c = x.split(':', 1)
        grupos.setdefault(int(g), set()).add(c)
    if any(set(codigos) <= cs for cs in grupos.values()):
        return reqs
    if not grupos and codigos != ['bench'] and False:
        pass
    n = max(grupos) + 1 if grupos else 1
    grupos[n] = set(codigos)
    return ','.join(sorted(f'{g}:{c}' for g, cs in grupos.items() for c in cs))


def main():
    cat = leer_catalogo(sys.argv[1])
    filas = list(csv.DictReader(open(MATRIZ), delimiter='\t'))
    cambios = {}  # (id, columna) -> (antes, despues, bloque, evidencia)
    no_incl = []

    def pon(i, col, despues, bloque, ev):
        r = cat[i]
        if col in COL_TSV:
            antes = 'true' if r[COL_TSV[col]] == 't' else 'false'
        elif col == 'difficulty_level':
            antes = r[11]
        elif col == 'movement_pattern':
            antes = r[13]
        elif col == 'body_part':
            antes = r[5]
        elif col == 'target_muscle':
            antes = r[6]
        elif col == 'equipment':
            antes = r[9]
        elif col == 'requisitos':
            antes = reqs_de(r)
        elif col == 'is_active':
            antes = 'true' if r[2] == 't' else 'false'
        else:
            raise ValueError(col)
        if antes == despues:
            return
        prev = cambios.get((i, col))
        if prev and prev[1] != despues:
            raise ValueError(f'conflicto en {i} {col}: {prev[1]} / {despues}')
        if not prev or prev[2] > bloque:
            cambios[(i, col)] = (antes, despues, bloque, ev)

    for f in filas:
        if f['veredicto'] != 'CONFIRMADO':
            continue
        i, attr, ev, grav = int(f['exercise_id']), f['atributo'], f['evidencia'], f['gravedad']
        attrs = [a.strip() for a in attr.split('/')]
        if all(a in BANDERAS for a in attrs):
            vals = [v.strip() for v in f['segun_gif'].split('/')]
            assert len(vals) == len(attrs), f
            for a, v in zip(attrs, vals):
                bloque = 'A1' if v == 'true' and grav == 'ALTA' else 'B3'
                pon(i, a, v, bloque, ev)
                if a == 'requires_hanging' and v == 'true':
                    pon(i, 'requires_overhead', 'true', 'A1', 'Suspension implica brazos arriba (V25)')
        elif attr == 'difficulty_level':
            pon(i, 'difficulty_level', f['segun_gif'].strip(), 'A2', ev)
        elif attr == 'movement_pattern' and i != 1374:
            pon(i, 'movement_pattern', f['segun_gif'].strip(), 'B1', ev)
        elif attr in ('body_part / target', 'body_part / movement_pattern'):
            bp, otro = [x.strip() for x in f['segun_gif'].split('/')]
            pon(i, 'body_part', bp, 'B1', ev)
            if attr == 'body_part / target':
                pon(i, 'target_muscle', otro, 'B1', ev)
            else:
                pon(i, 'target_muscle', TARGET[bp], 'B1', ev)
                pon(i, 'movement_pattern', otro, 'B1', ev)
        elif attr == 'equipment':
            if i in APARATO_A:
                pon(i, 'equipment', APARATO_A[i], 'A3', ev)
                pon(i, 'requisitos', f'1:{APARATO_A[i]}', 'A3', ev)
            elif i in REQ_SET_B:
                pon(i, 'requisitos', ','.join(sorted(x for x in REQ_SET_B[i].split(',') if x)),
                    'B3' if i in (1152, 617, 977) else 'B2', ev)
                if i in PRINCIPAL_B:
                    pon(i, 'equipment', PRINCIPAL_B[i], 'B3' if i == 1152 else 'B2', ev)
            elif i in EQUIPO_B_IDS:
                reqs = reqs_de(cat[i])
                for k in EQUIPO_B_IDS[i]:
                    reqs = anade(reqs, EQUIPO_B[k][1])
                pon(i, 'requisitos', reqs, 'B2', ev)
                if i in PRINCIPAL_B and cat[i][9] == 'body weight':
                    pon(i, 'equipment', PRINCIPAL_B[i], 'B2', ev)
            else:
                no_incl.append((i, attr, f['actual'], f['segun_gif'], 'implemento no modelado en equipment_types', ev))
        else:
            no_incl.append((i, attr, f['actual'], f['segun_gif'],
                            'texto: requiere redactar instrucciones o nombre' if attr in ('instructions', 'name_en')
                            else 'sin traduccion directa', ev))
    for (i, col), (a, d, b, e) in list(cambios.items()):
        if col != 'requisitos':
            continue
        codigos = [x.split(':', 1)[1] for x in d.split(',') if x]
        principal = cambios.get((i, 'equipment'), (None, cat[i][9]))[1]
        if codigos and principal not in codigos:
            nuevos = sorted(set(codigos) - set(x.split(':', 1)[1] for x in a.split(',') if x)) or sorted(codigos)
            pon(i, 'equipment', nuevos[0], b, 'El implemento principal debe estar entre los requisitos (V23/V29)')
        if not codigos and principal != 'body weight':
            pon(i, 'equipment', 'body weight', b, 'Sin requisitos = peso corporal (V23)')
    for (i, col), v in EXTRA_A.items():
        pon(i, col, v, 'A4', 'Correccion completa de 1374 (colgado de la barra, no cardio)')
    for i, ev in DESACTIVAR_A.items():
        pon(i, 'is_active', 'false', 'A5', ev)

    # hanging => overhead tras los cambios
    for (i, col), (a, d, b, e) in list(cambios.items()):
        if col == 'requires_overhead' and d == 'false':
            hang = cambios.get((i, 'requires_hanging'), (None, 't' if cat[i][19] == 't' else 'f'))[1]
            assert hang in ('false', 'f'), f'{i}: brazos arriba false con suspension'

    out = os.path.join(AQUI, 'correcciones_v37.tsv')
    with open(out, 'w') as fh:
        fh.write('exercise_id\tsource_code\tname_en\tbloque\tcolumna\tantes\tdespues\tevidencia\n')
        for (i, col), (a, d, b, e) in sorted(cambios.items(), key=lambda x: (x[1][2], x[0][0], x[0][1])):
            fh.write('\t'.join([str(i), cat[i][1], cat[i][3], b, col, a, d, e]) + '\n')
    with open(os.path.join(AQUI, 'no_incluidas_v37.tsv'), 'w') as fh:
        fh.write('exercise_id\tname_en\tatributo\tactual\tsegun_gif\tmotivo\tevidencia\n')
        for i, a, ac, sg, m, e in sorted(no_incl):
            fh.write('\t'.join([str(i), cat[i][3], a, ac, sg, m, e]) + '\n')
    escribir_sql(cat, cambios, ('A1', 'A2', 'A3', 'A4', 'A5'), OFICIAL,
                 'V37__catalog_safety_corrections.sql', 'V37', 'correcciones de seguridad y dificultad', ESTADO_V37)
    escribir_sql(cat, cambios, ('B1', 'B2', 'B3'), os.path.join(AQUI, 'separables'),
                 'V37_1__catalog_classification_equipment.sql', 'V37.1',
                 'grupo, patron, implemento y restricciones de mas', ESTADO_V37_1)
    from collections import Counter
    print(Counter(v[2] for v in cambios.values()), 'ejercicios:',
          {b: len({k[0] for k, v in cambios.items() if v[2].startswith(b)}) for b in 'AB'}, 'no incluidas:', len(no_incl))


def q(s):
    return "'" + s.replace("'", "''") + "'"


ESTADO_V37 = [
    'ESTADO: CANDIDATA A DESPLIEGUE; NO APLICADA EN NEON. Aprobada por el responsable',
    'del proyecto el 29/10/2026 (bloque A: seguridad, dificultad, aparato de suspension,',
    '1374 y desactivacion de 776) para implementarla y probarla en entornos aislados.',
    'Aplicarla en Neon y desplegar requiere una autorizacion aparte. Cuidado: cualquier',
    'arranque del backend contra una base con Flyway activo la aplicaria.',
    'Generada por docs/FitSense/auditoria_catalogo/v37/generar_v37.py desde',
    'revision_visual/matriz_revision_visual_completa.tsv (solo CONFIRMADO).',
    'Revision visual asistida por IA; la doble revision humana sigue PENDIENTE.']
ESTADO_V37_1 = [
    'ESTADO: PROPUESTA PENDIENTE (no aprobada). NO esta en src/main/resources/db/migration',
    'y NO se ha aplicado en ninguna base real. Depende de la correccion de V20 y de los',
    'criterios pendientes de grupo y patron. Solo se ejecuta en bases aisladas para simular.',
    'Generada por docs/FitSense/auditoria_catalogo/v37/generar_v37.py desde',
    'revision_visual/matriz_revision_visual_completa.tsv (solo CONFIRMADO).',
    'Revision visual asistida por IA; la doble revision humana sigue PENDIENTE.']


def escribir_sql(cat, cambios, bloques, carpeta, nombre, etiqueta, titulo, estado):
    os.makedirs(carpeta, exist_ok=True)
    sel = sorted(((i, col, a, d, b) for (i, col), (a, d, b, e) in cambios.items() if b in bloques),
                 key=lambda x: (x[0], x[1]))
    ids = sorted({x[0] for x in sel})
    L = []
    L.append('-- =====================================================================')
    L.append(f'-- FitSense MVP 1.0 - {etiqueta}: {titulo}')
    L.append('--')
    L.extend('-- ' + x for x in estado)
    L.append('--')
    L.append(f'-- {len(ids)} ejercicios, {len(sel)} cambios de columna. Detalle con evidencia:')
    L.append('-- docs/FitSense/auditoria_catalogo/v37/correcciones_v37.tsv')
    L.append('--')
    L.append('-- Comprueba el estado anterior de CADA columna (y source_code y name_en) antes')
    L.append('-- de cambiar nada, y el estado final al terminar. Si algo no coincide, aborta.')
    L.append('-- No toca exercise_difficulty_approvals (V27/V28), planes, adaptacion ni Java.')
    L.append('-- =====================================================================')
    L.append('')
    L.append('CREATE TEMP TABLE v37_cambios (exercise_id BIGINT, source_code TEXT, name_en TEXT, columna TEXT,')
    L.append('                               antes TEXT, despues TEXT);')
    L.append('INSERT INTO v37_cambios VALUES')
    vals = [f'    ({i}, {q(cat[i][1])}, {q(cat[i][3])}, {q(col)}, {q(a)}, {q(d)})' for i, col, a, d, b in sel]
    L.append(',\n'.join(vals) + ';')
    L.append('')
    L.append(VISTA)
    L.append('')
    L.append(f"""DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL') || ' (se esperaba '
                      || c.antes || ')', '; ' ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v37_cambios c
      LEFT JOIN pg_temp.v37_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.antes
        OR NOT EXISTS (SELECT 1 FROM public.exercises e WHERE e.exercise_id = c.exercise_id
                          AND e.source_code = c.source_code AND e.name_en = c.name_en AND e.is_active);
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION '{etiqueta}: estado anterior inesperado -> %', distintos;
    END IF;
END $$;
""")
    flags = [c for c in ('requires_floor', 'requires_kneeling', 'requires_hanging', 'requires_overhead',
                         'requires_inversion', 'high_impact', 'axial_load') if any(x[1] == c for x in sel)]
    if flags:
        # Un solo UPDATE: ck_exercises_hanging_is_overhead se comprueba fila a fila.
        sets = ',\n       '.join(
            f"{c} = coalesce((SELECT c.despues::boolean FROM v37_cambios c WHERE c.exercise_id = e.exercise_id"
            f" AND c.columna = '{c}'), e.{c})" for c in flags)
        L.append(f"UPDATE public.exercises e\n   SET {sets}\n WHERE e.exercise_id IN (SELECT exercise_id FROM v37_cambios"
                 f" WHERE columna IN ({', '.join(q(c) for c in flags)}));")
    if any(x[1] == 'is_active' for x in sel):
        L.append("UPDATE public.exercises e SET is_active = c.despues::boolean FROM v37_cambios c\n"
                 " WHERE c.exercise_id = e.exercise_id AND c.columna = 'is_active';")
    if any(x[1] == 'difficulty_level' for x in sel):
        L.append("UPDATE public.exercises e SET difficulty_level = c.despues::smallint FROM v37_cambios c\n"
                 " WHERE c.exercise_id = e.exercise_id AND c.columna = 'difficulty_level';")
    for col in ('movement_pattern', 'target_muscle'):
        if any(x[1] == col for x in sel):
            L.append(f"UPDATE public.exercises e SET {col} = c.despues FROM v37_cambios c\n"
                     f" WHERE c.exercise_id = e.exercise_id AND c.columna = '{col}';")
    if any(x[1] == 'body_part' for x in sel):
        L.append("UPDATE public.exercises e SET body_part_id = bp.body_part_id FROM v37_cambios c\n"
                 "  JOIN public.body_parts bp ON bp.code = c.despues\n"
                 " WHERE c.exercise_id = e.exercise_id AND c.columna = 'body_part';")
    if any(x[1] == 'equipment' for x in sel):
        L.append("UPDATE public.exercises e SET equipment_id = q.equipment_id FROM v37_cambios c\n"
                 "  JOIN public.equipment_types q ON q.code = c.despues\n"
                 " WHERE c.exercise_id = e.exercise_id AND c.columna = 'equipment';")
    if any(x[1] == 'requisitos' for x in sel):
        L.append("DELETE FROM public.exercise_equipment_requirements r USING v37_cambios c\n"
                 " WHERE c.exercise_id = r.exercise_id AND c.columna = 'requisitos';")
        L.append("INSERT INTO public.exercise_equipment_requirements (exercise_id, group_no, equipment_id)\n"
                 "SELECT c.exercise_id, split_part(x, ':', 1)::smallint, q.equipment_id\n"
                 "  FROM v37_cambios c CROSS JOIN LATERAL unnest(string_to_array(nullif(c.despues, ''), ',')) AS x\n"
                 "  JOIN public.equipment_types q ON q.code = split_part(x, ':', 2)\n"
                 " WHERE c.columna = 'requisitos';")
    L.append('')
    L.append(f"""DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL'), '; '
                      ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v37_cambios c
      LEFT JOIN pg_temp.v37_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.despues;
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION '{etiqueta}: estado final inesperado -> %', distintos;
    END IF;
    IF EXISTS (SELECT 1 FROM public.exercises WHERE requires_hanging AND NOT requires_overhead) THEN
        RAISE EXCEPTION '{etiqueta}: hay ejercicios con suspension y sin brazos arriba';
    END IF;
END $$;

-- Flyway ejecuta las migraciones en la misma sesion: los objetos temporales se borran aqui.
DROP VIEW pg_temp.v37_valor;
DROP TABLE pg_temp.v37_cambios;
""")
    with open(os.path.join(carpeta, nombre), 'w') as fh:
        fh.write('\n'.join(L))


VISTA = """-- Valor actual de cada columna afectada, como texto comparable con antes/despues.
CREATE TEMP VIEW v37_valor AS
SELECT e.exercise_id, x.columna, x.valor
  FROM public.exercises e
  JOIN public.body_parts bp ON bp.body_part_id = e.body_part_id
  JOIN public.equipment_types eq ON eq.equipment_id = e.equipment_id
 CROSS JOIN LATERAL (VALUES
       ('requires_floor', e.requires_floor::text), ('requires_kneeling', e.requires_kneeling::text),
       ('requires_hanging', e.requires_hanging::text), ('requires_overhead', e.requires_overhead::text),
       ('requires_inversion', e.requires_inversion::text), ('high_impact', e.high_impact::text),
       ('axial_load', e.axial_load::text), ('difficulty_level', e.difficulty_level::text),
       ('movement_pattern', e.movement_pattern), ('target_muscle', e.target_muscle), ('body_part', bp.code),
       ('equipment', eq.code), ('is_active', e.is_active::text),
       ('requisitos', coalesce((SELECT string_agg(r.group_no || ':' || q.code, ',' ORDER BY r.group_no || ':' || q.code)
                                  FROM public.exercise_equipment_requirements r
                                  JOIN public.equipment_types q ON q.equipment_id = r.equipment_id
                                 WHERE r.exercise_id = e.exercise_id), ''))) AS x(columna, valor);"""


if __name__ == '__main__':
    main()
