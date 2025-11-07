package com.mythicalgames.ranks;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.permission.Permission;
import org.allaymc.api.permission.PermissionGroup;
import org.allaymc.api.permission.PermissionGroups;
import com.mythicalgames.ranks.database.DatabaseHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class RankHelper {

    private final Map<UUID, PlayerPermissible> permissibles = new HashMap<>();
    private DatabaseHandler databaseHandler;

    public void setDatabaseHandler(DatabaseHandler handler) {
        databaseHandler = handler;
    }

    // ---------------------------------------
    // Groups
    // ---------------------------------------

    public void ensureDefaultGroup(Config config) {
        if (config.groups == null || config.groups.isEmpty()) {
            log.info("No permission groups found, creating default one...");

            Config.GroupData defaultGroup = new Config.GroupData(
                    "§l§7[§bMember§7]",
                    "§7{player}",
                    "{prefix} {player} » §r§f{message}",
                    List.of(),
                    List.of()
            );
            config.groups.put("default", defaultGroup);
            config.defaultGroup = "default";
            config.save();
        }
        registerAllGroups(config);
    }

    public void registerAllGroups(Config config) {
        Map<String, PermissionGroup> loaded = new HashMap<>();

        for (Map.Entry<String, Config.GroupData> entry : config.groups.entrySet()) {
            String name = entry.getKey();
            Config.GroupData data = entry.getValue();

            Set<Permission> perms = data.permissions.stream()
                    .map(RankHelper::getOrCreatePermission)
                    .collect(Collectors.toSet());

            PermissionGroup group = getOrCreateGroup(name, perms, Collections.emptySet());
            loaded.put(name.toLowerCase(), group);
        }

        for (Map.Entry<String, Config.GroupData> entry : config.groups.entrySet()) {
            PermissionGroup group = loaded.get(entry.getKey().toLowerCase());
            for (String parentName : entry.getValue().parents) {
                PermissionGroup parentGroup = loaded.get(parentName.toLowerCase());
                if (parentGroup != null) group.addParent(parentGroup, null);
            }
        }

        PermissionGroup defaultGroup = loaded.get(config.defaultGroup.toLowerCase());
        if (defaultGroup != null) {
            PermissionGroups.DEFAULT.set(defaultGroup);
            log.info("Default group set to '{}'", defaultGroup.getName());
        }

        log.info("Loaded {} permission groups from config.", loaded.size());
    }

    public PermissionGroup getGroup(Config config, String name) {
        if (name == null) return null;

        PermissionGroup group = PermissionGroup.get(name);
        if (group != null) return group;

        Config.GroupData data = config.groups.get(name.toLowerCase());
        if (data == null) return null;

        Set<Permission> perms = data.permissions.stream()
                .map(RankHelper::getOrCreatePermission)
                .collect(Collectors.toSet());

        Set<PermissionGroup> parents = data.parents.stream()
                .map(RankHelper::getOrCreateGroup)
                .collect(Collectors.toSet());

        return getOrCreateGroup(name, perms, parents);
    }

    private static Permission getOrCreatePermission(String permissionName) {
        Permission perm = Permission.get(permissionName);
        if (perm == null) perm = Permission.create(permissionName);
        return perm;
    }

    private static PermissionGroup getOrCreateGroup(String groupName) {
        PermissionGroup group = PermissionGroup.get(groupName);
        if (group == null) group = PermissionGroup.create(groupName, Collections.emptySet(), Collections.emptySet());
        return group;
    }

    private static PermissionGroup getOrCreateGroup(String groupName, Set<Permission> perms, Set<PermissionGroup> parents) {
        PermissionGroup group = PermissionGroup.get(groupName);
        if (group != null) return group;
        return PermissionGroup.create(groupName, perms, parents);
    }

    // ---------------------------------------
    // PlayerPermissible
    // ---------------------------------------

    public CompletableFuture<PlayerPermissible> getOrCreatePermissible(EntityPlayer player, Config config) {
        UUID uuid = player.getLoginData().getUuid();
        PlayerPermissible existing = permissibles.get(uuid);
        if (existing != null) return CompletableFuture.completedFuture(existing);

        if (databaseHandler != null) {
            return databaseHandler.getGroup(uuid).thenCompose(dbGroupName -> {
                String groupName = (dbGroupName != null) ? dbGroupName : config.defaultGroup;
                PermissionGroup group = getGroup(config, groupName);
                if (group == null) group = PermissionGroups.DEFAULT.get();

                PlayerPermissible permissible = new PlayerPermissible(player, group);
                permissibles.put(uuid, permissible);

                // Load player-specific permissions
                return databaseHandler.getPlayerPermissions(uuid).thenApply(permissions -> {
                    permissions.forEach(p -> permissible.addPersonalPermission(getOrCreatePermission(p)));
                    return permissible;
                });

            }).exceptionally(e -> {
                log.error("Failed to load group or permissions for {}", player.getOriginName(), e);
                PermissionGroup defaultGroup = getGroup(config, config.defaultGroup);
                PlayerPermissible fallback = new PlayerPermissible(player, defaultGroup);
                permissibles.put(uuid, fallback);
                return fallback;
            });
        }

        PermissionGroup defaultGroup = getGroup(config, config.defaultGroup);
        PlayerPermissible permissible = new PlayerPermissible(player, defaultGroup);
        permissibles.put(uuid, permissible);
        return CompletableFuture.completedFuture(permissible);
    }

    public CompletableFuture<Void> setPlayerGroup(EntityPlayer player, Config config, String groupName) {
        UUID uuid = player.getLoginData().getUuid();
        PermissionGroup resolvedGroup = getGroup(config, groupName);
        final PermissionGroup targetGroup = (resolvedGroup != null) ? resolvedGroup : PermissionGroups.DEFAULT.get();

        PlayerPermissible permissible = permissibles.computeIfAbsent(uuid, id -> new PlayerPermissible(player, targetGroup));
        permissible.setPermissionGroup(targetGroup);

        log.info("Player {} assigned to group '{}'", player.getOriginName(), targetGroup.getName());

        if (databaseHandler != null) {
            return databaseHandler.setGroup(uuid, groupName)
                    .exceptionally(e -> {
                        log.error("Failed to set group for {}", player.getOriginName(), e);
                        return null;
                    });
        }

        return CompletableFuture.completedFuture(null);
    }

    // ---------------------------------------
    // Player Permissions
    // ---------------------------------------

    public CompletableFuture<Void> addPermission(EntityPlayer player, String permission) {
        UUID uuid = player.getLoginData().getUuid();
        PlayerPermissible permissible = permissibles.computeIfAbsent(uuid, id -> new PlayerPermissible(player, PermissionGroups.DEFAULT.get()));
        Permission perm = getOrCreatePermission(permission);
        permissible.addPersonalPermission(perm);

        if (databaseHandler != null) {
            return databaseHandler.addPlayerPermission(uuid, permission)
                    .exceptionally(e -> {
                        log.error("Failed to add permission '{}' to {}", permission, player.getOriginName(), e);
                        return null;
                    });
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> removePermission(EntityPlayer player, String permission) {
        UUID uuid = player.getLoginData().getUuid();
        PlayerPermissible permissible = permissibles.get(uuid);
        if (permissible != null) permissible.removePersonalPermission(getOrCreatePermission(permission));

        if (databaseHandler != null) {
            return databaseHandler.removePlayerPermission(uuid, permission)
                    .exceptionally(e -> {
                        log.error("Failed to remove permission '{}' from {}", permission, player.getOriginName(), e);
                        return null;
                    });
        }

        return CompletableFuture.completedFuture(null);
    }

    public boolean hasPermission(EntityPlayer player, String permissionName) {
        PlayerPermissible permissible = permissibles.get(player.getLoginData().getUuid());
        if (permissible == null) return false;
        return permissible.hasPermissionIncludingPersonal(getOrCreatePermission(permissionName));
    }

    // ---------------------------------------
    // Config Utilities
    // ---------------------------------------

    public boolean createAndSaveGroup(Config config, String groupName, @Nullable String parentGroup) {
        PermissionGroup group = getOrCreateGroup(groupName);

        List<String> parents = new ArrayList<>();
        if (parentGroup != null && !parentGroup.isEmpty()) {
            PermissionGroup parent = getOrCreateGroup(parentGroup);
            group.addParent(parent, null);
            parents.add(parentGroup.toLowerCase());
        }

        Config.GroupData data = new Config.GroupData(
                "§7[" + groupName + "]",
                "§7{player}",
                "{prefix} {player} §f» §7{message}",
                List.of(),
                parents
        );

        config.groups.put(groupName.toLowerCase(), data);
        config.save();

        log.info("Saved new group '{}' to config.", groupName);
        return true;
    }

    public static boolean setChatFormat(Config config, String groupName, String newChatFormat) {
        if (groupName == null || newChatFormat == null) return false;
        Config.GroupData groupData = config.groups.get(groupName.toLowerCase());
        if (groupData == null) return false;
        groupData.chatFormat = newChatFormat;
        config.save();
        return true;
    }

    public static boolean setNameTagFormat(Config config, String groupName, String newNameTagFormat) {
        if (groupName == null || newNameTagFormat == null) return false;
        Config.GroupData groupData = config.groups.get(groupName.toLowerCase());
        if (groupData == null) return false;
        groupData.nametag = newNameTagFormat;
        config.save();
        return true;
    }

}
