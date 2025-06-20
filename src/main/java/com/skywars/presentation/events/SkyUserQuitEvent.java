package com.skywars.presentation.events;

import com.skywars.domain.entity.SkyUser;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * SkyUserQuitEvent - Custom event fired when a SkyWars player leaves the server
 * 
 * This event is fired before the user data is saved to database.
 */
public class SkyUserQuitEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final SkyUser skyUser;
    
    public SkyUserQuitEvent(Player player, SkyUser skyUser) {
        this.player = player;
        this.skyUser = skyUser;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public SkyUser getSkyUser() {
        return skyUser;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
