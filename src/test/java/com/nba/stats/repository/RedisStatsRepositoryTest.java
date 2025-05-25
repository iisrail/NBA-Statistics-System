package com.nba.stats.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.dto.PlayerStatsDelta;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:test-schema.sql",
    "nba.current-season=2024/25",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class RedisStatsRepositoryTest {

    @Autowired
    private RedisStatsRepository redisRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            System.out.println("Redis not available for test cleanup: " + e.getMessage());
        }
    }

    @Test
    void shouldStoreAndRetrieveSeasonStats() {
        // Given
        String seasonKey = "s:2024_25:p:23";
        Map<String, Object> stats = Map.of(
            "player_id", 23,
            "games_played", 5,
            "sum_points", 125,
            "sum_rebounds", 40,
            "sum_assists", 30
        );

        // When
        redisRepository.storeSeasonStats(seasonKey, stats);

        // Then
        Map<String, Object> retrieved = redisRepository.getSeasonStats(seasonKey);
        assertNotNull(retrieved);
        assertEquals("23", retrieved.get("player_id").toString());
        assertEquals("5", retrieved.get("games_played").toString());
        assertEquals("125", retrieved.get("sum_points").toString());
    }

    @Test
    void shouldCheckSeasonStatsExistence() {
        // Given
        String seasonKey = "s:2024_25:p:30";

        // When/Then - Initially doesn't exist
        assertFalse(redisRepository.seasonStatsExist(seasonKey));

        // When - Store stats
        redisRepository.storeSeasonStats(seasonKey, Map.of("games_played", 1));

        // Then - Now exists
        assertTrue(redisRepository.seasonStatsExist(seasonKey));
    }

    @Test
    void shouldUpdateSeasonAggregates() {
        // Given - Initial stats
        String seasonKey = "s:2024_25:p:35";
        Map<String, Object> initialStats = Map.of(
            "games_played", 1,
            "sum_points", 20,
            "sum_rebounds", 8,
            "sum_assists", 5
        );
        redisRepository.storeSeasonStats(seasonKey, initialStats);

        // When - Apply delta
        PlayerStatsDelta delta = new PlayerStatsDelta(
            15, // +15 points
            5,  // +5 rebounds  
            3,  // +3 assists
            2, 1, 2, 1, // steals, blocks, fouls, turnovers
            12.5, // +12.5 minutes
            0  // no new games (live game update)
        );
        redisRepository.updateSeasonAggregates(seasonKey, delta);

        // Then - Stats should be updated
        Map<String, Object> updated = redisRepository.getSeasonStats(seasonKey);
        assertEquals("35", updated.get("sum_points").toString()); // 20 + 15
        assertEquals("13", updated.get("sum_rebounds").toString()); // 8 + 5
        assertEquals("8", updated.get("sum_assists").toString()); // 5 + 3
        assertEquals("1", updated.get("games_played").toString()); // 1 + 0 (no new game)
    }

    @Test
    void shouldStoreAndRetrieveGameStats() {
        // Given
        LiveStatDto gameStats = LiveStatDto.builder()
            .gameId(1001)
            .teamId(10)
            .playerId(23)
            .points(25)
            .rebounds(8)
            .assists(6)
            .steals(2)
            .blocks(1)
            .fouls(3)
            .turnovers(2)
            .minutesPlayed(35.5)
            .build();

        String gameKey = "g:1001:p:23";

        // When
        redisRepository.storeCurrentGameStats(gameKey, gameStats);

        // Then
        LiveStatDto retrieved = redisRepository.getPreviousGameStats(gameKey);
        assertNotNull(retrieved);
        assertEquals(1001, retrieved.getGameId());
        assertEquals(25, retrieved.getPoints());
        assertEquals(8, retrieved.getRebounds());
        assertEquals(35.5, retrieved.getMinutesPlayed());
    }

    @Test
    void shouldReturnNullForNonExistentGameStats() {
        // Given
        String nonExistentKey = "g:9999:p:999";

        // When/Then
        LiveStatDto stats = redisRepository.getPreviousGameStats(nonExistentKey);
        assertNull(stats);
    }

    @Test
    void shouldDetectLiveGames() {
        // Given
        int playerId = 40;
        LiveStatDto gameStats = LiveStatDto.builder()
            .gameId(2001)
            .playerId(playerId)
            .teamId(25)
            .points(30)
            .rebounds(10)
            .assists(5)
            .steals(3)
            .blocks(2)
            .fouls(4)
            .turnovers(3)
            .minutesPlayed(40.0)
            .build();

        // When - No live game initially
        assertFalse(redisRepository.hasLiveGame(playerId));

        // When - Store game stats (creates live game)
        String gameKey = "g:2001:p:" + playerId;
        redisRepository.storeCurrentGameStats(gameKey, gameStats);

        // Then - Should detect live game
        assertTrue(redisRepository.hasLiveGame(playerId));
    }

    @Test
    void shouldCompleteGame() {
        // Given - Player has a live game
        int gameId = 3001;
        int playerId = 50;
        String gameKey = "g:" + gameId + ":p:" + playerId;
        
        LiveStatDto gameStats = LiveStatDto.builder()
            .gameId(gameId)
            .playerId(playerId)
            .teamId(30)
            .points(28)
            .rebounds(6)
            .assists(8)
            .steals(1)
            .blocks(0)
            .fouls(2)
            .turnovers(4)
            .minutesPlayed(42.0)
            .build();

        redisRepository.storeCurrentGameStats(gameKey, gameStats);
        assertTrue(redisRepository.hasLiveGame(playerId));

        // When - Complete the game
        redisRepository.completeGame(gameId, playerId);

        // Then - Should no longer have live game
        assertFalse(redisRepository.hasLiveGame(playerId));
        assertNull(redisRepository.getPreviousGameStats(gameKey));
    }
}
