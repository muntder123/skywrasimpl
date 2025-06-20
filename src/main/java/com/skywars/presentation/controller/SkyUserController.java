package com.skywars.presentation.controller;

import com.skywars.application.usecase.*;
import com.skywars.domain.entity.SkyUser;
import com.skywars.presentation.events.SkyUserJoinEvent;
import com.skywars.presentation.events.SkyUserKillEvent;
import com.skywars.presentation.events.SkyUserQuitEvent;
import com.skywars.presentation.events.SkyUserWinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SkyUserController - Main controller handling player events and use case orchestration
 * 
 * This controller implements the Clean Architecture pattern by coordinating
 * between the presentation layer (Bukkit events) and application layer (use cases).
 */
public class SkyUserController implements Listener {
    
    private final GetSkyUserUseCase getSkyUserUseCase;
    private final SaveSkyUserUseCase saveSkyUserUseCase;
    private final CreateSkyUserUseCase createSkyUserUseCase;
    private final UpdateStatsUseCase updateStatsUseCase;
    
    // In-memory cache for active players (for performance)
    private final ConcurrentMap<String, SkyUser> activePlayers = new ConcurrentHashMap<>();
    
    // Configuration values
    private final int killReward;
    private final int winReward;
    
    public SkyUserController(GetSkyUserUseCase getSkyUserUseCase,
                           SaveSkyUserUseCase saveSkyUserUseCase,
                           CreateSkyUserUseCase createSkyUserUseCase,
                           UpdateStatsUseCase updateStatsUseCase,
                           int killReward,
                           int winReward) {
        this.getSkyUserUseCase = getSkyUserUseCase;
        this.saveSkyUserUseCase = saveSkyUserUseCase;
        this.createSkyUserUseCase = createSkyUserUseCase;
        this.updateStatsUseCase = updateStatsUseCase;
        this.killReward = killReward;
        this.winReward = winReward;
    }
    
    /**
     * Handle player join - Load or create user data
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Try to get existing user or create new one
        createSkyUserUseCase.execute(player.getUniqueId(), player.getName())
                .thenAccept(skyUser -> {
                    // Cache the user for quick access
                    activePlayers.put(player.getName(), skyUser);
                    
                    // Fire custom event
                    boolean isNewUser = skyUser.getKills() == 0 && skyUser.getDeaths() == 0 && 
                                       skyUser.getWins() == 0 && skyUser.getLosses() == 0;
                    SkyUserJoinEvent joinEvent = new SkyUserJoinEvent(player, skyUser, isNewUser);
                    Bukkit.getPluginManager().callEvent(joinEvent);
                    
                    // Welcome message for new users
                    if (isNewUser) {
                        player.sendMessage("§6Welcome to SkyWars! §7You start with §6" + skyUser.getCoins() + " coins§7.");
                    } else {
                        player.sendMessage("§7Welcome back! §7Stats: §a" + skyUser.getKills() + " kills§7, §c" + 
                                         skyUser.getDeaths() + " deaths§7, §6" + skyUser.getCoins() + " coins§7.");
                    }
                })
                .exceptionally(throwable -> {
                    player.sendMessage("§cError loading your data. Please contact an administrator.");
                    System.err.println("Error loading user data for " + player.getName() + ": " + throwable.getMessage());
                    return null;
                });
    }
    
    /**
     * Handle player quit - Save user data
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SkyUser skyUser = activePlayers.remove(player.getName());
        
        if (skyUser != null) {
            // Fire custom event
            SkyUserQuitEvent quitEvent = new SkyUserQuitEvent(player, skyUser);
            Bukkit.getPluginManager().callEvent(quitEvent);
            
            // Save to database (force database save on quit for data persistence)
            saveSkyUserUseCase.executeDatabaseOnly(skyUser)
                    .exceptionally(throwable -> {
                        System.err.println("Error saving user data for " + player.getName() + ": " + throwable.getMessage());
                        return null;
                    });
        }
    }
    
    /**
     * Handle player death - Update killer and victim stats
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Update victim's death count
        updateStatsUseCase.addDeath(victim.getUniqueId())
                .thenAccept(success -> {
                    if (success) {
                        // Update cached user
                        SkyUser cachedVictim = activePlayers.get(victim.getName());
                        if (cachedVictim != null) {
                            cachedVictim.addDeath();
                        }
                    }
                });
        
        // If there's a killer, update their stats
        if (killer != null && killer != victim) {
            String killMethod = getKillMethod(event);
            
            // Fire custom kill event
            SkyUserKillEvent killEvent = new SkyUserKillEvent(killer, victim, killMethod, killReward);
            Bukkit.getPluginManager().callEvent(killEvent);
            
            if (!killEvent.isCancelled()) {
                // Update killer's kill count
                updateStatsUseCase.addKill(killer.getUniqueId())
                        .thenAccept(success -> {
                            if (success) {
                                // Update cached user
                                SkyUser cachedKiller = activePlayers.get(killer.getName());
                                if (cachedKiller != null) {
                                    cachedKiller.addKill();
                                }
                            }
                        });
                
                // Give coin reward
                if (killEvent.getCoinReward() > 0) {
                    updateStatsUseCase.addCoins(killer.getUniqueId(), killEvent.getCoinReward())
                            .thenAccept(success -> {
                                if (success) {
                                    killer.sendMessage("§6+§e" + killEvent.getCoinReward() + " coins §6for killing §c" + victim.getName() + "§6!");
                                    
                                    // Update cached user
                                    SkyUser cachedKiller = activePlayers.get(killer.getName());
                                    if (cachedKiller != null) {
                                        cachedKiller.addCoins(killEvent.getCoinReward());
                                    }
                                }
                            });
                }
            }
        }
    }
    
    /**
     * Get cached user data for quick access
     */
    public Optional<SkyUser> getCachedUser(String playerName) {
        return Optional.ofNullable(activePlayers.get(playerName));
    }
    
