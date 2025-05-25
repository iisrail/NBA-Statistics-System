package com.nba.stats.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nba.stats.repository.DbStatsRepository;
import com.nba.stats.repository.RedisStatsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFlushService {

    private final RedisStatsRepository redisRepository;
    private final DbStatsRepository dbRepository;
    
    @Value("${nba.current-season}")
    private String currentSeason;

    /**
     * Flush specific player's season stats from Redis to Database
     */
    @Transactional
    public void flushPlayerSeasonStats(int playerId) {
        String seasonKey = "s:%s:p:%d".formatted(currentSeason.replace('/', '_'), playerId);
        
        Map<String, Object> redisStats = redisRepository.getSeasonStats(seasonKey);
        if (redisStats == null || redisStats.isEmpty()) {
            log.debug("No Redis stats found for player {} current season", playerId);
            return;
        }

        try {
            // Upsert to database
            dbRepository.upsertPlayerSeasonStats(playerId, currentSeason, redisStats);
            
            // Remove from Redis after successful DB write
            redisRepository.deleteSeasonStats(seasonKey);
            
            log.info("Flushed player {} current season stats to database", playerId);
        } catch (Exception e) {
            log.error("Failed to flush player {} current season stats", playerId, e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    /**
     * Flush specific team's season stats from Redis to Database
     */
    @Transactional
    public void flushTeamSeasonStats(int teamId) {
        String seasonKey = "s:%s:t:%d".formatted(currentSeason.replace('/', '_'), teamId);
        
        Map<String, Object> redisStats = redisRepository.getSeasonStats(seasonKey);
        if (redisStats == null || redisStats.isEmpty()) {
            log.debug("No Redis stats found for team {} current season", teamId);
            return;
        }

        try {
            // Upsert to database
            dbRepository.upsertTeamSeasonStats(teamId, currentSeason, redisStats);
            
            // Remove from Redis after successful DB write
            redisRepository.deleteSeasonStats(seasonKey);
            
            log.info("Flushed team {} current season stats to database", teamId);
        } catch (Exception e) {
            log.error("Failed to flush team {} current season stats", teamId, e);
            throw e;
        }
    }

    /**
     * Flush all stats for a completed game
     */
    @Transactional
    public void flushGameStats(int gameId) {
        log.info("Flushing all stats for game {} current season", gameId);
        
        // Get all players who participated in this game
        Set<Integer> playerIds = redisRepository.getPlayersInGame(gameId);
        Set<Integer> teamIds = redisRepository.getTeamsInGame(gameId);
        
        // Flush player stats
        for (Integer playerId : playerIds) {
            try {
                flushPlayerSeasonStats(playerId);
            } catch (Exception e) {
                log.error("Failed to flush player {} stats for game {}", playerId, gameId, e);
                // Continue with other players
            }
        }
        
        // Flush team stats
        for (Integer teamId : teamIds) {
            try {
                flushTeamSeasonStats(teamId);
            } catch (Exception e) {
                log.error("Failed to flush team {} stats for game {}", teamId, gameId, e);
            }
        }
        
        log.info("Completed flushing stats for game {}", gameId);
    }

    /**
     * Flush all current season stats (end of season cleanup)
     */
    public void flushAllCurrentSeasonStats() {
        log.info("Starting bulk flush for current season {}", currentSeason);
        
        // Get all Redis keys for current season
        String playerPattern = "s:" + currentSeason.replace('/', '_') + ":p:*";
        String teamPattern = "s:" + currentSeason.replace('/', '_') + ":t:*";
        
        Set<String> playerKeys = redisRepository.getKeysByPattern(playerPattern);
        Set<String> teamKeys = redisRepository.getKeysByPattern(teamPattern);
        
        log.info("Found {} player keys and {} team keys to flush", 
                 playerKeys.size(), teamKeys.size());
        
        // Flush player stats
        for (String key : playerKeys) {
            try {
                int playerId = extractPlayerIdFromKey(key);
                flushPlayerSeasonStats(playerId);
            } catch (Exception e) {
                log.error("Failed to flush player key {}", key, e);
            }
        }
        
        // Flush team stats  
        for (String key : teamKeys) {
            try {
                int teamId = extractTeamIdFromKey(key);
                flushTeamSeasonStats(teamId);
            } catch (Exception e) {
                log.error("Failed to flush team key {}", key, e);
            }
        }
        
        log.info("Completed bulk flush for current season {}", currentSeason);
    }

    private int extractPlayerIdFromKey(String key) {
        // Extract from "s:2024_25:p:23" -> 23
        String[] parts = key.split(":");
        return Integer.parseInt(parts[3]);
    }

    private int extractTeamIdFromKey(String key) {
        // Extract from "s:2024_25:t:10" -> 10
        String[] parts = key.split(":");
        return Integer.parseInt(parts[3]);
    }
}
