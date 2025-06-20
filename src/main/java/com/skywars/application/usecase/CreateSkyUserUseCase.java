package com.skywars.application.usecase;

import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.repository.SkyUserCacheRepository;
import com.skywars.domain.repository.SkyUserRepository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * CreateSkyUserUseCase - Business logic for creating new users
 * 
 * This use case handles the creation of new SkyWars players
 * with proper validation and initialization.
 */
public class CreateSkyUserUseCase {
    
    private final SkyUserRepository userRepository;
    private final SkyUserCacheRepository cacheRepository;
    
    public CreateSkyUserUseCase(SkyUserRepository userRepository, SkyUserCacheRepository cacheRepository) {
        this.userRepository = userRepository;
        this.cacheRepository = cacheRepository;
    }
    
    /**
     * Create a new user if they don't exist
     */
    public CompletableFuture<SkyUser> execute(UUID uuid, String name) {
        return userRepository.existsByUuid(uuid)
                .thenCompose(exists -> {
                    if (exists) {
                        // User already exists, get existing user
                        return userRepository.findByUuid(uuid)
                                .thenApply(optionalUser -> {
                                    if (optionalUser.isPresent()) {
                                        SkyUser existingUser = optionalUser.get();
                                        // Update name in case it changed
                                        existingUser.updateName(name);
                                        // Save the updated user
                                        userRepository.save(existingUser);
                                        cacheRepository.put(existingUser);
                                        return existingUser;
                                    } else {
                                        // This shouldn't happen, but handle it gracefully
                                        return createNewUser(uuid, name);
                                    }
                                });
                    } else {
                        // Create new user
                        SkyUser newUser = createNewUser(uuid, name);
                        return userRepository.save(newUser)
                                .thenCompose(v -> cacheRepository.put(newUser))
                                .thenApply(v -> newUser);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error in CreateSkyUserUseCase: " + throwable.getMessage());
                    // Fallback: create user without database operations
                    return createNewUser(uuid, name);
                });
    }
    
    /**
     * Force create a new user (overwrites existing)
     */
    public CompletableFuture<SkyUser> executeForceCreate(UUID uuid, String name) {
        SkyUser newUser = createNewUser(uuid, name);
        
        return userRepository.save(newUser)
                .thenCompose(v -> cacheRepository.put(newUser))
                .thenApply(v -> newUser)
                .exceptionally(throwable -> {
                    System.err.println("Error in force create user: " + throwable.getMessage());
                    return newUser;
                });
    }
    
    /**
     * Create user with custom initial stats
     */
    public CompletableFuture<SkyUser> executeWithStats(UUID uuid, String name, 
                                                      int kills, int deaths, 
                                                      int wins, int losses, 
                                                      int coins) {
        return userRepository.existsByUuid(uuid)
                .thenCompose(exists -> {
                    if (exists) {
                        throw new IllegalArgumentException("User already exists: " + uuid);
                    }
                    
                    SkyUser newUser = SkyUser.create(uuid, name, kills, deaths, wins, losses, 
                            coins, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
                    
                    return userRepository.save(newUser)
                            .thenCompose(v -> cacheRepository.put(newUser))
                            .thenApply(v -> newUser);
                })
                .exceptionally(throwable -> {
                    System.err.println("Error creating user with custom stats: " + throwable.getMessage());
                    return null;
                });
    }
    
    private SkyUser createNewUser(UUID uuid, String name) {
        return SkyUser.createNew(uuid, name);
    }
}
