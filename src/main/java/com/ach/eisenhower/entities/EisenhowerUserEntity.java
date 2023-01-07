package com.ach.eisenhower.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Custom RBAC user for the application
 */
@Entity
@Table(name="users")
@Builder
@NoArgsConstructor @AllArgsConstructor
public class EisenhowerUserEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter @Setter
    UUID id;

    @OneToMany(mappedBy = "id")
    private Set<BoardEntity> boards;

    @NotEmpty
    @Column(unique = true)
    @Getter @Setter
    private String email;

    @Getter @Setter
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    @Getter @Setter
    private List<String> roles = new ArrayList<>();

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Getter @Setter
    public Date registrationDate;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Getter @Setter
    public Date lastLoginDate;

    @Column(nullable = false)
    @Length(max = 8192)
    @Builder.Default
    @Getter @Setter
    public String numberOfVisitsJson = "{}";

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream().map(SimpleGrantedAuthority::new).collect(toList());
    }

    @Override
    public String getUsername() {
        return this.email;
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public boolean isEnabled() {
        return true;
    }
}