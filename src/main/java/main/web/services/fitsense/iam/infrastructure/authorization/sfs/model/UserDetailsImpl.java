package main.web.services.fitsense.iam.infrastructure.authorization.sfs.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import main.web.services.fitsense.iam.domain.model.aggregates.User;
import main.web.services.fitsense.iam.domain.model.valueobjects.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@EqualsAndHashCode
public class UserDetailsImpl implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserDetailsImpl(Long userId, String username, String password, boolean enabled,
                            UserRole role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.accountNonLocked = enabled;
        // Una sola autoridad, la del rol guardado. Que ADMIN tambien pueda lo de
        // USER lo resuelve la jerarquia (WebSecurityConfiguration.roleHierarchy).
        this.authorities = List.of(new SimpleGrantedAuthority(role.authority()));
    }

    /** El rol sale de la base, no del token: un cambio vale desde la peticion siguiente. */
    public static UserDetailsImpl build(User user) {
        return new UserDetailsImpl(user.getId(), user.emailAddress(),
                user.getPasswordHash(), user.canSignIn(), user.getRole());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountNonLocked;
    }
}
