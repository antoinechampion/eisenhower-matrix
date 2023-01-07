package com.ach.eisenhower.controllers;

import com.ach.eisenhower.repositories.EisenhowerUserRepository;
import com.ach.eisenhower.services.UserService;
import com.ach.eisenhower.services.UserTokenParserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Controller for authentication and user management
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    final AuthenticationManager authenticationManager;
    final UserTokenParserService userTokenParserService;
    final EisenhowerUserRepository users;
    final PasswordEncoder encoder;

    public AuthController(AuthenticationManager authenticationManager, UserTokenParserService userTokenParserService, EisenhowerUserRepository users, PasswordEncoder encoder) {
        this.authenticationManager = authenticationManager;
        this.userTokenParserService = userTokenParserService;
        this.users = users;
        this.encoder = encoder;
    }

    /**
     * Registers a new user and authenticates him right away,
     * returning an HttpOnly cookie with an authentication token
     */
    @PostMapping("/register")
    public LoginSuccessResponse register(@RequestBody AuthenticationRequestDto data, HttpServletResponse response) {
        var existingUser = users.findByEmail(data.email);
        if (existingUser.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
        }

        var roles = new ArrayList<String>();
        String username = data.email;
        var passwordHash = encoder.encode(data.password);
        users.createUser(data.email, passwordHash, roles);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.password));
        String token = userTokenParserService.createToken(username, roles);
        var resp = new LoginSuccessResponse(username, token);
        response.addCookie(UserTokenParserService.forgeAuthenticationCookie(token));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        return resp;
    }

    /**
     * Performs log-in for a user, returning an HttpOnly cookie with an authentication token
     */
    @PostMapping("/login")
    public LoginSuccessResponse login(@RequestBody AuthenticationRequestDto data, HttpServletResponse response) {
        try {
            String username = data.email;
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.password));
            String token = userTokenParserService.createToken(username,
                    this.users.findByEmail(username)
                            .orElseThrow(() -> new UsernameNotFoundException("Username " + username + "not found"))
                            .getRoles());
            var resp = new LoginSuccessResponse(username, token);
            response.addCookie(UserTokenParserService.forgeAuthenticationCookie(token));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            return resp;
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username/password supplied");
        }
    }

    /**
     * Log-outs a user by removing its token cookie
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logout(HttpServletResponse response) {
        var cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Gets the roles for a user
     */
    @GetMapping("/user-details")
    public UserDetailsResponse currentUser() {
        var user = UserService.getContextUser(SecurityContextHolder.getContext());
        var roles = Objects.requireNonNull(user)
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(toList());
        return new UserDetailsResponse(user.getUsername(), roles);
    }

    private record AuthenticationRequestDto(String email, String password) {}

    private record LoginSuccessResponse(String email, String token) {}
    private record UserDetailsResponse(String email, List<String> roles) {}
}