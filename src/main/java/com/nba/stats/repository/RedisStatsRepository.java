package com.nba.stats.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.dto.PlayerStatsDelta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisStatsRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check if season stats exist in Redis
     */
    public boolean seasonStatsExist(String seasonKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(seasonKey));
    }

    /**
     * Store season stats in Redis
     */
    public void storeSeasonStats(String seasonKey, Map<String, Object> stats) {
        redisTemplate.opsForHash().putAll(seasonKey, stats);
        log.debug("Stored season stats in Redis: {}", seasonKey);
    }

    /**
     * Get season stats from Redis
     */
    public Map<String, Object> getSeasonStats(String seasonKey) {
        Map<Object, Object> rawStats = redisTemplate.opsForHash().entries(seasonKey);
        
        if (rawStats.isEmpty()) {
            return null;
        }
        
        // Convert Object keys to String keys
        return rawStats.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                entry -> entry.getKey().toString(),
                Map.Entry::getValue
            ));
    }

    /**
     * Update season aggregates with delta
     */
    public void updateSeasonAggregates(String seasonKey, PlayerStatsDelta delta) {
        // Convert minutes to seconds for easy Redis increment
        int deltaSeconds = minutesToSeconds(delta.getMinutesPlayed());
        //s:2024_25:p:25
        // Increment all stats atomically
        int debug = delta.getPoints();
        System.out.println(debug);
        redisTemplate.opsForHash().increment(seasonKey, "sum_points", delta.getPoints());
        redisTemplate.opsForHash().increment(seasonKey, "sum_rebounds", delta.getRebounds());
        redisTemplate.opsForHash().increment(seasonKey, "sum_assists", delta.getAssists());
        redisTemplate.opsForHash().increment(seasonKey, "sum_steals", delta.getSteals());
        redisTemplate.opsForHash().increment(seasonKey, "sum_blocks", delta.getBlocks());
        redisTemplate.opsForHash().increment(seasonKey, "sum_fouls", delta.getFouls());
        redisTemplate.opsForHash().increment(seasonKey, "sum_turnovers", delta.getTurnovers());
        redisTemplate.opsForHash().increment(seasonKey, "sum_seconds", deltaSeconds);  // Store as seconds!
        
        log.debug("Updated season aggregates for key: {}", seasonKey);
    }

    /**
     * Get previous game stats for delta calculation
     */
    public LiveStatDto getPreviousGameStats(String gameKey) {
        Map<Object, Object> rawStats = redisTemplate.opsForHash().entries(gameKey);
        
        if (rawStats.isEmpty()) {
            return null;
        }
        
        // Convert seconds back to minutes for LiveStatDto
        int seconds = getIntValue(rawStats, "seconds");
        BigDecimal minutes = secondsToMinutes(seconds);
        
        // Convert to LiveStatDto
        return LiveStatDto.builder()
            .gameId(getIntValue(rawStats, "gameId"))
            .teamId(getIntValue(rawStats, "teamId"))
            .playerId(getIntValue(rawStats, "playerId"))
            .points(getIntValue(rawStats, "points"))
            .rebounds(getIntValue(rawStats, "rebounds"))
            .assists(getIntValue(rawStats, "assists"))
            .steals(getIntValue(rawStats, "steals"))
            .blocks(getIntValue(rawStats, "blocks"))
            .fouls(getIntValue(rawStats, "fouls"))
            .turnovers(getIntValue(rawStats, "turnovers"))
            .minutesPlayed(minutes)  // Converted back to BigDecimal minutes
            .build();
    }

    /**
     * Store current game stats for next delta calculation
     */
    public void storeCurrentGameStats(String gameKey, LiveStatDto liveStat) {
        // Convert minutes to seconds for consistency
        int seconds = minutesToSeconds(liveStat.getMinutesPlayed());
        
        Map<String, Object> stats = Map.ofEntries(
        	    Map.entry("gameId", liveStat.getGameId()),
        	    Map.entry("teamId", liveStat.getTeamId()),
        	    Map.entry("playerId", liveStat.getPlayerId()),
        	    Map.entry("points", liveStat.getPoints()),
        	    Map.entry("rebounds", liveStat.getRebounds()),
        	    Map.entry("assists", liveStat.getAssists()),
        	    Map.entry("steals", liveStat.getSteals()),
        	    Map.entry("blocks", liveStat.getBlocks()),
        	    Map.entry("fouls", liveStat.getFouls()),
        	    Map.entry("turnovers", liveStat.getTurnovers()),
        	    Map.entry("seconds", seconds) // Store as seconds instead of minutes
        	);
        
        redisTemplate.opsForHash().putAll(gameKey, stats);
        log.debug("Stored current game stats: {}", gameKey);
    }

    /**
     * Helper method to safely get integer values
     */
    private int getIntValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Convert BigDecimal minutes to integer seconds
     */
    private int minutesToSeconds(BigDecimal minutes) {
        if (minutes == null) {
            return 0;
        }
        return minutes.multiply(BigDecimal.valueOf(60)).intValue();
    }

    /**
     * Convert integer seconds back to BigDecimal minutes
     */
    private BigDecimal secondsToMinutes(int seconds) {
         BigDecimal retValue = BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(60), 1, java.math.RoundingMode.HALF_UP);
         return retValue;
    }
}