package com.skywars.presentation.command;

import com.skywars.domain.cosmetic.Cosmetic;
import com.skywars.domain.cosmetic.CosmeticType;
import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.service.CosmeticService;
import com.skywars.domain.service.SkyUserService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Command handler for cosmetics
 */
public class CosmeticCommand implements CommandExecutor, TabCompleter {
    
    private final CosmeticService cosmeticService;
    private final SkyUserService skyUserService;
    
    public CosmeticCommand(CosmeticService cosmeticService, SkyUserService skyUserService) {
        this.cosmeticService = cosmeticService;
        this.skyUserService = skyUserService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showMainMenu(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                handleListCommand(player, args);
                break;
            case "select":
                handleSelectCommand(player, args);
                break;
            case "preview":
                handlePreviewCommand(player, args);
                break;
            case "buy":
                handleBuyCommand(player, args);
                break;
            case "clear":
                handleClearCommand(player, args);
                break;
            default:
                showMainMenu(player);
                break;
        }
        
        return true;
    }
    
    private void showMainMenu(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== SkyWars Cosmetics ===");
        player.sendMessage(ChatColor.YELLOW + "/cosmetic list <type> " + ChatColor.WHITE + "- List all cosmetics of a type");
        player.sendMessage(ChatColor.YELLOW + "/cosmetic select <type> <id> " + ChatColor.WHITE + "- Select a cosmetic");
        player.sendMessage(ChatColor.YELLOW + "/cosmetic preview <id> " + ChatColor.WHITE + "- Preview a cosmetic");
        player.sendMessage(ChatColor.YELLOW + "/cosmetic buy <id> " + ChatColor.WHITE + "- Buy a cosmetic");
        player.sendMessage(ChatColor.YELLOW + "/cosmetic clear <type> " + ChatColor.WHITE + "- Clear selected cosmetic");
        
        player.sendMessage(ChatColor.GOLD + "Available cosmetic types:");
        for (CosmeticType type : cosmeticService.getAllCosmeticTypes()) {
            player.sendMessage(ChatColor.YELLOW + "- " + type.getDisplayName());
        }
    }
    
