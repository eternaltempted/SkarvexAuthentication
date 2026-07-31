package org.skarvex.auth.core.model;

public record PendingEmailRegistration(String email, String code) {
}
