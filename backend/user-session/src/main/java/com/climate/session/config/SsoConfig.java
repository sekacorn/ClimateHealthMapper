package com.climate.session.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * SSO Configuration for OAuth2 and SAML providers.
 */
@Configuration
public class SsoConfig {

    @Value("${sso.google.client-id:}")
    private String googleClientId;

    @Value("${sso.google.client-secret:}")
    private String googleClientSecret;

    @Value("${sso.google.redirect-uri:}")
    private String googleRedirectUri;

    @Value("${sso.azure.client-id:}")
    private String azureClientId;

    @Value("${sso.azure.client-secret:}")
    private String azureClientSecret;

    @Value("${sso.azure.tenant-id:}")
    private String azureTenantId;

    @Value("${sso.azure.redirect-uri:}")
    private String azureRedirectUri;

    @Value("${sso.okta.client-id:}")
    private String oktaClientId;

    @Value("${sso.okta.client-secret:}")
    private String oktaClientSecret;

    @Value("${sso.okta.domain:}")
    private String oktaDomain;

    @Value("${sso.okta.redirect-uri:}")
    private String oktaRedirectUri;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        // Google OAuth2
        if (googleClientId != null && !googleClientId.isEmpty()) {
            registrations.add(googleClientRegistration());
        }

        // Azure AD
        if (azureClientId != null && !azureClientId.isEmpty()) {
            registrations.add(azureClientRegistration());
        }

        // Okta
        if (oktaClientId != null && !oktaClientId.isEmpty()) {
            registrations.add(oktaClientRegistration());
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(googleRedirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
                .userNameAttributeName("email")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();
    }

    private ClientRegistration azureClientRegistration() {
        return ClientRegistration.withRegistrationId("azure")
                .clientId(azureClientId)
                .clientSecret(azureClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(azureRedirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri(String.format(
                        "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize",
                        azureTenantId))
                .tokenUri(String.format(
                        "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
                        azureTenantId))
                .userInfoUri("https://graph.microsoft.com/v1.0/me")
                .userNameAttributeName("userPrincipalName")
                .jwkSetUri(String.format(
                        "https://login.microsoftonline.com/%s/discovery/v2.0/keys",
                        azureTenantId))
                .clientName("Azure AD")
                .build();
    }

    private ClientRegistration oktaClientRegistration() {
        return ClientRegistration.withRegistrationId("okta")
                .clientId(oktaClientId)
                .clientSecret(oktaClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(oktaRedirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri(String.format("https://%s/oauth2/v1/authorize", oktaDomain))
                .tokenUri(String.format("https://%s/oauth2/v1/token", oktaDomain))
                .userInfoUri(String.format("https://%s/oauth2/v1/userinfo", oktaDomain))
                .userNameAttributeName("email")
                .jwkSetUri(String.format("https://%s/oauth2/v1/keys", oktaDomain))
                .clientName("Okta")
                .build();
    }
}
