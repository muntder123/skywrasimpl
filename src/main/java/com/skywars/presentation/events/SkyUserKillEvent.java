package com.skywars.presentation.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * SkyUserKillEvent - Custom event fired when a player kills another player in SkyWars
 * 
 * This event is fired before stats are updated, allowing other plugins
 * to listen and modify behavior if needed.
 */
public class SkyUserKillEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player killer;
    private final Player victim;
    private final String killMethod;
    private int coinReward;
    private boolean cancelled;
    
    public SkyUserKillEvent(Player killer, Player victim, String killMethod, int coinReward) {
        this.killer = killer;
        this.victim = victim;
        this.killMethod = killMethod;
        this.coinReward = coinReward;
        this.cancelled = false;
    }
    
    public Player getKiller() {
        return killer;
    }
    
    public Player getVictim() {
        return victim;
    }
    
    public String getKillMethod() {
        return killMethod;
    }
    
    public int getCoinReward() {
        return coinReward;
    }
    
    public void setCoinReward(int coinReward) {
        this.coinReward = coinReward;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
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
