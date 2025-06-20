package com.skywars.presentation.command;

import com.skywars.application.usecase.GetSkyUserUseCase;
import com.skywars.application.usecase.UpdateStatsUseCase;
import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.repository.SkyUserCacheRepository;
import com.skywars.domain.repository.SkyUserRepository;
import com.skywars.presentation.controller.SkyUserController;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * SkyWarsCommand - Main command handler for SkyWars plugin
 * 
 * Handles various administrative and player commands for the SkyWars system.
 */
public class SkyWarsCommand implements CommandExecutor, TabCompleter {
    
    private final GetSkyUserUseCase getSkyUserUseCase;
    private final UpdateStatsUseCase updateStatsUseCase;
    private final SkyUserController skyUserController;
    private final SkyUserRepository userRepository;
    private final SkyUserCacheRepository cacheRepository;
    
    public SkyWarsCommand(GetSkyUserUseCase getSkyUserUseCase,
                         UpdateStatsUseCase updateStatsUseCase,
                         SkyUserController skyUserController,
                         SkyUserRepository userRepository,
                         SkyUserCacheRepository cacheRepository) {
        this.getSkyUserUseCase = getSkyUserUseCase;
        this.updateStatsUseCase = updateStatsUseCase;
        this.skyUserController = skyUserController;
        this.userRepository = userRepository;
        this.cacheRepository = cacheRepository;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "stats":
                handleStatsCommand(sender, args);
                break;
            case "top":
                handleTopCommand(sender, args);
                break;
            case "reset":
                handleResetCommand(sender, args);
                break;
            case "addcoins":
                handleAddCoinsCommand(sender, args);
                break;
            case "removecoins":
                handleRemoveCoinsCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "info":
                handleInfoCommand(sender);
                break;
            case "cache":
                handleCacheCommand(sender, args);
                break;
            default:
                sender.sendMessage("§cUnknown subcommand. Use /skywars help for available commands.");
                break;
        }
        
