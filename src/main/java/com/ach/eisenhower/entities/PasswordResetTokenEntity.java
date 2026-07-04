package com.ach.eisenhower.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

/**
 * A single-use, time-limited token allowing a user to reset a forgotten password.
 * Only the SHA-256 hash of the token is stored; the raw token lives solely in the emailed link.
 */
@Entity
@Table(name = "password_reset_tokens")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class PasswordResetTokenEntity {
    @Getter @Setter
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Getter @Setter
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @OneToOne(targetEntity = EisenhowerUserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    @Getter @Setter
    private EisenhowerUserEntity user;

    @Getter @Setter
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;
}
