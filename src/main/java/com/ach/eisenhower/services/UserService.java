package com.ach.eisenhower.services;

import com.ach.eisenhower.entities.EisenhowerUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;

import java.util.Optional;
import java.util.UUID;

public class UserService {
    public static EisenhowerUserEntity getContextUser(SecurityContext context) {
        var authentication = context.getAuthentication();
        if (authentication != null && !AnonymousAuthenticationToken.class.
                isAssignableFrom(authentication.getClass())) {
            var userOpt = (Optional<EisenhowerUserEntity>) authentication.getPrincipal();
            return userOpt.orElse(null);
        }
        return null;
    }

    public static UUID getContextUserId(SecurityContext context) {
        var user = getContextUser(context);
        if (user == null) {
            return null;
        }
        else {
            return user.getId();
        }
    }

    public static UUID getContextUserIdOrThrow(SecurityContext context) {
        var userId = getContextUserId(context);
        if (userId == null) {
            throw new EisenhowerServiceException(HttpStatus.FORBIDDEN);
        }
        return userId;
    }
}
