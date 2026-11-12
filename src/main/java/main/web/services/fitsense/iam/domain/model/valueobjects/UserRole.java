package main.web.services.fitsense.iam.domain.model.valueobjects;

/**
 * Rol de la cuenta (V35). La jerarquia es ADMIN &gt; USER: un ADMIN puede todo lo
 * de un USER y, ademas, la administracion global. El registro publico crea
 * siempre USER; ADMIN solo se alcanza con el procedimiento de promocion.
 */
public enum UserRole {
    USER,
    ADMIN;

    /** La autoridad de Spring Security que corresponde al rol. */
    public String authority() {
        return "ROLE_" + name();
    }
}
