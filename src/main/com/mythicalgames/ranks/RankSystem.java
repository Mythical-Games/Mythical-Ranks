package com.mythicalgames.ranks;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.plugin.Plugin;
import org.allaymc.api.registry.Registries;
import org.allaymc.api.server.Server;

import com.mythicalgames.ranks.commands.GroupsCommand;
import com.mythicalgames.ranks.database.DatabaseHandler;
import com.mythicalgames.ranks.database.SQLiteHandler;
import com.mythicalgames.ranks.database.MongoDBHandler;
import com.mythicalgames.ranks.listeners.PlayerListener;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;

@Slf4j
public class RankSystem extends Plugin {

    private static RankSystem instance;
    public Config config;
    public DatabaseHandler database;
    public String PLUGIN_PREFIX = "§l§7[§dMythical-Ranks§7] §r";

    public enum DatabaseType { SQLITE, MONGODB }

    @Override
    public void onLoad() {
        instance = this;
        log.info("Loading configuration...");

        config = ConfigManager.create(Config.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(pluginContainer.dataFolder().resolve("config.yml"));
            it.saveDefaults();
            it.load(true);
        });
    }

    @Override
    public void onEnable() {
        initializeDatabase();
        RankHelper.setDatabaseHandler(database);
        RankHelper.ensureDefaultGroup(config);

        Server.getInstance().getEventBus().registerListener(new PlayerListener());
        Registries.COMMANDS.register(new GroupsCommand(this));

        log.info("Mythical-Ranks enabled with database: {}", database.getClass().getSimpleName());
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }

    private void initializeDatabase() {
        switch (config.database_type) {
            case SQLITE:
                database = new SQLiteHandler(pluginContainer.dataFolder() + "/ranks.db");
                break;

            case MONGODB:
                if (config.mongo_uri.isEmpty() || config.mongo_database_name.isEmpty()) {
                    log.error("MongoDB configuration missing. Check mongo-uri and mongo-database-name in config!");
                    throw new IllegalArgumentException("Invalid MongoDB configuration.");
                }
                database = new MongoDBHandler(config.mongo_uri, config.mongo_database_name);
                break;

            default:
                throw new UnsupportedOperationException("Unsupported database type: " + config.database_type);
        }
    }

    public static RankSystem getInstance() {
        return instance;
    }
}
