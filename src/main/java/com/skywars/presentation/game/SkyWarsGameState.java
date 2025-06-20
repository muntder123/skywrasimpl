package com.skywars.presentation.game;

/**
 * SkyWarsGameState - Enum representing the possible states of a SkyWars game
 */
public enum SkyWarsGameState {
    /**
     * Game is waiting for players to join
     */
    WAITING,
    
    /**
     * Game has enough players and is counting down to start
     */
    COUNTDOWN,
    
    /**
     * Game is in progress
     */
    ACTIVE,
    
    /**
     * Game has ended and is cleaning up
     */
    ENDING
}
