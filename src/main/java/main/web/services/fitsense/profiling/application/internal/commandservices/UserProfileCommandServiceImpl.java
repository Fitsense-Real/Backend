package main.web.services.fitsense.profiling.application.internal.commandservices;

import main.web.services.fitsense.profiling.application.internal.outboundservices.acl.ExternalIamService;
import main.web.services.fitsense.profiling.application.internal.outboundservices.acl.ExternalPlanningService;
import main.web.services.fitsense.profiling.domain.exceptions.UserProfileAlreadyExistsException;
import main.web.services.fitsense.profiling.domain.exceptions.UserProfileLockedException;
import main.web.services.fitsense.profiling.domain.exceptions.UserProfileNotFoundException;
import main.web.services.fitsense.profiling.domain.model.aggregates.UserProfile;
import main.web.services.fitsense.profiling.domain.model.commands.CreateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.model.commands.UpdateUserProfileCommand;
import main.web.services.fitsense.profiling.domain.services.UserProfileCommandService;
import main.web.services.fitsense.profiling.infrastructure.persistence.jpa.repositories.UserProfileRepository;
import main.web.services.fitsense.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserProfileCommandServiceImpl implements UserProfileCommandService {

    private final UserProfileRepository userProfileRepository;
    private final ExternalIamService externalIamService;
    private final ExternalPlanningService externalPlanningService;

    private final int edadMinima;

    public UserProfileCommandServiceImpl(UserProfileRepository userProfileRepository,
                                         ExternalIamService externalIamService,
                                         ExternalPlanningService externalPlanningService,
                                         @org.springframework.beans.factory.annotation.Value(
                                                 "${fitsense.profile.minimum-age:18}") int edadMinima) {
        if (edadMinima < 18)
            throw new IllegalStateException("fitsense.profile.minimum-age no puede ser menor que 18 (CONSENTIMIENTO_Y_EDAD.md).");
        this.userProfileRepository = userProfileRepository;
        this.externalIamService = externalIamService;
        this.externalPlanningService = externalPlanningService;
        this.edadMinima = edadMinima;
    }

    @Override
    @Transactional
    public Optional<UserProfile> handle(CreateUserProfileCommand command) {
        if (!externalIamService.userExists(command.userId()))
            throw new ResourceNotFoundException("Usuario", command.userId());
        if (userProfileRepository.existsById(command.userId()))
            throw new UserProfileAlreadyExistsException(command.userId());

        // Kora es solo para mayores de edad: la fecha se comprueba aqui, en el backend, no solo en la app. La fecha
        // de nacimiento no se puede cambiar despues (update no la toca), asi que basta con validarla al crear.
        if (command.birthDate() != null && command.birthDate().isBefore(java.time.LocalDate.now())
                && java.time.Period.between(command.birthDate(), java.time.LocalDate.now()).getYears() < edadMinima)
            throw new main.web.services.fitsense.shared.domain.exceptions.DomainRuleViolationException(
                    "Debes tener al menos %d anos para usar la aplicacion.".formatted(edadMinima));

        var profile = new UserProfile(command.userId(), command);
        return Optional.of(userProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public Optional<UserProfile> handle(UpdateUserProfileCommand command) {
        var profile = userProfileRepository.findById(command.userId())
                .orElseThrow(() -> new UserProfileNotFoundException(command.userId()));
        // Regla del estudio: desde el primer plan, el perfil no cambia.
        if (externalPlanningService.userHasAnyPlan(command.userId()))
            throw new UserProfileLockedException(command.userId());

        profile.update(command);
        return Optional.of(userProfileRepository.save(profile));
    }
}
