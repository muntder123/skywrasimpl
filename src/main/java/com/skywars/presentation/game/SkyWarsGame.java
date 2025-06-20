package com.skywars.presentation.game;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SkyWarsGame - Represents a single SkyWars game instance
 * 
 * This class manages the state and players for a single game.
 */
public class SkyWarsGame {
    
    private final String gameId;
    private final String mapName;
    private final int minPlayers;
    private final int maxPlayers;
    private final int countdownTime;
    private final List<Player> players;
    private final Instant createdAt;
    
    private SkyWarsGameState state;
    private int countdown;
    
    /**
     * Create a new SkyWarsGame
     * 
     * @param gameId Unique game identifier
     * @param mapName Map name
     * @param minPlayers Minimum players to start
     * @param maxPlayers Maximum players allowed
     * @param countdownTime Countdown time in seconds
     */
    public SkyWarsGame(String gameId, String mapName, int minPlayers, int maxPlayers, int countdownTime) {
        this.gameId = gameId;
        this.mapName = mapName;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.countdownTime = countdownTime;
        this.players = new ArrayList<>();
        this.state = SkyWarsGameState.WAITING;
        this.countdown = countdownTime;
        this.createdAt = Instant.now();
    }
    
    /**
     * Get the game ID
     * 
     * @return Game ID
     */
    public String getGameId() {
        return gameId;
    }
    
    /**
     * Get the map name
     * 
     * @return Map name
     */
    public String getMapName() {
        return mapName;
    }
    
    /**
     * Get the minimum players required
     * 
     * @return Minimum players
     */
    public int getMinPlayers() {
        return minPlayers;
    }
    
    /**
     * Get the maximum players allowed
     * 
     * @return Maximum players
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    /**
     * Get the countdown time
     * 
     * @return Countdown time in seconds
     */
    public int getCountdownTime() {
        return countdownTime;
    }
    
    /**
     * Get the current countdown value
     * 
     * @return Current countdown value
     */
    public int getCountdown() {
        return countdown;
    }
    
    /**
     * Set the current countdown value
     * 
     * @param countdown New countdown value
     */
    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }
    
    /**
     * Get the current game state
     * 
     * @return Game state
     */
    public SkyWarsGameState getState() {
        return state;
    }
    
    /**
     * Set the game state
     * 
     * @param state New game state
     */
    public void setState(SkyWarsGameState state) {
        this.state = state;
    }
    
    /**
     * Get the list of players in the game
     * 
     * @return List of players
     */
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }
    
    /**
     * Get the number of players in the game
     * 
     * @return Player count
     */
    public int getPlayerCount() {
        return players.size();
    }
    
    /**
     * Check if the game has a specific player
     * 
     * @param player Player to check
     * @return True if the player is in the game
     */
    public boolean hasPlayer(Player player) {
        return players.contains(player);
    }
    
    /**
     * Add a player to the game
     * 
     * @param player Player to add
     * @return True if the player was added successfully
     */
    public boolean addPlayer(Player player) {
        if (state != SkyWarsGameState.WAITING || players.size() >= maxPlayers || players.contains(player)) {
            return false;
        }
        
        players.add(player);
        
        // Broadcast join message to all players in the game
        broadcastMessage("§e" + player.getName() + " §7has joined the game! §8(§e" + players.size() + "§8/§e" + maxPlayers + "§8)");
        
        // Check if we have enough players to start countdown
        if (players.size() >= minPlayers && state == SkyWarsGameState.WAITING) {
            setState(SkyWarsGameState.COUNTDOWN);
            broadcastMessage("§6Game starting in §e" + countdownTime + " §6seconds!");
        }
        
        return true;
    }
    
    /**
     * Remove a player from the game
     * 
     * @param player Player to remove
     */
    public void removePlayer(Player player) {
        if (players.remove(player)) {
            // Broadcast leave message
            broadcastMessage("§e" + player.getName() + " §7has left the game! §8(§e" + players.size() + "§8/§e" + maxPlayers + "§8)");
            
            // Check if we need to cancel countdown
            if (players.size() < minPlayers && state == SkyWarsGameState.COUNTDOWN) {
                setState(SkyWarsGameState.WAITING);
                countdown = countdownTime;
                broadcastMessage("§cNot enough players! Countdown cancelled.");
            }
            
            // Check if game is in progress and only one player remains
            if (state == SkyWarsGameState.ACTIVE && players.size() == 1) {
                setState(SkyWarsGameState.ENDING);
                broadcastMessage("§6§lVICTORY! §e" + players.get(0).getName() + " §6is the last player standing!");
            }
        }
    }
    
    /**
     * Send a message to all players in the game
     * 
     * @param message Message to send
     */
    public void broadcastMessage(String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }
    
    /**
     * Get the time when the game was created
     * 
     * @return Creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Check if the game is full
     * 
     * @return True if the game is full
     */
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
    
    /**
     * Check if the game can start
     * 
     * @return True if the game has enough players to start
     */
    public boolean canStart() {
        return players.size() >= minPlayers;
    }
}
