package com.climate.session.service;

import com.climate.session.dto.AuthRequest;
import com.climate.session.dto.AuthResponse;
import com.climate.session.dto.RegisterRequest;
import com.climate.session.model.User;
import com.climate.session.repository.OrganizationRepository;
import com.climate.session.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Auth Service.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private MfaService mfaService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private String testPassword = "TestPass123!";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash(BCrypt.hashpw(testPassword, BCrypt.gensalt()))
                .fullName("Test User")
                .roles(new HashSet<>())
                .mfaEnabled(false)
                .isActive(true)
                .failedLoginAttempts(0)
                .build();
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("NewPass123!")
                .fullName("New User")
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(auditService).logUserRegistration(any(User.class));

        User result = authService.register(request);

        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditService, times(1)).logUserRegistration(any(User.class));
    }

    @Test
    void testRegister_UsernameExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("existinguser")
                .email("newuser@example.com")
                .password("NewPass123!")
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_EmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("NewPass123!")
                .build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            authService.register(request);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        AuthRequest request = AuthRequest.builder()
                .usernameOrEmail("testuser")
                .password(testPassword)
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        doNothing().when(auditService).logSuccessfulLogin(any(User.class), anyString());

        AuthResponse response = authService.login(request, "127.0.0.1", "Mozilla");

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertFalse(response.isRequiresMfa());
        verify(auditService, times(1)).logSuccessfulLogin(any(User.class), anyString());
    }

    @Test
    void testLogin_InvalidCredentials() {
        AuthRequest request = AuthRequest.builder()
                .usernameOrEmail("testuser")
                .password("WrongPassword")
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(auditService).logFailedLogin(any(User.class), anyString());

        assertThrows(RuntimeException.class, () -> {
            authService.login(request, "127.0.0.1", "Mozilla");
        });

        verify(auditService, times(1)).logFailedLogin(any(User.class), anyString());
    }

    @Test
    void testLogin_UserNotFound() {
        AuthRequest request = AuthRequest.builder()
                .usernameOrEmail("nonexistent")
                .password(testPassword)
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.login(request, "127.0.0.1", "Mozilla");
        });
    }

    @Test
    void testLogin_AccountInactive() {
        testUser.setIsActive(false);

        AuthRequest request = AuthRequest.builder()
                .usernameOrEmail("testuser")
                .password(testPassword)
                .build();

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> {
            authService.login(request, "127.0.0.1", "Mozilla");
        });
    }

    @Test
    void testHashPassword() {
        String password = "TestPassword123!";
        String hash = authService.hashPassword(password);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$"));
        assertTrue(BCrypt.checkpw(password, hash));
    }

    @Test
    void testVerifyPassword_Success() {
        String password = "TestPassword123!";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        boolean result = authService.verifyPassword(password, hash);

        assertTrue(result);
    }

    @Test
    void testVerifyPassword_Failure() {
        String password = "TestPassword123!";
        String wrongPassword = "WrongPassword123!";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        boolean result = authService.verifyPassword(wrongPassword, hash);

        assertFalse(result);
    }

    @Test
    void testRefreshToken_Success() {
        String refreshToken = "valid-refresh-token";

        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtService.extractUserId(refreshToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("new-refresh-token");

        AuthResponse response = authService.refreshToken(refreshToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }

    @Test
    void testRefreshToken_InvalidToken() {
        String refreshToken = "invalid-token";

        when(jwtService.validateToken(refreshToken)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.refreshToken(refreshToken);
        });
    }
}
