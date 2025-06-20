package com.skywars.domain.repository;

import com.skywars.domain.entity.SkyUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SkyUserRepository Interface - Domain layer contract
 * 
 * This interface defines the contract for data persistence operations
 * without specifying the implementation details (Clean Architecture principle).
 */
public interface SkyUserRepository {
    
    /**
     * Find a user by UUID
     * @param uuid Player's UUID
     * @return Optional containing the user if found
     */
    CompletableFuture<Optional<SkyUser>> findByUuid(UUID uuid);
    
    /**
     * Find a user by name (case-insensitive)
     * @param name Player's name
     * @return Optional containing the user if found
     */
    CompletableFuture<Optional<SkyUser>> findByName(String name);
    
    /**
     * Save or update a user
     * @param user The user to save
     * @return CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> save(SkyUser user);
    
    /**
     * Save multiple users (batch operation)
     * @param users List of users to save
     * @return CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> saveAll(List<SkyUser> users);
    
    /**
     * Delete a user by UUID
     * @param uuid Player's UUID
     * @return CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> deleteByUuid(UUID uuid);
    
    /**
     * Check if a user exists by UUID
     * @param uuid Player's UUID
     * @return CompletableFuture containing true if user exists
     */
    CompletableFuture<Boolean> existsByUuid(UUID uuid);
    
    /**
     * Get top players by kills
     * @param limit Number of players to return
     * @return CompletableFuture containing list of top players
     */
    CompletableFuture<List<SkyUser>> getTopPlayersByKills(int limit);
    
    /**
     * Get top players by wins
     * @param limit Number of players to return
     * @return CompletableFuture containing list of top players
     */
    CompletableFuture<List<SkyUser>> getTopPlayersByWins(int limit);
    
    /**
     * Get all users (use with caution for large datasets)
     * @return CompletableFuture containing list of all users
     */
    CompletableFuture<List<SkyUser>> findAll();
    
    /**
     * Get total number of registered users
     * @return CompletableFuture containing the count
     */
    CompletableFuture<Long> count();
}
