package com.skywars.domain.repository;

import com.skywars.domain.cosmetic.Cosmetic;
import com.skywars.domain.cosmetic.CosmeticType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for cosmetics
 */
public interface CosmeticRepository {
    
    /**
     * Get all cosmetics
     */
    CompletableFuture<List<Cosmetic>> getAllCosmetics();
    
    /**
     * Get cosmetics by type
     */
    CompletableFuture<List<Cosmetic>> getCosmeticsByType(CosmeticType type);
    
    /**
     * Get a cosmetic by ID
     */
    CompletableFuture<Optional<Cosmetic>> getCosmeticById(String id);
    
    /**
     * Save a cosmetic
     */
    CompletableFuture<Void> saveCosmetic(Cosmetic cosmetic);
    
    /**
     * Delete a cosmetic
     */
    CompletableFuture<Boolean> deleteCosmetic(String id);
    
    /**
     * Get default cosmetic for a type
     */
    CompletableFuture<Optional<Cosmetic>> getDefaultCosmetic(CosmeticType type);
}
