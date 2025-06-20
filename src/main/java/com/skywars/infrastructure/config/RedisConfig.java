package com.skywars.infrastructure.config;

import org.bukkit.configuration.ConfigurationSection;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * RedisConfig - Configuration and setup for Redis connection
 * 
 * This class manages Jedis connection pool configuration
 * for optimal cache performance and reliability.
 */
public class RedisConfig {
    
    private final JedisPool jedisPool;
    
    public RedisConfig(ConfigurationSection config) {
        this.jedisPool = createJedisPool(config);
    }
    
    private JedisPool createJedisPool(ConfigurationSection config) {
        // Basic connection settings
        String host = config.getString("redis.host", "localhost");
        int port = config.getInt("redis.port", 6379);
        String password = config.getString("redis.password", "");
        int database = config.getInt("redis.database", 0);
        int timeout = config.getInt("redis.timeout", 2000);
        
        // Pool configuration
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        
        ConfigurationSection redisPoolConfig = config.getConfigurationSection("redis.pool");
        if (redisPoolConfig != null) {
            poolConfig.setMaxTotal(redisPoolConfig.getInt("max-total", 8));
            poolConfig.setMaxIdle(redisPoolConfig.getInt("max-idle", 8));
            poolConfig.setMinIdle(redisPoolConfig.getInt("min-idle", 0));
        } else {
            // Default pool settings
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(8);
            poolConfig.setMinIdle(0);
        }
        
        // Performance settings
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWaitMillis(Duration.ofSeconds(5).toMillis());
        
        // Create pool
        if (password != null && !password.trim().isEmpty()) {
            return new JedisPool(poolConfig, host, port, timeout, password, database);
        } else {
            return new JedisPool(poolConfig, host, port, timeout, null, database);
        }
    }
    
    public JedisPool getJedisPool() {
        return jedisPool;
    }
    
    public boolean isHealthy() {
        try {
            return !jedisPool.isClosed() && "PONG".equals(jedisPool.getResource().ping());
        } catch (Exception e) {
            return false;
        }
    }
    
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
    
    public String getStatus() {
        if (jedisPool.isClosed()) {
            return "CLOSED";
        }
        
        return String.format("ACTIVE - Pool: %d active, %d idle, %d waiting",
                jedisPool.getNumActive(),
                jedisPool.getNumIdle(),
                jedisPool.getNumWaiters());
    }
}
