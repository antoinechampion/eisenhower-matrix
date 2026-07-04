package com.ach.eisenhower.repositories;

import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.ach.eisenhower.entities.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    void deleteByUser(EisenhowerUserEntity user);
}
