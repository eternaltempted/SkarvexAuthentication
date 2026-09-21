package org.skarvex.auth.core.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.skarvex.auth.core.result.EmailBindResult;
import org.skarvex.auth.core.model.EmailTemplate;
import org.skarvex.auth.core.model.PendingEmailRegistration;
import org.skarvex.auth.core.model.User;
import org.skarvex.auth.core.repository.UserRepository;
import org.skarvex.auth.core.security.PasswordHasher;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final SessionService sessionService;
    private final EmailService emailService;

    private final Cache<UUID, PendingEmailRegistration> pendingEmailCache;

    public AuthService(
            UserRepository users,
            PasswordHasher hasher,
            SessionService sessions,
            EmailService emailService
    ) {
        this.userRepository = users;
        this.passwordHasher = hasher;
        this.sessionService = sessions;
        this.emailService = emailService;
        this.pendingEmailCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(10_000)
                .build();
    }

    public boolean register(UUID uuid, String password, String registrationIp, String loginIp, boolean rememberSession) {
        if (userRepository.findById(uuid).isPresent()) {
            return false;
        }

        userRepository.save(
                new User(uuid,
                        passwordHasher.hash(password),
                        registrationIp,
                        loginIp,
                        rememberSession,
                        null,
                        false
                )
        );

        sessionService.login(uuid);

        return true;
    }
    public boolean authenticate(UUID uuid, String password, String incomingIp) {
        return userRepository.findById(uuid)
                .filter(user ->
                        passwordHasher.verify(password, user.passwordHash())
        ).map(user -> {
            sessionService.login(uuid);

            userRepository.setAutoLogin(uuid, true);
            userRepository.updateLastLoginIp(uuid, incomingIp);
            return true;
                }).orElse(false);
    }

    public void logout(UUID uuid) {
        sessionService.logout(uuid);
    }
    public void revokeSession(UUID uuid) {
        sessionService.logout(uuid);
        userRepository.setAutoLogin(uuid, false);
    }

    public boolean isRegistered(UUID uuid) {
        return userRepository.findById(uuid).isPresent();
    }
    public boolean isAuthenticated(UUID uuid) {
        return sessionService.isAuthenticated(uuid);
    }

    public boolean comparePassword(UUID uuid, String password) {
        return userRepository.findById(uuid)
                .map(user ->
                        passwordHasher.verify(password, user.passwordHash())
                )
                .orElse(false);
    }
    public boolean updatePassword(UUID uuid, String newPassword) {
        return userRepository.updatePassword(
                uuid,
                passwordHasher.hash(newPassword)
        );
    }

    public boolean tryAutoLogin(UUID uuid, String incomingIp) {
        return userRepository.findById(uuid).filter(User::autoLogin)
                .filter(user -> {
                    boolean trustedSession = incomingIp.equals(user.lastLoginIp());

                    if (!trustedSession) {
                        userRepository.setAutoLogin(uuid, false);
                    }

                    return trustedSession;
                }).map(user -> {
                    sessionService.login(uuid);
                    return true;
        }).orElse(false);
    }

    public Optional<String> getEmail(UUID uuid) {
        return userRepository.findById(uuid)
                .map(User::email)
                .filter(email -> !email.isBlank());
    }

    public EmailBindResult requestEmailBinding(UUID uuid, String email, EmailTemplate template) {
        Optional<User> user = userRepository.findById(uuid);

        if (user.map(User::isEmailVerified).orElse(false)) {
            return EmailBindResult.ALREADY_BOUND;
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        if (pendingEmailCache.asMap().putIfAbsent(uuid, new PendingEmailRegistration(email, code)) != null) {
            return EmailBindResult.AWAITING_CONFIRMATION;
        }

        return emailService.sendConfirmationCode(code, template);
    }

    public EmailBindResult confirmEmailBinding(UUID uuid, String code) {
        if (code == null) {
            return EmailBindResult.INVALID_CODE;
        }

        PendingEmailRegistration pendingRequest = pendingEmailCache.getIfPresent(uuid);

        if (pendingRequest == null) {
            return EmailBindResult.NO_PENDING_REQUEST;
        }

        if (!pendingRequest.code().equals(code)) {
            return EmailBindResult.INVALID_CODE;
        }

        userRepository.updateEmail(uuid, pendingRequest.email());
        return EmailBindResult.SUCCESS;
    }

    public EmailBindResult unbindEmail(UUID uuid) {
        Optional<User> userOpt = userRepository.findById(uuid);

        if (!userOpt.map(User::isEmailVerified).orElse(false)) {
            return EmailBindResult.NOT_BOUND;
        }

        userRepository.updateEmail(uuid, null);
        return EmailBindResult.SUCCESS;
    }
}
