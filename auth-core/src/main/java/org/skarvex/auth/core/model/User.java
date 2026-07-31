package org.skarvex.auth.core.model;

import java.util.UUID;

public record User(UUID id,
                   String passwordHash,
                   String registrationIp,
                   String lastLoginIp,
                   boolean autoLogin,
                   String email,
                   boolean isEmailVerified) {
}
