package com.skywars.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.skywars.domain.cosmetic.CosmeticType;
import com.skywars.domain.cosmetic.UserCosmetics;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * SkyUser Entity - Core domain model representing a SkyWars player
 * 
 * This entity contains all the essential data for a SkyWars player
 * and follows Clean Architecture principles by being framework-agnostic.
 */
@Data
@EqualsAndHashCode(of = "uuid")
public class SkyUser {
    
    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int wins;
    private int losses;
    private int coins;
    private LocalDateTime lastSeen;
    private LocalDateTime firstJoin;
    private UserCosmetics cosmetics;
    
    /**
     * Constructor for Jackson JSON deserialization
     */
    @JsonCreator
    public SkyUser(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("name") String name,
            @JsonProperty("kills") int kills,
            @JsonProperty("deaths") int deaths,
            @JsonProperty("wins") int wins,
            @JsonProperty("losses") int losses,
            @JsonProperty("coins") int coins,
            @JsonProperty("lastSeen") LocalDateTime lastSeen,
            @JsonProperty("firstJoin") LocalDateTime firstJoin,
            @JsonProperty("cosmetics") UserCosmetics cosmetics) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.wins = wins;
        this.losses = losses;
        this.coins = coins;
        this.lastSeen = lastSeen;
        this.firstJoin = firstJoin;
        this.cosmetics = cosmetics != null ? cosmetics : UserCosmetics.createDefault();
    }
    
    /**
     * Create a new SkyUser with default stats
     */
    public static SkyUser createNew(UUID uuid, String name) {
        LocalDateTime now = LocalDateTime.now();
        return new SkyUser(uuid, name, 0, 0, 0, 0, 100, now, now, UserCosmetics.createDefault());
    }
    
    /**
     * Create SkyUser with custom stats (for loading from database)
     */
    public static SkyUser create(UUID uuid, String name, int kills, int deaths, 
                                int wins, int losses, int coins, 
                                LocalDateTime lastSeen, LocalDateTime firstJoin) {
        return new SkyUser(uuid, name, kills, deaths, wins, losses, coins, lastSeen, firstJoin, UserCosmetics.createDefault());
    }
    
    /**
     * Create SkyUser with custom stats and cosmetics
     */
    public static SkyUser create(UUID uuid, String name, int kills, int deaths, 
                                int wins, int losses, int coins, 
                                LocalDateTime lastSeen, LocalDateTime firstJoin,
                                UserCosmetics cosmetics) {
        return new SkyUser(uuid, name, kills, deaths, wins, losses, coins, lastSeen, firstJoin, cosmetics);
    }
    
    /**
     * Business logic methods
     */
    public void addKill() {
        this.kills++;
        updateLastSeen();
    }
    
    public void addDeath() {
        this.deaths++;
        updateLastSeen();
    }
    
    public void addWin() {
        this.wins++;
        updateLastSeen();
    }
    
    public void addLoss() {
        this.losses++;
        updateLastSeen();
    }
    
    public void addCoins(int amount) {
        this.coins += amount;
        updateLastSeen();
    }
    
    public boolean removeCoins(int amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            updateLastSeen();
            return true;
        }
        return false;
    }
    
    public double getKillDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
    
    public double getWinLossRatio() {
        return losses == 0 ? wins : (double) wins / losses;
    }
    
    public int getTotalGames() {
        return wins + losses;
    }
    
    public double getWinRate() {
        int totalGames = getTotalGames();
        return totalGames == 0 ? 0.0 : (double) wins / totalGames * 100;
    }
    
    private void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }
    
    /**
     * Update player name (in case of name change)
     */
    public void updateName(String newName) {
        this.name = newName;
        updateLastSeen();
    }
    
    /**
     * Reset all stats to default values
     */
    public void resetStats() {
        this.kills = 0;
        this.deaths = 0;
        this.wins = 0;
        this.losses = 0;
        this.coins = 100;
        updateLastSeen();
    }
    
    /**
     * Cosmetics related methods
     */
    public boolean ownsCosmetic(String cosmeticId) {
        return cosmetics.ownsCosmetic(cosmeticId);
    }
    
    public void addCosmetic(String cosmeticId) {
        cosmetics.addCosmetic(cosmeticId);
        updateLastSeen();
    }
    
    public boolean removeCosmetic(String cosmeticId) {
        boolean removed = cosmetics.removeCosmetic(cosmeticId);
        if (removed) {
            updateLastSeen();
        }
        return removed;
    }
    
    public void selectCosmetic(CosmeticType type, String cosmeticId) {
        cosmetics.selectCosmetic(type, cosmeticId);
        updateLastSeen();
    }
    
    public Optional<String> getSelectedCosmetic(CosmeticType type) {
        return cosmetics.getSelectedCosmetic(type);
    }
    
    public void clearSelectedCosmetic(CosmeticType type) {
        cosmetics.clearSelectedCosmetic(type);
        updateLastSeen();
    }
    
    @Override
    public String toString() {
        return String.format("SkyUser{uuid=%s, name='%s', kills=%d, deaths=%d, wins=%d, losses=%d, coins=%d}", 
                uuid, name, kills, deaths, wins, losses, coins);
    }
}
