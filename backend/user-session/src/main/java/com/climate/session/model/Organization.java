package com.climate.session.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Organization entity for enterprise customers with SSO capabilities.
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_domain", columnList = "domain"),
    @Index(name = "idx_org_slug", columnList = "slug")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @Column(unique = true, nullable = false, length = 255)
    private String domain;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "sso_enabled")
    @Builder.Default
    private Boolean ssoEnabled = false;

    @Column(name = "sso_provider", length = 50)
    private String ssoProvider;

    @Column(name = "sso_entity_id", length = 500)
    private String ssoEntityId;

    @Column(name = "sso_metadata_url", length = 500)
    private String ssoMetadataUrl;

    @Column(name = "sso_acs_url", length = 500)
    private String ssoAcsUrl;

    @Column(name = "sso_certificate", columnDefinition = "TEXT")
    private String ssoCertificate;

    @Column(name = "require_mfa")
    @Builder.Default
    private Boolean requireMfa = false;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "subscription_tier", length = 50)
    @Builder.Default
    private String subscriptionTier = "FREE";

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean canAddUser() {
        if (maxUsers == null) {
            return true;
        }
        return users.size() < maxUsers;
    }

    public boolean isSubscriptionActive() {
        if (subscriptionExpiresAt == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(subscriptionExpiresAt);
    }
}
