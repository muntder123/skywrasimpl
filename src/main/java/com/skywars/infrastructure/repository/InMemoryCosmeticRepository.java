package com.skywars.infrastructure.repository;

import com.skywars.domain.cosmetic.Cosmetic;
import com.skywars.domain.cosmetic.CosmeticType;
import com.skywars.domain.repository.CosmeticRepository;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * In-memory implementation of the CosmeticRepository
 */
public class InMemoryCosmeticRepository implements CosmeticRepository {
    
    private final Map<String, Cosmetic> cosmeticsById = new HashMap<>();
    private final Map<CosmeticType, List<Cosmetic>> cosmeticsByType = new EnumMap<>(CosmeticType.class);
    private final Map<CosmeticType, Cosmetic> defaultCosmetics = new EnumMap<>(CosmeticType.class);
    
    @Override
    public CompletableFuture<Optional<Cosmetic>> getCosmeticById(String id) {
        return CompletableFuture.completedFuture(Optional.ofNullable(cosmeticsById.get(id)));
    }
    
    @Override
    public CompletableFuture<List<Cosmetic>> getCosmeticsByType(CosmeticType type) {
        if (type == null) {
            return CompletableFuture.completedFuture(new ArrayList<>(cosmeticsById.values()));
        }
        return CompletableFuture.completedFuture(
            cosmeticsByType.getOrDefault(type, Collections.emptyList())
        );
    }
    
    @Override
    public CompletableFuture<List<Cosmetic>> getAllCosmetics() {
        return CompletableFuture.completedFuture(new ArrayList<>(cosmeticsById.values()));
    }
    
    @Override
    public CompletableFuture<Void> saveCosmetic(Cosmetic cosmetic) {
        cosmeticsById.put(cosmetic.getId(), cosmetic);
        
        // Add to type map
        cosmeticsByType.computeIfAbsent(cosmetic.getType(), k -> new ArrayList<>())
                .add(cosmetic);
        
        // Set as default if it's a default cosmetic
        if (cosmetic.isDefault()) {
            defaultCosmetics.put(cosmetic.getType(), cosmetic);
        }
                
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Boolean> deleteCosmetic(String id) {
        Cosmetic removed = cosmeticsById.remove(id);
        if (removed != null) {
            // Remove from type map
            cosmeticsByType.getOrDefault(removed.getType(), Collections.emptyList())
                    .remove(removed);
            
            // Remove from default cosmetics if it was the default
            if (defaultCosmetics.get(removed.getType()) == removed) {
                defaultCosmetics.remove(removed.getType());
            }
            
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }
    
    @Override
    public CompletableFuture<Optional<Cosmetic>> getDefaultCosmetic(CosmeticType type) {
        return CompletableFuture.completedFuture(Optional.ofNullable(defaultCosmetics.get(type)));
    }
}
