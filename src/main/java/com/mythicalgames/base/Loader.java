package com.mythicalgames.base;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.plugin.Plugin;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;

@Slf4j
public class Loader extends Plugin {
    private static Loader instance;
    public Config config;

    @Override
    public void onLoad() {
        instance = this;
        log.info("Loading Configuration file..!");
        config = ConfigManager.create(Config.class, config -> {
            config.withConfigurer(new YamlSnakeYamlConfigurer());
            config.withBindFile(pluginContainer.dataFolder().resolve("config.yml"));
            config.withRemoveOrphans(true);
            config.saveDefaults();
            config.load(true);
        });
    }

    @Override
    public void onEnable() {
        log.info("AllayBasePlugin enabled!");
    }

    @Override
    public void onDisable() {
        log.info("AllayBasePlugin disabled!");
    }

    public static Loader getInstance() {
        return instance;
    }
}