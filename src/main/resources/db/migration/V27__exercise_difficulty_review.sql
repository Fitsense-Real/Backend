-- =====================================================================
-- FitSense MVP 1.0 - V27: historial de dificultad aprobada por personas
--
-- ESTADO: CANDIDATA A DESPLIEGUE; NO APLICADA NI VERIFICADA CONTRA LA BD REAL.
-- Este archivo reemplaza la propuesta anterior de V27 SOLO mientras ninguna
-- version V27 haya sido aplicada por Flyway. Una migracion aplicada es inmutable.
--
-- CONTEXTO
--   exercises.difficulty_level conserva el nivel existente, que NO representa
--   por si solo autorizacion humana de prescripcion.
--   El clasificador v5 no supero la validacion; v6 aun no esta validado.
--   Las propuestas del clasificador no se importan a esta tabla.
--   criterion_version es descriptiva; criterion_sha256 identifica los bytes
--   del criterio efectivamente utilizado; es obligatoria para APPROVED,
--   opcional para REVOKED por seguridad.
--   El CHECK valida su formato, NO prueba que el documento exista o coincida:
--   esa comprobacion corresponde al expediente y al flujo de aprobacion.
--
-- POLITICA DE APROBACION DEL MVP
--   APPROVED exige DOS revisores identificados y distintos, mas referencia
--   trazable a la evidencia. Una revision individual NO autoriza prescripcion.
--   REVOKED puede registrarse por UN revisor para retirar inmediatamente una
--   aprobacion ante un problema de seguridad; puede incluir segundo revisor.
--   Para REVOKED no se exige criterion_sha256 (la falta de esa huella no
--   debe impedir retirar de inmediato un ejercicio inseguro).
--   Las revisiones individuales que aun no llegan a APPROVED se conservan en
--   su expediente, NO se representan falsamente como aprobadas.
--
-- HISTORIAL
--   Un cambio se registra como nuevo EVENTO (INSERT). No UPDATE ni DELETE.
--   La vista vigente utiliza el ultimo evento por ejercicio, segun event_id.
--   Un ejercicio sin evento APPROVED vigente esta NO APROBADO. Un evento
--   REVOKED retira la aprobacion anterior sin borrar su evidencia.
--   ON DELETE RESTRICT evita borrar un ejercicio con historial asociado.
--
-- DESPLIEGUE EN TRES FASES
--   1. Esta migracion crea SOLO esquema/funcion/vista: 0 aprobaciones nuevas.
--   2. Tras la revision humana, una migracion posterior carga las aprobaciones
--      trazables, sin copiar automaticamente exercises.difficulty_level.
--   3. Solo con >=3 ejercicios aprobados por slot obligatorio Y por perfil,
--      desplegar la seleccion Java que use la vista + filtros de seguridad,
--      equipamiento, restricciones y Step 4C. V27 por si sola NO cambia Java.
--
-- Una vez ejecutada con Flyway, NO modificar ni reformatear este archivo.
-- =====================================================================

-- PRECONDICIONES: esquema real, V26 e identidad compatible con la FK.
DO $$
DECLARE
faltantes TEXT;
    tipo_id TEXT;
    v26_aplicada BOOLEAN;
BEGIN
    IF to_regclass('public.exercises') IS NULL THEN
        RAISE EXCEPTION 'V27: no existe public.exercises';
END IF;

SELECT string_agg(req.nombre, ', ' ORDER BY req.nombre) INTO faltantes
FROM unnest(ARRAY[
                'exercise_id', 'difficulty_level', 'movement_pattern',
            'is_active', 'source_code', 'name_en'
                ]) AS req(nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name = 'exercises'
      AND c.column_name = req.nombre
);
IF faltantes IS NOT NULL THEN
        RAISE EXCEPTION 'V27: faltan columnas de public.exercises: %', faltantes;
END IF;

SELECT c.data_type INTO tipo_id
FROM information_schema.columns c
WHERE c.table_schema = 'public'
  AND c.table_name = 'exercises'
  AND c.column_name = 'exercise_id';
IF tipo_id <> 'bigint' THEN
        RAISE EXCEPTION 'V27: exercises.exercise_id es %, se esperaba bigint; revisar DDL antes de crear FK', tipo_id;
END IF;

    -- Solo si existe la tabla de historial Flyway. EXECUTE evita referenciar
    -- una tabla que pueda no existir durante la planificacion del bloque DO.
    -- Acepta 26, 026 y 26.0; no acepta 26.1 ni 126.
    IF to_regclass('public.flyway_schema_history') IS NOT NULL THEN
        EXECUTE $q$
SELECT EXISTS (
    SELECT 1
    FROM public.flyway_schema_history
    WHERE version ~ '^0*26(\.0+)*$'
                  AND success = TRUE
)
           $q$ INTO v26_aplicada;
IF NOT v26_aplicada THEN
            RAISE EXCEPTION 'V27: no figura V26 exitosa en flyway_schema_history; revisar formato de version';
END IF;
END IF;
END $$;

