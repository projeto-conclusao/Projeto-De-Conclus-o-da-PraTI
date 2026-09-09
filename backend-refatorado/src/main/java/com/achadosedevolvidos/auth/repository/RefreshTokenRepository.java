package com.achadosedevolvidos.auth.repository;

import com.achadosedevolvidos.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revokedAt = CURRENT_TIMESTAMP where r.user.id = :userId and r.revokedAt is null")
    void revokeAllByUserId(@Param("userId") UUID userId);
}
