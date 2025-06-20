package com.skywars.infrastructure.service;

import com.skywars.application.usecase.CreateSkyUserUseCase;
import com.skywars.application.usecase.GetSkyUserUseCase;
import com.skywars.application.usecase.SaveSkyUserUseCase;
import com.skywars.domain.entity.SkyUser;
import com.skywars.domain.service.SkyUserService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of SkyUserService that delegates to use cases
 */
public class SkyUserServiceImpl implements SkyUserService {
    
    private final GetSkyUserUseCase getSkyUserUseCase;
    private final SaveSkyUserUseCase saveSkyUserUseCase;
    private final CreateSkyUserUseCase createSkyUserUseCase;
    
    public SkyUserServiceImpl(GetSkyUserUseCase getSkyUserUseCase,
                             SaveSkyUserUseCase saveSkyUserUseCase,
                             CreateSkyUserUseCase createSkyUserUseCase) {
        this.getSkyUserUseCase = getSkyUserUseCase;
        this.saveSkyUserUseCase = saveSkyUserUseCase;
        this.createSkyUserUseCase = createSkyUserUseCase;
    }
    
    @Override
    public CompletableFuture<Optional<SkyUser>> getUser(UUID uuid) {
        return getSkyUserUseCase.execute(uuid);
    }
    
    @Override
    public CompletableFuture<Void> saveUser(SkyUser user) {
        return saveSkyUserUseCase.execute(user);
    }
    
    @Override
    public CompletableFuture<SkyUser> createUser(UUID uuid, String name) {
        return createSkyUserUseCase.execute(uuid, name);
    }
    
    @Override
    public CompletableFuture<Boolean> deleteUser(UUID uuid) {
        // This functionality isn't implemented in the use cases yet
        // You would need to create a DeleteSkyUserUseCase
        return CompletableFuture.completedFuture(false);
    }
}
