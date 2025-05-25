package com.nba.stats.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:test-schema.sql",
    "nba.current-season=2024/25"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NBAStatsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
    void shouldProcessLiveStatAndRetrievePlayerStats() throws Exception {
        // Given: Live stat JSON
        String liveStatJson = """
            {
                "gameId": 1001,
                "teamId": 10,
                "playerId": 23,
                "points": 25,
                "rebounds": 8,
                "assists": 6,
                "steals": 2,
                "blocks": 1,
                "fouls": 3,
                "turnovers": 2,
                "minutesPlayed": 35.5
            }
            """;

        // When: Send live stat via REST API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(liveStatJson, headers);
        
        ResponseEntity<Void> putResponse = restTemplate.exchange(
            "http://localhost:" + port + "/stat/live/game",
            HttpMethod.PUT,
            request,
            Void.class
        );

        // Then: PUT should succeed
        assertTrue(putResponse.getStatusCode().is2xxSuccessful());

        // And: Player stats should be retrievable
        ResponseEntity<Map> getResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/stat/player/23",
            Map.class
        );

        assertTrue(getResponse.getStatusCode().is2xxSuccessful());
        
        Map<String, Object> playerStats = getResponse.getBody();
        assertNotNull(playerStats);
        assertEquals(23, playerStats.get("playerId"));
        assertEquals("LeBron James", playerStats.get("playerName"));
        assertEquals(1, playerStats.get("gamesPlayed"));
        assertEquals(25.0, playerStats.get("avgPoints"));
        assertEquals(8.0, playerStats.get("avgRebounds"));
        assertEquals(6.0, playerStats.get("avgAssists"));
        assertEquals(35.5, playerStats.get("avgMinutes"));
    }

    @Test
    void shouldRetrieveTeamStatsAfterPlayerUpdate() throws Exception {
        // Given: Two players on same team
        String player1Json = """
            {
                "gameId": 3001,
                "teamId": 25,
                "playerId": 40,
                "points": 25,
                "rebounds": 8,
                "assists": 5,
                "steals": 2,
                "blocks": 1,
                "fouls": 3,
                "turnovers": 2,
                "minutesPlayed": 35.0
            }
            """;

        String player2Json = """
            {
                "gameId": 3001,
                "teamId": 25,
                "playerId": 41,
                "points": 18,
                "rebounds": 6,
                "assists": 3,
                "steals": 1,
                "blocks": 2,
                "fouls": 4,
                "turnovers": 1,
                "minutesPlayed": 32.0
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When: Send both player stats
        restTemplate.exchange(
            "http://localhost:" + port + "/stat/live/game",
            HttpMethod.PUT,
            new HttpEntity<>(player1Json, headers),
            Void.class
        );

        restTemplate.exchange(
            "http://localhost:" + port + "/stat/live/game",
            HttpMethod.PUT,
            new HttpEntity<>(player2Json, headers),
            Void.class
        );

        // Then: Team stats should aggregate both players
        ResponseEntity<Map> teamResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/stat/team/25",
            Map.class
        );

        assertTrue(teamResponse.getStatusCode().is2xxSuccessful());
        
        Map<String, Object> teamStats = teamResponse.getBody();
        assertNotNull(teamStats);
        assertEquals(25, teamStats.get("teamId"));
        assertEquals("Boston Celtics", teamStats.get("teamName"));
        assertEquals(1, teamStats.get("gamesPlayed")); // 1 team game (not 2)
        assertEquals(43.0, teamStats.get("avgPoints")); // (25+18) total in 1 game = 43.0
        assertEquals(14.0, teamStats.get("avgRebounds")); // (8+6) total in 1 game = 14.0
        assertEquals(8.0, teamStats.get("avgAssists")); // (5+3) total in 1 game = 8.0
    }

    @Test
    void shouldHandleSeasonParameterCorrectly() throws Exception {
        // Given: Current season stat
        String currentSeasonJson = """
            {
                "gameId": 4001,
                "teamId": 30,
                "playerId": 50,
                "points": 20,
                "rebounds": 5,
                "assists": 3,
                "steals": 1,
                "blocks": 0,
                "fouls": 2,
                "turnovers": 1,
                "minutesPlayed": 28.0
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When: Send current season stat
        restTemplate.exchange(
            "http://localhost:" + port + "/stat/live/game",
            HttpMethod.PUT,
            new HttpEntity<>(currentSeasonJson, headers),
            Void.class
        );

        // Then: Default season should work
        ResponseEntity<Map> defaultResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/stat/player/50",
            Map.class
        );
        
        assertTrue(defaultResponse.getStatusCode().is2xxSuccessful());
        Map<String, Object> defaultStats = defaultResponse.getBody();
        assertEquals(20.0, defaultStats.get("avgPoints"));

        // And: Explicit season parameter should work
        ResponseEntity<Map> explicitResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/stat/player/50?season=2024/25",
            Map.class
        );
        
        assertTrue(explicitResponse.getStatusCode().is2xxSuccessful());
        Map<String, Object> explicitStats = explicitResponse.getBody();
        assertEquals(20.0, explicitStats.get("avgPoints"));

        // And: Different season should return empty stats
        ResponseEntity<Map> differentResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/stat/player/50?season=2023/24",
            Map.class
        );
        
        assertTrue(differentResponse.getStatusCode().is2xxSuccessful());
        Map<String, Object> differentStats = differentResponse.getBody();
        assertEquals(0.0, differentStats.get("avgPoints"));
    }

    @Test
    void shouldHandleHistoricalDataCorrectly() throws Exception {
        // Test player with existing historical data (LeBron 2023/24)
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/stat/player/23?season=2023/24",
            Map.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful());
        
        Map<String, Object> stats = response.getBody();
        assertNotNull(stats);
        assertEquals(23, stats.get("playerId"));
        assertEquals("LeBron James", stats.get("playerName"));
        assertEquals(71, stats.get("gamesPlayed"));
        
        // Check calculated averages
        Double avgPoints = (Double) stats.get("avgPoints");
        assertTrue(avgPoints > 25.0 && avgPoints < 26.0); // 1800/71 ≈ 25.35
    }
}