    private void handleListCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cosmetic list <type>");
            return;
        }
        
        String typeArg = args[1].toUpperCase();
        CosmeticType type;
        
        try {
            type = CosmeticType.valueOf(typeArg);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid cosmetic type: " + typeArg);
            return;
        }
        
        skyUserService.getUser(player.getUniqueId()).thenAccept(userOpt -> {
            if (userOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Failed to load your user data.");
                return;
            }
            
            SkyUser user = userOpt.get();
            List<Cosmetic> availableCosmetics = cosmeticService.getAvailableCosmetics(user, type);
            Optional<Cosmetic> activeCosmetic = cosmeticService.getActiveCosmetic(user, type);
            
            player.sendMessage(ChatColor.GOLD + "=== " + type.getDisplayName() + " Cosmetics ===");
            
            if (availableCosmetics.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "You don't have any " + type.getDisplayName() + " cosmetics.");
                return;
            }
            
            for (Cosmetic cosmetic : availableCosmetics) {
                boolean isActive = activeCosmetic.isPresent() && activeCosmetic.get().getId().equals(cosmetic.getId());
                String status = isActive ? ChatColor.GREEN + " [ACTIVE]" : "";
                String ownership = user.ownsCosmetic(cosmetic.getId()) ? 
                        ChatColor.GREEN + " [OWNED]" : 
                        (cosmetic.isFree() ? ChatColor.AQUA + " [FREE]" : ChatColor.GOLD + " [" + cosmetic.getPrice() + " coins]");
                
                player.sendMessage(ChatColor.YELLOW + cosmetic.getId() + ": " + 
                        ChatColor.WHITE + cosmetic.getName() + 
                        ownership + status);
            }
            
            player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/cosmetic select " + 
                    type.name().toLowerCase() + " <id>" + ChatColor.YELLOW + " to select a cosmetic.");
        });
    }
    
    private void handleSelectCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /cosmetic select <type> <id>");
            return;
        }
        
        String typeArg = args[1].toUpperCase();
        CosmeticType type;
        
        try {
            type = CosmeticType.valueOf(typeArg);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid cosmetic type: " + typeArg);
            return;
        }
        
        String cosmeticId = args[2];
        
        skyUserService.getUser(player.getUniqueId()).thenAccept(userOpt -> {
            if (userOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Failed to load your user data.");
                return;
            }
            
            SkyUser user = userOpt.get();
            boolean success = cosmeticService.selectCosmetic(user, cosmeticId);
            
            if (success) {
                cosmeticService.getCosmeticById(cosmeticId).ifPresent(cosmetic -> {
                    player.sendMessage(ChatColor.GREEN + "Selected " + cosmetic.getName() + " as your " + 
                            type.getDisplayName() + " cosmetic!");
                });
                
                // Save user data
                skyUserService.saveUser(user);
            } else {
                player.sendMessage(ChatColor.RED + "You don't own this cosmetic or it doesn't exist.");
            }
        });
    }
    
    private void handlePreviewCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cosmetic preview <id>");
            return;
        }
        
        String cosmeticId = args[1];
        
        cosmeticService.getCosmeticById(cosmeticId).ifPresentOrElse(
            cosmetic -> {
                player.sendMessage(ChatColor.GOLD + "=== Cosmetic Preview ===");
                player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + cosmetic.getId());
                player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + cosmetic.getName());
                player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + cosmetic.getType().getDisplayName());
                player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + cosmetic.getDescription());
                player.sendMessage(ChatColor.YELLOW + "Price: " + ChatColor.WHITE + cosmetic.getPrice() + " coins");
                
                // TODO: Add visual preview based on cosmetic type
            },
            () -> player.sendMessage(ChatColor.RED + "Cosmetic not found: " + cosmeticId)
        );
    }
    
    private void handleBuyCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cosmetic buy <id>");
            return;
        }
        
        String cosmeticId = args[1];
        
        skyUserService.getUser(player.getUniqueId()).thenAccept(userOpt -> {
            if (userOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Failed to load your user data.");
                return;
            }
            
            SkyUser user = userOpt.get();
            
            // Check if user already owns this cosmetic
            if (user.ownsCosmetic(cosmeticId)) {
                player.sendMessage(ChatColor.RED + "You already own this cosmetic.");
                return;
            }
            
            Optional<Cosmetic> cosmeticOpt = cosmeticService.getCosmeticById(cosmeticId);
            
            if (cosmeticOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Cosmetic not found: " + cosmeticId);
                return;
            }
            
            Cosmetic cosmetic = cosmeticOpt.get();
            
            // Check if cosmetic is free
            if (cosmetic.isFree()) {
                player.sendMessage(ChatColor.RED + "This cosmetic is free, you don't need to buy it.");
                return;
            }
            
            // Check if user has enough coins
            if (user.getCoins() < cosmetic.getPrice()) {
                player.sendMessage(ChatColor.RED + "You don't have enough coins to buy this cosmetic. " +
                        "You need " + cosmetic.getPrice() + " coins, but you only have " + user.getCoins() + ".");
                return;
            }
            
            // Purchase the cosmetic
            boolean success = cosmeticService.purchaseCosmetic(user, cosmeticId);
            
            if (success) {
                player.sendMessage(ChatColor.GREEN + "You purchased " + cosmetic.getName() + " for " + 
                        cosmetic.getPrice() + " coins!");
                player.sendMessage(ChatColor.GREEN + "You now have " + user.getCoins() + " coins.");
                
                // Save user data
                skyUserService.saveUser(user);
            } else {
                player.sendMessage(ChatColor.RED + "Failed to purchase cosmetic.");
            }
        });
    }
    
    private void handleClearCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cosmetic clear <type>");
            return;
        }
        
        String typeArg = args[1].toUpperCase();
        CosmeticType type;
        
        try {
            type = CosmeticType.valueOf(typeArg);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid cosmetic type: " + typeArg);
            return;
        }
        
        skyUserService.getUser(player.getUniqueId()).thenAccept(userOpt -> {
            if (userOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Failed to load your user data.");
                return;
            }
            
            SkyUser user = userOpt.get();
            cosmeticService.clearSelectedCosmetic(user, type);
            
            player.sendMessage(ChatColor.GREEN + "Cleared your selected " + type.getDisplayName() + " cosmetic.");
            
            // Save user data
            skyUserService.saveUser(user);
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartingWith(args[0], Arrays.asList("list", "select", "preview", "buy", "clear"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("clear")) {
                return filterStartingWith(args[1], Arrays.stream(CosmeticType.values())
                        .map(type -> type.name().toLowerCase())
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("preview") || args[0].equalsIgnoreCase("buy")) {
                // Return all cosmetic IDs (would be better to filter by available ones, but this is simpler)
                return new ArrayList<>(cosmeticService.getCosmeticsByType(null).stream()
                        .map(Cosmetic::getId)
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("select")) {
                try {
                    CosmeticType type = CosmeticType.valueOf(args[1].toUpperCase());
                    return cosmeticService.getCosmeticsByType(type).stream()
                            .map(Cosmetic::getId)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    return new ArrayList<>();
                }
            }
        }
        
        return new ArrayList<>();
    }
    
    private List<String> filterStartingWith(String prefix, List<String> options) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
