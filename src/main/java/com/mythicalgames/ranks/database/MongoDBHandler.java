package com.mythicalgames.ranks.database;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MongoDBHandler implements DatabaseHandler {

    private final MongoClient client;
    private final MongoDatabase database;

    public MongoDBHandler(String uri, String dbName) {
        this.client = MongoClients.create(uri);
        this.database = client.getDatabase(dbName);

        // Ensure indexes
        database.getCollection("rank_data")
                .createIndex(new Document("player_uuid", 1), new IndexOptions().unique(true));

        database.getCollection("player_permissions")
                .createIndex(new Document("player_uuid", 1));

        System.out.println("[Mythical-Ranks] MongoDB connection established to database: " + dbName);
    }

    @Override
    public CompletableFuture<Boolean> hasPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                database.getCollection("rank_data")
                        .find(Filters.eq("player_uuid", uuid.toString()))
                        .first() != null
        );
    }

    @Override
    public CompletableFuture<Boolean> createPlayer(UUID uuid, String defaultGroup) {
        return CompletableFuture.supplyAsync(() -> {
            if (hasPlayer(uuid).join()) return false;
            try {
                Document doc = new Document("player_uuid", uuid.toString())
                        .append("group_name", defaultGroup);
                database.getCollection("rank_data").insertOne(doc);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<String> getGroup(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = database.getCollection("rank_data")
                        .find(Filters.eq("player_uuid", uuid.toString()))
                        .first();
                if (doc != null && doc.containsKey("group_name")) {
                    return doc.getString("group_name");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> setGroup(UUID uuid, String group) {
        return CompletableFuture.runAsync(() ->
                database.getCollection("rank_data")
                        .updateOne(Filters.eq("player_uuid", uuid.toString()),
                                new Document("$set", new Document("group_name", group)),
                                new com.mongodb.client.model.UpdateOptions().upsert(true))
        );
    }

    // ----------------------------------------------------------
    //  Player-specific permission management
    // ----------------------------------------------------------

    @Override
    public CompletableFuture<Void> addPlayerPermission(UUID uuid, String permission) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> perms = database.getCollection("player_permissions");
            Document existing = perms.find(Filters.and(
                    Filters.eq("player_uuid", uuid.toString()),
                    Filters.eq("permission", permission.toLowerCase())
            )).first();

            if (existing == null) {
                perms.insertOne(new Document("player_uuid", uuid.toString())
                        .append("permission", permission.toLowerCase()));
            }
        });
    }

    @Override
    public CompletableFuture<Void> removePlayerPermission(UUID uuid, String permission) {
        return CompletableFuture.runAsync(() ->
                database.getCollection("player_permissions")
                        .deleteOne(Filters.and(
                                Filters.eq("player_uuid", uuid.toString()),
                                Filters.eq("permission", permission.toLowerCase())
                        ))
        );
    }

    @Override
    public CompletableFuture<List<String>> getPlayerPermissions(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> permissions = new ArrayList<>();
            try (MongoCursor<Document> cursor = database.getCollection("player_permissions")
                    .find(Filters.eq("player_uuid", uuid.toString()))
                    .iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    permissions.add(doc.getString("permission"));
                }
            }
            return permissions;
        });
    }

    @Override
    public void close() {
        try {
            client.close();
            System.out.println("[Mythical-Ranks] MongoDB connection closed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


