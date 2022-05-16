package com.gmail.virustotalop.obsidianauctions.config;

import com.clubobsidian.wrappy.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ConfigMigrator {

    private static final Map<Integer, ConfigMigrator> mainConfigMigrators = new LinkedHashMap<>();
    private static final Map<Integer, ConfigMigrator> languageConfigMigrators = new LinkedHashMap<>();

    static {
        //Main config migrators
        //mainConfigMigrators.put(2, new FakeConfigMigrator());
        //Language config migrators
        //languageConfigMigrators.put(2, new FakeConfigMigrator());
    }

    public static void migrateMainConfig(Configuration config, int version) {
        migrateConfig(config, version, mainConfigMigrators);
    }

    public static void migrateLanguageConfig(Configuration textConfig, int version) {
        migrateConfig(textConfig, version, languageConfigMigrators);
    }

    private static void migrateConfig(Configuration config, int version, Map<Integer, ConfigMigrator> migrators) {
        boolean migrated = false;
        for (Map.Entry<Integer, ConfigMigrator> entry : migrators.entrySet()) {
            int key = entry.getKey();
            ConfigMigrator migrator = entry.getValue();
            if (version < key) {
                migrator.migrate(config);
                migrated = true;
            }
        }
        if (migrated) {
            config.save();
        }
    }

    private final int version;

    public ConfigMigrator(int version) {
        this.version = version;
    }



    public abstract void migrate(Configuration config);

    public int getVersion() {
        return this.version;
    }
}