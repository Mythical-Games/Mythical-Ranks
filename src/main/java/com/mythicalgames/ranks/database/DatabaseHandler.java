package com.mythicalgames.ranks.database;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseHandler {

    CompletableFuture<Boolean> hasPlayer(UUID uuid);
    CompletableFuture<Boolean> createPlayer(UUID uuid, String defaultGroup);
    CompletableFuture<String> getGroup(UUID uuid);
    CompletableFuture<Void> setGroup(UUID uuid, String group);
    CompletableFuture<Void> addPlayerPermission(UUID uuid, String permission);
    CompletableFuture<Void> removePlayerPermission(UUID uuid, String permission);
    CompletableFuture<List<String>> getPlayerPermissions(UUID uuid);
    void close();
}

