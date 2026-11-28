-- =====================================================================
-- V33 (CANDIDATA, 2026-10-20) · APLICADA SI Y SOLO SI HAY SESIONES APLICADAS
-- =====================================================================
--
-- NO ESTA APROBADA. El sufijo .CANDIDATA esta para que Flyway no la recoja. Si
-- se aprueba, se renombra a V33__aplicada_exige_sesiones.sql y se mueve a
-- src/main/resources/db/migration.
--
-- ANTES DE APLICARLA hay que ejecutar lectura-filas.sql, que es de solo
-- lectura, y aplicar el espejo en AdaptationAttempt.java que va en este mismo
-- ZIP. Las tres cosas van juntas o no van.
--
-- ---------------------------------------------------------------------
-- LA REGLA
-- ---------------------------------------------------------------------
--     status = 'APLICADA'  <=>  cardinality(applied_sessions) > 0
--
-- Es una BICONDICIONAL, y la version anterior de esta candidata solo tenia la
-- mitad: exigia que APLICADA tuviera sesiones, y que SIN_EFECTO no las tuviera.
-- Eso dejaba RESPALDO, SIN_ORDEN, RECHAZADA y NO_FACTIBLE sin proteger, sin
-- ningun motivo.
--
-- La bicondicional es exactamente lo que hace AdaptationEvidenceMapper hoy, y
-- se puede leer en dos sitios del archivo:
--
--   1. estado(resultado, decision): APLICADA sale UNICAMENTE de
--      Motivo.CORRECCION_IA con resultado publicable. Ningun otro motivo llega
--      a APLICADA.
--
--   2. aplicadas = motivo == CORRECCION_IA ? informe.aplicadas() : List.of()
--      y, antes de eso, el retorno temprano de !status.publico() vacia las
--      CUATRO listas. Asi que applied_sessions solo se puebla en el camino
--      CORRECCION_IA + publicable, que es justo APLICADA.
--
-- Y no vacio: AdaptedWeekProducer devuelve CORRECCION_IA solo cuando
-- informe.huboAdaptacionEfectiva(), que en ConservationReport es literalmente
-- !aplicadas.isEmpty().
--
-- ---------------------------------------------------------------------
-- QUE HUECO TAPA
-- ---------------------------------------------------------------------
-- Hoy la base acepta las dos incoherencias: una fila APLICADA con
-- applied_sessions = '{}' -una adaptacion que consta aplicada y no aplico
-- nada- y una fila RESPALDO o SIN_ORDEN con sesiones aplicadas -un plan
-- conservado que afirma haber cambiado sesiones-.
--
-- Ni V32 ni el constructor compacto de AdaptationAttempt lo impiden. Los
-- diecisiete CHECK que ese record replica cubren subconjuntos, disjunciones,
-- rangos y la coherencia decision/estado; ninguno es este. En V32, las 21
-- restricciones nombradas estan listadas en su propio bloque de comprobacion y
-- tampoco incluye ninguna asi.
--
-- Imposible en produccion, aceptado por el record y por la base: la garantia
-- entera descansa en una linea de AdaptedWeekProducer. Y se vio lo facil que es
-- construir el estado imposible sin querer: el primer fixture de
-- PublishAdaptedTest lo hizo, con ConservationReport.vacio().
--
-- Afecta a una cifra de la tesis. La proporcion de adaptaciones efectivamente
-- aplicadas se calcula contando filas APLICADA; una fila APLICADA sin sesiones
-- aplicadas la infla, y una RESPALDO con sesiones aplicadas desvirtua el
-- recuento por sesion.
--
-- ---------------------------------------------------------------------
-- NOTA DE ESTILO, no de correccion
-- ---------------------------------------------------------------------
-- V32 escribe estas cuentas como coalesce(array_length(col, 1), 0). Aqui se usa
-- cardinality(), que es lo revisado y aprobado. Las dos formas son equivalentes
-- en esta tabla porque applied_sessions es NOT NULL DEFAULT '{}', asi que
-- cardinality nunca devuelve NULL. Se deja constancia por si se prefiere
-- uniformar con V32.
--
-- =====================================================================

ALTER TABLE plan_adaptation_attempts
    ADD CONSTRAINT ck_paa_aplicada_con_sesiones
        CHECK (
            status <> 'APLICADA'
                OR cardinality(applied_sessions) > 0
            );

ALTER TABLE plan_adaptation_attempts
    ADD CONSTRAINT ck_paa_solo_aplicada_con_sesiones
        CHECK (
            status = 'APLICADA'
                OR cardinality(applied_sessions) = 0
            );

-- ---------------------------------------------------------------------
-- COMPROBACION, en el estilo de V32
-- ---------------------------------------------------------------------
DO
$$
    DECLARE
restricciones INTEGER;
BEGIN
SELECT COUNT(*)
INTO restricciones
FROM pg_constraint
WHERE conrelid = 'plan_adaptation_attempts'::regclass
          AND conname IN ('ck_paa_aplicada_con_sesiones',
                          'ck_paa_solo_aplicada_con_sesiones');

IF restricciones <> 2 THEN
            RAISE EXCEPTION 'V33: se esperaban 2 restricciones nuevas y hay %', restricciones;
END IF;
END
$$;

-- ---------------------------------------------------------------------
-- LO QUE ESTA COMPROBACION *NO* DEMUESTRA
-- ---------------------------------------------------------------------
-- Que las dos restricciones existan, y nada mas. Que RECHACEN hay que verlo
-- fallar, y aqui no se puede: un INSERT de prueba con identificadores
-- inventados choca antes con fk_paa_user, asi que no llegaria al CHECK y la
-- comprobacion pasaria sin haber ejercitado nada.
--
-- La demostracion va en P2-D, con usuario, medicion y plan reales en la
-- PostgreSQL desechable, y son DOS casos, uno por restriccion:
--
--   a) status='APLICADA' con applied_sessions='{}'
--      -> debe fallar con ck_paa_aplicada_con_sesiones
--   b) status='RESPALDO' con applied_sessions='{101}'
--      -> debe fallar con ck_paa_solo_aplicada_con_sesiones
--
-- Hasta entonces esto son dos restricciones declaradas y sin demostrar, que es
-- exactamente lo que no cuenta.
