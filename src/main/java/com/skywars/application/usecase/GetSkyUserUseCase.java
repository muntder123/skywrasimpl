package com.skywars.application.usecase;

import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.repository.SkyUserCacheRepository;
import com.skywars.domain.repository.SkyUserRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GetSkyUserUseCase - Business logic for retrieving user data
 * 
 * This use case implements the cache-aside pattern:
 * 1. Try to get from cache (Redis) first
 * 2. If not found, get from database (MySQL)
 * 3. Store in cache for future requests
 */
public class GetSkyUserUseCase {
    
    private final SkyUserRepository userRepository;
    private final SkyUserCacheRepository cacheRepository;
    
    public GetSkyUserUseCase(SkyUserRepository userRepository, SkyUserCacheRepository cacheRepository) {
        this.userRepository = userRepository;
        this.cacheRepository = cacheRepository;
    }
    
    /**
     * Get user by UUID with cache-aside pattern
     */
    public CompletableFuture<Optional<SkyUser>> execute(UUID uuid) {
        return cacheRepository.get(uuid)
                .thenCompose(cachedUser -> {
                    if (cachedUser.isPresent()) {
                        // Cache hit - return cached data
                        return CompletableFuture.completedFuture(cachedUser);
                    } else {
                        // Cache miss - get from database and cache it
                        return userRepository.findByUuid(uuid)
                                .thenCompose(dbUser -> {
                                    if (dbUser.isPresent()) {
                                        // Store in cache for future requests
                                        return cacheRepository.put(dbUser.get())
                                                .thenApply(v -> dbUser);
                                    } else {
                                        return CompletableFuture.completedFuture(dbUser);
                                    }
                                });
                    }
                })
                .exceptionally(throwable -> {
                    // Log error and fallback to database only
                    System.err.println("Error in GetSkyUserUseCase: " + throwable.getMessage());
                    return userRepository.findByUuid(uuid).join();
                });
    }
    
    /**
     * Get user by name with cache lookup by UUID if found
     */
    public CompletableFuture<Optional<SkyUser>> executeByName(String name) {
        return userRepository.findByName(name)
                .thenCompose(dbUser -> {
                    if (dbUser.isPresent()) {
                        // Check if this user is also in cache and update if needed
                        return cacheRepository.get(dbUser.get().getUuid())
                                .thenCompose(cachedUser -> {
                                    if (cachedUser.isEmpty()) {
                                        // Not in cache, add it
                                        return cacheRepository.put(dbUser.get())
                                                .thenApply(v -> dbUser);
                                    } else {
                                        // Already in cache, return database version (more up-to-date)
                                        return CompletableFuture.completedFuture(dbUser);
                                    }
                                });
                    } else {
                        return CompletableFuture.completedFuture(dbUser);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error in GetSkyUserUseCase (by name): " + throwable.getMessage());
                    return Optional.empty();
                });
    }
    
    /**
     * Get user from cache only (fast lookup)
     */
    public CompletableFuture<Optional<SkyUser>> executeFromCacheOnly(UUID uuid) {
        return cacheRepository.get(uuid)
                .exceptionally(throwable -> {
                    System.err.println("Error getting user from cache: " + throwable.getMessage());
                    return Optional.empty();
                });
    }
    
    /**
     * Get user from database only (bypass cache)
     */
    public CompletableFuture<Optional<SkyUser>> executeFromDatabaseOnly(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .exceptionally(throwable -> {
                    System.err.println("Error getting user from database: " + throwable.getMessage());
                    return Optional.empty();
                });
    }
}
