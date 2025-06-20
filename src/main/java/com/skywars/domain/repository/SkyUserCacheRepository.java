package com.skywars.domain.repository;

import com.skywars.domain.entity.SkyUser;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SkyUserCacheRepository Interface - Cache layer contract
 * 
 * This interface defines the contract for cache operations (Redis)
 * to provide fast access to frequently used data.
 */
public interface SkyUserCacheRepository {
    
    /**
     * Get user from cache
     * @param uuid Player's UUID
     * @return Optional containing the cached user if found
     */
    CompletableFuture<Optional<SkyUser>> get(UUID uuid);
    
    /**
     * Store user in cache with TTL
     * @param user The user to cache
     * @return CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> put(SkyUser user);
    
    /**
     * Store user in cache with custom TTL
     * @param user The user to cache
     * @param ttlSeconds Time to live in seconds
     * @return CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> put(SkyUser user, long ttlSeconds);
    
    /**
     * Remove user from cache
     * @param uuid Player's UUID
     * @return CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> remove(UUID uuid);
    
    /**
     * Check if user exists in cache
     * @param uuid Player's UUID
     * @return CompletableFuture containing true if user is cached
     */
    CompletableFuture<Boolean> exists(UUID uuid);
    
    /**
     * Clear all cached users
     * @return CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> clear();
    
    /**
     * Get cache statistics
     * @return CompletableFuture containing cache info
     */
    CompletableFuture<CacheInfo> getInfo();
    
    /**
     * Cache information data class
     */
    class CacheInfo {
        private final long totalKeys;
        private final long usedMemory;
        private final boolean connected;
        
        public CacheInfo(long totalKeys, long usedMemory, boolean connected) {
            this.totalKeys = totalKeys;
            this.usedMemory = usedMemory;
            this.connected = connected;
        }
        
        public long getTotalKeys() { return totalKeys; }
        public long getUsedMemory() { return usedMemory; }
        public boolean isConnected() { return connected; }
        
        @Override
        public String toString() {
            return String.format("CacheInfo{keys=%d, memory=%d bytes, connected=%s}", 
                    totalKeys, usedMemory, connected);
        }
    }
}
