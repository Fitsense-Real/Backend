"""Genera V51: impact_level e is_stretch EXPLICITOS, fijados al valor que hoy deriva la regla por nombre
(ImpactoPolicy + exclusion por 'estiramiento'), para que el comportamiento no cambie en esta migracion.
Las correcciones revisadas van en una migracion posterior. Uso: python3 -I -B gen_v51.py <catalogo.tsv> <salida.sql>"""
import csv, re, sys
csv.field_size_limit(10**8)
MOD = re.compile(r"\b(trote|trotar|trotando|correr|carrera|corriendo|skipping|rodillas altas|jumping jacks?|piernas separadas|"
                 r"comba|saltar la cuerda|salto de cuerda|saltos? a la cuerda|tijeras|escaladores? r[aá]pidos?)\b")
ALTO = re.compile(r"\b(salto|saltos|saltando|pliom[eé]trico|burpee|burpees|sprint|esprint|caja|rebote|zancada con salto|"
                  r"a una pierna con salto|patinador)\b")
def impacto(nombre, hi):
    n = nombre.lower(); m = bool(MOD.search(n))
    if hi:
        return 'MODERADO' if m and not ALTO.search(n.replace('saltos con las piernas separadas', '')) else 'ALTO'
    return 'MODERADO' if m else 'BAJO'
rows = sorted(csv.DictReader(open(sys.argv[1], encoding='utf-8'), delimiter='\t', quoting=csv.QUOTE_NONE), key=lambda r: int(r['id']))
vals = []
for r in rows:
    if not r['name_es']:
        continue
    vals.append(f"({r['id']}, '{impacto(r['name_es'], r['hi'] == 't')}', {'TRUE' if 'estiramiento' in r['name_es'].lower() else 'FALSE'})")
from collections import Counter
c = Counter(v.split("'")[1] for v in vals); s = sum('TRUE' in v for v in vals)
sql = f"""-- =====================================================================
-- FitSense - V51: impacto y estiramiento como ATRIBUTOS EXPLICITOS del catalogo (cierre del catalogo, 13/11/2026)
--
-- POR QUE. Dos reglas del motor dependian del NOMBRE en espanol:
--   * ImpactoPolicy derivaba el impacto (BAJO/MODERADO/ALTO) de high_impact y de expresiones regulares sobre el
--     nombre ("correr", "skipping", "salto"...). Renombrar un ejercicio cambiaba su impacto (caso 1093, V49).
--   * CatalogEligibilityPolicy excluia del trabajo principal todo nombre que contuviera "estiramiento".
-- El javadoc de ImpactoPolicy ya preveia "un campo impact_level revisado". Esta migracion lo crea.
--
-- QUE HACE. Anade exercises.impact_level (BAJO, MODERADO, ALTO) y exercises.is_stretch, y los rellena con el valor
-- EXACTO que la regla vigente deriva hoy del catalogo V50 ({len(vals)} filas con nombre: BAJO {c['BAJO']}, MODERADO
-- {c['MODERADO']}, ALTO {c['ALTO']}; estiramientos {s}). NO cambia el comportamiento: las correcciones revisadas
-- contra el GIF van en una migracion posterior, una a una y con evidencia. Un ejercicio activo debe tener los dos
-- valores (CHECK): reactivar uno sin clasificarlo falla.
--
-- Generada por docs/FitSense/catalogo_integral/cierre/herramientas/gen_v51.py. Paridad comprobada por
-- CatalogoAtributosExplicitosIT contra la regla Java (ImpactoPolicy.derivadoDelNombre y
-- CatalogEligibilityPolicy.estiramientoPorNombre).
-- =====================================================================

ALTER TABLE exercises ADD COLUMN impact_level VARCHAR(10);
ALTER TABLE exercises ADD COLUMN is_stretch BOOLEAN;
ALTER TABLE exercises ADD CONSTRAINT ck_exercises_impact_level
    CHECK (impact_level IS NULL OR impact_level IN ('BAJO', 'MODERADO', 'ALTO'));

COMMENT ON COLUMN exercises.impact_level IS
    'Impacto articular explicito (P-C2): BAJO siempre un pie en el suelo; MODERADO vuelo bajo bilateral o carrera bajo '
    'el sprint; ALTO saltos, pliometria, olimpicos, sprint. Revisado contra el GIF; no se deriva del nombre (V51).';
COMMENT ON COLUMN exercises.is_stretch IS
    'TRUE si el ejercicio es un estiramiento (fuera del trabajo principal salvo que el objetivo exija movilidad). '
    'Explicito desde V51; antes se deducia de la palabra estiramiento en el nombre.';

CREATE TEMP TABLE v51_atributos (exercise_id BIGINT PRIMARY KEY, impact_level VARCHAR(10) NOT NULL, is_stretch BOOLEAN NOT NULL)
    ON COMMIT DROP;
INSERT INTO v51_atributos VALUES
{(',' + chr(10)).join(vals)};

DO $$
DECLARE faltan INT;
BEGIN
    SELECT count(*) INTO faltan FROM v51_atributos a LEFT JOIN exercises e ON e.exercise_id = a.exercise_id
     WHERE e.exercise_id IS NULL;
    IF faltan > 0 THEN RAISE EXCEPTION 'V51: % ids no existen', faltan; END IF;
END $$;

UPDATE exercises e SET impact_level = a.impact_level, is_stretch = a.is_stretch
  FROM v51_atributos a WHERE a.exercise_id = e.exercise_id;

DO $$
DECLARE sin_valor INT;
BEGIN
    SELECT count(*) INTO sin_valor FROM exercises WHERE is_active AND (impact_level IS NULL OR is_stretch IS NULL);
    IF sin_valor > 0 THEN RAISE EXCEPTION 'V51: % ejercicios activos sin impacto o estiramiento', sin_valor; END IF;
    -- high_impact y el impacto explicito no pueden contradecirse: lo que el catalogo marca de alto impacto nunca es BAJO.
    IF EXISTS (SELECT 1 FROM exercises WHERE high_impact AND impact_level = 'BAJO') THEN
        RAISE EXCEPTION 'V51: high_impact con impact_level BAJO';
    END IF;
END $$;

ALTER TABLE exercises ADD CONSTRAINT ck_exercises_active_impact_stretch
    CHECK (NOT is_active OR (impact_level IS NOT NULL AND is_stretch IS NOT NULL));
ALTER TABLE exercises ADD CONSTRAINT ck_exercises_high_impact_not_bajo
    CHECK (NOT high_impact OR impact_level IS DISTINCT FROM 'BAJO');
"""
open(sys.argv[2], 'w', encoding='utf-8').write(sql)
print(len(vals), c, 'estiramientos', s)
