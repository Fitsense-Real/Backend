package main.web.services.fitsense.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import main.web.services.fitsense.iam.application.internal.consent.ConsentimientoService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * B2.2 · Documentos legales vigentes, SIN autenticacion: la app los muestra antes de registrarse. Solo salen los
 * {@code PUBLISHED} (con referencia de aprobacion) que ya entraron en vigor; los borradores nunca. La referencia de
 * aprobacion es interna y no se publica.
 */
@RestController
@SecurityRequirements
@RequestMapping(value = "/api/v1/legal-documents", produces = "application/json")
@Tag(name = "Consentimientos", description = "Historial, aceptacion y retirada de consentimientos versionados")
public class LegalDocumentsController {

    public record DocumentoResource(String type, String version, String title, String url, String status,
                                    String effectiveFrom) {}

    public record DocumentosResource(boolean enforcedOnSignup, String privacyRecord,
                                     List<DocumentoResource> documents) {}

    private final ConsentimientoService servicio;

    public LegalDocumentsController(ConsentimientoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Documentos legales vigentes (condiciones, privacidad, consentimiento informado)",
            description = "Publico. Solo documentos publicados y aprobados. La version de cada uno es la que el"
                    + " registro debe enviar en acceptedTermsVersion, acceptedPrivacyVersion y"
                    + " acceptedStudyConsentVersion. Lista vacia: todavia no hay documentos aprobados.")
    public ResponseEntity<DocumentosResource> vigentes() {
        // Sin limite por IP a proposito: devuelve configuracion estatica, sin tocar la base ni datos de nadie.
        var docs = servicio.documentosVigentes().stream().map(d -> new DocumentoResource(d.getType().name(),
                d.getVersion().trim(), d.getTitle(), d.getUrl().trim(), d.getStatus().name(),
                d.getEffectiveFrom().toString())).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .body(new DocumentosResource(servicio.exigirEnRegistro(), servicio.registroPrivacidad().name(), docs));
    }
}
