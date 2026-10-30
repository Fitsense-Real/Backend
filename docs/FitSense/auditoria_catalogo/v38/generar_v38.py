"""Genera la migracion candidata de V38 (o una etapa suelta para simular) desde cambios_v38.tsv.

Uso: generar_v38.py <salida.sql> <etiqueta> <etapas>   (etapas: cualquier combinacion de A, B, C)

Mismo formato que V37.1: tabla temporal de cambios, comprobacion del estado anterior de
CADA columna (y source_code y name_en), aplicacion, comprobacion del estado final y
borrado de los objetos temporales. Si algo no coincide, aborta. Con la etapa C crea antes
los implementos que faltan en equipment_types.
"""
import csv, sys, os

salida, etiqueta, etapas = sys.argv[1], sys.argv[2], set(sys.argv[3])
base = os.path.dirname(os.path.abspath(__file__))
cambios = [r for r in csv.DictReader(open(os.path.join(base, "cambios_v38.tsv")), delimiter="\t")
           if r["etapa"] in etapas]

NUEVOS = [  # code, name_es, requires_gym, display_order
    ("wall bars", "Espaldera", False, 32),
    ("anchor point", "Punto de anclaje (puerta o poste)", False, 33),
    ("ghd bench", "Banco GHD", True, 34),
    ("hyperextension bench", "Banco de hiperextension (romano)", True, 35),
    ("squat rack", "Rack de sentadilla", True, 36),
    ("preacher bench", "Banco Scott (predicador)", True, 37),
]

def q(s):
    return "'" + s.replace("'", "''") + "'"

o = []
o.append("-- " + "=" * 69)
o.append(f"-- FitSense - {etiqueta}")
o.append("--")
o.append("-- Correcciones del catalogo con evidencia del GIF (DIAGNOSTICO_V37_1.md y decisiones")
o.append("-- D1-D7 del 30/10/2026). Generada por docs/FitSense/auditoria_catalogo/v38/generar_v38.py")
o.append(f"-- desde cambios_v38.tsv, etapas {''.join(sorted(etapas))}: {len(cambios)} cambios de columna en "
         f"{len({c['exercise_id'] for c in cambios})} ejercicios.")
o.append("-- Comprueba el estado anterior de cada columna antes de cambiar nada y el final al terminar.")
o.append("-- No toca exercise_difficulty_approvals (V27/V28), planes, adaptacion ni progresion.")
o.append("-- Revision visual asistida por IA; la doble revision humana sigue PENDIENTE. La dificultad")
o.append("-- 2 de 1106 es PROVISIONAL (D7), no una evaluacion validada con el instrumento.")
o.append("-- " + "=" * 69)
o.append("")
if "C" in etapas:
    o.append("-- Implementos que el GIF exige y el catalogo no podia expresar (D3).")
    o.append("INSERT INTO public.equipment_types (code, name_es, requires_gym, display_order) VALUES")
    o.append(",\n".join(f"    ({q(c)}, {q(n)}, {'TRUE' if g else 'FALSE'}, {d})" for c, n, g, d in NUEVOS))
    o.append("ON CONFLICT (code) DO NOTHING;")
    o.append("")
o.append("CREATE TEMP TABLE v38_cambios (exercise_id BIGINT, source_code TEXT, name_en TEXT, columna TEXT,")
o.append("                               antes TEXT, despues TEXT);")
o.append("INSERT INTO v38_cambios VALUES")
o.append(",\n".join(f"    ({c['exercise_id']}, {q(c['source_code'])}, {q(c['name_en'])}, {q(c['columna'])}, "
                    f"{q(c['antes'])}, {q(c['despues'])})" for c in cambios) + ";")
o.append("""
CREATE TEMP VIEW v38_valor AS
SELECT e.exercise_id, x.columna, x.valor
  FROM public.exercises e
  JOIN public.body_parts bp ON bp.body_part_id = e.body_part_id
 CROSS JOIN LATERAL (VALUES
       ('difficulty_level', e.difficulty_level::text), ('movement_pattern', e.movement_pattern),
       ('target_muscle', e.target_muscle), ('body_part', bp.code),
       ('default_prescription', e.default_prescription::text),
       ('requisitos', coalesce((SELECT string_agg(r.group_no || ':' || q.code, ',' ORDER BY r.group_no || ':' || q.code)
                                  FROM public.exercise_equipment_requirements r
                                  JOIN public.equipment_types q ON q.equipment_id = r.equipment_id
                                 WHERE r.exercise_id = e.exercise_id), ''))) AS x(columna, valor);

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL') || ' (se esperaba '
                      || c.antes || ')', '; ' ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v38_cambios c
      LEFT JOIN pg_temp.v38_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.antes
        OR NOT EXISTS (SELECT 1 FROM public.exercises e WHERE e.exercise_id = c.exercise_id
                          AND e.source_code = c.source_code AND e.name_en = c.name_en AND e.is_active);
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V38: estado anterior inesperado -> %', distintos;
    END IF;
END $$;

UPDATE public.exercises e SET movement_pattern = c.despues FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'movement_pattern';
UPDATE public.exercises e SET target_muscle = c.despues FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'target_muscle';
UPDATE public.exercises e SET difficulty_level = c.despues::smallint FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'difficulty_level';
UPDATE public.exercises e SET default_prescription = c.despues FROM v38_cambios c
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'default_prescription';
UPDATE public.exercises e SET body_part_id = bp.body_part_id FROM v38_cambios c
  JOIN public.body_parts bp ON bp.code = c.despues
 WHERE c.exercise_id = e.exercise_id AND c.columna = 'body_part';
DELETE FROM public.exercise_equipment_requirements r USING v38_cambios c
 WHERE c.exercise_id = r.exercise_id AND c.columna = 'requisitos';
INSERT INTO public.exercise_equipment_requirements (exercise_id, group_no, equipment_id)
SELECT c.exercise_id, split_part(x, ':', 1)::smallint, q.equipment_id
  FROM v38_cambios c CROSS JOIN LATERAL unnest(string_to_array(nullif(c.despues, ''), ',')) AS x
  JOIN public.equipment_types q ON q.code = split_part(x, ':', 2)
 WHERE c.columna = 'requisitos';

DO $$
DECLARE distintos TEXT;
BEGIN
    SELECT string_agg(c.exercise_id || ' ' || c.columna || ' = ' || coalesce(v.valor, 'NULL'), '; '
                      ORDER BY c.exercise_id, c.columna)
      INTO distintos
      FROM v38_cambios c
      LEFT JOIN pg_temp.v38_valor v ON v.exercise_id = c.exercise_id AND v.columna = c.columna
     WHERE v.valor IS DISTINCT FROM c.despues;
    IF distintos IS NOT NULL THEN
        RAISE EXCEPTION 'V38: estado final inesperado -> %', distintos;
    END IF;
END $$;

-- Flyway ejecuta las migraciones en la misma sesion: los objetos temporales se borran aqui.
DROP VIEW pg_temp.v38_valor;
DROP TABLE pg_temp.v38_cambios;""")
open(salida, "w").write("\n".join(o) + "\n")
print(salida, len(cambios))
