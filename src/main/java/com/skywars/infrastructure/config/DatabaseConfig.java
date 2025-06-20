package com.skywars.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.util.concurrent.TimeUnit;

/**
 * DatabaseConfig - Configuration and setup for MySQL database connection
 * 
 * This class manages HikariCP connection pool configuration
 * for optimal database performance and reliability.
 */
public class DatabaseConfig {
    
    private final HikariDataSource dataSource;
    
    public DatabaseConfig(ConfigurationSection config) {
        this.dataSource = createDataSource(config);
    }
    
    private HikariDataSource createDataSource(ConfigurationSection config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        // Basic connection settings
        String host = config.getString("mysql.host", "localhost");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "skywars");
        String username = config.getString("mysql.username", "root");
        String password = config.getString("mysql.password", "password");
        
        // Build JDBC URL
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        
        // Driver class
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Connection pool settings
        ConfigurationSection poolConfig = config.getConfigurationSection("mysql.pool");
        if (poolConfig != null) {
            hikariConfig.setMaximumPoolSize(poolConfig.getInt("maximum-pool-size", 10));
            hikariConfig.setMinimumIdle(poolConfig.getInt("minimum-idle", 2));
            hikariConfig.setConnectionTimeout(poolConfig.getLong("connection-timeout", 30000));
            hikariConfig.setIdleTimeout(poolConfig.getLong("idle-timeout", 600000));
            hikariConfig.setMaxLifetime(poolConfig.getLong("max-lifetime", 1800000));
        } else {
            // Default pool settings
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
        }
        
        // Performance settings
        hikariConfig.setLeakDetectionThreshold(TimeUnit.MINUTES.toMillis(2));
        hikariConfig.setPoolName("SkyWars-MySQL-Pool");
        
        // MySQL specific settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        // UTF-8 encoding
        hikariConfig.addDataSourceProperty("characterEncoding", "utf8");
        hikariConfig.addDataSourceProperty("useUnicode", "true");
        
        // SSL settings (disable for local development, enable for production)
        hikariConfig.addDataSourceProperty("useSSL", "false");
        hikariConfig.addDataSourceProperty("allowPublicKeyRetrieval", "true");
        
        // Connection validation
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        return new HikariDataSource(hikariConfig);
    }
    
    public HikariDataSource getDataSource() {
        return dataSource;
    }
    
    public boolean isHealthy() {
        try {
            return !dataSource.isClosed() && dataSource.getConnection().isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    public String getStatus() {
        if (dataSource.isClosed()) {
            return "CLOSED";
        }
        
        return String.format("ACTIVE - Pool: %d/%d, Idle: %d, Active: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariConfigMXBean().getMaximumPoolSize(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }
}
