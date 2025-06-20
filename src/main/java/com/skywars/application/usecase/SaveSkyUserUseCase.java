package com.skywars.application.usecase;

import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.repository.SkyUserCacheRepository;
import com.skywars.domain.repository.SkyUserRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SaveSkyUserUseCase - Business logic for saving user data
 * 
 * This use case implements write-through caching:
 * 1. Save to database (MySQL) first for persistence
 * 2. Update cache (Redis) for fast future access
 * 3. Handle failures gracefully
 */
public class SaveSkyUserUseCase {
    
    private final SkyUserRepository userRepository;
    private final SkyUserCacheRepository cacheRepository;
    
    public SaveSkyUserUseCase(SkyUserRepository userRepository, SkyUserCacheRepository cacheRepository) {
        this.userRepository = userRepository;
        this.cacheRepository = cacheRepository;
    }
    
    /**
     * Save user with write-through caching
     */
    public CompletableFuture<Void> execute(SkyUser user) {
        return userRepository.save(user)
                .thenCompose(v -> {
                    // After successful database save, update cache
                    return cacheRepository.put(user);
                })
                .exceptionally(throwable -> {
                    System.err.println("Error in SaveSkyUserUseCase: " + throwable.getMessage());
                    // Try to save to database only if cache fails
                    try {
                        userRepository.save(user).join();
                    } catch (Exception e) {
                        System.err.println("Critical: Failed to save to database: " + e.getMessage());
                    }
                    return null;
                });
    }
    
    /**
     * Save multiple users (batch operation)
     */
    public CompletableFuture<Void> executeAll(List<SkyUser> users) {
        return userRepository.saveAll(users)
                .thenCompose(v -> {
                    // Update cache for all users after successful batch save
                    CompletableFuture<Void>[] cacheFutures = users.stream()
                            .map(cacheRepository::put)
                            .toArray(CompletableFuture[]::new);
                    
                    return CompletableFuture.allOf(cacheFutures);
                })
                .exceptionally(throwable -> {
                    System.err.println("Error in batch SaveSkyUserUseCase: " + throwable.getMessage());
                    return null;
                });
    }
    
    /**
     * Save to database only (bypass cache)
     */
    public CompletableFuture<Void> executeDatabaseOnly(SkyUser user) {
        return userRepository.save(user)
                .exceptionally(throwable -> {
                    System.err.println("Error saving to database only: " + throwable.getMessage());
                    return null;
                });
    }
    
    /**
     * Save to cache only (temporary data)
     */
    public CompletableFuture<Void> executeCacheOnly(SkyUser user) {
        return cacheRepository.put(user)
                .exceptionally(throwable -> {
                    System.err.println("Error saving to cache only: " + throwable.getMessage());
                    return null;
                });
    }
    
    /**
     * Save to cache with custom TTL
     */
    public CompletableFuture<Void> executeCacheOnly(SkyUser user, long ttlSeconds) {
        return cacheRepository.put(user, ttlSeconds)
                .exceptionally(throwable -> {
                    System.err.println("Error saving to cache with TTL: " + throwable.getMessage());
                    return null;
                });
    }
}
