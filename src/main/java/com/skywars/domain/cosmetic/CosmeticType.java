package com.skywars.domain.cosmetic;

/**
 * Enum representing different types of cosmetics in the SkyWars game
 */
public enum CosmeticType {
    KILL_MESSAGE("Kill Message"),
    BALLOON("Balloon"),
    WIN_EFFECT("Win Effect"),
    KILL_EFFECT("Kill Effect"),
    CAGE("Cage");
    
    private final String displayName;
    
    CosmeticType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
