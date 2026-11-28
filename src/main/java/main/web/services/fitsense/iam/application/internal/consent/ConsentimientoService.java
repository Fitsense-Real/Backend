package main.web.services.fitsense.iam.application.internal.consent;

import main.web.services.fitsense.iam.domain.exceptions.ConsentimientoRechazadoException;
import main.web.services.fitsense.iam.domain.model.commands.WithdrawFromStudyCommand;
import main.web.services.fitsense.iam.domain.services.UserCommandService;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Consentimientos versionados (V63, CONSENTIMIENTO_Y_EDAD.md). Distingue el consentimiento de investigacion de la
 * aceptacion de condiciones y privacidad, guarda version, momento, accion afirmativa y origen, y registra la retirada.
 * <p>
 * <b>Nada de esto afirma que exista un documento aprobado.</b> La version vigente de cada tipo sale del registro de
 * documentos ({@link DocumentosLegales}, B2.2): solo un documento {@code PUBLISHED}, con referencia de aprobacion,
 * cuenta como aprobado. Si un tipo no tiene ningun documento declarado, se usan las propiedades anteriores
 * ({@code fitsense.consent.terms-version}, {@code fitsense.consent.privacy-version}, vacias por omision; y
 * {@code fitsense.study.consent-version}, que vale {@code MVP-1.0} y <b>no</b> es un documento aprobado).
 * <p>
 * Evidencia del consentimiento informado (B2.1, {@code affirmative_action}):
 * <ul>
 *   <li>{@code CASILLA_REGISTRO}: la app envio la version mostrada y coincide con el documento aprobado vigente;</li>
 *   <li>{@code CASILLA_SIN_VERSION_CONFIRMADA}: hay documento aprobado, pero la app no envio que version mostro
 *       (solo posible con la exigencia apagada: app anterior);</li>
 *   <li>{@code CASILLA_SIN_DOCUMENTO_APROBADO}: no hay documento aprobado; la fila se conserva por compatibilidad,
 *       pero <b>no es prueba valida</b> de un consentimiento informado.</li>
 * </ul>
 * {@code fitsense.consent.enforce-on-signup} (apagado) exige en el registro condiciones y privacidad vigentes y, si la
 * persona acepta participar, la version vigente del consentimiento informado. Encenderlo exige los tres documentos.
 * {@code fitsense.consent.privacy-record} decide como se registra la privacidad: {@code ACEPTACION} (por omision) o
 * {@code CONSTANCIA} (constancia de que se informo, segun el fundamento juridico que se apruebe; ver
 * 03_CONSENTIMIENTOS_Y_CONTRATOS.md). Ninguna de las dos sustituye a otro consentimiento que la ley pudiera exigir.
 * <p>
 * <b>Opcion A (decision de producto, C1):</b> Kora solo es para participantes del estudio. Con el registro
 * definitivo ({@code enforce-on-signup=true} y {@code plans-require-participation=true}):
 * <ul>
 *   <li>sin aceptar participar no se crea la cuenta ({@code PARTICIPACION_REQUERIDA});</li>
 *   <li>solo un consentimiento {@code CASILLA_REGISTRO} (version aprobada que la app confirmo haber mostrado) cuenta
 *       como consentimiento informado valido para recibir planes ({@link #consentimientoValido}). Una casilla sin
 *       documento aprobado ({@code MVP-1.0}) no lo es.</li>
 * </ul>
 * Los rechazos llevan un codigo estable ({@link ConsentimientoRechazadoException}). Encender el registro definitivo
 * exige documentos aprobados y una compilacion de Mobile que envie las tres versiones: no se hace en esta orden.
 */
@Service
public class ConsentimientoService {

    public enum Tipo { STUDY_PARTICIPATION, TERMS_OF_SERVICE, PRIVACY_POLICY }

    public enum RegistroPrivacidad { ACEPTACION, CONSTANCIA }

    public static final String CASILLA_REGISTRO = "CASILLA_REGISTRO";
    public static final String CASILLA_SIN_VERSION_CONFIRMADA = "CASILLA_SIN_VERSION_CONFIRMADA";
    public static final String CASILLA_SIN_DOCUMENTO_APROBADO = "CASILLA_SIN_DOCUMENTO_APROBADO";
    public static final String CONSTANCIA_DE_LECTURA = "CONSTANCIA_DE_LECTURA";

    public record Consentimiento(Tipo tipo, String version, Instant aceptadoEn, String accion, String origen,
                                 Instant retiradoEn) {}

    private final JdbcTemplate jdbc;
    private final UserCommandService usuarios;
    private final DocumentosLegales documentos;
    private final String versionEstudio;
    private final Optional<String> versionCondiciones;
    private final Optional<String> versionPrivacidad;
    private final boolean exigirEnRegistro;
    private final RegistroPrivacidad registroPrivacidad;
    private final boolean planesExigenParticipacion;
    private final Clock clock;

    private static final Logger log = LoggerFactory.getLogger(ConsentimientoService.class);

    public ConsentimientoService(JdbcTemplate jdbc, UserCommandService usuarios, DocumentosLegales documentos,
                                 @Value("${fitsense.study.consent-version:MVP-1.0}") String versionEstudio,
                                 @Value("${fitsense.consent.terms-version:}") String condiciones,
                                 @Value("${fitsense.consent.privacy-version:}") String privacidad,
                                 @Value("${fitsense.consent.enforce-on-signup:false}") boolean exigir,
                                 @Value("${fitsense.consent.privacy-record:ACEPTACION}") RegistroPrivacidad registro,
                                 @Value("${fitsense.study.plans-require-participation:true}") boolean planesExigenParticipacion,
                                 Optional<Clock> clock) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.documentos = documentos;
        this.versionEstudio = versionEstudio;
        this.versionCondiciones = vacia(condiciones);
        this.versionPrivacidad = vacia(privacidad);
        this.exigirEnRegistro = exigir;
        this.registroPrivacidad = registro;
        this.planesExigenParticipacion = planesExigenParticipacion;
        this.clock = clock.orElse(Clock.systemUTC());
        documentos.validar();
        for (var t : Tipo.values()) {
            if (!documentos.declarado(t)) continue;
            var anterior = t == Tipo.TERMS_OF_SERVICE ? versionCondiciones
                    : t == Tipo.PRIVACY_POLICY ? versionPrivacidad : Optional.<String>empty();
            var vigente = documentos.vigente(t, this.clock).map(d -> d.getVersion().trim());
            if (anterior.isPresent() && !anterior.equals(vigente))
                throw new IllegalStateException("Dos fuentes para la version vigente de " + t + ": la propiedad"
                        + " anterior (" + anterior.get() + ") y fitsense.legal-documents (" + vigente.orElse("ninguna")
                        + "). Deja solo fitsense.legal-documents.");
        }
        documentos.vigente(Tipo.STUDY_PARTICIPATION, this.clock).ifPresent(d -> {
            if (d.getVersion().trim().length() > 20)
                throw new IllegalStateException("La version del consentimiento informado no puede superar 20"
                        + " caracteres (users.consent_version).");
        });
        if (exigir && (versionVigente(Tipo.TERMS_OF_SERVICE).isEmpty() || versionVigente(Tipo.PRIVACY_POLICY).isEmpty()
                || !documentoAprobado(Tipo.STUDY_PARTICIPATION)))
            throw new IllegalStateException("fitsense.consent.enforce-on-signup=true exige condiciones, privacidad y un"
                    + " consentimiento informado PUBLICADO (documentos aprobados).");
        if (!exigir)
            log.warn("REGISTRO_NO_DEFINITIVO: fitsense.consent.enforce-on-signup=false. Las cuentas nuevas no quedan con"
                    + " un consentimiento informado valido: este modo es para pruebas con usuarios sinteticos, no para"
                    + " inscribir participantes del estudio.");
    }

    /**
     * Antes de crear la cuenta. Con la exigencia encendida, condiciones y privacidad vigentes son obligatorias y, si
     * la persona acepta participar, tambien la version vigente del consentimiento informado. Una version enviada que
     * no sea la vigente se rechaza siempre (este o no encendida la exigencia).
     */
    public void validarRegistro(String condiciones, String privacidad, boolean participa, String estudio) {
        if (exigirEnRegistro) {
            if (condiciones == null || privacidad == null)
                throw new ConsentimientoRechazadoException(ConsentimientoRechazadoException.CONSENTIMIENTO_REQUERIDO,
                        "Para registrarte tienes que aceptar las condiciones de uso y la politica de privacidad.");
            // Opcion A: Kora solo es para participantes. Sin aceptar participar, una cuenta no tendria servicio.
            if (planesExigenParticipacion && !participa)
                throw new ConsentimientoRechazadoException(ConsentimientoRechazadoException.PARTICIPACION_REQUERIDA,
                        "Kora solo esta disponible para las personas que participan en el estudio. Para registrarte"
                                + " tienes que aceptar participar.");
            if (participa && estudio == null)
                throw new ConsentimientoRechazadoException(ConsentimientoRechazadoException.CONSENTIMIENTO_REQUERIDO,
                        "Para participar en el estudio tienes que aceptar la version vigente del consentimiento"
                                + " informado.");
        }
        if (condiciones != null) vigente(Tipo.TERMS_OF_SERVICE, condiciones);
        if (privacidad != null) vigente(Tipo.PRIVACY_POLICY, privacidad);
        if (estudio != null) {
            if (!participa)
                throw new ConsentimientoRechazadoException(ConsentimientoRechazadoException.VERSION_SIN_PARTICIPACION,
                        "Se envio una version del consentimiento informado sin aceptar participar en el estudio.");
            if (!documentoAprobado(Tipo.STUDY_PARTICIPATION))
                throw new ConsentimientoRechazadoException(ConsentimientoRechazadoException.DOCUMENTO_NO_PUBLICADO,
                        "Todavia no hay una version aprobada del consentimiento informado: no se puede aceptar.");
            vigente(Tipo.STUDY_PARTICIPATION, estudio);
        }
    }

    /** Compatibilidad: el registro anterior a B2 (sin version del consentimiento informado). */
    public void validarRegistro(String condiciones, String privacidad) {
        validarRegistro(condiciones, privacidad, false, null);
    }

    /** En la misma transaccion que crea la cuenta. */
    @Transactional
    public void registrarAlta(long userId, boolean participa, String condiciones, String privacidad, String estudio) {
        if (participa) {
            String version = versionVigente(Tipo.STUDY_PARTICIPATION).orElse(versionEstudio);
            String evidencia = !documentoAprobado(Tipo.STUDY_PARTICIPATION) ? CASILLA_SIN_DOCUMENTO_APROBADO
                    : estudio == null ? CASILLA_SIN_VERSION_CONFIRMADA : CASILLA_REGISTRO;
            insertar(userId, Tipo.STUDY_PARTICIPATION, version, evidencia, "APP");
            // La columna anterior de users, que leen el panel y los analisis, con la misma version.
            jdbc.update("UPDATE users SET consent_version = ? WHERE user_id = ?", version, userId);
        }
        if (condiciones != null) insertar(userId, Tipo.TERMS_OF_SERVICE, condiciones, CASILLA_REGISTRO, "APP");
        if (privacidad != null) insertar(userId, Tipo.PRIVACY_POLICY, privacidad,
                registroPrivacidad == RegistroPrivacidad.CONSTANCIA ? CONSTANCIA_DE_LECTURA : CASILLA_REGISTRO, "APP");
    }

    /** Compatibilidad: el registro anterior a B2. */
    @Transactional
    public void registrarAlta(long userId, boolean participa, String condiciones, String privacidad) {
        registrarAlta(userId, participa, condiciones, privacidad, null);
    }

    /** Acepta una version nueva de condiciones o privacidad (la de investigacion solo se da al registrarse). */
    @Transactional
    public Consentimiento aceptar(long userId, Tipo tipo, String version) {
        if (tipo == Tipo.STUDY_PARTICIPATION)
            throw new DomainRuleViolationException("La participacion en el estudio solo se acepta al registrarse;"
                    + " la reincorporacion la gestiona el equipo del estudio.");
        vigente(tipo, version);
        var ya = activo(userId, tipo, version);
        if (ya.isPresent()) return ya.get();
        insertar(userId, tipo, version, tipo == Tipo.PRIVACY_POLICY && registroPrivacidad == RegistroPrivacidad.CONSTANCIA
                ? CONSTANCIA_DE_LECTURA : "BOTON_ACEPTAR", "APP");
        return activo(userId, tipo, version).orElseThrow();
    }

    /**
     * Retira el consentimiento de investigacion: lo marca retirado y aplica la retirada del estudio que ya existia
     * (no mas planes; los datos se conservan hasta la decision de PENDIENTES_APROBACION.md). Condiciones y
     * privacidad no se "retiran": quien no las acepta deja de usar el servicio eliminando su cuenta.
     */
    @Transactional
    public void retirar(long userId, Tipo tipo) {
        if (tipo != Tipo.STUDY_PARTICIPATION)
            throw new DomainRuleViolationException("Las condiciones y la politica de privacidad no se retiran: para"
                    + " dejar de usar el servicio, elimina tu cuenta.");
        jdbc.update("UPDATE user_consents SET withdrawn_at = ? WHERE user_id = ? AND consent_type = ?"
                + " AND withdrawn_at IS NULL", Timestamp.from(clock.instant()), userId, tipo.name());
        usuarios.handle(new WithdrawFromStudyCommand(userId));
    }

    public List<Consentimiento> historial(long userId) {
        return jdbc.query("SELECT consent_type, document_version, granted_at, affirmative_action, source,"
                + " withdrawn_at FROM user_consents WHERE user_id = ? ORDER BY granted_at, consent_id", (rs, i) ->
                new Consentimiento(Tipo.valueOf(rs.getString(1)), rs.getString(2), rs.getTimestamp(3).toInstant(),
                        rs.getString(4), rs.getString(5),
                        rs.getTimestamp(6) == null ? null : rs.getTimestamp(6).toInstant()), userId);
    }

    /**
     * Version vigente de cada tipo: la del documento publicado si el tipo esta en el registro; si no, la propiedad
     * anterior (vacia = sin documento aprobado; la de investigacion vale {@code MVP-1.0}, que NO es un documento
     * aprobado: ver {@link #documentoAprobado}).
     */
    public Optional<String> versionVigente(Tipo tipo) {
        if (documentos.declarado(tipo)) return documentos.vigente(tipo, clock).map(d -> d.getVersion().trim());
        return switch (tipo) {
            case STUDY_PARTICIPATION -> Optional.of(versionEstudio);
            case TERMS_OF_SERVICE -> versionCondiciones;
            case PRIVACY_POLICY -> versionPrivacidad;
        };
    }

    /**
     * ¿La version vigente corresponde a un documento aprobado? Condiciones y privacidad: si tienen version (por el
     * registro o por la propiedad anterior, que solo se rellena con documentos aprobados). Investigacion: solo con un
     * documento PUBLICADO en el registro; {@code MVP-1.0} no lo es.
     */
    public boolean documentoAprobado(Tipo tipo) {
        if (tipo == Tipo.STUDY_PARTICIPATION) return documentos.vigente(tipo, clock).isPresent();
        return versionVigente(tipo).isPresent();
    }

    /** Los documentos publicados y vigentes (lo que la app consulta antes de registrarse). */
    public List<DocumentosLegales.Documento> documentosVigentes() {
        return documentos.vigentes(clock);
    }

    public RegistroPrivacidad registroPrivacidad() {
        return registroPrivacidad;
    }

    public boolean exigirEnRegistro() {
        return exigirEnRegistro;
    }

    /**
     * Opcion A con el registro definitivo: para recibir planes no basta la casilla, hace falta un consentimiento
     * informado valido. Con la exigencia apagada (pruebas) no se exige, y el comportamiento es el anterior.
     */
    public boolean exigeConsentimientoValidoParaPlanes() {
        return exigirEnRegistro && planesExigenParticipacion;
    }

    /**
     * Consentimiento informado VALIDO y no retirado: una fila {@code CASILLA_REGISTRO}, que solo se escribe si habia un
     * documento aprobado vigente y la app confirmo que version mostro. {@code CASILLA_SIN_DOCUMENTO_APROBADO}
     * ({@code MVP-1.0}) y {@code CASILLA_SIN_VERSION_CONFIRMADA} no cuentan.
     */
    public boolean consentimientoValido(long userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM user_consents WHERE user_id = ?"
                + " AND consent_type = 'STUDY_PARTICIPATION' AND withdrawn_at IS NULL AND affirmative_action = ?)",
                Boolean.class, userId, CASILLA_REGISTRO));
    }

    private void vigente(Tipo tipo, String version) {
        var v = versionVigente(tipo).orElseThrow(() -> new ConsentimientoRechazadoException(
                ConsentimientoRechazadoException.DOCUMENTO_NO_PUBLICADO,
                "Todavia no hay una version aprobada de " + tipo + ": no se puede aceptar."));
        // Inexistente, vencida o cambiada mientras la persona se registraba: la app vuelve a leer /legal-documents.
        if (!v.equals(version))
            throw new ConsentimientoRechazadoException(ConsentimientoRechazadoException.VERSION_NO_VIGENTE,
                    "La version aceptada de " + tipo + " no es la vigente (" + v + ").");
    }

    private Optional<Consentimiento> activo(long userId, Tipo tipo, String version) {
        return historial(userId).stream().filter(c -> c.tipo() == tipo && c.version().equals(version)
                && c.retiradoEn() == null).findFirst();
    }

    private void insertar(long userId, Tipo tipo, String version, String accion, String origen) {
        jdbc.update("INSERT INTO user_consents (user_id, consent_type, document_version, granted_at,"
                        + " affirmative_action, source) VALUES (?, ?, ?, ?, ?, ?)", userId, tipo.name(), version,
                Timestamp.from(clock.instant()), accion, origen);
    }

    private static Optional<String> vacia(String s) {
        return s == null || s.isBlank() ? Optional.empty() : Optional.of(s.trim());
    }
}
