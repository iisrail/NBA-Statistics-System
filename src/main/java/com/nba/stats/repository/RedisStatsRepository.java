package com.nba.stats.repository;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.nba.stats.constants.RedisFields;
import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.dto.PlayerStatsDelta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisStatsRepository {

    // Use ONLY StringRedisTemplate for consistency
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Check if season stats exist in Redis
     */
    public boolean seasonStatsExist(String seasonKey) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(seasonKey));
    }

    /**
     * Store season stats in Redis - FIXED to handle DB objects properly
     */
    public void storeSeasonStats(String seasonKey, Map<String, Object> stats) {
        try {
            // Convert all values to strings to avoid serialization issues
            Map<String, String> stringStats = convertToStringMap(stats);
            stringRedisTemplate.opsForHash().putAll(seasonKey, stringStats);
            log.debug("Stored season stats in Redis: {} with {} fields", seasonKey, stringStats.size());
        } catch (Exception e) {
            log.error("Failed to store season stats for key: {}", seasonKey, e);
            throw e;
        }
    }

    /**
     * Get season stats from Redis
     */
    public Map<String, Object> getSeasonStats(String seasonKey) {
        try {
            Map<Object, Object> rawStats = stringRedisTemplate.opsForHash().entries(seasonKey);

            if (rawStats.isEmpty()) {
                return null;
            }

            // Convert back to proper types
            return convertFromStringMap(rawStats);
        } catch (Exception e) {
            log.error("Failed to get season stats for key: {}", seasonKey, e);
            return null;
        }
    }

    /**
     * Update season aggregates with delta
     */
	public void updateSeasonAggregates(String seasonKey, PlayerStatsDelta delta) {
		// ensures Redis's concurrency safety
		stringRedisTemplate.execute(new SessionCallback<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object execute(RedisOperations operations) {
				operations.watch(seasonKey);
				operations.multi();
					// @formatter:off
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_POINTS, delta.getPoints());
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_REBOUNDS, delta.getRebounds());
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_ASSISTS, delta.getAssists());
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_STEALS, delta.getSteals());
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_BLOCKS, delta.getBlocks());
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_FOULS, delta.getFouls());
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_TURNOVERS, delta.getTurnovers());
				operations.opsForHash().increment(seasonKey, RedisFields.SUM_MINUTES, delta.getMinutesPlayed());
				operations.opsForHash().increment(seasonKey, RedisFields.GAMES_PLAYED, delta.getGamesPlayed());
					// @formatter:on
				List<Object> results = operations.exec();
				if (results == null) {
					log.warn("Redis transaction failed for key: {}", seasonKey);
					throw new IllegalStateException("Transaction failed — retry required.");
				}
				return null;
			}
		});
	}

    /**
     * Get previous game stats for delta calculation
     */
    public LiveStatDto getPreviousGameStats(String gameKey) {
        try {
            Map<Object, Object> rawStats = stringRedisTemplate.opsForHash().entries(gameKey);

            if (rawStats.isEmpty()) {
                return null;
            }

            // Convert string values back to proper types
            return LiveStatDto.builder()
                    .gameId(getIntFromMap(rawStats, RedisFields.GAME_ID))
                    .teamId(getIntFromMap(rawStats, RedisFields.TEAM_ID))
                    .playerId(getIntFromMap(rawStats, RedisFields.PLAYER_ID))
                    .points(getIntFromMap(rawStats, RedisFields.POINTS))
                    .rebounds(getIntFromMap(rawStats, RedisFields.REBOUNDS))
                    .assists(getIntFromMap(rawStats, RedisFields.ASSISTS))
                    .steals(getIntFromMap(rawStats, RedisFields.STEALS))
                    .blocks(getIntFromMap(rawStats, RedisFields.BLOCKS))
                    .fouls(getIntFromMap(rawStats, RedisFields.FOULS))
                    .turnovers(getIntFromMap(rawStats, RedisFields.TURNOVERS))
                    .minutesPlayed(getDoubleFromMap(rawStats, RedisFields.MINUTES_PLAYED))
                    .build();
        } catch (Exception e) {
            log.error("Failed to get previous game stats for key: {}", gameKey, e);
            return null;
        }
    }

    /**
     * Store current game stats for next delta calculation with TTL
     */
    public void storeCurrentGameStats(String gameKey, LiveStatDto liveStat) {
        try {
            Map<String, String> stats = Map.ofEntries(
                    Map.entry(RedisFields.GAME_ID, String.valueOf(liveStat.getGameId())),
                    Map.entry(RedisFields.TEAM_ID, String.valueOf(liveStat.getTeamId())),
                    Map.entry(RedisFields.PLAYER_ID, String.valueOf(liveStat.getPlayerId())),
                    Map.entry(RedisFields.POINTS, String.valueOf(liveStat.getPoints())),
                    Map.entry(RedisFields.REBOUNDS, String.valueOf(liveStat.getRebounds())),
                    Map.entry(RedisFields.ASSISTS, String.valueOf(liveStat.getAssists())),
                    Map.entry(RedisFields.STEALS, String.valueOf(liveStat.getSteals())),
                    Map.entry(RedisFields.BLOCKS, String.valueOf(liveStat.getBlocks())),
                    Map.entry(RedisFields.FOULS, String.valueOf(liveStat.getFouls())),
                    Map.entry(RedisFields.TURNOVERS, String.valueOf(liveStat.getTurnovers())),
                    Map.entry(RedisFields.MINUTES_PLAYED, String.valueOf(liveStat.getMinutesPlayed()))
            );

            stringRedisTemplate.opsForHash().putAll(gameKey, stats);
            stringRedisTemplate.expire(gameKey, 4, TimeUnit.HOURS);
            
            log.debug("Stored current game stats with 4h TTL: {}", gameKey);
        } catch (Exception e) {
            log.error("Failed to store current game stats for key: {}", gameKey, e);
            throw e;
        }
    }

    /**
     * Check if a player currently has a live game (based on TTL)
     */
    public boolean hasLiveGame(int playerId) {
        try {
            String pattern = "g:*:p:" + playerId;
            Set<String> keys = stringRedisTemplate.keys(pattern);
            return keys != null && !keys.isEmpty();
        } catch (Exception e) {
            log.error("Failed to check live game for player: {}", playerId, e);
            return false;
        }
    }

    /**
     * Mark team-game combination as processed with TTL
     */
    public void markTeamGameProcessed(int teamId, int gameId) {
        try {
            String teamGameKey = "team_game:" + teamId + ":" + gameId;
            Map<String, String> marker = Map.of(
                "processed", "true", 
                "timestamp", String.valueOf(System.currentTimeMillis())
            );
            
            stringRedisTemplate.opsForHash().putAll(teamGameKey, marker);
            stringRedisTemplate.expire(teamGameKey, 4, TimeUnit.HOURS);
            
            log.debug("Marked team {} game {} as processed with 4h TTL", teamId, gameId);
        } catch (Exception e) {
            log.error("Failed to mark team-game as processed: team={}, game={}", teamId, gameId, e);
        }
    }

    /**
     * Check if team-game combination has been processed
     */
    public boolean isTeamGameProcessed(int teamId, int gameId) {
        try {
            String teamGameKey = "team_game:" + teamId + ":" + gameId;
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(teamGameKey));
        } catch (Exception e) {
            log.error("Failed to check team-game processed status: team={}, game={}", teamId, gameId, e);
            return false;
        }
    }

    /**
     * Complete a game (remove the live game key)
     */
    public void completeGame(int gameId, int playerId) {
        try {
            String gameKey = "g:%d:p:%d".formatted(gameId, playerId);
            stringRedisTemplate.delete(gameKey);
            log.debug("Completed game: {}", gameKey);
        } catch (Exception e) {
            log.error("Failed to complete game: gameId={}, playerId={}", gameId, playerId, e);
        }
    }

    /**
     * Get all Redis keys matching a pattern
     */
    public Set<String> getKeysByPattern(String pattern) {
        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);
            return keys != null ? keys : Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to get keys by pattern: {}", pattern, e);
            return Collections.emptySet();
        }
    }

    /**
     * Get all players who participated in a specific game
     */
    public Set<Integer> getPlayersInGame(int gameId) {
        try {
            String pattern = "g:" + gameId + ":p:*";
            Set<String> keys = stringRedisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return Collections.emptySet();
            }
            
            Set<Integer> playerIds = new HashSet<>();
            for (String key : keys) {
                try {
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        int playerId = Integer.parseInt(parts[3]);
                        playerIds.add(playerId);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid player ID in key: {}", key);
                }
            }
            
            return playerIds;
        } catch (Exception e) {
            log.error("Failed to get players in game: {}", gameId, e);
            return Collections.emptySet();
        }
    }
    
    /**
     * Mark a season stats key as dirty (needs syncing)
     */
    public void markSeasonStatsDirty(String seasonKey) {
        try {
            String dirtyKey = "dirty:" + seasonKey;
            stringRedisTemplate.opsForValue().set(dirtyKey, "1");
            log.debug("Marked season stats as dirty: {}", dirtyKey);
        } catch (Exception e) {
            log.error("Failed to mark season stats as dirty: {}", seasonKey, e);
        }
    }

    /**
     * Delete dirty flag after successful sync
     */
    public void deleteDirtyFlag(String dirtyKey) {
        try {
            stringRedisTemplate.delete(dirtyKey);
            log.debug("Removed dirty flag: {}", dirtyKey);
        } catch (Exception e) {
            log.error("Failed to delete dirty flag: {}", dirtyKey, e);
        }
    }

    // Helper methods for conversion
    private Map<String, String> convertToStringMap(Map<String, Object> originalMap) {
        Map<String, String> stringMap = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value == null) {
                stringMap.put(key, "0");
            } else {
                String stringValue = convertObjectToString(value);
                stringMap.put(key, stringValue);
            }
        }
        
        return stringMap;
    }

    private String convertObjectToString(Object value) {
        if (value == null) {
            return "0";
        }
        
        // Handle BigDecimal from database
        if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).toString();
        }
        
        // Handle Timestamp from database
        if (value instanceof java.sql.Timestamp) {
            return String.valueOf(((java.sql.Timestamp) value).getTime());
        }
        
        // Handle Date from database
        if (value instanceof java.util.Date) {
            return String.valueOf(((java.util.Date) value).getTime());
        }
        
        // Handle Long (common from database)
        if (value instanceof Long) {
            return value.toString();
        }
        
        // Handle Double
        if (value instanceof Double) {
            return value.toString();
        }
        
        // Handle Integer
        if (value instanceof Integer) {
            return value.toString();
        }
        
        // Handle Boolean
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        
        // Default: convert to string
        return value.toString();
    }

    private Map<String, Object> convertFromStringMap(Map<Object, Object> rawMap) {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            
            Object convertedValue = convertStringValue(key, value);
            result.put(key, convertedValue);
        }
        
        return result;
    }

    private Object convertStringValue(String key, String value) {
        try {
            // Minutes are stored as double
            if (key.contains("minutes") || key.equals("sum_minutes")) {
                return Double.parseDouble(value);
            }
            
            // Everything else as integer
            return Integer.parseInt(value);
            
        } catch (NumberFormatException e) {
            log.warn("Failed to convert value '{}' for key '{}', defaulting to 0", value, key);
            return key.contains("minutes") ? 0.0 : 0;
        }
    }

    private int getIntFromMap(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        
        try {
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert '{}' to int for key '{}'", value, key);
            return 0;
        }
    }

    private double getDoubleFromMap(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        
        try {
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to convert '{}' to double for key '{}'", value, key);
            return 0.0;
        }
    }
}