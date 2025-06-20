package com.skywars.domain.cosmetic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.*;

/**
 * Class representing a user's cosmetic selections and owned cosmetics
 */
@Data
public class UserCosmetics {
    
    // Map of cosmetic type to selected cosmetic ID
    private final Map<CosmeticType, String> selectedCosmetics;
    
    // Set of owned cosmetic IDs
    private final Set<String> ownedCosmetics;
    
    @JsonCreator
    public UserCosmetics(
            @JsonProperty("selectedCosmetics") Map<CosmeticType, String> selectedCosmetics,
            @JsonProperty("ownedCosmetics") Set<String> ownedCosmetics) {
        this.selectedCosmetics = selectedCosmetics != null ? selectedCosmetics : new EnumMap<>(CosmeticType.class);
        this.ownedCosmetics = ownedCosmetics != null ? ownedCosmetics : new HashSet<>();
    }
    
    /**
     * Create a new UserCosmetics instance with default values
     */
    public static UserCosmetics createDefault() {
        return new UserCosmetics(new EnumMap<>(CosmeticType.class), new HashSet<>());
    }
    
    /**
     * Check if the user owns a specific cosmetic
     */
    public boolean ownsCosmetic(String cosmeticId) {
        return ownedCosmetics.contains(cosmeticId);
    }
    
    /**
     * Add a cosmetic to the user's owned cosmetics
     */
    public void addCosmetic(String cosmeticId) {
        ownedCosmetics.add(cosmeticId);
    }
    
    /**
     * Remove a cosmetic from the user's owned cosmetics
     */
    public boolean removeCosmetic(String cosmeticId) {
        // Remove from selected if it's selected
        for (CosmeticType type : CosmeticType.values()) {
            if (cosmeticId.equals(selectedCosmetics.get(type))) {
                selectedCosmetics.remove(type);
            }
        }
        
        return ownedCosmetics.remove(cosmeticId);
    }
    
    /**
     * Select a cosmetic for a specific type
     */
    public void selectCosmetic(CosmeticType type, String cosmeticId) {
        selectedCosmetics.put(type, cosmeticId);
    }
    
    /**
     * Get the selected cosmetic ID for a specific type
     */
    public Optional<String> getSelectedCosmetic(CosmeticType type) {
        return Optional.ofNullable(selectedCosmetics.get(type));
    }
    
    /**
     * Clear the selected cosmetic for a specific type
     */
    public void clearSelectedCosmetic(CosmeticType type) {
        selectedCosmetics.remove(type);
    }
    
    /**
     * Get all owned cosmetics
     */
    public Set<String> getOwnedCosmetics() {
        return Collections.unmodifiableSet(ownedCosmetics);
    }
    
    /**
     * Get all selected cosmetics
     */
    public Map<CosmeticType, String> getSelectedCosmetics() {
        return Collections.unmodifiableMap(selectedCosmetics);
    }
}
