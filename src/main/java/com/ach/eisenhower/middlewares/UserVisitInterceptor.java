package com.ach.eisenhower.middlewares;

import com.ach.eisenhower.entities.EisenhowerUserEntity;
import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.services.UserService;
import com.ach.eisenhower.services.UserVisitCountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

/**
 * Increments the number of visits for a user and update its last visit date whenever he goes to the matrix page
 */
public class UserVisitInterceptor implements HandlerInterceptor {

    private final EisenhowerUserRepository userRepository;
    private final UserVisitCountService userVisitCountService = new UserVisitCountService();

    public UserVisitInterceptor(EisenhowerUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        var user = UserService.getContextUser(SecurityContextHolder.getContext());
        if (user != null) {
            user.lastLoginDate = new Date();
            userVisitCountService.deserialize(user.numberOfVisitsJson);
            userVisitCountService.incrementVisitsCount(LocalDate.now());
            userVisitCountService.serialize().ifPresent(json -> user.numberOfVisitsJson = json);
            userRepository.save(user);
        }

        return true;
    }
}
