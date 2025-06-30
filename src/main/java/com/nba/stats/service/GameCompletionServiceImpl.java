package com.nba.stats.service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameCompletionServiceImpl implements GameSubscriptionService, GameCompletionManager {
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * Subscribe player to game completion - store in Redis for tracking
     */
    @Override
	public void subscribePlayerToGame(int playerId, int gameId) {
        String gameSubscriptionKey = "game:" + gameId + ":players";
        
        // Just add player to the set of players in this game
        redisTemplate.opsForSet().add(gameSubscriptionKey, String.valueOf(playerId));
        redisTemplate.expire(gameSubscriptionKey, 4, TimeUnit.HOURS);
        
        log.debug("Added player {} to game {} tracking", playerId, gameId);
    }
    
    /**
     * Mark game as completed - update ALL player game snapshots
     */
    @Override
	public void markGameAsCompleted(int gameId) {
        String gameSubscriptionKey = "game:" + gameId + ":players";
        
        // Get all players who participated in this game
        Set<String> playerIds = redisTemplate.opsForSet().members(gameSubscriptionKey);
        
        if (playerIds == null || playerIds.isEmpty()) {
            log.info("Game {} completed but no players found", gameId);
            return;
        }
        
        log.info("Marking game {} as completed for {} players", gameId, playerIds.size());
        
        // Update each player's game snapshot to mark game as FINISHED
        for (String playerIdStr : playerIds) {
            int playerId = Integer.parseInt(playerIdStr);
            markPlayerGameAsFinished(playerId, gameId);
        }
        
        // Clean up the tracking set
        redisTemplate.delete(gameSubscriptionKey);
        
        log.info("Game {} marked as completed for all players", gameId);
    }
    
    /**
     * Update the specific player's game snapshot to mark as finished
     */
    private void markPlayerGameAsFinished(int playerId, int gameId) {
        String gameKey = "g:%d:p:%d".formatted(gameId, playerId);
        
        // Check if the game snapshot exists
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(gameKey))) {
            log.debug("No game snapshot found for player {} in game {}", playerId, gameId);
            return;
        }
        
        // Update the game snapshot to mark as FINISHED
        redisTemplate.opsForHash().put(gameKey, "gameStatus", "FINISHED");
        
        // Keep the TTL for cleanup, but now we have explicit status
        // The TTL is just for cleanup, not for determining if game is live
        
        log.debug("Marked game {} as FINISHED for player {}", gameId, playerId);
    }
}