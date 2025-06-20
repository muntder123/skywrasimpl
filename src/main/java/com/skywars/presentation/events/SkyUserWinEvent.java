package com.skywars.presentation.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * SkyUserWinEvent - Custom event fired when a player wins a SkyWars game
 * 
 * This event is fired before stats are updated, allowing other plugins
 * to listen and modify behavior if needed.
 */
public class SkyUserWinEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player winner;
    private final List<Player> losers;
    private final String gameId;
    private int coinReward;
    private boolean cancelled;
    
    /**
     * Create a new SkyUserWinEvent
     * 
     * @param winner The player who won the game
     * @param losers List of players who lost the game
     * @param gameId Unique identifier for the game
     * @param coinReward Coin reward for winning
     */
    public SkyUserWinEvent(Player winner, List<Player> losers, String gameId, int coinReward) {
        this.winner = winner;
        this.losers = losers;
        this.gameId = gameId;
        this.coinReward = coinReward;
        this.cancelled = false;
    }
    
    /**
     * Get the player who won
     * 
     * @return Winner player
     */
    public Player getWinner() {
        return winner;
    }
    
    /**
     * Get the players who lost
     * 
     * @return List of loser players
     */
    public List<Player> getLosers() {
        return losers;
    }
    
    /**
     * Get the game identifier
     * 
     * @return Game ID
     */
    public String getGameId() {
        return gameId;
    }
    
    /**
     * Get the coin reward for winning
     * 
     * @return Coin reward amount
     */
    public int getCoinReward() {
        return coinReward;
    }
    
    /**
     * Set the coin reward for winning
     * 
     * @param coinReward New coin reward amount
     */
    public void setCoinReward(int coinReward) {
        this.coinReward = coinReward;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
