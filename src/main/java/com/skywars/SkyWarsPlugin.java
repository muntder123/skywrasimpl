package com.skywars;

import com.skywars.application.usecase.*;
import com.skywars.domain.repository.CosmeticRepository;
import com.skywars.domain.repository.SkyUserCacheRepository;
import com.skywars.domain.repository.SkyUserRepository;
import com.skywars.domain.service.CosmeticService;
import com.skywars.domain.service.SkyUserService;
import com.skywars.infrastructure.cache.RedisSkyUserCacheRepository;
import com.skywars.infrastructure.config.DatabaseConfig;
import com.skywars.infrastructure.config.RedisConfig;
import com.skywars.infrastructure.database.MySQLSkyUserRepository;
import com.skywars.infrastructure.repository.InMemoryCosmeticRepository;
import com.skywars.infrastructure.service.SkyUserServiceImpl;
import com.skywars.presentation.command.CosmeticCommand;
import com.skywars.presentation.command.SkyWarsCommand;
import com.skywars.presentation.controller.SkyUserController;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SkyWarsPlugin - Main plugin class
 * 
 * This class initializes all components and manages the lifecycle
 * of the Clean Architecture SkyWars system.
 */
public class SkyWarsPlugin extends JavaPlugin {
    
    // Configuration
    private DatabaseConfig databaseConfig;
    private RedisConfig redisConfig;
    
    // Thread pool for async operations
    private ScheduledExecutorService executorService;
    
    // Repositories
    private SkyUserRepository userRepository;
    private SkyUserCacheRepository cacheRepository;
    
    // Use cases
    private GetSkyUserUseCase getSkyUserUseCase;
    private SaveSkyUserUseCase saveSkyUserUseCase;
    private CreateSkyUserUseCase createSkyUserUseCase;
    private UpdateStatsUseCase updateStatsUseCase;
    
    // Services
    private SkyUserService skyUserService;
    private CosmeticService cosmeticService;
    private CosmeticRepository cosmeticRepository;
    
    // Controllers
    private SkyUserController skyUserController;
    
    // Auto-save task ID
    private int autoSaveTaskId = -1;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize thread pool
        executorService = Executors.newScheduledThreadPool(4);
        
        // Initialize configurations
        initializeConfigurations();
        
        // Initialize repositories
        initializeRepositories();
        
        // Initialize use cases
        initializeUseCases();
        
        // Initialize controllers
        initializeControllers();
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        registerEventListeners();
        
        // Start auto-save task
        startAutoSaveTask();
        
