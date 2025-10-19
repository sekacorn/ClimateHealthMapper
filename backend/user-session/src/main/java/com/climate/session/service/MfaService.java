package com.climate.session.service;

import com.climate.session.model.MfaSession;
import com.climate.session.model.User;
import com.climate.session.repository.MfaSessionRepository;
import com.climate.session.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * MFA Service implementing TOTP (Time-based One-Time Password) authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private static final String ISSUER = "ClimateHealthMapper";
    private static final int SECRET_SIZE = 20;
    private static final int BACKUP_CODES_COUNT = 10;
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP = 30; // seconds

    private final UserRepository userRepository;
    private final MfaSessionRepository mfaSessionRepository;

    /**
     * Generate MFA secret for user.
     */
    public String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        return new Base32().encodeToString(bytes);
    }

    /**
     * Generate backup codes for MFA.
     */
    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            int code = random.nextInt(900000) + 100000; // 6-digit code
            codes.add(String.valueOf(code));
        }

        return codes;
    }

    /**
     * Generate QR code URL for authenticator apps.
     */
    public String generateQrCodeUrl(String username, String secret) {
        try {
            String data = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                URLEncoder.encode(ISSUER, StandardCharsets.UTF_8),
                URLEncoder.encode(username, StandardCharsets.UTF_8),
                secret,
                URLEncoder.encode(ISSUER, StandardCharsets.UTF_8)
            );
            return data;
        } catch (Exception e) {
            log.error("Error generating QR code URL", e);
            throw new RuntimeException("Failed to generate QR code URL");
        }
    }

    /**
     * Generate QR code image as Base64.
     */
    public String generateQrCodeImage(String qrCodeUrl) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                qrCodeUrl,
                BarcodeFormat.QR_CODE,
                300,
                300
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("Error generating QR code image", e);
            throw new RuntimeException("Failed to generate QR code image");
        }
    }

    /**
     * Verify TOTP code.
     */
    public boolean verifyTotpCode(String secret, String code) {
        try {
            long currentTime = System.currentTimeMillis() / 1000L;
            long timeWindow = currentTime / TIME_STEP;

            // Check current time window and adjacent windows (±1) for clock skew
            for (int i = -1; i <= 1; i++) {
                String generatedCode = generateTotpCode(secret, timeWindow + i);
                if (generatedCode.equals(code)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }

    /**
     * Generate TOTP code for given time window.
     */
    private String generateTotpCode(String secret, long timeWindow) throws Exception {
        byte[] key = new Base32().decode(secret);
        byte[] data = new byte[8];

        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (timeWindow & 0xff);
            timeWindow >>= 8;
        }

        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xf;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

        int otp = binary % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }

    /**
     * Enable MFA for user.
     */
    @Transactional
    public void enableMfa(User user, String secret, List<String> backupCodes) {
        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        user.setBackupCodes(String.join(",", backupCodes));
        userRepository.save(user);

        log.info("MFA enabled for user: {}", user.getUsername());
    }

    /**
     * Disable MFA for user.
     */
    @Transactional
    public void disableMfa(User user) {
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setBackupCodes(null);
        userRepository.save(user);

        log.info("MFA disabled for user: {}", user.getUsername());
    }

    /**
     * Create MFA session for login flow.
     */
    @Transactional
    public MfaSession createMfaSession(User user, String ipAddress, String userAgent) {
        MfaSession session = MfaSession.builder()
                .user(user)
                .sessionToken(UUID.randomUUID().toString())
                .isVerified(false)
                .attempts(0)
                .maxAttempts(3)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return mfaSessionRepository.save(session);
    }

    /**
     * Verify MFA session with code.
     */
    @Transactional
    public boolean verifyMfaSession(String sessionToken, String code) {
        MfaSession session = mfaSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Invalid MFA session"));

        if (!session.canAttempt()) {
            throw new RuntimeException("MFA session expired or max attempts reached");
        }

        User user = session.getUser();
        boolean isValid = false;

        // Check TOTP code
        if (verifyTotpCode(user.getMfaSecret(), code)) {
            isValid = true;
        }
        // Check backup codes
        else if (user.getBackupCodes() != null && !user.getBackupCodes().isEmpty()) {
            String[] backupCodes = user.getBackupCodes().split(",");
            for (int i = 0; i < backupCodes.length; i++) {
                if (backupCodes[i].equals(code)) {
                    isValid = true;
                    // Remove used backup code
                    backupCodes[i] = "";
                    user.setBackupCodes(String.join(",", backupCodes));
                    userRepository.save(user);
                    break;
                }
            }
        }

        if (isValid) {
            session.setIsVerified(true);
            session.setVerifiedAt(LocalDateTime.now());
            mfaSessionRepository.save(session);
            log.info("MFA verified for user: {}", user.getUsername());
            return true;
        } else {
            session.incrementAttempts();
            mfaSessionRepository.save(session);
            log.warn("Invalid MFA code for user: {}", user.getUsername());
            return false;
        }
    }

    /**
     * Clean up expired MFA sessions.
     */
    @Transactional
    public void cleanupExpiredSessions() {
        mfaSessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }
}
