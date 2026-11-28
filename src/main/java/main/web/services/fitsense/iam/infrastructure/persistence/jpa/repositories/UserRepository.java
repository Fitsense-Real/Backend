package main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories;

import main.web.services.fitsense.iam.domain.model.aggregates.User;
import main.web.services.fitsense.iam.domain.model.valueobjects.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);

    /** Participantes activos: los unicos que reciben planes en el cierre semanal. */
    @Query("""
           SELECT u FROM User u
           WHERE u.status = main.web.services.fitsense.iam.domain.model.valueobjects.UserStatus.ACTIVE
             AND u.enrollment.studyParticipant = true
             AND u.enrollment.withdrawnAt IS NULL
             AND u.timezone.value = :zone
           """)
    List<User> findActiveParticipantsInTimezone(String zone);

    /** B2.4, opcion B: todas las cuentas activas de una zona (participen o no en el estudio). */
    @Query("""
           SELECT u FROM User u
           WHERE u.status = main.web.services.fitsense.iam.domain.model.valueobjects.UserStatus.ACTIVE
             AND u.timezone.value = :zone
           """)
    List<User> findActiveUsersInTimezone(String zone);

    @Query("""
           SELECT DISTINCT u.timezone.value FROM User u
           WHERE u.status = main.web.services.fitsense.iam.domain.model.valueobjects.UserStatus.ACTIVE
           """)
    List<String> findDistinctActiveTimezones();

    @Query("SELECT COUNT(u) FROM User u WHERE u.enrollment.studyParticipant = true")
    long countStudyParticipants();

    /** V60: siguiente numero de participante; nunca se reutiliza, aunque se eliminen cuentas. */
    @Query(value = "SELECT nextval('participant_code_seq')", nativeQuery = true)
    long nextParticipantSequence();
}
