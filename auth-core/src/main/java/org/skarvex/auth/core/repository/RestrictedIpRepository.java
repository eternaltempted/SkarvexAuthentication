package org.skarvex.auth.core.repository;

import org.skarvex.auth.core.model.RestrictedIp;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RestrictedIpRepository {

    void block(RestrictedIp restrictedIp);

    Optional<RestrictedIp> findByIp(String ip);

    void unblock(String ip);
}
