package com.climate.session.repository;

import com.climate.session.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findBySsoProviderAndSsoId(String ssoProvider, String ssoId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByOrganizationId(Long organizationId);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.deletedAt IS NULL")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.accountLockedUntil < :now AND u.accountLockedUntil IS NOT NULL")
    List<User> findUsersWithExpiredLocks(LocalDateTime now);
}
