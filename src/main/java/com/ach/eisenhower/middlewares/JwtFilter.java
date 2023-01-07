package com.ach.eisenhower.middlewares;

import com.ach.eisenhower.services.UserTokenParserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;
import java.io.IOException;

/**
 * Injects the authentication principal from the request to the security context
 */
public class JwtFilter extends GenericFilterBean {
    private final UserTokenParserService userTokenParserService;

    public JwtFilter(UserTokenParserService jwtTokenProvider) {
        this.userTokenParserService = jwtTokenProvider;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
            throws ServletException, IOException {
        String token = userTokenParserService.resolveToken((HttpServletRequest) req);
        if (token != null && userTokenParserService.validateToken(token)) {
            Authentication auth = userTokenParserService.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(req, res);
    }
}