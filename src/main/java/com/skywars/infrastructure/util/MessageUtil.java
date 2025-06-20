package com.skywars.infrastructure.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * MessageUtil - Utility class for handling messages and formatting
 * 
 * This class provides centralized message handling with configuration-based
 * messages and proper formatting.
 */
public class MessageUtil {
    
    private static String prefix = "&8[&bSkyWars&8] &r";
    private static FileConfiguration config;
    
    /**
     * Initialize the message utility with configuration
     * 
     * @param configuration Plugin configuration
     */
    public static void initialize(FileConfiguration configuration) {
        config = configuration;
        prefix = config.getString("messages.prefix", prefix);
    }
    
    /**
     * Send a formatted message to a player
     * 
     * @param player Player to send message to
     * @param message Message to send
     */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(colorize(prefix + message));
    }
    
    /**
     * Send a formatted message to a player without prefix
     * 
     * @param player Player to send message to
     * @param message Message to send
     */
    public static void sendMessageRaw(Player player, String message) {
        player.sendMessage(colorize(message));
    }
    
    /**
     * Get a message from configuration with placeholders
     * 
     * @param key Message key in configuration
     * @param placeholders Placeholder replacements (key1, value1, key2, value2, ...)
     * @return Formatted message
     */
    public static String getMessage(String key, Object... placeholders) {
        String message = config.getString("messages." + key, key);
        
        // Replace placeholders
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = "{" + placeholders[i].toString() + "}";
                String value = placeholders[i + 1].toString();
                message = message.replace(placeholder, value);
            }
        }
        
        return colorize(message);
    }
    
    /**
     * Send a configured message to a player
     * 
     * @param player Player to send message to
     * @param key Message key in configuration
     * @param placeholders Placeholder replacements (key1, value1, key2, value2, ...)
     */
    public static void sendConfigMessage(Player player, String key, Object... placeholders) {
        sendMessage(player, getMessage(key, placeholders));
    }
    
    /**
     * Convert color codes in a string
     * 
     * @param text Text with color codes
     * @return Colored text
     */
    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Format a number with commas
     * 
     * @param number Number to format
     * @return Formatted number string
     */
    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }
    
    /**
     * Format a decimal number
     * 
     * @param number Number to format
     * @param decimals Number of decimal places
     * @return Formatted decimal string
     */
    public static String formatDecimal(double number, int decimals) {
        return String.format("%." + decimals + "f", number);
    }
    
    /**
     * Format time in seconds to a readable format
     * 
     * @param seconds Time in seconds
     * @return Formatted time string (e.g., "2h 30m 15s")
     */
    public static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        
        return hours + "h " + remainingMinutes + "m " + remainingSeconds + "s";
    }
    
    /**
     * Get the message prefix
     * 
     * @return Current prefix
     */
    public static String getPrefix() {
        return prefix;
    }
}
