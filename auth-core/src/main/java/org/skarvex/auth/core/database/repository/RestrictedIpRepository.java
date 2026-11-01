package org.skarvex.auth.core.database.repository;

import org.skarvex.auth.core.model.RestrictedIp;

import java.util.Optional;

public interface RestrictedIpRepository {

    void block(RestrictedIp restrictedIp);

    Optional<RestrictedIp> findByIp(String ip);

    boolean unblock(String ip);
}
