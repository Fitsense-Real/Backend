package main.web.services.fitsense.planning.domain.services;

/**
 * PROTOTIPOS DE LABORATORIO de la auditoria post R/R' (05/11/2026). APAGADOS por omision: ningun perfil de
 * despliegue ni propiedad de Spring los enciende; solo una propiedad de sistema JVM explicita en una prueba de
 * laboratorio. Sirven para MEDIR una propuesta que cambia el contrato de un validador antes de pedir su aprobacion
 * (06_CORRECCIONES_Y_JUSTIFICACION.md). Con todo apagado, el comportamiento es exactamente el aprobado.
 */
public final class LaboratorioAuditoria {

    /** P-G2 (propuesta): la regla de la mitad de V15 solo se aplica si el enfoque tiene 2+ grupos PRESCRIBIBLES. */
    public static final String V15_MITAD_PRESCRIBIBLES = "fitsense.lab.v15-mitad-prescribibles";

    private LaboratorioAuditoria() {}

    public static boolean v15MitadSobrePrescribibles() {
        return Boolean.getBoolean(V15_MITAD_PRESCRIBIBLES);
    }
}
