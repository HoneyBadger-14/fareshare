package com.fareshare.service;

import com.fareshare.model.Model;
import com.fareshare.controller.ApiError;
import com.fareshare.repository.Users;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface Identity {
    Model.User current(HttpServletRequest request);
}

@Service
@Profile("local")
class LocalIdentity implements Identity {
    private final Users users;
    LocalIdentity(Users users) { this.users = users; }

    @Override @Transactional
    public Model.User current(HttpServletRequest request) {
        String email = request.getHeader("X-Dev-Email");
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ApiError(HttpStatus.UNAUTHORIZED, "Set a valid X-Dev-Email header in local mode");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return users.findByEmail(normalized).orElseGet(() -> {
            String name = request.getHeader("X-Dev-Name");
            if (name == null || name.isBlank()) name = normalized.substring(0, normalized.indexOf('@'));
            return users.save(new Model.User(UUID.randomUUID(), normalized, name.trim()));
        });
    }
}

@Service
@Profile("!local")
class PendingIdentity implements Identity {
    @Override
    public Model.User current(HttpServletRequest request) {
        throw new ApiError(HttpStatus.SERVICE_UNAVAILABLE, "Authentication provider is not configured");
    }
}
