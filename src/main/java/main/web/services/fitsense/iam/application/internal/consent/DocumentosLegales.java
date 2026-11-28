package main.web.services.fitsense.iam.application.internal.consent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * B2.2 (cierre de preproduccion, P0-03): documentos legales VERSIONADOS que la app puede consultar antes de registrarse
 * (condiciones de uso, politica de privacidad y consentimiento informado de investigacion).
 * <p>
 * Se declaran en la configuracion ({@code fitsense.legal-documents.items[i].*}), no en una tabla: no hace falta una
 * migracion (la numeracion la reserva V66) y cambiar un documento vigente queda en el historial de despliegues. Un
 * documento solo puede estar {@code PUBLISHED} con <b>referencia de aprobacion</b> (p. ej. el acta del comite) y una
 * URL {@code https}: el backend no puede presentar como aprobado algo que nadie aprobo. Los {@code DRAFT} sirven para
 * preparar la integracion en pruebas y nunca se publican.
 * <p>
 * Version vigente de un tipo: el {@code PUBLISHED} de {@code effective-from} mas reciente que ya haya empezado.
 */
@ConfigurationProperties(prefix = "fitsense.legal-documents")
public class DocumentosLegales {

    public enum Estado { DRAFT, PUBLISHED, RETIRED }

    public static class Documento {
        private ConsentimientoService.Tipo type;
        private String version;
        private String title;
        private String url;
        private Estado status = Estado.DRAFT;
        private LocalDate effectiveFrom;
        private String approvalReference;

        public ConsentimientoService.Tipo getType() { return type; }
        public void setType(ConsentimientoService.Tipo type) { this.type = type; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Estado getStatus() { return status; }
        public void setStatus(Estado status) { this.status = status; }
        public LocalDate getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        public String getApprovalReference() { return approvalReference; }
        public void setApprovalReference(String approvalReference) { this.approvalReference = approvalReference; }
    }

    private List<Documento> items = new ArrayList<>();

    public List<Documento> getItems() { return items; }

    public void setItems(List<Documento> items) { this.items = items == null ? new ArrayList<>() : items; }

    /** Al arrancar: un documento mal declarado impide arrancar (fallar visible, nunca publicar algo a medias). */
    public void validar() {
        for (int i = 0; i < items.size(); i++) {
            var d = items.get(i);
            String donde = "fitsense.legal-documents.items[" + i + "]";
            if (d.type == null) throw new IllegalStateException(donde + ".type es obligatorio");
            if (vacio(d.version) || d.version.trim().length() > 40)
                throw new IllegalStateException(donde + ".version es obligatoria (1-40 caracteres)");
            if (d.status == Estado.PUBLISHED) {
                if (vacio(d.title)) throw new IllegalStateException(donde + ": un documento publicado necesita title");
                if (vacio(d.approvalReference))
                    throw new IllegalStateException(donde + ": un documento publicado necesita approval-reference"
                            + " (quien lo aprobo y cuando). Sin aprobacion, dejalo en DRAFT.");
                if (d.effectiveFrom == null)
                    throw new IllegalStateException(donde + ": un documento publicado necesita effective-from");
                if (!esHttps(d.url))
                    throw new IllegalStateException(donde + ".url debe ser una URL https para un documento publicado");
            }
        }
        for (var t : ConsentimientoService.Tipo.values()) {
            var publicados = items.stream().filter(d -> d.type == t && d.status == Estado.PUBLISHED).toList();
            if (publicados.stream().map(d -> d.version.trim()).distinct().count() != publicados.size())
                throw new IllegalStateException("fitsense.legal-documents: version publicada repetida para " + t);
        }
    }

    /** Hay al menos un documento declarado de ese tipo (en cualquier estado). */
    public boolean declarado(ConsentimientoService.Tipo tipo) {
        return items.stream().anyMatch(d -> d.type == tipo);
    }

    /** El documento vigente de un tipo, si hay uno publicado que ya empezo. */
    public Optional<Documento> vigente(ConsentimientoService.Tipo tipo, Clock reloj) {
        var hoy = LocalDate.now(reloj);
        return items.stream()
                .filter(d -> d.type == tipo && d.status == Estado.PUBLISHED && !d.effectiveFrom.isAfter(hoy))
                .max(Comparator.comparing(Documento::getEffectiveFrom));
    }

    /** Los vigentes, uno por tipo como mucho: lo que se publica sin autenticacion. */
    public List<Documento> vigentes(Clock reloj) {
        var r = new ArrayList<Documento>();
        for (var t : ConsentimientoService.Tipo.values()) vigente(t, reloj).ifPresent(r::add);
        return r;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static boolean esHttps(String url) {
        if (vacio(url)) return false;
        try {
            var u = URI.create(url.trim());
            return "https".equals(u.getScheme()) && u.getHost() != null && !u.getHost().isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
