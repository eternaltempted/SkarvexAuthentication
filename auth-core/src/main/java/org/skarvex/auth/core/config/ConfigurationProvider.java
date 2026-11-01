package org.skarvex.auth.core.config;

public interface ConfigurationProvider {
    String getString(String path);
    int getInt(String path);
}
