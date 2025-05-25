package com.nba.stats.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

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
    "nba.current-season=2024/25"
})
@Transactional
class DbStatsRepositoryTest {

    @Autowired
    private DbStatsRepository repository;

    @Test
    void shouldReturnEmptyStatsForNewPlayer() {
        Map<String, Object> stats = repository.getPlayerSeasonStats(999, "2024/25");
        
        assertNotNull(stats);
        assertEquals(0, stats.get("games_played"));
        assertEquals(0, stats.get("sum_points"));
        assertEquals(0, stats.get("sum_rebounds"));
        assertEquals(0, stats.get("sum_assists"));
        assertEquals(0.0, stats.get("sum_minutes"));
    }

    @Test
    void shouldReturnExistingPlayerStats() {
        // LeBron has test data: 71 games, 1800 points in 2023/24
        Map<String, Object> stats = repository.getPlayerSeasonStats(23, "2023/24");
        
        assertNotNull(stats);
        assertEquals(71, stats.get("games_played"));
        assertEquals(1800, stats.get("sum_points"));
        assertEquals(550, stats.get("sum_rebounds"));
        assertEquals(600, stats.get("sum_assists"));
    }

    @Test
    void shouldReturnEmptyStatsForNewTeam() {
        Map<String, Object> stats = repository.getTeamSeasonStats(999, "2024/25");
        
        assertNotNull(stats);
        assertEquals(0, stats.get("games_played"));
        assertEquals(0, stats.get("sum_points"));
    }

    @Test
    void shouldHandleDifferentSeasons() {
        // Same player, different seasons should be independent
        Map<String, Object> currentSeason = repository.getPlayerSeasonStats(23, "2024/25");
        Map<String, Object> lastSeason = repository.getPlayerSeasonStats(23, "2023/24");
        
        // Current season should be empty
        assertEquals(0, currentSeason.get("games_played"));
        
        // Last season should have data
        assertEquals(71, lastSeason.get("games_played"));
    }
}
