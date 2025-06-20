# SkyWars Clean Architecture

A professional SkyWars plugin implementation using Clean Architecture principles with Redis caching and MySQL persistence.

## Architecture Overview

This plugin follows Clean Architecture principles to create a maintainable, testable, and scalable codebase:

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────┐  │
│  │   Presentation  │    │   Application   │    │ Domain  │  │
│  │     Layer       │◄───┤     Layer       │◄───┤  Layer  │  │
│  │                 │    │                 │    │         │  │
│  └────────┬────────┘    └────────┬────────┘    └────┬────┘  │
│           │                      │                  │       │
│           │                      │                  │       │
│           │                      │                  │       │
│           ▼                      ▼                  ▼       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Infrastructure Layer                  ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Layers

1. **Domain Layer** - Core business entities and repository interfaces
   - `SkyUser` - Core entity representing player data
   - Repository interfaces defining data access contracts

2. **Application Layer** - Use cases implementing business logic
   - `GetSkyUserUseCase` - Retrieves user data with cache-aside pattern
   - `SaveSkyUserUseCase` - Saves user data with write-through caching
   - `CreateSkyUserUseCase` - Creates new users
   - `UpdateStatsUseCase` - Updates player statistics

3. **Presentation Layer** - Controllers, commands, and events
   - `SkyUserController` - Handles Bukkit events and coordinates use cases
   - `SkyWarsCommand` - Processes player commands
   - Custom events for plugin extensibility

4. **Infrastructure Layer** - External implementations
   - `MySQLSkyUserRepository` - MySQL implementation of repositories
   - `RedisSkyUserCacheRepository` - Redis implementation of cache
   - Configuration classes for database and Redis

## Data Flow

### Player Join Flow
1. Player joins the server
2. `SkyUserController` intercepts the join event
3. `CreateSkyUserUseCase` attempts to load from Redis cache
4. If not in cache, loads from MySQL database
5. If not in database, creates a new user
6. Caches the user data in Redis
7. Fires `SkyUserJoinEvent` for extensibility

### Player Kill Flow
1. Player kills another player
2. `SkyUserController` intercepts the death event
3. `SkyUserKillEvent` is fired (can be cancelled)
4. `UpdateStatsUseCase` updates killer's stats
5. Stats are updated in memory and Redis cache
6. Asynchronously saved to MySQL database

### Player Quit Flow
1. Player leaves the server
2. `SkyUserController` intercepts the quit event
3. `SkyUserQuitEvent` is fired
4. User data is saved to MySQL database for persistence

## Features

- **High Performance**: Redis caching for fast data access
- **Data Persistence**: MySQL storage for reliable data persistence
- **Asynchronous Operations**: Non-blocking database and cache operations
- **Clean Separation of Concerns**: Each layer has a specific responsibility
- **Extensibility**: Easy to add new features through use cases
- **Testability**: Business logic isolated from framework dependencies
- **Scalability**: Can handle large player bases efficiently

## Configuration

The plugin uses a comprehensive configuration system:

```yaml
# Database Configuration
database:
  mysql:
    host: "localhost"
    port: 3306
    database: "skywars"
    username: "root"
    password: "password"
    # Connection Pool Settings
    pool:
      maximum-pool-size: 10
      minimum-idle: 2

# Redis Configuration
redis:
  host: "localhost"
  port: 6379
  password: ""
  database: 0
  timeout: 2000
  # Connection Pool Settings
  pool:
    max-total: 8
    max-idle: 8

# Cache Settings
cache:
  # Time in seconds to keep data in Redis
  user-cache-ttl: 1800  # 30 minutes
  # Auto-save interval in seconds
  auto-save-interval: 300  # 5 minutes

# Game Settings
game:
  # Kill rewards
  rewards:
    kill-coins: 10
    win-coins: 50
```

## Commands

- `/skywars stats [player]` - View player statistics
- `/skywars top <kills|wins> [limit]` - View top players
- `/skywars info` - View plugin information

**Admin Commands:**
- `/skywars reset <player>` - Reset player stats
- `/skywars addcoins <player> <amount>` - Add coins
- `/skywars removecoins <player> <amount>` - Remove coins
- `/skywars reload` - Reload configuration
- `/skywars cache <clear|info>` - Manage cache

## Dependencies

- **Spigot API**: Core Bukkit/Spigot API
- **Jackson**: JSON serialization/deserialization
- **Jedis**: Redis client
- **HikariCP**: High-performance JDBC connection pool
- **MySQL Connector**: MySQL database driver

## Installation

1. Place the plugin JAR in your server's `plugins` folder
2. Start the server to generate the default configuration
3. Configure the `config.yml` with your database and Redis settings
4. Restart the server

## Developer API

Other plugins can access the SkyWars system:

```java
SkyWarsPlugin skyWarsPlugin = (SkyWarsPlugin) Bukkit.getPluginManager().getPlugin("SkyWarsCleanArchitecture");
SkyUserController controller = skyWarsPlugin.getSkyUserController();

// Get player stats
controller.getCachedUser(player).ifPresent(skyUser -> {
    int kills = skyUser.getKills();
    int coins = skyUser.getCoins();
    // Use the data...
});

// Update player stats
UpdateStatsUseCase updateStatsUseCase = skyWarsPlugin.getUpdateStatsUseCase();
updateStatsUseCase.addCoins(player.getUniqueId(), 100)
    .thenAccept(success -> {
        if (success) {
            player.sendMessage("You received 100 coins!");
        }
    });
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
