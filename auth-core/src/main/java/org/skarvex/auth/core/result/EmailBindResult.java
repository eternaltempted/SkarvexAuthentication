package org.skarvex.auth.core.result;

public enum EmailBindResult {
    SUCCESS,
    INVALID_CODE,
    NO_PENDING_REQUEST,
    ALREADY_BOUND,
    AWAITING_CONFIRMATION,
    NOT_BOUND,
    IDLE
}
