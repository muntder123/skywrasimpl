package com.skywars.domain.service;

import com.skywars.domain.entity.SkyUser;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing SkyUser entities
 */
public interface SkyUserService {
    
    /**
     * Get a user by UUID
     * 
     * @param uuid The UUID of the user to retrieve
     * @return A CompletableFuture containing an Optional SkyUser
     */
    CompletableFuture<Optional<SkyUser>> getUser(UUID uuid);
    
    /**
     * Save a user to the database
     * 
     * @param user The user to save
     * @return A CompletableFuture indicating completion
     */
    CompletableFuture<Void> saveUser(SkyUser user);
    
    /**
     * Create a new user
     * 
     * @param uuid The UUID of the new user
     * @param name The name of the new user
     * @return A CompletableFuture containing the newly created SkyUser
     */
    CompletableFuture<SkyUser> createUser(UUID uuid, String name);
    
    /**
     * Delete a user
     * 
     * @param uuid The UUID of the user to delete
     * @return A CompletableFuture indicating completion
     */
    CompletableFuture<Boolean> deleteUser(UUID uuid);
}
