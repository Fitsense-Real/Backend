"""Genera V52 (correcciones de datos del cierre del catalogo) a partir de decisiones_v52.tsv.

Cada fila de decisiones es UN cambio ya adjudicado (segunda revision + verificacion independiente + coordinador), con
columnas: id, name_en, atributo, antes, despues, fundamento. Atributos admitidos:
  name_es | instructions_es (pasos separados por ' || ') | high_impact requires_floor axial_load requires_kneeling
  requires_hanging requires_overhead requires_inversion (t/f) | movement_pattern | default_prescription |
  difficulty_level (solo SUBIR: bajar exige aprobacion humana, V27) | impact_level | is_stretch (t/f) |
  zona (body_part_code/target_muscle) | equipo ('principal; 1:a,2:b'; mismo grupo = alternativas)

Protecciones: todos los cambios de un ejercicio van en UN UPDATE que exige el estado anterior exacto de cada columna
(texto: md5 normalizado). Si no actualiza nada y el ejercicio no esta ya en el estado nuevo, la migracion FALLA
(no se salta en silencio). El equipamiento exige la cadena de requisitos anterior exacta.
Uso: python3 -I -B gen_v52.py <decisiones_v52.tsv> <catalogo_V50.tsv> <salida.sql>"""
import csv, hashlib, json, re, sys, collections
csv.field_size_limit(10**8)
dec, cat, out = sys.argv[1:4]
C = {r['id']: r for r in csv.DictReader(open(cat, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE)}
D = list(csv.DictReader(open(dec, encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE))
BOOL = {'high_impact': 'hi', 'requires_floor': 'floor', 'axial_load': 'axial', 'requires_kneeling': 'kneel',
        'requires_hanging': 'hang', 'requires_overhead': 'overhead', 'requires_inversion': 'inv'}
TXT = {'movement_pattern': 'pattern', 'default_prescription': 'presc', 'impact_level': None}
PATRONES = set('KNEE_DOMINANT HIP_HINGE HIP_EXTENSION HIP_ABDUCTION_ADDUCTION ANKLE_PLANTAR_FLEXION HORIZONTAL_PUSH '
               'VERTICAL_PUSH HORIZONTAL_PULL VERTICAL_PULL SHOULDER_RAISE HORIZONTAL_ADDUCTION SHOULDER_EXTENSION '
               'SCAPULAR_ELEVATION ELBOW_FLEXION ELBOW_EXTENSION WRIST_FOREARM TRUNK_FLEXION TRUNK_EXTENSION '
               'TRUNK_ROTATION_LATERAL_FLEXION CORE_STABILITY LOCOMOTION_CARDIO MOBILITY BALANCE_CONTROL OTHER'.split())
def q(s): return "'" + s.replace("'", "''") + "'"
def b(s): return {'t': 'TRUE', 'f': 'FALSE'}[s]
def md5n(s): return hashlib.md5(re.sub(r'[\t\r\n]+', ' ', s).encode('utf-8')).hexdigest()
def norm_req(s): return ','.join(sorted(x.strip() for x in s.split(',') if x.strip()))
por_id = collections.OrderedDict()
for d in sorted(D, key=lambda d: int(d['id'])):
    c = C[d['id']]
    if d['name_en'] != c['name_en']: sys.exit(f"{d['id']}: name_en no coincide con la linea base")
    por_id.setdefault(d['id'], []).append(d)
sql = []
cuenta = collections.Counter()
for i, ds in por_id.items():
    c = C[i]
    sets, guards, nuevos, eq = [], [], [], None
    for d in ds:
        a, antes, despues = d['atributo'], d['antes'], d['despues']
        cuenta[a] += 1
        if a == 'name_es':
            assert antes == c['name_es'], (i, a)
            sets.append(f"name_es = {q(despues)}"); guards.append(f"name_es = {q(antes)}"); nuevos.append(f"name_es = {q(despues)}")
        elif a == 'instructions_es':
            assert antes == c['instr_es'], (i, a)
            pasos = [p.strip() for p in despues.split('||') if p.strip()]
            assert 3 <= len(pasos) <= 7 and pasos[-1].startswith(('Repite el número', 'Mantén la duración')), (i, pasos)
            texto = ' '.join(pasos)
            sets.append(f"instructions_es = {q(texto)}, instruction_steps_es = {q(json.dumps(pasos, ensure_ascii=False))}::jsonb")
            guards.append(f"md5(regexp_replace(instructions_es, '[\\t\\r\\n]+', ' ', 'g')) = '{md5n(antes)}'")
            nuevos.append(f"md5(regexp_replace(instructions_es, '[\\t\\r\\n]+', ' ', 'g')) = '{md5n(texto)}'")
        elif a in BOOL:
            assert antes == c[BOOL[a]] and despues in 'tf' and antes != despues, (i, a)
            sets.append(f"{a} = {b(despues)}"); guards.append(f"{a} = {b(antes)}"); nuevos.append(f"{a} = {b(despues)}")
        elif a == 'is_stretch':
            sets.append(f"is_stretch = {b(despues)}"); guards.append(f"is_stretch = {b(antes)}"); nuevos.append(f"is_stretch = {b(despues)}")
        elif a in ('movement_pattern', 'default_prescription', 'impact_level'):
            if a == 'movement_pattern': assert antes == c['pattern'] and despues in PATRONES, (i, a)
            if a == 'default_prescription': assert antes == c['presc'] and despues in ('SETS_REPS', 'DURATION'), (i, a)
            if a == 'impact_level': assert despues in ('BAJO', 'MODERADO', 'ALTO'), (i, a)
            sets.append(f"{a} = {q(despues)}"); guards.append(f"{a} = {q(antes)}"); nuevos.append(f"{a} = {q(despues)}")
        elif a == 'difficulty_level':
            assert antes == c['dif'] and int(despues) > int(antes), (i, 'nunca se baja una dificultad sin aprobacion humana')
            sets.append(f"difficulty_level = {int(despues)}"); guards.append(f"difficulty_level = {int(antes)}")
            nuevos.append(f"difficulty_level = {int(despues)}")
        elif a == 'zona':
            bp0, t0 = antes.split('/', 1); bp1, t1 = despues.split('/', 1)
            assert bp0 == c['bp'] and t0 == c['target'], (i, a)
            sets.append(f"body_part_id = (SELECT body_part_id FROM body_parts WHERE code = {q(bp1)}), target_muscle = {q(t1)}")
            guards.append(f"body_part_id = (SELECT body_part_id FROM body_parts WHERE code = {q(bp0)}) AND target_muscle = {q(t0)}")
            nuevos.append(f"body_part_id = (SELECT body_part_id FROM body_parts WHERE code = {q(bp1)}) AND target_muscle = {q(t1)}")
        elif a == 'equipo':
            p0, r0 = [x.strip() for x in antes.split(';', 1)]; p1, r1 = [x.strip() for x in despues.split(';', 1)]
            assert p0 == c['equip'] and norm_req(r0 if r0 != '-' else '') == norm_req(c['reqs']), (i, a, c['reqs'])
            eq = (p0, norm_req(r0 if r0 != '-' else ''), p1, norm_req(r1))
        else:
            sys.exit(f'{i}: atributo no admitido {a}')
    fund = ' | '.join(f"{d['atributo']}: {d['fundamento']}" for d in ds)
    sql.append(f"-- {i} {c['name_en']}: {fund}")
    if sets:
        w = f"exercise_id = {i} AND name_en = {q(c['name_en'])}"
        sql.append("DO $v52$ DECLARE n INT; BEGIN\n"
                   f"  UPDATE exercises SET {', '.join(sets)}\n   WHERE {w} AND {' AND '.join(guards)};\n"
                   "  GET DIAGNOSTICS n = ROW_COUNT;\n"
                   f"  IF n = 0 AND NOT EXISTS (SELECT 1 FROM exercises WHERE {w} AND {' AND '.join(nuevos)}) THEN\n"
                   f"    RAISE EXCEPTION 'V52: ejercicio {i} no esta en el estado anterior esperado';\n  END IF;\nEND $v52$;")
    if eq:
        p0, r0, p1, r1 = eq
        agg = ("(SELECT coalesce(string_agg(r.group_no || ':' || t.code, ',' ORDER BY r.group_no || ':' || t.code), '') "
               f"FROM exercise_equipment_requirements r JOIN equipment_types t ON t.equipment_id = r.equipment_id WHERE r.exercise_id = {i})")
        codigos = sorted({p1} | {x.split(':', 1)[1] for x in r1.split(',') if x})
        ins = '\n'.join(
            f"  INSERT INTO exercise_equipment_requirements (exercise_id, group_no, equipment_id) VALUES ({i}, {x.split(':', 1)[0]}, "
            f"(SELECT equipment_id FROM equipment_types WHERE code = {q(x.split(':', 1)[1])}));" for x in r1.split(',') if x)
        sql.append("DO $v52$ BEGIN\n"
                   f"  IF (SELECT count(*) FROM equipment_types WHERE code IN ({', '.join(q(x) for x in codigos)})) <> {len(codigos)} THEN\n"
                   f"    RAISE EXCEPTION 'V52: faltan codigos de equipamiento para {i}';\n  END IF;\n"
                   f"  IF {agg} = {q(r1)} THEN RETURN; END IF;\n"
                   f"  IF {agg} <> {q(r0)} THEN RAISE EXCEPTION 'V52: requisitos de {i} distintos de los esperados'; END IF;\n"
                   f"  DELETE FROM exercise_equipment_requirements WHERE exercise_id = {i};\n{ins}\n"
                   f"  UPDATE exercises SET equipment_id = (SELECT equipment_id FROM equipment_types WHERE code = {q(p1)}) WHERE exercise_id = {i};\n"
                   "END $v52$;")
cab = f"""-- =====================================================================
-- FitSense - V52: correcciones de datos del CIERRE DEL CATALOGO (13/11/2026)
--
-- Cada cambio se adjudico con tres miradas sobre la animacion COMPLETA del GIF original: segunda revision visual
-- independiente (20 lotes), verificacion independiente de cada propuesta (tercera mirada) y adjudicacion del
-- coordinador para los cambios de seguridad. Evidencia por cambio: docs/FitSense/catalogo_integral/cierre/
-- (decisiones_v52.tsv, revision2_consolidada.tsv, verificacion3_consolidada.tsv).
--
-- NO baja ninguna dificultad (V27: requiere aprobacion humana). NO cambia el significado de ninguna restriccion del
-- perfil. NO activa ni desactiva ejercicios. Cada ejercicio exige su estado anterior exacto: si no coincide y no esta
-- ya corregido, la migracion FALLA.
-- Cambios: {dict(sorted(cuenta.items()))} en {len(por_id)} ejercicios.
-- Generada por docs/FitSense/catalogo_integral/cierre/herramientas/gen_v52.py.
-- =====================================================================
"""
open(out, 'w', encoding='utf-8').write(cab + '\n' + '\n'.join(sql) + '\n')
print(len(por_id), 'ejercicios', dict(cuenta))