        return true;
    }
    
    private void handleStatsCommand(CommandSender sender, String[] args) {
        String targetName;
        
        if (args.length >= 2) {
            targetName = args[1];
        } else if (sender instanceof Player) {
            targetName = sender.getName();
        } else {
            sender.sendMessage("§cYou must specify a player name when using this command from console.");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer != null) {
            // Player is online, try cached data first
            Optional<SkyUser> cachedUser = skyUserController.getCachedUser(targetPlayer);
            if (cachedUser.isPresent()) {
                sendStatsMessage(sender, cachedUser.get());
                return;
            }
        }
        
        // Get from database/cache
        getSkyUserUseCase.executeByName(targetName)
                .thenAccept(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        sendStatsMessage(sender, optionalUser.get());
                    } else {
                        sender.sendMessage("§cPlayer '" + targetName + "' not found.");
                    }
                })
                .exceptionally(throwable -> {
                    sender.sendMessage("§cError retrieving player stats. Please try again later.");
                    return null;
                });
    }
    
    private void handleTopCommand(CommandSender sender, String[] args) {
        String type = args.length >= 2 ? args[1].toLowerCase() : "kills";
        int limit = args.length >= 3 ? parseInteger(args[2], 10) : 10;
        
        if (limit > 50) limit = 50; // Prevent excessive queries
        
        switch (type) {
            case "kills":
                userRepository.getTopPlayersByKills(limit)
                        .thenAccept(users -> sendTopMessage(sender, users, "Kills", SkyUser::getKills))
                        .exceptionally(throwable -> {
                            sender.sendMessage("§cError retrieving top players. Please try again later.");
                            return null;
                        });
                break;
            case "wins":
                userRepository.getTopPlayersByWins(limit)
                        .thenAccept(users -> sendTopMessage(sender, users, "Wins", SkyUser::getWins))
                        .exceptionally(throwable -> {
                            sender.sendMessage("§cError retrieving top players. Please try again later.");
                            return null;
                        });
                break;
            default:
                sender.sendMessage("§cInvalid top type. Use 'kills' or 'wins'.");
                break;
        }
    }
    
    private void handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skywars.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /skywars reset <player>");
            return;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer != null) {
            updateStatsUseCase.resetStats(targetPlayer.getUniqueId())
                    .thenAccept(success -> {
                        if (success) {
                            sender.sendMessage("§aSuccessfully reset stats for " + targetName + ".");
                            targetPlayer.sendMessage("§6Your stats have been reset by an administrator.");
                            
                            // Update cached data
                            Optional<SkyUser> cachedUser = skyUserController.getCachedUser(targetPlayer);
                            if (cachedUser.isPresent()) {
                                cachedUser.get().resetStats();
                            }
                        } else {
                            sender.sendMessage("§cFailed to reset stats for " + targetName + ".");
                        }
                    });
        } else {
            getSkyUserUseCase.executeByName(targetName)
                    .thenCompose(optionalUser -> {
                        if (optionalUser.isPresent()) {
                            return updateStatsUseCase.resetStats(optionalUser.get().getUuid());
                        } else {
                            sender.sendMessage("§cPlayer '" + targetName + "' not found.");
                            return null;
                        }
                    })
                    .thenAccept(success -> {
                        if (success != null && success) {
                            sender.sendMessage("§aSuccessfully reset stats for " + targetName + ".");
                        } else if (success != null) {
                            sender.sendMessage("§cFailed to reset stats for " + targetName + ".");
                        }
                    });
        }
    }
    
    private void handleAddCoinsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skywars.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /skywars addcoins <player> <amount>");
            return;
        }
        
        String targetName = args[1];
        int amount = parseInteger(args[2], 0);
        
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be a positive number.");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer != null) {
            updateStatsUseCase.addCoins(targetPlayer.getUniqueId(), amount)
                    .thenAccept(success -> {
                        if (success) {
                            sender.sendMessage("§aAdded " + amount + " coins to " + targetName + ".");
                            targetPlayer.sendMessage("§6You received " + amount + " coins from an administrator!");
                            
                            // Update cached data
                            Optional<SkyUser> cachedUser = skyUserController.getCachedUser(targetPlayer);
                            if (cachedUser.isPresent()) {
                                cachedUser.get().addCoins(amount);
                            }
                        } else {
                            sender.sendMessage("§cFailed to add coins to " + targetName + ".");
                        }
                    });
        } else {
            getSkyUserUseCase.executeByName(targetName)
                    .thenCompose(optionalUser -> {
                        if (optionalUser.isPresent()) {
                            return updateStatsUseCase.addCoins(optionalUser.get().getUuid(), amount);
                        } else {
                            sender.sendMessage("§cPlayer '" + targetName + "' not found.");
                            return null;
                        }
                    })
                    .thenAccept(success -> {
                        if (success != null && success) {
                            sender.sendMessage("§aAdded " + amount + " coins to " + targetName + ".");
                        } else if (success != null) {
                            sender.sendMessage("§cFailed to add coins to " + targetName + ".");
                        }
                    });
        }
    }
    
    private void handleRemoveCoinsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skywars.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /skywars removecoins <player> <amount>");
            return;
        }
        
        String targetName = args[1];
        int amount = parseInteger(args[2], 0);
        
        if (amount <= 0) {
            sender.sendMessage("§cAmount must be a positive number.");
            return;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer != null) {
            updateStatsUseCase.removeCoins(targetPlayer.getUniqueId(), amount)
                    .thenAccept(success -> {
                        if (success) {
                            sender.sendMessage("§aRemoved " + amount + " coins from " + targetName + ".");
                            targetPlayer.sendMessage("§c" + amount + " coins were removed from your account by an administrator.");
                            
                            // Update cached data
                            Optional<SkyUser> cachedUser = skyUserController.getCachedUser(targetPlayer);
                            if (cachedUser.isPresent()) {
                                cachedUser.get().removeCoins(amount);
                            }
                        } else {
                            sender.sendMessage("§cFailed to remove coins from " + targetName + " (insufficient funds or error).");
                        }
                    });
        } else {
            getSkyUserUseCase.executeByName(targetName)
                    .thenCompose(optionalUser -> {
                        if (optionalUser.isPresent()) {
                            return updateStatsUseCase.removeCoins(optionalUser.get().getUuid(), amount);
                        } else {
                            sender.sendMessage("§cPlayer '" + targetName + "' not found.");
                            return null;
                        }
                    })
                    .thenAccept(success -> {
                        if (success != null && success) {
                            sender.sendMessage("§aRemoved " + amount + " coins from " + targetName + ".");
                        } else if (success != null) {
                            sender.sendMessage("§cFailed to remove coins from " + targetName + " (insufficient funds or error).");
                        }
                    });
        }
    }
    
    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("skywars.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        // Save all active players before reload
        skyUserController.saveAllActivePlayers();
        sender.sendMessage("§aConfiguration reloaded and all active player data saved.");
    }
    
    private void handleInfoCommand(CommandSender sender) {
        sender.sendMessage("§6=== SkyWars Clean Architecture Info ===");
        sender.sendMessage("§7Active Players: §a" + skyUserController.getActivePlayerCount());
        
        userRepository.count()
                .thenAccept(count -> {
                    sender.sendMessage("§7Total Registered Users: §a" + count);
                });
        
        cacheRepository.getInfo()
                .thenAccept(cacheInfo -> {
                    sender.sendMessage("§7Cache Status: " + (cacheInfo.isConnected() ? "§aConnected" : "§cDisconnected"));
                    sender.sendMessage("§7Cached Users: §a" + cacheInfo.getTotalKeys());
                    sender.sendMessage("§7Cache Memory: §a" + formatBytes(cacheInfo.getUsedMemory()));
                });
    }
    
    private void handleCacheCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skywars.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /skywars cache <clear|info>");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "clear":
                cacheRepository.clear()
                        .thenAccept(v -> {
                            skyUserController.clearCache();
                            sender.sendMessage("§aCache cleared successfully.");
                        })
                        .exceptionally(throwable -> {
                            sender.sendMessage("§cError clearing cache: " + throwable.getMessage());
                            return null;
                        });
                break;
            case "info":
                handleInfoCommand(sender);
                break;
            default:
                sender.sendMessage("§cInvalid cache action. Use 'clear' or 'info'.");
                break;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== SkyWars Commands ===");
        sender.sendMessage("§e/skywars stats [player] §7- View player statistics");
        sender.sendMessage("§e/skywars top <kills|wins> [limit] §7- View top players");
        sender.sendMessage("§e/skywars info §7- View plugin information");
        
        if (sender.hasPermission("skywars.admin")) {
            sender.sendMessage("§c=== Admin Commands ===");
            sender.sendMessage("§e/skywars reset <player> §7- Reset player stats");
            sender.sendMessage("§e/skywars addcoins <player> <amount> §7- Add coins");
            sender.sendMessage("§e/skywars removecoins <player> <amount> §7- Remove coins");
            sender.sendMessage("§e/skywars reload §7- Reload configuration");
            sender.sendMessage("§e/skywars cache <clear|info> §7- Manage cache");
        }
    }
    
    private void sendStatsMessage(CommandSender sender, SkyUser user) {
        sender.sendMessage("§6=== Stats for " + user.getName() + " ===");
        sender.sendMessage("§7Kills: §a" + user.getKills());
        sender.sendMessage("§7Deaths: §c" + user.getDeaths());
        sender.sendMessage("§7K/D Ratio: §e" + String.format("%.2f", user.getKillDeathRatio()));
        sender.sendMessage("§7Wins: §a" + user.getWins());
        sender.sendMessage("§7Losses: §c" + user.getLosses());
        sender.sendMessage("§7W/L Ratio: §e" + String.format("%.2f", user.getWinLossRatio()));
        sender.sendMessage("§7Win Rate: §e" + String.format("%.1f%%", user.getWinRate()));
        sender.sendMessage("§7Coins: §6" + user.getCoins());
        sender.sendMessage("§7Total Games: §b" + user.getTotalGames());
    }
    
    private void sendTopMessage(CommandSender sender, List<SkyUser> users, String type, java.util.function.Function<SkyUser, Integer> statGetter) {
        sender.sendMessage("§6=== Top " + users.size() + " Players by " + type + " ===");
        for (int i = 0; i < users.size(); i++) {
            SkyUser user = users.get(i);
            sender.sendMessage("§e" + (i + 1) + ". §7" + user.getName() + " §a- §e" + statGetter.apply(user));
        }
    }
    
    private int parseInteger(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String[] subCommands = {"stats", "top", "info"};
            if (sender.hasPermission("skywars.admin")) {
                subCommands = new String[]{"stats", "top", "info", "reset", "addcoins", "removecoins", "reload", "cache"};
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if ("top".equals(subCommand)) {
                completions.addAll(Arrays.asList("kills", "wins"));
            } else if ("cache".equals(subCommand) && sender.hasPermission("skywars.admin")) {
                completions.addAll(Arrays.asList("clear", "info"));
            } else if (Arrays.asList("stats", "reset", "addcoins", "removecoins").contains(subCommand)) {
                // Add online player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
