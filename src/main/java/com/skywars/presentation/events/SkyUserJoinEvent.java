package com.skywars.presentation.events;

import com.skywars.domain.entity.SkyUser;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * SkyUserJoinEvent - Custom event fired when a SkyWars player joins the server
 * 
 * This event is fired after the user data is loaded from cache/database.
 */
public class SkyUserJoinEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final SkyUser skyUser;
    private final boolean isNewUser;
    
    public SkyUserJoinEvent(Player player, SkyUser skyUser, boolean isNewUser) {
        this.player = player;
        this.skyUser = skyUser;
        this.isNewUser = isNewUser;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public SkyUser getSkyUser() {
        return skyUser;
    }
    
    public boolean isNewUser() {
        return isNewUser;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
