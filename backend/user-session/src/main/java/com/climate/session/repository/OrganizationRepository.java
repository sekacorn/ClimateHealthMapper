package com.climate.session.repository;

import com.climate.session.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findBySlug(String slug);

    Optional<Organization> findByDomain(String domain);

    boolean existsBySlug(String slug);

    boolean existsByDomain(String domain);
}
