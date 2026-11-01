package org.skarvex.auth.core.database;

public interface DatabaseProvider {
    void connect();
    void disconnect();
    boolean isConnected();
}
