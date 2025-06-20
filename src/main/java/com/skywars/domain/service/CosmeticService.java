package com.skywars.domain.service;

import com.skywars.domain.cosmetic.Cosmetic;
import com.skywars.domain.cosmetic.CosmeticType;
import com.skywars.domain.entity.SkyUser;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing cosmetics in the SkyWars game
 */
public class CosmeticService {
    
    private final Map<String, Cosmetic> cosmeticsById = new HashMap<>();
    private final Map<CosmeticType, List<Cosmetic>> cosmeticsByType = new EnumMap<>(CosmeticType.class);
    private final Map<CosmeticType, Cosmetic> defaultCosmetics = new EnumMap<>(CosmeticType.class);
    
    /**
     * Register a cosmetic in the service
     */
    public void registerCosmetic(Cosmetic cosmetic) {
        cosmeticsById.put(cosmetic.getId(), cosmetic);
        
        // Add to type map
        cosmeticsByType.computeIfAbsent(cosmetic.getType(), k -> new ArrayList<>())
                .add(cosmetic);
        
        // Set as default if it's a default cosmetic
        if (cosmetic.isDefault()) {
            defaultCosmetics.put(cosmetic.getType(), cosmetic);
        }
    }
    
    /**
     * Get a cosmetic by its ID
     */
    public Optional<Cosmetic> getCosmeticById(String id) {
        return Optional.ofNullable(cosmeticsById.get(id));
    }
    
    /**
     * Get all cosmetics of a specific type
     */
    public List<Cosmetic> getCosmeticsByType(CosmeticType type) {
        return cosmeticsByType.getOrDefault(type, Collections.emptyList());
    }
    
    /**
     * Get the default cosmetic for a specific type
     */
    public Optional<Cosmetic> getDefaultCosmetic(CosmeticType type) {
        return Optional.ofNullable(defaultCosmetics.get(type));
    }
    
    /**
     * Get all available cosmetics for a user (owned + free)
     */
    public List<Cosmetic> getAvailableCosmetics(SkyUser user, CosmeticType type) {
        return getCosmeticsByType(type).stream()
                .filter(cosmetic -> cosmetic.isFree() || user.ownsCosmetic(cosmetic.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get the active cosmetic for a user and type
     */
    public Optional<Cosmetic> getActiveCosmetic(SkyUser user, CosmeticType type) {
        return user.getSelectedCosmetic(type)
                .flatMap(this::getCosmeticById)
                .or(() -> getDefaultCosmetic(type));
    }
    
    /**
     * Purchase a cosmetic for a user
     * 
     * @return true if purchase was successful, false otherwise
     */
    public boolean purchaseCosmetic(SkyUser user, String cosmeticId) {
        Optional<Cosmetic> cosmeticOpt = getCosmeticById(cosmeticId);
        
        if (cosmeticOpt.isEmpty()) {
            return false;
        }
        
        Cosmetic cosmetic = cosmeticOpt.get();
        
        // Check if user already owns this cosmetic
        if (user.ownsCosmetic(cosmeticId)) {
            return false;
        }
        
        // Check if user has enough coins
        if (!user.removeCoins(cosmetic.getPrice())) {
            return false;
        }
        
        // Add cosmetic to user's owned cosmetics
        user.addCosmetic(cosmeticId);
        return true;
    }
    
    /**
     * Select a cosmetic for a user
     * 
     * @return true if selection was successful, false otherwise
     */
    public boolean selectCosmetic(SkyUser user, String cosmeticId) {
        Optional<Cosmetic> cosmeticOpt = getCosmeticById(cosmeticId);
        
        if (cosmeticOpt.isEmpty()) {
            return false;
        }
        
        Cosmetic cosmetic = cosmeticOpt.get();
        
        // Check if user owns this cosmetic or if it's free
        if (!cosmetic.isFree() && !user.ownsCosmetic(cosmeticId)) {
            return false;
        }
        
        // Select the cosmetic
        user.selectCosmetic(cosmetic.getType(), cosmeticId);
        return true;
    }
    
    /**
     * Clear a selected cosmetic for a user
     */
    public void clearSelectedCosmetic(SkyUser user, CosmeticType type) {
        user.clearSelectedCosmetic(type);
    }
    
    /**
     * Get all cosmetic types
     */
    public List<CosmeticType> getAllCosmeticTypes() {
        return Arrays.asList(CosmeticType.values());
    }
}
