package main.web.services.fitsense.shared.domain.model.valueobjects;

/**
 * Que se le permite cambiar a la IA dentro de una sesion senalada.
 * <p>
 * VIVE EN {@code shared} PORQUE LO NECESITAN LOS DOS MODULOS. {@code adaptation}
 * lo produce -decide el tipo a partir del motivo registrado- y {@code planning}
 * lo consume -la validacion de conservacion comprueba que solo ocurrio lo
 * permitido-. Ponerlo en {@code adaptation} obligaria a {@code planning} a
 * importar de {@code adaptation}, y hoy no importa NADA de ahi: la dependencia
 * va en el otro sentido y la decision le llega traducida a
 * {@code PlanAdjustment}. Es la misma situacion de {@code SkipReason}, que ya
 * vive aqui, y de la que este enum se deriva.
 * <p>
 * EL MOTIVO ORIENTA, NO ORDENA. Una sesion omitida no demuestra que sus
 * ejercicios sean malos: si se omitio por falta de tiempo, cabe reducir el
 * trabajo autorizado o retirar un unico ejercicio de relleno, no sustituir
 * arbitrariamente ejercicios estructurales. Este enum impide que la IA cambie
 * lo que no corresponde al motivo registrado.
 * <p>
 * Tampoco ordena cambiar: {@link #permiteSustituirEjercicio()} dice que esta
 * PERMITIDO, no que sea obligatorio. Si no hay una alternativa valida, lo
 * correcto es conservar y declararlo, no inventar un reemplazo. La unica
 * excepcion es {@link #SEGURIDAD_ANTE_DOLOR}: ahi conservar es justamente lo que
 * no se puede hacer.
 */
public enum TipoIntervencion {

    /**
     * SCHEDULE_CHANGE: mover el dia. Los ejercicios no son el problema, y
     * tampoco la dosis: lo unico que estorbaba era la fecha.
     */
    REPROGRAMAR_DIAS(false, false, false,
            "Reprogramar el dia de la sesion. No sustituir ejercicios ni "
                    + "retirarlos, y no cambiar la dosis."),

    /**
     * LACK_OF_TIME: acortar o reorganizar la sesion. Autoriza cambiar dosis
     * cuando las demas reglas lo permitan o retirar UN ejercicio de relleno;
     * si se retira, las dosis de todos los restantes deben quedar intactas.
     * No autoriza sustituir ejercicios ni cambiar el dia.
     */
    ACORTAR_O_REORGANIZAR(false, true, false,
            "Acortar o reorganizar la sesion. Puedes bajar la dosis y retirar UNA "
                    + "colocacion de relleno. No sustituir ni anadir ejercicios, no "
                    + "cambiar el dia, y no tocar la dosis de las que se quedan si "
                    + "retiras una. La duracion minima de la sesion no te obliga en "
                    + "esta."),

    /** TOO_DIFFICULT: variante mas sencilla del MISMO patron, o ajustar la carga. */
    VARIANTE_MAS_SENCILLA_O_CARGA(true, true, false,
            "Buscar una variante mas sencilla del mismo patron de movimiento, o "
                    + "ajustar la carga. No cambiar el dia."),

    /**
     * PAIN_OR_DISCOMFORT: la seguridad manda. No insistir en el ejercicio que
     * dolio, y NO reemplazarlo automaticamente sin evaluar antes la restriccion
     * declarada del participante.
     * <p>
     * Es el unico tipo donde CONSERVAR no es una opcion neutra: volver a
     * prescribir el ejercicio que dolio, solo porque estaba en el plan anterior,
     * es publicar algo potencialmente perjudicial. Si no hay alternativa segura,
     * lo correcto es declarar que esa parte no se pudo resolver.
     */
    SEGURIDAD_ANTE_DOLOR(true, true, true,
            "No insistir en el ejercicio que causo dolor. Evaluar la restriccion "
                    + "declarada antes de proponer cualquier reemplazo; retirarlo sin "
                    + "reemplazo tambien vale. No cambiar el dia."),

    /** FATIGUE: revisar recuperacion y carga; no sustituir ni retirar ejercicios. */
    REVISAR_CARGA_Y_RECUPERACION(false, true, false,
            "Revisar carga y recuperacion. Puedes ajustar la dosis. No sustituir ni "
                    + "retirar ejercicios, y no cambiar el dia."),

    /**
     * LACK_OF_MOTIVATION: investigar que dificulta mantener la rutina y proponer
     * cambios pequenos y justificados. Falta de motivacion NO significa
     * necesariamente que la sesion sea demasiado larga.
     */
    INVESTIGAR_CAMBIO_MINIMO(false, true, false,
            "Investigar que dificulta mantener la rutina y proponer cambios pequenos "
                    + "en la dosis. No sustituir ni retirar ejercicios, y no cambiar "
                    + "el dia."),

    /**
     * OTHER o sin motivo: no se inventa una sustitucion, ni un recorte de dosis,
     * sobre una causa que no consta. Lo unico correcto es conservar y registrar
     * que no hubo adaptacion efectiva.
     */
    CAUSA_NO_REGISTRADA(false, false, false,
            "Informacion insuficiente sobre la causa. No inventar una sustitucion, no "
                    + "retirar ejercicios, y no cambiar la dosis ni el dia. Conservar "
                    + "la sesion es la respuesta correcta.");

