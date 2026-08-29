package com.caioamorimr.ordermanagement.services;

import com.caioamorimr.ordermanagement.entities.RefreshToken;
import com.caioamorimr.ordermanagement.entities.User;
import com.caioamorimr.ordermanagement.repositories.RefreshTokenRepository;
import com.caioamorimr.ordermanagement.services.exceptions.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 7L);
        user = new User(1L, "Caio", "caio@email.com", "988888888", "hashed_password");
    }

    @Test
    @DisplayName("issue should persist a hashed token (never the raw value) and return the raw token to the caller")
    void issue_shouldPersistHashedTokenAndReturnRawToken() {
        String rawToken = refreshTokenService.issue(user);

        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken persisted = captor.getValue();
        assertThat(persisted.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(persisted.getUser()).isEqualTo(user);
        assertThat(persisted.isRevoked()).isFalse();
        assertThat(persisted.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("issue should generate a different token on every call")
    void issue_shouldGenerateUniqueTokensAcrossCalls() {
        String first = refreshTokenService.issue(user);
        String second = refreshTokenService.issue(user);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("rotate should revoke the old token and issue a new one when the token is valid")
    void rotate_shouldRevokeOldTokenAndIssueNewOne_whenTokenIsValid() {
        RefreshToken stored = new RefreshToken("some-hash", user, Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        RefreshTokenService.RotationResult result = refreshTokenService.rotate("raw-token-value");

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.newRefreshToken()).isNotBlank();
        assertThat(stored.isRevoked()).isTrue();
        //once to revoke the old entity, once more inside issue() for the new one
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotate should throw InvalidRefreshTokenException and not persist anything when the token does not exist")
    void rotate_shouldThrow_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("rotate should throw InvalidRefreshTokenException when the token is expired")
    void rotate_shouldThrow_whenTokenIsExpired() {
        RefreshToken expired = new RefreshToken("some-hash", user, Instant.now().minus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate("raw-token-value"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("rotate should throw InvalidRefreshTokenException when the token was already revoked")
    void rotate_shouldThrow_whenTokenIsRevoked() {
        RefreshToken revoked = new RefreshToken("some-hash", user, Instant.now().plus(1, ChronoUnit.DAYS));
        revoked.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotate("raw-token-value"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revoke should mark an existing token as revoked")
    void revoke_shouldMarkTokenAsRevoked_whenTokenExists() {
        RefreshToken stored = new RefreshToken("some-hash", user, Instant.now().plus(1, ChronoUnit.DAYS));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        refreshTokenService.revoke("raw-token-value");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    @DisplayName("revoke should be a silent no-op when the token does not exist")
    void revoke_shouldNoOp_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        refreshTokenService.revoke("unknown-token");

        verify(refreshTokenRepository, never()).save(any());
    }
}