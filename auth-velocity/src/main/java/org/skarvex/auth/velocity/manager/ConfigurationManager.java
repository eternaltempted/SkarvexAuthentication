package org.skarvex.auth.velocity.manager;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationManager {

    private final Path dataDirectory;

    private Map<String, Object> config;

    public ConfigurationManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void load() {

        try {
            Path configPath = dataDirectory.resolve("config.yml");

            if (Files.notExists(configPath)) {
                Files.createDirectories(dataDirectory);

                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in == null) throw new IllegalStateException("File 'config.yml' not found in the resources of JAR-archive. Check the folder /src/main/resources");
                    Files.copy(in, configPath);
                }
            }
            Yaml yaml = new Yaml();

            try (InputStream in = Files.newInputStream(configPath)) {
                this.config = yaml.load(in);
                if (this.config == null) this.config = new HashMap<>();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getString(String path) {
        return (String) get(path);
    }

    public int getInt(String path) {
        return (Integer) get(path);
    }

    private Object get(String path) {
        String[] parts = path.split("\\.");
        Object current = config;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }
}
