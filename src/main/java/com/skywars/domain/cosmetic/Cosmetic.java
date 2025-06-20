package com.skywars.domain.cosmetic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base class for all cosmetics in the SkyWars game
 */
@Data
@EqualsAndHashCode(of = {"id", "type"})
public class Cosmetic {
    
    private final String id;
    private final String name;
    private final CosmeticType type;
    private final int price;
    private final String description;
    private final String permission;
    private final boolean isDefault;
    
    @JsonCreator
    public Cosmetic(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("type") CosmeticType type,
            @JsonProperty("price") int price,
            @JsonProperty("description") String description,
            @JsonProperty("permission") String permission,
            @JsonProperty("isDefault") boolean isDefault) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.description = description;
        this.permission = permission;
        this.isDefault = isDefault;
    }
    
    /**
     * Check if the cosmetic is free
     */
    public boolean isFree() {
        return price <= 0;
    }
    
    /**
     * Get the permission node for this cosmetic
     */
    public String getPermission() {
        return permission != null && !permission.isEmpty() 
                ? permission 
                : "skywars.cosmetic." + type.name().toLowerCase() + "." + id.toLowerCase();
    }
}