-- Un registro representa un evento de decision, NO el estado editable actual.
CREATE TABLE public.exercise_difficulty_approvals (
                                                      event_id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                      exercise_id               BIGINT NOT NULL,
                                                      decision                  VARCHAR(12) NOT NULL,
                                                      approved_difficulty_level SMALLINT,
                                                      criterion_version         VARCHAR(40) NOT NULL,
                                                      criterion_sha256          CHAR(64),
                                                      reviewer_1                VARCHAR(120) NOT NULL,
                                                      reviewer_2                VARCHAR(120),
                                                      evidence_reference        TEXT NOT NULL,
                                                      recorded_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                      notes                     TEXT,

                                                      CONSTRAINT fk_eda_exercise
                                                          FOREIGN KEY (exercise_id) REFERENCES public.exercises (exercise_id)
                                                              ON DELETE RESTRICT,
                                                      CONSTRAINT ck_eda_decision
                                                          CHECK (decision IN ('APPROVED', 'REVOKED')),
                                                      CONSTRAINT ck_eda_level_according_to_decision
                                                          CHECK (
                                                              (decision = 'APPROVED' AND approved_difficulty_level IS NOT NULL
                                                                  AND approved_difficulty_level BETWEEN 1 AND 3)
                                                                  OR (decision = 'REVOKED' AND approved_difficulty_level IS NULL)
                                                              ),
                                                      CONSTRAINT ck_eda_criterion
                                                          CHECK (length(btrim(criterion_version)) > 0),
                                                      CONSTRAINT ck_eda_criterion_sha256
                                                          CHECK (criterion_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_eda_sha_according_to_decision
        CHECK (decision <> 'APPROVED' OR criterion_sha256 IS NOT NULL),
    CONSTRAINT ck_eda_reviewer_1
        CHECK (length(btrim(reviewer_1)) > 0),
    CONSTRAINT ck_eda_reviewer_2_if_present
        CHECK (reviewer_2 IS NULL OR length(btrim(reviewer_2)) > 0),
    CONSTRAINT ck_eda_two_for_approval
        CHECK (decision <> 'APPROVED' OR reviewer_2 IS NOT NULL),
    CONSTRAINT ck_eda_distinct_reviewers
        CHECK (reviewer_2 IS NULL OR lower(btrim(reviewer_1)) <> lower(btrim(reviewer_2))),
    CONSTRAINT ck_eda_evidence
        CHECK (length(btrim(evidence_reference)) > 0)
);

COMMENT ON TABLE public.exercise_difficulty_approvals IS
    'Bitacora de eventos append-only: APPROVED (dos revisores) o REVOKED (uno o dos). No contiene predicciones automaticas.';
COMMENT ON COLUMN public.exercise_difficulty_approvals.event_id IS
    'Orden de precedencia: el mayor event_id por exercise_id determina el estado vigente.';
COMMENT ON COLUMN public.exercise_difficulty_approvals.criterion_version IS
    'Version legible del criterio; para APPROVED debe corresponder a criterion_sha256.';
COMMENT ON COLUMN public.exercise_difficulty_approvals.criterion_sha256 IS
    'SHA-256 hexadecimal minusculo del criterio congelado. Obligatorio para APPROVED, opcional para REVOKED; verificar procedencia externamente.';
COMMENT ON COLUMN public.exercise_difficulty_approvals.evidence_reference IS
    'Referencia al expediente de revision humana independiente/consensuada; no usar prediccion automatica como evidencia.';
COMMENT ON COLUMN public.exercise_difficulty_approvals.recorded_at IS
    'Fecha de registro del evento. La precedencia no depende de la fecha editable.';

CREATE INDEX ix_eda_exercise_latest
    ON public.exercise_difficulty_approvals (exercise_id, event_id DESC);

-- No permitir UPDATE, DELETE ni TRUNCATE: una correccion o revocacion
-- se registra con INSERT nuevo. Los privilegios DDL/superusuario quedan fuera
-- de esta proteccion (como en cualquier trigger de PostgreSQL).
CREATE FUNCTION public.prevent_exercise_difficulty_history_change()
    RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'V27: historial inmutable: use INSERT para nueva aprobacion o revocacion';
RETURN NULL;
END;
$$;

CREATE TRIGGER trg_eda_no_update_delete
    BEFORE UPDATE OR DELETE ON public.exercise_difficulty_approvals
    FOR EACH ROW
    EXECUTE FUNCTION public.prevent_exercise_difficulty_history_change();

CREATE TRIGGER trg_eda_no_truncate
    BEFORE TRUNCATE ON public.exercise_difficulty_approvals
    FOR EACH STATEMENT
    EXECUTE FUNCTION public.prevent_exercise_difficulty_history_change();

-- SOLO la ultima decision por ejercicio es vigente.
-- Si esa decision es REVOKED, el ejercicio no aparece.
-- No se confunde un APPROVED historico con una autorizacion actual.
CREATE VIEW public.v_exercises_manually_approved AS
SELECT
    e.exercise_id,
    e.source_code,
    e.name_en,
    e.movement_pattern,
    a.approved_difficulty_level,
    a.criterion_version,
    a.criterion_sha256,
    a.recorded_at AS approved_at,
    a.event_id AS approval_event_id
FROM public.exercises e
         JOIN (
    SELECT DISTINCT ON (exercise_id)
        exercise_id, event_id, decision, approved_difficulty_level,
        criterion_version, criterion_sha256, recorded_at
    FROM public.exercise_difficulty_approvals
    ORDER BY exercise_id, event_id DESC
) a ON a.exercise_id = e.exercise_id
WHERE e.is_active = TRUE
  AND a.decision = 'APPROVED';

COMMENT ON VIEW public.v_exercises_manually_approved IS
    'Solo ejercicios activos con ultima decision APPROVED; falta aplicar equipo, restricciones, edad y Step 4C por perfil.';

-- Sin INSERT: el conjunto prescribible arranca VACIO.
-- Diagnostico post-deploy (ejecutar manualmente, NO forma parte de V27):
-- SELECT COUNT(*) FROM public.exercise_difficulty_approvals;      -- 0
-- SELECT COUNT(*) FROM public.v_exercises_manually_approved;   -- 0
-- SELECT version, success FROM public.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
