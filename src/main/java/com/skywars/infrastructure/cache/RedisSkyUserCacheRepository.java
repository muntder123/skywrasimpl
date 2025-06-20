package com.skywars.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.repository.SkyUserCacheRepository;
import com.skywars.infrastructure.util.JsonSerializer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * RedisSkyUserCacheRepository - Redis implementation of SkyUserCacheRepository
 * 
 * This class handles all cache operations using Redis with Jackson JSON serialization
 * for fast data access and reduced database load.
 */
public class RedisSkyUserCacheRepository implements SkyUserCacheRepository {
    
    private final JedisPool jedisPool;
    private final Executor executor;
    private final long defaultTtlSeconds;
    
    private static final String KEY_PREFIX = "skyuser:";
    private static final String STATS_KEY = "skywars:stats";
    
    public RedisSkyUserCacheRepository(JedisPool jedisPool, Executor executor, long defaultTtlSeconds) {
        this.jedisPool = jedisPool;
        this.executor = executor;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }
    
    @Override
    public CompletableFuture<Optional<SkyUser>> get(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = getKey(uuid);
                String json = jedis.get(key);
                
                if (json != null) {
                    try {
                        SkyUser user = JsonSerializer.deserialize(json, SkyUser.class);
                        return Optional.of(user);
                    } catch (JsonProcessingException e) {
                        // Invalid JSON in cache, remove it
                        jedis.del(key);
                        System.err.println("Invalid JSON in cache for user " + uuid + ", removed: " + e.getMessage());
                        return Optional.<SkyUser>empty();
                    }
                } else {
                    return Optional.<SkyUser>empty();
                }
            } catch (Exception e) {
                throw new CompletionException("Failed to get user from cache: " + uuid, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> put(SkyUser user) {
        return put(user, defaultTtlSeconds);
    }
    
    @Override
    public CompletableFuture<Void> put(SkyUser user, long ttlSeconds) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = getKey(user.getUuid());
                String json = JsonSerializer.serialize(user);
                
                if (ttlSeconds > 0) {
                    jedis.setex(key, (int) ttlSeconds, json);
                } else {
                    jedis.set(key, json);
                }
                
                // Update cache statistics
                updateCacheStats(jedis, "put");
                
            } catch (JsonProcessingException e) {
                throw new CompletionException("Failed to serialize user to JSON: " + user.getUuid(), e);
            } catch (Exception e) {
                throw new CompletionException("Failed to put user in cache: " + user.getUuid(), e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> remove(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = getKey(uuid);
                jedis.del(key);
                
                // Update cache statistics
                updateCacheStats(jedis, "remove");
                
            } catch (Exception e) {
                throw new CompletionException("Failed to remove user from cache: " + uuid, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Boolean> exists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = getKey(uuid);
                return jedis.exists(key);
            } catch (Exception e) {
                throw new CompletionException("Failed to check if user exists in cache: " + uuid, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                // Get all keys with our prefix
                var keys = jedis.keys(KEY_PREFIX + "*");
                if (!keys.isEmpty()) {
                    jedis.del(keys.toArray(new String[0]));
                }
                
                // Reset cache statistics
                jedis.del(STATS_KEY);
                
            } catch (Exception e) {
                throw new CompletionException("Failed to clear cache", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<CacheInfo> getInfo() {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                // Get number of keys with our prefix
                var keys = jedis.keys(KEY_PREFIX + "*");
                long totalKeys = keys.size();
                
                // Get memory usage (approximate)
                String memoryInfo = jedis.info("memory");
                long usedMemory = parseUsedMemory(memoryInfo);
                
                // Check connection
                boolean connected = "PONG".equals(jedis.ping());
                
                return new CacheInfo(totalKeys, usedMemory, connected);
                
            } catch (Exception e) {
                // Return disconnected info if error occurs
                return new CacheInfo(0, 0, false);
            }
        }, executor);
    }
    
    /**
     * Get user from cache synchronously (for internal use)
     */
    public Optional<SkyUser> getSync(UUID uuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(uuid);
            String json = jedis.get(key);
            
            if (json != null) {
                try {
                    SkyUser user = JsonSerializer.deserialize(json, SkyUser.class);
                    return Optional.of(user);
                } catch (JsonProcessingException e) {
                    jedis.del(key);
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("Error getting user from cache sync: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Put user in cache synchronously (for internal use)
     */
    public void putSync(SkyUser user) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(user.getUuid());
            String json = JsonSerializer.serialize(user);
            jedis.setex(key, (int) defaultTtlSeconds, json);
            updateCacheStats(jedis, "put");
        } catch (Exception e) {
            System.err.println("Error putting user in cache sync: " + e.getMessage());
        }
    }
    
    /**
     * Extend TTL for a cached user
     */
    public CompletableFuture<Boolean> extendTtl(UUID uuid, long ttlSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = getKey(uuid);
                return jedis.expire(key, (int) ttlSeconds) == 1;
            } catch (Exception e) {
                throw new CompletionException("Failed to extend TTL for user: " + uuid, e);
            }
        }, executor);
    }
    
    /**
     * Get TTL for a cached user
     */
    public CompletableFuture<Long> getTtl(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = getKey(uuid);
                return jedis.ttl(key);
            } catch (Exception e) {
                throw new CompletionException("Failed to get TTL for user: " + uuid, e);
            }
        }, executor);
    }
    
    private String getKey(UUID uuid) {
        return KEY_PREFIX + uuid.toString();
    }
    
    private void updateCacheStats(Jedis jedis, String operation) {
        try {
            jedis.hincrBy(STATS_KEY, operation, 1);
            jedis.hset(STATS_KEY, "last_update", String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            // Ignore stats update errors
        }
    }
    
    private long parseUsedMemory(String memoryInfo) {
        try {
            String[] lines = memoryInfo.split("\r\n");
            for (String line : lines) {
                if (line.startsWith("used_memory:")) {
                    return Long.parseLong(line.split(":")[1]);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }
    
    /**
     * Health check for Redis connection
     */
    public boolean isHealthy() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get cache statistics
     */
    public CompletableFuture<java.util.Map<String, String>> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                return jedis.hgetAll(STATS_KEY);
            } catch (Exception e) {
                return java.util.Collections.emptyMap();
            }
        }, executor);
    }
}
