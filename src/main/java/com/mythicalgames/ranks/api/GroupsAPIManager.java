package com.mythicalgames.ranks.api;

import com.mythicalgames.ranks.Config;
import com.mythicalgames.ranks.RankHelper;

import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.permission.PermissionGroup;

import java.util.concurrent.CompletableFuture;

public class GroupsAPIManager {

    private static GroupsAPIManager instance;
    private final Config config;

    private GroupsAPIManager(Config config) {
        this.config = config;
    }

    public static void initialize(Config config) {
        if (instance == null) {
            instance = new GroupsAPIManager(config);
        }
    }

    public static GroupsAPIManager getAPI() {
        if (instance == null) {
            throw new IllegalStateException("GroupsAPIManager has not been initialized!");
        }
        return instance;
    }

    /**
     * Gets the player's group name asynchronously.
     */
    public CompletableFuture<String> getGroupName(EntityPlayer player) {
        return RankHelper.getOrCreatePermissible(player, config)
                .thenApply(permissible -> {
                    PermissionGroup group = permissible.getPermissionGroup();
                    return (group != null) ? group.getName() : config.defaultGroup;
                });
    }

    /**
     * Sets a player's group asynchronously.
     */
    public CompletableFuture<Void> setGroup(EntityPlayer player, String groupName) {
        return RankHelper.setPlayerGroup(player, config, groupName);
    }

    /**
     * Gets the player's group prefix asynchronously.
     * Returns "N/A" if none found.
     */
    public CompletableFuture<String> getPrefix(EntityPlayer player) {
        return RankHelper.getPlayerGroupPrefixAsync(player, config)
                .thenApply(prefix -> (prefix == null || prefix.isEmpty()) ? "N/A" : prefix);
    }

    /**
     * Checks if a player has a specific permission.
     */
    public boolean hasPermission(EntityPlayer player, String permission) {
        return RankHelper.hasPermission(player, permission);
    }

}
