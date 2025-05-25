package com.nba.stats.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.repository.RedisStatsRepository;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:test-schema.sql",
    "nba.current-season=2024/25",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class LiveStatServiceTest {

    @Autowired
    private LiveStatService liveStatService;

    @Autowired
    private StatsRetrievalService statsRetrievalService;
    
    @Autowired
    private RedisStatsRepository redisStatsRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            System.out.println("Redis not available for test cleanup: " + e.getMessage());
        }
    }

    @Test
    void shouldProcessFirstGameStatsCorrectly() {
        // Given: First game stats for LeBron
        LiveStatDto firstGame = LiveStatDto.builder()
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

        // When: Process the live stat
        liveStatService.processLiveStat(firstGame);

        // Then: Player stats should reflect the game
        Map<String, Object> playerStats = statsRetrievalService.getPlayerSeasonStats(23, "2024/25");
        
        assertEquals(1, playerStats.get("gamesPlayed"));
        assertEquals(25.0, playerStats.get("avgPoints"));
        assertEquals(8.0, playerStats.get("avgRebounds"));
        assertEquals(6.0, playerStats.get("avgAssists"));
        assertEquals(35.5, playerStats.get("avgMinutes"));

        // And: Team stats should also be updated
        Map<String, Object> teamStats = statsRetrievalService.getTeamSeasonStats(10, "2024/25");
        
        assertEquals(1, teamStats.get("gamesPlayed"));
        assertEquals(25.0, teamStats.get("avgPoints"));
        assertEquals(8.0, teamStats.get("avgRebounds"));
    }

    @Test
    void shouldHandleLiveGameUpdatesCorrectly() {
        // Given: First update in a live game
        LiveStatDto firstUpdate = LiveStatDto.builder()
                .gameId(1002)
                .teamId(15)
                .playerId(30) // Curry
                .points(10)
                .rebounds(3)
                .assists(2)
                .steals(1)
                .blocks(0)
                .fouls(1)
                .turnovers(1)
                .minutesPlayed(12.0)
                .build();

        // When: Process first update
        liveStatService.processLiveStat(firstUpdate);

        // Then: Stats should reflect first update
        Map<String, Object> stats = statsRetrievalService.getPlayerSeasonStats(30, "2024/25");
        assertEquals(1, stats.get("gamesPlayed"));
        assertEquals(10.0, stats.get("avgPoints"));

        // Given: Second update in same game (player scored more)
        LiveStatDto secondUpdate = LiveStatDto.builder()
                .gameId(1002)
                .teamId(15)
                .playerId(30)
                .points(18) // +8 points
                .rebounds(5) // +2 rebounds
                .assists(4)  // +2 assists
                .steals(2)   // +1 steal
                .blocks(1)   // +1 block
                .fouls(2)    // +1 foul
                .turnovers(1) // same
                .minutesPlayed(20.0) // +8 minutes
                .build();

        // When: Process second update
        liveStatService.processLiveStat(secondUpdate);

        // Then: Games played should still be 1, but stats updated
        stats = statsRetrievalService.getPlayerSeasonStats(30, "2024/25");
        assertEquals(1, stats.get("gamesPlayed")); // Still 1 game
        assertEquals(18.0, stats.get("avgPoints")); // Updated to latest
        assertEquals(5.0, stats.get("avgRebounds")); // Updated
        assertEquals(20.0, stats.get("avgMinutes")); // Updated
    }

    @Test
    void shouldCalculateAveragesAcrossMultipleGames() {
        // Given: First completed game
        LiveStatDto game1 = LiveStatDto.builder()
                .gameId(2001)
                .teamId(20)
                .playerId(35)
                .points(30)
                .rebounds(10)
                .assists(5)
                .steals(3)
                .blocks(2)
                .fouls(4)
                .turnovers(3)
                .minutesPlayed(40.0)
                .build();

        // When: Process first game
        liveStatService.processLiveStat(game1);
        // And: Complete first game directly via repository
        redisStatsRepository.completeGame(2001, 35);
        // Given: Second completed game
        LiveStatDto game2 = LiveStatDto.builder()
                .gameId(2002)
                .teamId(20)
                .playerId(35)
                .points(20)
                .rebounds(6)
                .assists(8)
                .steals(1)
                .blocks(0)
                .fouls(2)
                .turnovers(4)
                .minutesPlayed(35.0)
                .build();

        // When: Process second game
        liveStatService.processLiveStat(game2);
        // And: Complete second game directly via repository
        redisStatsRepository.completeGame(2002, 35);
        // Then: Should calculate averages across both games
        Map<String, Object> stats = statsRetrievalService.getPlayerSeasonStats(35, "2024/25");
        
        assertEquals(2, stats.get("gamesPlayed"));
        
        // 50 total points / 2 games = 25.0 average
        assertEquals(25.0, (Double) stats.get("avgPoints"), 0.01);
        // 16 total rebounds / 2 games = 8.0 average
        assertEquals(8.0, (Double) stats.get("avgRebounds"), 0.01);
        // 13 total assists / 2 games = 6.5 average
        assertEquals(6.5, (Double) stats.get("avgAssists"), 0.01);
        // 75 total minutes / 2 games = 37.5 average
        assertEquals(37.5, (Double) stats.get("avgMinutes"), 0.01);
    }
}
