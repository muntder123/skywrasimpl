package com.skywars.infrastructure.database;

import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.repository.SkyUserRepository;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * MySQLSkyUserRepository - MySQL implementation of SkyUserRepository
 * 
 * This class handles all database operations using HikariCP connection pooling
 * for optimal performance and reliability.
 */
public class MySQLSkyUserRepository implements SkyUserRepository {
    
    private final HikariDataSource dataSource;
    private final Executor executor;
    
    // SQL Queries
    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS sky_users (
                uuid VARCHAR(36) PRIMARY KEY,
                name VARCHAR(16) NOT NULL,
                kills INT DEFAULT 0,
                deaths INT DEFAULT 0,
                wins INT DEFAULT 0,
                losses INT DEFAULT 0,
                coins INT DEFAULT 100,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_name (name),
                INDEX idx_kills (kills DESC),
                INDEX idx_wins (wins DESC),
                INDEX idx_last_seen (last_seen DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
    
    private static final String INSERT_OR_UPDATE = """
            INSERT INTO sky_users (uuid, name, kills, deaths, wins, losses, coins, last_seen, first_join)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                kills = VALUES(kills),
                deaths = VALUES(deaths),
                wins = VALUES(wins),
                losses = VALUES(losses),
                coins = VALUES(coins),
                last_seen = VALUES(last_seen)
            """;
    
    private static final String SELECT_BY_UUID = """
            SELECT uuid, name, kills, deaths, wins, losses, coins, last_seen, first_join
            FROM sky_users WHERE uuid = ?
            """;
    
    private static final String SELECT_BY_NAME = """
            SELECT uuid, name, kills, deaths, wins, losses, coins, last_seen, first_join
            FROM sky_users WHERE LOWER(name) = LOWER(?)
            """;
    
    private static final String DELETE_BY_UUID = "DELETE FROM sky_users WHERE uuid = ?";
    
    private static final String EXISTS_BY_UUID = "SELECT 1 FROM sky_users WHERE uuid = ? LIMIT 1";
    
    private static final String SELECT_TOP_KILLS = """
            SELECT uuid, name, kills, deaths, wins, losses, coins, last_seen, first_join
            FROM sky_users ORDER BY kills DESC LIMIT ?
            """;
    
    private static final String SELECT_TOP_WINS = """
            SELECT uuid, name, kills, deaths, wins, losses, coins, last_seen, first_join
            FROM sky_users ORDER BY wins DESC LIMIT ?
            """;
    
    private static final String SELECT_ALL = """
            SELECT uuid, name, kills, deaths, wins, losses, coins, last_seen, first_join
            FROM sky_users
            """;
    
    private static final String COUNT_ALL = "SELECT COUNT(*) FROM sky_users";
    
    public MySQLSkyUserRepository(HikariDataSource dataSource, Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    @Override
    public CompletableFuture<Optional<SkyUser>> findByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SELECT_BY_UUID)) {
                
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToSkyUser(resultSet));
                    } else {
                        return Optional.<SkyUser>empty();
                    }
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to find user by UUID: " + uuid, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Optional<SkyUser>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SELECT_BY_NAME)) {
                
                statement.setString(1, name);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapResultSetToSkyUser(resultSet));
                    } else {
                        return Optional.<SkyUser>empty();
                    }
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to find user by name: " + name, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> save(SkyUser user) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(INSERT_OR_UPDATE)) {
                
                statement.setString(1, user.getUuid().toString());
                statement.setString(2, user.getName());
                statement.setInt(3, user.getKills());
                statement.setInt(4, user.getDeaths());
                statement.setInt(5, user.getWins());
                statement.setInt(6, user.getLosses());
                statement.setInt(7, user.getCoins());
                statement.setTimestamp(8, Timestamp.valueOf(user.getLastSeen()));
                statement.setTimestamp(9, Timestamp.valueOf(user.getFirstJoin()));
                
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException("Failed to save user: " + user.getUuid(), e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> saveAll(List<SkyUser> users) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                
                try (PreparedStatement statement = connection.prepareStatement(INSERT_OR_UPDATE)) {
                    for (SkyUser user : users) {
                        statement.setString(1, user.getUuid().toString());
                        statement.setString(2, user.getName());
                        statement.setInt(3, user.getKills());
                        statement.setInt(4, user.getDeaths());
                        statement.setInt(5, user.getWins());
                        statement.setInt(6, user.getLosses());
                        statement.setInt(7, user.getCoins());
                        statement.setTimestamp(8, Timestamp.valueOf(user.getLastSeen()));
                        statement.setTimestamp(9, Timestamp.valueOf(user.getFirstJoin()));
                        statement.addBatch();
                    }
                    
                    statement.executeBatch();
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to save users batch", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> deleteByUuid(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(DELETE_BY_UUID)) {
                
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException("Failed to delete user: " + uuid, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Boolean> existsByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(EXISTS_BY_UUID)) {
                
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to check if user exists: " + uuid, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<SkyUser>> getTopPlayersByKills(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SELECT_TOP_KILLS)) {
                
                statement.setInt(1, limit);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<SkyUser> users = new ArrayList<>();
                    while (resultSet.next()) {
                        users.add(mapResultSetToSkyUser(resultSet));
                    }
                    return users;
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to get top players by kills", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<SkyUser>> getTopPlayersByWins(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SELECT_TOP_WINS)) {
                
                statement.setInt(1, limit);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<SkyUser> users = new ArrayList<>();
                    while (resultSet.next()) {
                        users.add(mapResultSetToSkyUser(resultSet));
                    }
                    return users;
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to get top players by wins", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<List<SkyUser>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
                 ResultSet resultSet = statement.executeQuery()) {
                
                List<SkyUser> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(mapResultSetToSkyUser(resultSet));
                }
                return users;
            } catch (SQLException e) {
                throw new CompletionException("Failed to get all users", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(COUNT_ALL);
                 ResultSet resultSet = statement.executeQuery()) {
                
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                } else {
                    return 0L;
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to count users", e);
            }
        }, executor);
    }
    
    private SkyUser mapResultSetToSkyUser(ResultSet resultSet) throws SQLException {
        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
        String name = resultSet.getString("name");
        int kills = resultSet.getInt("kills");
        int deaths = resultSet.getInt("deaths");
        int wins = resultSet.getInt("wins");
        int losses = resultSet.getInt("losses");
        int coins = resultSet.getInt("coins");
        LocalDateTime lastSeen = resultSet.getTimestamp("last_seen").toLocalDateTime();
        LocalDateTime firstJoin = resultSet.getTimestamp("first_join").toLocalDateTime();
        
        return SkyUser.create(uuid, name, kills, deaths, wins, losses, coins, lastSeen, firstJoin);
    }
}
