package com.mythicalgames.ranks.database;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SQLiteHandler implements DatabaseHandler {
    private final Connection connection;

    public SQLiteHandler(String dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            try (Statement stmt = connection.createStatement()) {
                // Main player rank data
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rank_data (
                        player_uuid TEXT PRIMARY KEY,
                        group_name TEXT NOT NULL
                    )
                """);

                // Individual player permissions
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_permissions (
                        player_uuid TEXT NOT NULL,
                        permission TEXT NOT NULL,
                        UNIQUE(player_uuid, permission)
                    )
                """);
            }

            System.out.println("[Mythical-Ranks] SQLite initialized successfully!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SQLite for ranks", e);
        }
    }

    @Override
    public CompletableFuture<Boolean> hasPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT 1 FROM rank_data WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> createPlayer(UUID uuid, String defaultGroup) {
        return hasPlayer(uuid).thenApplyAsync(exists -> {
            if (exists) return false;
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO rank_data (player_uuid, group_name) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, defaultGroup);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<String> getGroup(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT group_name FROM rank_data WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("group_name");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> setGroup(UUID uuid, String group) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE rank_data SET group_name = ? WHERE player_uuid = ?")) {
                ps.setString(1, group);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // ----------------------------------------------------------
    //  Player-specific permission management
    // ----------------------------------------------------------

    @Override
    public CompletableFuture<Void> addPlayerPermission(UUID uuid, String permission) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR IGNORE INTO player_permissions (player_uuid, permission) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, permission.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> removePlayerPermission(UUID uuid, String permission) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM player_permissions WHERE player_uuid = ? AND permission = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, permission.toLowerCase());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> getPlayerPermissions(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> perms = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT permission FROM player_permissions WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        perms.add(rs.getString("permission"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return perms;
        });
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
            System.out.println("[Mythical-Ranks] SQLite connection closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


