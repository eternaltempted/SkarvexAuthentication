package org.skarvex.auth.core.database.repository;

import org.skarvex.auth.core.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID uuid);

    void save(User user);

    boolean updatePassword(UUID uuid, String passwordHash);

    boolean updateLastLoginIp(UUID uuid, String ip);

    boolean setAutoLogin(UUID uuid, boolean enabled);

    boolean updateEmail(UUID uuid, String email);

    boolean isEmailLinked(String email);

}
