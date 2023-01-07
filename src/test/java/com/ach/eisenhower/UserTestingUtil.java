package com.ach.eisenhower;

import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.services.UserTokenParserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

@TestComponent
public class UserTestingUtil {
    private final EisenhowerUserRepository userRepository;
    private final UserTokenParserService userTokenParserService;
    private final PasswordEncoder passwordEncoder;

    public UserTestingUtil(EisenhowerUserRepository userRepository, UserTokenParserService userTokenParserService) {
        this.userRepository = userRepository;
        this.userTokenParserService = userTokenParserService;
        passwordEncoder = new BCryptPasswordEncoder();
    }

    public EisenhowerUserEntity createTestUser(String email, String password) {
        return userRepository.createUser(
                 email,
                passwordEncoder.encode(password),
                new ArrayList<>()
        );
    }

    public String getAccessTokenForUser(EisenhowerUserEntity user) {
        return userTokenParserService.createToken(user.getUsername(), user.getRoles());
    }

    public EisenhowerUserEntity getValidUser() {
        return createTestUser(RandomStringUtils.randomAlphabetic(8) + "@test.com", RandomStringUtils.randomAlphabetic(8));
    }

    public String getValidAccessToken() {
        return getAccessTokenForUser(getValidUser());
    }
}