        getLogger().info("SkyWars Clean Architecture plugin enabled successfully!");
        getLogger().info("Using Redis: " + (redisConfig.isHealthy() ? "Connected" : "Disconnected"));
        getLogger().info("Using MySQL: " + (databaseConfig.isHealthy() ? "Connected" : "Disconnected"));
    }
    
    @Override
    public void onDisable() {
        // Cancel auto-save task
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        
        // Save all active players
        if (skyUserController != null) {
            getLogger().info("Saving all active player data...");
            skyUserController.saveAllActivePlayers();
        }
        
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Close database and Redis connections
        if (databaseConfig != null) {
            databaseConfig.close();
        }
        
        if (redisConfig != null) {
            redisConfig.close();
        }
        
        getLogger().info("SkyWars Clean Architecture plugin disabled successfully!");
    }
    
    private void initializeConfigurations() {
        try {
            databaseConfig = new DatabaseConfig(getConfig());
            getLogger().info("Database configuration initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize database configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            redisConfig = new RedisConfig(getConfig());
            getLogger().info("Redis configuration initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Redis configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeRepositories() {
        try {
            userRepository = new MySQLSkyUserRepository(databaseConfig.getDataSource(), executorService);
            getLogger().info("MySQL repository initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize MySQL repository: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            long cacheTtl = getConfig().getLong("cache.user-cache-ttl", 1800);
            cacheRepository = new RedisSkyUserCacheRepository(redisConfig.getJedisPool(), executorService, cacheTtl);
            getLogger().info("Redis cache repository initialized successfully.");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Redis cache repository: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeUseCases() {
        getSkyUserUseCase = new GetSkyUserUseCase(userRepository, cacheRepository);
        saveSkyUserUseCase = new SaveSkyUserUseCase(userRepository, cacheRepository);
        createSkyUserUseCase = new CreateSkyUserUseCase(userRepository, cacheRepository);
        updateStatsUseCase = new UpdateStatsUseCase(userRepository, cacheRepository, getSkyUserUseCase, saveSkyUserUseCase);
        
        // Initialize services
        skyUserService = new SkyUserServiceImpl(getSkyUserUseCase, saveSkyUserUseCase, createSkyUserUseCase);
        
        // Initialize cosmetic repository and service
        cosmeticRepository = new InMemoryCosmeticRepository();
        cosmeticService = new CosmeticService();
        
        // Load cosmetics (this would normally load from a config or database)
        loadDefaultCosmetics();
        
        getLogger().info("Use cases and services initialized successfully.");
    }
    
    private void initializeControllers() {
        int killReward = getConfig().getInt("game.rewards.kill-coins", 10);
        int winReward = getConfig().getInt("game.rewards.win-coins", 50);
        
        skyUserController = new SkyUserController(
                getSkyUserUseCase,
                saveSkyUserUseCase,
                createSkyUserUseCase,
                updateStatsUseCase,
                killReward,
                winReward
        );
        
        getLogger().info("Controllers initialized successfully.");
    }
    
    private void registerCommands() {
        SkyWarsCommand skyWarsCommand = new SkyWarsCommand(
                getSkyUserUseCase,
                updateStatsUseCase,
                skyUserController,
                userRepository,
                cacheRepository
        );
        
        // Create and register cosmetic command
        CosmeticCommand cosmeticCommand = new CosmeticCommand(cosmeticService, skyUserService);
        
        getCommand("skywars").setExecutor(skyWarsCommand);
        getCommand("skywars").setTabCompleter(skyWarsCommand);
        getCommand("skywarsstats").setExecutor(skyWarsCommand);
        getCommand("skywarsstats").setTabCompleter(skyWarsCommand);
        getCommand("cosmetic").setExecutor(cosmeticCommand);
        getCommand("cosmetic").setTabCompleter(cosmeticCommand);
        
        getLogger().info("Commands registered successfully.");
    }
    
    private void registerEventListeners() {
        Bukkit.getPluginManager().registerEvents(skyUserController, this);
        
        getLogger().info("Event listeners registered successfully.");
    }
    
    private void startAutoSaveTask() {
        long autoSaveInterval = getConfig().getLong("cache.auto-save-interval", 300) * 20L; // Convert seconds to ticks
        
        autoSaveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            getLogger().info("Auto-saving player data...");
            skyUserController.saveAllActivePlayers();
            getLogger().info("Auto-save complete for " + skyUserController.getActivePlayerCount() + " players.");
        }, autoSaveInterval, autoSaveInterval);
        
        getLogger().info("Auto-save task started with interval: " + (autoSaveInterval / 20) + " seconds.");
    }
    
    /**
     * Get the SkyUserController instance
     * This allows other plugins to interact with the SkyWars system
     */
    public SkyUserController getSkyUserController() {
        return skyUserController;
    }
    
    /**
     * Get the GetSkyUserUseCase instance
     * This allows other plugins to query user data
     */
    public GetSkyUserUseCase getGetSkyUserUseCase() {
        return getSkyUserUseCase;
    }
    
    /**
     * Get the UpdateStatsUseCase instance
     * This allows other plugins to update user stats
     */
    public UpdateStatsUseCase getUpdateStatsUseCase() {
        return updateStatsUseCase;
    }
    
    /**
     * Get the SkyUserService instance
     * This allows other plugins to interact with user data
     */
    public SkyUserService getSkyUserService() {
        return skyUserService;
    }
    
    /**
     * Get the CosmeticService instance
     * This allows other plugins to interact with cosmetics
     */
    public CosmeticService getCosmeticService() {
        return cosmeticService;
    }
    
    /**
     * Load default cosmetics into the repository
     * This would normally load from a configuration file or database
     */
    private void loadDefaultCosmetics() {
        try {
            // Example of loading some default cosmetics
            // In a real implementation, this would load from config or database
            
            // Use the repository to save cosmetics
            // This demonstrates how the cosmeticRepository field is used
            cosmeticRepository.getAllCosmetics()
                .thenAccept(existingCosmetics -> {
                    if (existingCosmetics.isEmpty()) {
                        getLogger().info("Loading default cosmetics...");
                        // Load default cosmetics here
                    } else {
                        getLogger().info("Cosmetics already loaded: " + existingCosmetics.size());
                    }
                });
        } catch (Exception e) {
            getLogger().severe("Error loading default cosmetics: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