    /**
     * Get cached user data by player
     */
    public Optional<SkyUser> getCachedUser(Player player) {
        return getCachedUser(player.getName());
    }
    
    /**
     * Update cached user data
     */
    public void updateCachedUser(SkyUser skyUser) {
        activePlayers.put(skyUser.getName(), skyUser);
    }
    
    /**
     * Handle game win
     * 
     * @param winner The player who won the game
     * @param losers List of players who lost the game
     * @param gameId Unique identifier for the game
     */
    public void handleGameWin(Player winner, List<Player> losers, String gameId) {
        // Fire custom win event
        SkyUserWinEvent winEvent = new SkyUserWinEvent(winner, losers, gameId, winReward);
        Bukkit.getPluginManager().callEvent(winEvent);
        
        if (!winEvent.isCancelled()) {
            updateStatsUseCase.addWin(winner.getUniqueId())
                    .thenAccept(success -> {
                        if (success) {
                            // Update cached user
                            SkyUser cachedWinner = activePlayers.get(winner.getName());
                            if (cachedWinner != null) {
                                cachedWinner.addWin();
                            }
                        }
                    });
        
            // Give coin reward
            int finalReward = winEvent.getCoinReward();
            if (finalReward > 0) {
                updateStatsUseCase.addCoins(winner.getUniqueId(), finalReward)
                        .thenAccept(success -> {
                            if (success) {
                                winner.sendMessage("§6+§e" + finalReward + " coins §6for winning the game!");
                                
                                // Update cached user
                                SkyUser cachedWinner = activePlayers.get(winner.getName());
                                if (cachedWinner != null) {
                                    cachedWinner.addCoins(finalReward);
                                }
                            }
                        });
            }
            
            // Handle losers
            for (Player loser : losers) {
                handleGameLoss(loser);
            }
        }
    }
    
    /**
     * Handle game loss
     * 
     * @param loser The player who lost the game
     */
    public void handleGameLoss(Player loser) {
        updateStatsUseCase.addLoss(loser.getUniqueId())
                .thenAccept(success -> {
                    if (success) {
                        // Update cached user
                        SkyUser cachedLoser = activePlayers.get(loser.getName());
                        if (cachedLoser != null) {
                            cachedLoser.addLoss();
                        }
                        
                        // Notify player
                        loser.sendMessage("§cYou lost the game! Better luck next time.");
                    }
                });
    }
    
    /**
     * Save all active players (for shutdown or periodic saves)
     */
    public void saveAllActivePlayers() {
        activePlayers.values().forEach(skyUser -> {
            saveSkyUserUseCase.execute(skyUser)
                    .exceptionally(throwable -> {
                        System.err.println("Error saving user data for " + skyUser.getName() + ": " + throwable.getMessage());
                        return null;
                    });
        });
    }
    
    /**
     * Get number of active players
     */
    public int getActivePlayerCount() {
        return activePlayers.size();
    }
    
    /**
     * Clear all cached players
     */
    public void clearCache() {
        activePlayers.clear();
    }
    
    private String getKillMethod(PlayerDeathEvent event) {
        String deathMessage = event.getDeathMessage();
        if (deathMessage == null) return "unknown";
        
        if (deathMessage.contains("sword")) return "sword";
        if (deathMessage.contains("bow")) return "bow";
        if (deathMessage.contains("fell")) return "fall";
        if (deathMessage.contains("lava")) return "lava";
        if (deathMessage.contains("fire")) return "fire";
        if (deathMessage.contains("explosion")) return "explosion";
        
        return "melee";
    }
}
