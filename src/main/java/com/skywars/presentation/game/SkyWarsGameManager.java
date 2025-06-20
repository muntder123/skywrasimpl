package com.skywars.presentation.game;

import com.skywars.presentation.controller.SkyUserController;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SkyWarsGameManager - Manages SkyWars games
 * 
 * This class handles game creation, player management, and game state.
 * It coordinates with the SkyUserController for player stats.
 */
public class SkyWarsGameManager {
    
    private final Plugin plugin;
    private final SkyUserController skyUserController;
    private final Map<String, SkyWarsGame> activeGames;
    private final Random random;
    
    // Game configuration
    private final int minPlayers;
    private final int maxPlayers;
    private final int countdownTime;
    private final List<Location> lobbySpawns;
    
    /**
     * Create a new SkyWarsGameManager
     * 
     * @param plugin Plugin instance
     * @param skyUserController SkyUserController for player stats
     * @param config Plugin configuration
     */
    public SkyWarsGameManager(Plugin plugin, SkyUserController skyUserController, FileConfiguration config) {
        this.plugin = plugin;
        this.skyUserController = skyUserController;
        this.activeGames = new ConcurrentHashMap<>();
        this.random = new Random();
        
        // Load configuration
        this.minPlayers = config.getInt("game.min-players", 2);
        this.maxPlayers = config.getInt("game.max-players", 12);
        this.countdownTime = config.getInt("game.countdown-time", 30);
        
        // Load lobby spawns
        this.lobbySpawns = new ArrayList<>();
        ConfigurationSection spawnSection = config.getConfigurationSection("game.lobby-spawns");
        if (spawnSection != null) {
            for (String key : spawnSection.getKeys(false)) {
                ConfigurationSection spawn = spawnSection.getConfigurationSection(key);
                if (spawn != null) {
                    String worldName = spawn.getString("world");
                    double x = spawn.getDouble("x");
                    double y = spawn.getDouble("y");
                    double z = spawn.getDouble("z");
                    float yaw = (float) spawn.getDouble("yaw", 0);
                    float pitch = (float) spawn.getDouble("pitch", 0);
                    
                    if (worldName != null && Bukkit.getWorld(worldName) != null) {
                        lobbySpawns.add(new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch));
                    }
                }
            }
        }
        
        plugin.getLogger().info("SkyWarsGameManager initialized with " + lobbySpawns.size() + " lobby spawns");
        plugin.getLogger().info("Min players: " + minPlayers + ", Max players: " + maxPlayers);
    }
    
    /**
     * Create a new game
     * 
     * @param mapName Map name
     * @return New game instance
     */
    public SkyWarsGame createGame(String mapName) {
        String gameId = UUID.randomUUID().toString().substring(0, 8);
        SkyWarsGame game = new SkyWarsGame(gameId, mapName, minPlayers, maxPlayers, countdownTime);
        activeGames.put(gameId, game);
        return game;
    }
    
    /**
     * Get a game by ID
     * 
     * @param gameId Game ID
     * @return Game instance or null if not found
     */
    public SkyWarsGame getGame(String gameId) {
        return activeGames.get(gameId);
    }
    
    /**
     * Get all active games
     * 
     * @return List of active games
     */
    public List<SkyWarsGame> getActiveGames() {
        return new ArrayList<>(activeGames.values());
    }
    
    /**
     * Get games in a specific state
     * 
     * @param state Game state
     * @return List of games in the specified state
     */
    public List<SkyWarsGame> getGamesByState(SkyWarsGameState state) {
        return activeGames.values().stream()
                .filter(game -> game.getState() == state)
                .collect(Collectors.toList());
    }
    
    /**
     * Add a player to a game
     * 
     * @param player Player to add
     * @param gameId Game ID
     * @return True if player was added successfully
     */
    public boolean addPlayerToGame(Player player, String gameId) {
        SkyWarsGame game = activeGames.get(gameId);
        if (game != null && game.getState() == SkyWarsGameState.WAITING) {
            return game.addPlayer(player);
        }
        return false;
    }
    
    /**
     * Remove a player from their current game
     * 
     * @param player Player to remove
     * @return True if player was removed from a game
     */
    public boolean removePlayerFromGame(Player player) {
        for (SkyWarsGame game : activeGames.values()) {
            if (game.hasPlayer(player)) {
                game.removePlayer(player);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the game a player is in
     * 
     * @param player Player to check
     * @return Game the player is in, or null if not in a game
     */
    public SkyWarsGame getPlayerGame(Player player) {
        for (SkyWarsGame game : activeGames.values()) {
            if (game.hasPlayer(player)) {
                return game;
            }
        }
        return null;
    }
    
    /**
     * Handle a player win
     * 
     * @param game Game instance
     * @param winner Winning player
     */
    public void handleGameEnd(SkyWarsGame game, Player winner) {
        List<Player> losers = game.getPlayers().stream()
                .filter(player -> player != winner)
                .collect(Collectors.toList());
        
        // Update player stats
        skyUserController.handleGameWin(winner, losers, game.getGameId());
        
        // Broadcast win message
        Bukkit.broadcastMessage("§6§lSKYWARS §8» §e" + winner.getName() + " §6has won the game on map §e" + game.getMapName() + "§6!");
        
        // Remove game after a delay
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            activeGames.remove(game.getGameId());
        }, 200L); // 10 seconds
    }
    
    /**
     * Send a player to the lobby
     * 
     * @param player Player to send to lobby
     */
    public void sendToLobby(Player player) {
        if (!lobbySpawns.isEmpty()) {
            Location lobbyLocation = lobbySpawns.get(random.nextInt(lobbySpawns.size()));
            player.teleport(lobbyLocation);
        }
    }
    
    /**
     * Clean up resources when plugin is disabled
     */
    public void shutdown() {
        // End all active games
        for (SkyWarsGame game : new ArrayList<>(activeGames.values())) {
            for (Player player : game.getPlayers()) {
                sendToLobby(player);
                player.sendMessage("§cAll games have been ended due to server shutdown.");
            }
        }
        
        activeGames.clear();
    }
}
