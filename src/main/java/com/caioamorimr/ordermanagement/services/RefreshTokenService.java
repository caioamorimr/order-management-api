package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.entities.RefreshToken;
import com.caioamorimr.ordermanagement.entities.User;
import com.caioamorimr.ordermanagement.repositories.RefreshTokenRepository;
import com.caioamorimr.ordermanagement.services.exceptions.InvalidRefreshTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, @Value("${api.security.refresh-token.expiration-days}") long expirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationDays = expirationDays;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateOpaqueToken();

        RefreshToken entity = new RefreshToken(hash(rawToken), user, Instant.now().plus(expirationDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken entity = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isValid)
                .orElseThrow(InvalidRefreshTokenException::new);

        entity.setRevoked(true);
        refreshTokenRepository.save(entity);

        String newRawToken = issue(entity.getUser());
        return new RotationResult(entity.getUser(), newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(entity -> {
                    entity.setRevoked(true);
                    refreshTokenRepository.save(entity);
                });
    }

    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record RotationResult(User user, String newRefreshToken) {

    }


}
