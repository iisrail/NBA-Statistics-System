package com.nba.stats.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.nba.stats.constants.RedisFields;
import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.dto.PlayerStatsDelta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisStatsRepository {
	private final RedisTemplate<String, Object> redisTemplate;
	
	

	/**
	 * Check if a player currently has a live game
	 */
    public boolean hasLiveGame(int playerId) {
        String pattern = "g:*:p:" + playerId;
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null && !keys.isEmpty();
    }	

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

		// Cast Object keys to String keys
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<Object, Object> entry : rawStats.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}

	/**
	 * Update season aggregates with delta
	 */
	public void updateSeasonAggregates(String seasonKey, PlayerStatsDelta delta) {
		// Increment all stats atomically
		// @formatter:off
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_POINTS, delta.getPoints());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_REBOUNDS, delta.getRebounds());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_ASSISTS, delta.getAssists());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_STEALS, delta.getSteals());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_BLOCKS, delta.getBlocks());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_FOULS, delta.getFouls());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_TURNOVERS, delta.getTurnovers());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.SUM_MINUTES, delta.getMinutesPlayed());
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.GAMES_PLAYED, delta.getGamesPlayed());
		log.debug("Updated season aggregates for key: {}", seasonKey);
		// @formatter:on
	}

	/**
	 * Get previous game stats for delta calculation
	 */
	public LiveStatDto getPreviousGameStats(String gameKey) {
		Map<Object, Object> rawStats = redisTemplate.opsForHash().entries(gameKey);

		if (rawStats.isEmpty()) {
			return null;
		}

		// Convert to LiveStatDto
		return LiveStatDto.builder()
				.gameId(getIntValue(rawStats, RedisFields.GAME_ID))
				.teamId(getIntValue(rawStats, RedisFields.TEAM_ID))
				.playerId(getIntValue(rawStats, RedisFields.PLAYER_ID))
				.points(getIntValue(rawStats, RedisFields.POINTS))
				.rebounds(getIntValue(rawStats, RedisFields.REBOUNDS))
				.assists(getIntValue(rawStats, RedisFields.ASSISTS))
				.steals(getIntValue(rawStats, RedisFields.STEALS))
				.blocks(getIntValue(rawStats, RedisFields.BLOCKS))
				.fouls(getIntValue(rawStats, RedisFields.FOULS))
				.turnovers(getIntValue(rawStats, RedisFields.TURNOVERS))
				.minutesPlayed(getDoubleValue(rawStats, RedisFields.MINUTES_PLAYED))
				.build();
	}

	/**
	 * Store current game stats for next delta calculation
	 */
	public void storeCurrentGameStats(String gameKey, LiveStatDto liveStat) {
		Map<String, Object> stats = Map.ofEntries(
				Map.entry(RedisFields.GAME_ID, liveStat.getGameId()),
				Map.entry(RedisFields.TEAM_ID, liveStat.getTeamId()),
				Map.entry(RedisFields.PLAYER_ID, liveStat.getPlayerId()),
				Map.entry(RedisFields.POINTS, liveStat.getPoints()),
				Map.entry(RedisFields.REBOUNDS, liveStat.getRebounds()),
				Map.entry(RedisFields.ASSISTS, liveStat.getAssists()),
				Map.entry(RedisFields.STEALS, liveStat.getSteals()),
				Map.entry(RedisFields.BLOCKS, liveStat.getBlocks()),
				Map.entry(RedisFields.FOULS, liveStat.getFouls()),
				Map.entry(RedisFields.TURNOVERS, liveStat.getTurnovers()),
				Map.entry(RedisFields.MINUTES_PLAYED, liveStat.getMinutesPlayed()));

		redisTemplate.opsForHash().putAll(gameKey, stats);
		// Add TTL expiration - game is "live" for 4 hours
	    redisTemplate.expire(gameKey, 4, TimeUnit.HOURS);
	    
	    log.debug("Stored current game stats with 4h TTL: {}", gameKey);
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
	 * Convert value to double safely
	 */
	private double getDoubleValue(Map<Object, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) {
			return 0.0;
		}

		if (value instanceof Number num) {
			return num.doubleValue();
		}

		if (value instanceof String str) {
			try {
				return Double.parseDouble(str);
			} catch (NumberFormatException e) {
				return 0.0;
			}
		}

		return 0.0;
	}

	public void incrementGamesPlayed(String seasonKey) {
		redisTemplate.opsForHash().increment(seasonKey, RedisFields.GAMES_PLAYED, 1);
		log.debug("Incremented games_played for key: {}", seasonKey);
	}
	
	/**
	 * Mark a game as completed (remove the live game key)
	 */
	public void completeGame(int gameId, int playerId) {
	    String gameKey = "g:%d:p:%d".formatted(gameId, playerId);
	    redisTemplate.delete(gameKey);
	    log.debug("Completed game: {}", gameKey);
	}
	
	/**
	 * Mark team-game combination as processed with TTL
	 */
	public void markTeamGameProcessed(int teamId, int gameId) {
	    String teamGameKey = "team_game:" + teamId + ":" + gameId;
	    Map<String, Object> marker = Map.of(
	        "processed", true, 
	        "timestamp", System.currentTimeMillis()
	    );
	    
	    // Store the marker
	    redisTemplate.opsForHash().putAll(teamGameKey, marker);
	    
	    // Set TTL - same as game stats (4 hours)
	    redisTemplate.expire(teamGameKey, 4, TimeUnit.HOURS);
	    
	    log.debug("Marked team {} game {} as processed with 4h TTL", teamId, gameId);
	}

	/**
	 * Check if team-game combination has been processed
	 */
	public boolean isTeamGameProcessed(int teamId, int gameId) {
	    String teamGameKey = "team_game:" + teamId + ":" + gameId;
	    return Boolean.TRUE.equals(redisTemplate.hasKey(teamGameKey));
	}
	
	/**
	 * Delete season stats from Redis
	 */
	public void deleteSeasonStats(String seasonKey) {
	    Boolean deleted = redisTemplate.delete(seasonKey);
	    if (Boolean.TRUE.equals(deleted)) {
	        log.debug("Deleted season stats key: {}", seasonKey);
	    } else {
	        log.debug("Season stats key not found or already deleted: {}", seasonKey);
	    }
	}

	/**
	 * Get all Redis keys matching a pattern
	 */
	public Set<String> getKeysByPattern(String pattern) {
	    Set<String> keys = redisTemplate.keys(pattern);
	    return keys != null ? keys : Collections.emptySet();
	}

	/**
	 * Get all players who participated in a specific game
	 */
	public Set<Integer> getPlayersInGame(int gameId) {
	    String pattern = "g:" + gameId + ":p:*";
	    Set<String> keys = redisTemplate.keys(pattern);
	    
	    if (keys == null || keys.isEmpty()) {
	        return Collections.emptySet();
	    }
	    
	    return keys.stream()
	        .map(key -> {
	            // Extract player ID from "g:1001:p:23" -> 23
	            String[] parts = key.split(":");
	            if (parts.length >= 4) {
	                try {
	                    return Integer.parseInt(parts[3]);
	                } catch (NumberFormatException e) {
	                    log.warn("Invalid player ID in key: {}", key);
	                    return null;
	                }
	            }
	            return null;
	        })
	        .filter(Objects::nonNull)
	        .collect(Collectors.toSet());
	}

	/**
	 * Get all teams who participated in a specific game
	 */
	public Set<Integer> getTeamsInGame(int gameId) {
	    String pattern = "g:" + gameId + ":p:*";
	    Set<String> keys = redisTemplate.keys(pattern);
	    
	    if (keys == null || keys.isEmpty()) {
	        return Collections.emptySet();
	    }
	    
	    Set<Integer> teamIds = new HashSet<>();
	    
	    for (String key : keys) {
	        // Get the game stats to extract team ID
	        LiveStatDto gameStats = getPreviousGameStats(key);
	        if (gameStats != null) {
	            teamIds.add(gameStats.getTeamId());
	        }
	    }
	    
	    return teamIds;
	}

	/**
	 * Delete multiple keys by pattern (for bulk cleanup)
	 */
	public void deleteKeysByPattern(String pattern) {
	    Set<String> keys = redisTemplate.keys(pattern);
	    if (keys != null && !keys.isEmpty()) {
	        Long deletedCount = redisTemplate.delete(keys);
	        log.debug("Deleted {} keys matching pattern: {}", deletedCount, pattern);
	    } else {
	        log.debug("No keys found matching pattern: {}", pattern);
	    }
	}	
}