    private final boolean permiteSustituirEjercicio;
    private final boolean permiteCambiarDosis;
    private final boolean exigeRevisionDeSeguridad;
    private final String instruccion;

    TipoIntervencion(boolean permiteSustituirEjercicio, boolean permiteCambiarDosis,
                     boolean exigeRevisionDeSeguridad, String instruccion) {
        this.permiteSustituirEjercicio = permiteSustituirEjercicio;
        this.permiteCambiarDosis = permiteCambiarDosis;
        this.exigeRevisionDeSeguridad = exigeRevisionDeSeguridad;
        this.instruccion = instruccion;
    }

    /** Si la validacion de conservacion debe aceptar que un ejercicio cambie. */
    public boolean permiteSustituirEjercicio() {
        return permiteSustituirEjercicio;
    }

    /**
     * Si la validacion de conservacion debe aceptar que cambien series,
     * repeticiones, segundos o descanso.
     * <p>
     * SEPARADO DE LA SUSTITUCION A PROPOSITO. Son dos permisos distintos y la
     * tabla de motivos los necesita por separado: con SCHEDULE_CHANGE lo unico
     * que estorbaba era la fecha, asi que recortar la dosis seria un cambio que
     * nadie pidio; con LACK_OF_TIME recortar la dosis es EXACTAMENTE lo que hay
     * que hacer, y sustituir ejercicios no.
     */
    public boolean permiteCambiarDosis() {
        return permiteCambiarDosis;
    }

    /** Si el reemplazo exige comprobar antes la restriccion declarada. */
    public boolean exigeRevisionDeSeguridad() {
        return exigeRevisionDeSeguridad;
    }

    /**
     * Si esta sesion puede cambiar de dia.
     * <p>
     * ES UN PERMISO APARTE, con la misma logica que los de sustitucion y dosis.
     * Sin el, cualquier sesion senalada podia moverse de miercoles a jueves y
     * contar como adaptacion aplicada: {@code CAUSA_NO_REGISTRADA} incluida, que
     * es el tipo que por definicion no conoce ninguna causa que justifique el
     * cambio.
     * <p>
     * Hoy lo tiene SOLO {@link #REPROGRAMAR_DIAS}, donde el motivo registrado es
     * literalmente un cambio de horario. {@link #REVISAR_CARGA_Y_RECUPERACION} es
     * el candidato discutible -espaciar sesiones es una intervencion de
     * recuperacion, y V18 ya gobierna los dias consecutivos- y queda FUERA a
     * proposito: la fatiga no autoriza por si sola mover el dia, igual que no
     * autoriza incumplir el tiempo minimo. Si se define una politica de descarga,
     * se autoriza expresamente y se prueba.
     */
    public boolean permiteReprogramar() {
        return this == REPROGRAMAR_DIAS;
    }

    /**
     * Si esta sesion puede bajar del piso de duracion del perfil (V20).
     * <p>
     * MAS ESTRECHO QUE {@link #permiteCambiarDosis()} A PROPOSITO, y por eso es un
     * permiso aparte. Cambiar la dosis no es lo mismo que acortar la sesion por
     * debajo del minimo que pidio la persona: sustituir por una variante mas
     * sencilla, o revisar la carga, no justifica saltarse el piso. Acortar por
     * falta de tiempo si: es literalmente lo que se ordeno.
     * <p>
     * La exencion se aplica SOLO a la sesion senalada, nunca a la semana: el piso
     * lo lee tambien {@code PlanInputSnapshot}, asi que relajarlo globalmente
     * cambiaria lo que el prompt le dice a la IA.
     */
    public boolean permiteReducirDuracion() {
        return this == ACORTAR_O_REORGANIZAR;
    }

    /**
     * Permite quitar como maximo un ejercicio de relleno en una sesion
     * ACORTAR_O_REORGANIZAR; las restricciones adicionales las verifica V8C.
     * Es un permiso derivado, no cambia los tres booleanos del constructor.
     */
    public boolean permiteQuitarEjercicio() {
        return this == ACORTAR_O_REORGANIZAR;
    }

    /** Texto para el prompt. Describe permisos y prohibiciones de cada intervencion. */
    public String instruccion() {
        return instruccion;
    }

    /**
     * El motivo registrado decide el tipo. Los siete valores son los que admite
     * {@code ck_wse_skip_reason}; cualquier otra cosa, incluido el nulo, cae en
     * CAUSA_NO_REGISTRADA, que no permite sustituir nada.
     */
    public static TipoIntervencion deMotivo(String skipReason) {
        if (skipReason == null) return CAUSA_NO_REGISTRADA;
        return switch (skipReason) {
            case "SCHEDULE_CHANGE" -> REPROGRAMAR_DIAS;
            case "LACK_OF_TIME" -> ACORTAR_O_REORGANIZAR;
            case "TOO_DIFFICULT" -> VARIANTE_MAS_SENCILLA_O_CARGA;
            case "PAIN_OR_DISCOMFORT" -> SEGURIDAD_ANTE_DOLOR;
            case "FATIGUE" -> REVISAR_CARGA_Y_RECUPERACION;
            case "LACK_OF_MOTIVATION" -> INVESTIGAR_CAMBIO_MINIMO;
            default -> CAUSA_NO_REGISTRADA;
        };
    }
}
