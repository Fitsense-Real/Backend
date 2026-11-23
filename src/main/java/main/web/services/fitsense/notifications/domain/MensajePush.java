package main.web.services.fitsense.notifications.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que se envia a un dispositivo. Los textos son genericos a proposito: NUNCA llevan datos de salud, nombres de
 * ejercicios, dolor, adherencia, peso ni el nombre de la persona. Los datos solo llevan identificadores.
 *
 * @param titulo     titulo visible de la notificacion
 * @param cuerpo     texto visible
 * @param datos      carga de datos del contrato (todas las claves y valores son cadenas, como exige FCM)
 * @param agrupacion clave de colapso: una notificacion nueva de la misma clave reemplaza a la anterior en el telefono
 * @param canal      canal de notificaciones de Android que la app crea al arrancar
 */
public record MensajePush(String titulo, String cuerpo, Map<String, String> datos, String agrupacion, String canal) {

    public static final String VERSION_ESQUEMA = "1";
    public static final String CANAL_PLANES = "kora_planes";
    public static final String CANAL_CUENTA = "kora_cuenta";

    public MensajePush {
        Objects.requireNonNull(titulo);
        Objects.requireNonNull(cuerpo);
        datos = Map.copyOf(datos);
    }

    /** Los datos minimos del contrato con Mobile, en este orden de lectura. */
    public static MensajePush para(TipoEventoPush tipo, UUID eventId, String noticeCode, String resourceType,
                                   Long resourceId, Instant creadoEn) {
        var datos = new LinkedHashMap<String, String>();
        datos.put("schemaVersion", VERSION_ESQUEMA);
        datos.put("eventType", tipo.name());
        datos.put("eventId", eventId.toString());
        if (resourceType != null && resourceId != null) {
            datos.put("resourceType", resourceType);
            datos.put("resourceId", String.valueOf(resourceId));
        }
        datos.put("targetScreen", tipo.pantalla().name());
        datos.put("createdAt", creadoEn.toString());
        if (noticeCode != null) datos.put("noticeCode", noticeCode);
        return switch (tipo) {
            case PLAN_GENERATED -> new MensajePush("Tu plan está listo",
                    "Ya puedes ver tu plan de entrenamiento.", datos, "plan_semana", CANAL_PLANES);
            case PLAN_ADAPTED -> new MensajePush("Ajustamos tu plan",
                    "Revisa los cambios de tu plan de esta semana.", datos, "plan_semana", CANAL_PLANES);
            case WEEKLY_PLAN_READY -> new MensajePush("Tu nueva semana está lista",
                    "Ya puedes ver tu plan de la semana.", datos, "plan_semana", CANAL_PLANES);
            case ACCOUNT_NOTICE -> new MensajePush("Aviso de seguridad",
                    "La contraseña de tu cuenta se cambió. Si no fuiste tú, recupérala desde la app.",
                    datos, "aviso_cuenta", CANAL_CUENTA);
        };
    }
}
