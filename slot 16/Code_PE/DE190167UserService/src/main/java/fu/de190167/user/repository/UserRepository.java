package fu.de190167.user.repository;

import fu.de190167.user.entity.Role;
import fu.de190167.user.entity.User;
import fu.de190167.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByUsernameAndUserIdNot(String username, Long userId);

    // Null-safe filter: a null param is ignored
    @Query("""
            SELECT u FROM User u
            WHERE (:role   IS NULL OR u.role   = :role)
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> search(@Param("role") Role role,
                      @Param("status") UserStatus status,
                      Pageable pageable);
}
