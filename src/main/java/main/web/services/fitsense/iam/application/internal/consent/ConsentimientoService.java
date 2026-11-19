package main.web.services.fitsense.iam.application.internal.consent;

import main.web.services.fitsense.iam.domain.model.commands.WithdrawFromStudyCommand;
import main.web.services.fitsense.iam.domain.services.UserCommandService;
import main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException;
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
 * <b>Nada de esto afirma que exista un documento aprobado.</b> Las versiones vigentes de condiciones y privacidad se
 * configuran ({@code fitsense.consent.terms-version}, {@code fitsense.consent.privacy-version}) y estan vacias por
 * omision: sin version aprobada no se puede aceptar nada. {@code fitsense.consent.enforce-on-signup} (apagado) hace
 * obligatorias ambas aceptaciones en el registro; encenderlo exige tener las dos versiones.
 */
@Service
public class ConsentimientoService {

    public enum Tipo { STUDY_PARTICIPATION, TERMS_OF_SERVICE, PRIVACY_POLICY }

    public record Consentimiento(Tipo tipo, String version, Instant aceptadoEn, String accion, String origen,
                                 Instant retiradoEn) {}

    private final JdbcTemplate jdbc;
    private final UserCommandService usuarios;
    private final String versionEstudio;
    private final Optional<String> versionCondiciones;
    private final Optional<String> versionPrivacidad;
    private final boolean exigirEnRegistro;
    private final Clock clock;

    public ConsentimientoService(JdbcTemplate jdbc, UserCommandService usuarios,
                                 @Value("${fitsense.study.consent-version:MVP-1.0}") String versionEstudio,
                                 @Value("${fitsense.consent.terms-version:}") String condiciones,
                                 @Value("${fitsense.consent.privacy-version:}") String privacidad,
                                 @Value("${fitsense.consent.enforce-on-signup:false}") boolean exigir,
                                 Optional<Clock> clock) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.versionEstudio = versionEstudio;
        this.versionCondiciones = vacia(condiciones);
        this.versionPrivacidad = vacia(privacidad);
        this.exigirEnRegistro = exigir;
        this.clock = clock.orElse(Clock.systemUTC());
        if (exigir && (versionCondiciones.isEmpty() || versionPrivacidad.isEmpty()))
            throw new IllegalStateException("fitsense.consent.enforce-on-signup=true exige fitsense.consent."
                    + "terms-version y privacy-version (documentos aprobados).");
    }

    /** Antes de crear la cuenta: con la exigencia encendida, condiciones y privacidad vigentes son obligatorias. */
    public void validarRegistro(String condiciones, String privacidad) {
        if (exigirEnRegistro) {
            if (condiciones == null || privacidad == null)
                throw new DomainRuleViolationException(
                        "Para registrarte tienes que aceptar las condiciones de uso y la politica de privacidad.");
        }
        if (condiciones != null) vigente(Tipo.TERMS_OF_SERVICE, condiciones);
        if (privacidad != null) vigente(Tipo.PRIVACY_POLICY, privacidad);
    }

    /** En la misma transaccion que crea la cuenta. */
    @Transactional
    public void registrarAlta(long userId, boolean participa, String condiciones, String privacidad) {
        if (participa) insertar(userId, Tipo.STUDY_PARTICIPATION, versionEstudio, "CASILLA_REGISTRO", "APP");
        if (condiciones != null) insertar(userId, Tipo.TERMS_OF_SERVICE, condiciones, "CASILLA_REGISTRO", "APP");
        if (privacidad != null) insertar(userId, Tipo.PRIVACY_POLICY, privacidad, "CASILLA_REGISTRO", "APP");
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
        insertar(userId, tipo, version, "BOTON_ACEPTAR", "APP");
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

    /** Versiones vigentes configuradas (vacias = sin documento aprobado). */
    public Optional<String> versionVigente(Tipo tipo) {
        return switch (tipo) {
            case STUDY_PARTICIPATION -> Optional.of(versionEstudio);
            case TERMS_OF_SERVICE -> versionCondiciones;
            case PRIVACY_POLICY -> versionPrivacidad;
        };
    }

    public boolean exigirEnRegistro() {
        return exigirEnRegistro;
    }

    private void vigente(Tipo tipo, String version) {
        var v = versionVigente(tipo).orElseThrow(() -> new DomainRuleViolationException(
                "Todavia no hay una version aprobada de " + tipo + ": no se puede aceptar."));
        if (!v.equals(version))
            throw new DomainRuleViolationException("La version aceptada de " + tipo + " no es la vigente (" + v + ").");
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
