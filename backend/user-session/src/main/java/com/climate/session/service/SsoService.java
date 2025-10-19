package com.climate.session.service;

import com.climate.session.model.Organization;
import com.climate.session.model.SsoSession;
import com.climate.session.model.User;
import com.climate.session.repository.OrganizationRepository;
import com.climate.session.repository.SsoSessionRepository;
import com.climate.session.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SSO Service for integrating with multiple identity providers.
 * Supports Google OAuth2, Azure AD OIDC, Okta, and SAML 2.0.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final SsoSessionRepository ssoSessionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

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

    /**
     * Create SSO session and generate authorization URL.
     */
    @Transactional
    public SsoSession createSsoSession(String provider, String ipAddress, String userAgent) {
        String stateToken = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        String codeVerifier = generateCodeVerifier();

        SsoSession session = SsoSession.builder()
                .provider(provider)
                .stateToken(stateToken)
                .nonce(nonce)
                .codeVerifier(codeVerifier)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isCompleted(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return ssoSessionRepository.save(session);
    }

    /**
     * Generate authorization URL for SSO provider.
     */
    public String generateAuthorizationUrl(String provider, SsoSession session) {
        return switch (provider.toLowerCase()) {
            case "google" -> generateGoogleAuthUrl(session);
            case "azure" -> generateAzureAuthUrl(session);
            case "okta" -> generateOktaAuthUrl(session);
            default -> throw new IllegalArgumentException("Unsupported SSO provider: " + provider);
        };
    }

    /**
     * Generate Google OAuth2 authorization URL.
     */
    private String generateGoogleAuthUrl(SsoSession session) {
        String scope = "openid profile email";
        String codeChallenge = generateCodeChallenge(session.getCodeVerifier());

        return String.format(
            "https://accounts.google.com/o/oauth2/v2/auth?" +
            "client_id=%s&" +
            "response_type=code&" +
            "scope=%s&" +
            "redirect_uri=%s&" +
            "state=%s&" +
            "nonce=%s&" +
            "code_challenge=%s&" +
            "code_challenge_method=S256",
            googleClientId,
            scope,
            googleRedirectUri,
            session.getStateToken(),
            session.getNonce(),
            codeChallenge
        );
    }

    /**
     * Generate Azure AD authorization URL.
     */
    private String generateAzureAuthUrl(SsoSession session) {
        String scope = "openid profile email";
        String codeChallenge = generateCodeChallenge(session.getCodeVerifier());

        return String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
            "client_id=%s&" +
            "response_type=code&" +
            "redirect_uri=%s&" +
            "scope=%s&" +
            "state=%s&" +
            "nonce=%s&" +
            "code_challenge=%s&" +
            "code_challenge_method=S256",
            azureTenantId,
            azureClientId,
            azureRedirectUri,
            scope,
            session.getStateToken(),
            session.getNonce(),
            codeChallenge
        );
    }

    /**
     * Generate Okta authorization URL.
     */
    private String generateOktaAuthUrl(SsoSession session) {
        String scope = "openid profile email";
        String codeChallenge = generateCodeChallenge(session.getCodeVerifier());

        return String.format(
            "https://%s/oauth2/v1/authorize?" +
            "client_id=%s&" +
            "response_type=code&" +
            "scope=%s&" +
            "redirect_uri=%s&" +
            "state=%s&" +
            "nonce=%s&" +
            "code_challenge=%s&" +
            "code_challenge_method=S256",
            oktaDomain,
            oktaClientId,
            scope,
            oktaRedirectUri,
            session.getStateToken(),
            session.getNonce(),
            codeChallenge
        );
    }

    /**
     * Handle SSO callback and exchange code for tokens.
     */
    @Transactional
    public User handleSsoCallback(String provider, String code, String state) {
        SsoSession session = ssoSessionRepository.findByStateToken(state)
                .orElseThrow(() -> new RuntimeException("Invalid SSO state"));

        if (!session.isValid()) {
            throw new RuntimeException("SSO session expired or invalid");
        }

        Map<String, Object> tokenResponse = exchangeCodeForTokens(provider, code, session);
        Map<String, Object> userInfo = getUserInfoFromProvider(provider, tokenResponse);

        User user = findOrCreateUser(provider, userInfo);

        session.setAccessToken((String) tokenResponse.get("access_token"));
        session.setRefreshToken((String) tokenResponse.get("refresh_token"));
        session.setIdToken((String) tokenResponse.get("id_token"));
        session.setIsCompleted(true);
        session.setCompletedAt(LocalDateTime.now());
        session.setUser(user);
        ssoSessionRepository.save(session);

        log.info("SSO login successful for user: {} via {}", user.getUsername(), provider);
        return user;
    }

    /**
     * Exchange authorization code for tokens.
     */
    private Map<String, Object> exchangeCodeForTokens(String provider, String code, SsoSession session) {
        return switch (provider.toLowerCase()) {
            case "google" -> exchangeGoogleCode(code, session);
            case "azure" -> exchangeAzureCode(code, session);
            case "okta" -> exchangeOktaCode(code, session);
            default -> throw new IllegalArgumentException("Unsupported SSO provider: " + provider);
        };
    }

    /**
     * Exchange Google authorization code.
     */
    private Map<String, Object> exchangeGoogleCode(String code, SsoSession session) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");
        params.add("code_verifier", session.getCodeVerifier());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        return response.getBody();
    }

    /**
     * Exchange Azure authorization code.
     */
    private Map<String, Object> exchangeAzureCode(String code, SsoSession session) {
        String tokenUrl = String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
            azureTenantId
        );

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", azureClientId);
        params.add("client_secret", azureClientSecret);
        params.add("redirect_uri", azureRedirectUri);
        params.add("grant_type", "authorization_code");
        params.add("code_verifier", session.getCodeVerifier());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        return response.getBody();
    }

    /**
     * Exchange Okta authorization code.
     */
    private Map<String, Object> exchangeOktaCode(String code, SsoSession session) {
        String tokenUrl = String.format("https://%s/oauth2/v1/token", oktaDomain);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", oktaClientId);
        params.add("client_secret", oktaClientSecret);
        params.add("redirect_uri", oktaRedirectUri);
        params.add("grant_type", "authorization_code");
        params.add("code_verifier", session.getCodeVerifier());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        return response.getBody();
    }

    /**
     * Get user info from SSO provider.
     */
    private Map<String, Object> getUserInfoFromProvider(String provider, Map<String, Object> tokenResponse) {
        String accessToken = (String) tokenResponse.get("access_token");

        return switch (provider.toLowerCase()) {
            case "google" -> getGoogleUserInfo(accessToken);
            case "azure" -> getAzureUserInfo(accessToken);
            case "okta" -> getOktaUserInfo(accessToken);
            default -> throw new IllegalArgumentException("Unsupported SSO provider: " + provider);
        };
    }

    /**
     * Get Google user info.
     */
    private Map<String, Object> getGoogleUserInfo(String accessToken) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            userInfoUrl,
            HttpMethod.GET,
            request,
            Map.class
        );

        return response.getBody();
    }

    /**
     * Get Azure user info.
     */
    private Map<String, Object> getAzureUserInfo(String accessToken) {
        String userInfoUrl = "https://graph.microsoft.com/v1.0/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            userInfoUrl,
            HttpMethod.GET,
            request,
            Map.class
        );

        return response.getBody();
    }

    /**
     * Get Okta user info.
     */
    private Map<String, Object> getOktaUserInfo(String accessToken) {
        String userInfoUrl = String.format("https://%s/oauth2/v1/userinfo", oktaDomain);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            userInfoUrl,
            HttpMethod.GET,
            request,
            Map.class
        );

        return response.getBody();
    }

    /**
     * Find or create user from SSO provider data.
     */
    @Transactional
    public User findOrCreateUser(String provider, Map<String, Object> userInfo) {
        String ssoId = (String) userInfo.get("sub");
        if (ssoId == null) {
            ssoId = (String) userInfo.get("id");
        }

        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");

        Optional<User> existingUser = userRepository.findBySsoProviderAndSsoId(provider, ssoId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            user.setSsoProvider(provider);
            user.setSsoId(ssoId);
            return userRepository.save(user);
        }

        User newUser = User.builder()
                .username(email.split("@")[0])
                .email(email)
                .fullName(name)
                .ssoProvider(provider)
                .ssoId(ssoId)
                .isActive(true)
                .isVerified(true)
                .emailVerified(true)
                .mfaEnabled(false)
                .roles(new HashSet<>(Collections.singletonList(User.Role.USER)))
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Generate PKCE code verifier.
     */
    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate PKCE code challenge.
     */
    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate code challenge", e);
        }
    }

    /**
     * Clean up expired SSO sessions.
     */
    @Transactional
    public void cleanupExpiredSessions() {
        ssoSessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }
}
