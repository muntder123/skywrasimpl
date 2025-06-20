package com.skywars.application.usecase;

import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.repository.SkyUserCacheRepository;
import com.skywars.domain.repository.SkyUserRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * UpdateStatsUseCase - Business logic for updating player statistics
 * 
 * This use case handles various stat updates like kills, deaths, wins, etc.
 * with proper validation and persistence.
 */
public class UpdateStatsUseCase {
    
    private final SkyUserRepository userRepository;
    private final SkyUserCacheRepository cacheRepository;
    private final GetSkyUserUseCase getSkyUserUseCase;
    private final SaveSkyUserUseCase saveSkyUserUseCase;
    
    public UpdateStatsUseCase(SkyUserRepository userRepository, 
                             SkyUserCacheRepository cacheRepository,
                             GetSkyUserUseCase getSkyUserUseCase,
                             SaveSkyUserUseCase saveSkyUserUseCase) {
        this.userRepository = userRepository;
        this.cacheRepository = cacheRepository;
        this.getSkyUserUseCase = getSkyUserUseCase;
        this.saveSkyUserUseCase = saveSkyUserUseCase;
    }
    
    /**
     * Add a kill to player's stats
     */
    public CompletableFuture<Boolean> addKill(UUID uuid) {
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        user.addKill();
                        return saveSkyUserUseCase.execute(user)
                                .thenApply(v -> true);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error adding kill: " + throwable.getMessage());
                    return false;
                });
    }
    
    /**
     * Add a death to player's stats
     */
    public CompletableFuture<Boolean> addDeath(UUID uuid) {
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        user.addDeath();
                        return saveSkyUserUseCase.execute(user)
                                .thenApply(v -> true);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error adding death: " + throwable.getMessage());
                    return false;
                });
    }
    
    /**
     * Add a win to player's stats
     */
    public CompletableFuture<Boolean> addWin(UUID uuid) {
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        user.addWin();
                        return saveSkyUserUseCase.execute(user)
                                .thenApply(v -> true);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error adding win: " + throwable.getMessage());
                    return false;
                });
    }
    
    /**
     * Add a loss to player's stats
     */
    public CompletableFuture<Boolean> addLoss(UUID uuid) {
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        user.addLoss();
                        return saveSkyUserUseCase.execute(user)
                                .thenApply(v -> true);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error adding loss: " + throwable.getMessage());
                    return false;
                });
    }
    
    /**
     * Add coins to player's account
     */
    public CompletableFuture<Boolean> addCoins(UUID uuid, int amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        user.addCoins(amount);
                        return saveSkyUserUseCase.execute(user)
                                .thenApply(v -> true);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error adding coins: " + throwable.getMessage());
                    return false;
                });
    }
    
    /**
     * Remove coins from player's account
     */
    public CompletableFuture<Boolean> removeCoins(UUID uuid, int amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        boolean success = user.removeCoins(amount);
                        if (success) {
                            return saveSkyUserUseCase.execute(user)
                                    .thenApply(v -> true);
                        } else {
                            return CompletableFuture.completedFuture(false);
                        }
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error removing coins: " + throwable.getMessage());
                    return false;
                });
    }
    
    /**
     * Reset all player stats
     */
    public CompletableFuture<Boolean> resetStats(UUID uuid) {
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        user.resetStats();
                        return saveSkyUserUseCase.execute(user)
                                .thenApply(v -> true);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error resetting stats: " + throwable.getMessage());
                    return false;
                });
    }
    
    /**
     * Update player name
     */
    public CompletableFuture<Boolean> updateName(UUID uuid, String newName) {
        return getSkyUserUseCase.execute(uuid)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        SkyUser user = optionalUser.get();
                        user.updateName(newName);
                        return saveSkyUserUseCase.execute(user)
                                .thenApply(v -> true);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Error updating name: " + throwable.getMessage());
                    return false;
                });
    }
}
