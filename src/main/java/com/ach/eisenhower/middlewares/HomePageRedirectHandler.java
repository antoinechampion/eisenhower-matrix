package com.ach.eisenhower.middlewares;

import com.ach.eisenhower.services.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Redirects a user which is already authenticated from the home page to its matrix board.
 */
public class HomePageRedirectHandler implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var user = UserService.getContextUser(SecurityContextHolder.getContext());
        if (user != null) {
            response.sendRedirect("/matrix");
        }

        return true;
    }
}