package com.nba.stats.service;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nba.stats.repository.DbStatsRepository;
import com.nba.stats.repository.RedisStatsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSyncService {

    private final RedisStatsRepository redisRepository;
    private final DbStatsRepository dbRepository;
    
    @Value("${nba.current-season}")
    private String currentSeason;
    
    @Value("${nba.sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * Sync Redis season stats to database every minute (configurable)
     * Only syncs keys that have been marked as dirty (modified since last sync)
     */
    @Scheduled(fixedRateString = "${nba.sync.interval-ms:60000}") // Default: 1 minute
    @ConditionalOnProperty(name = "nba.sync.enabled", havingValue = "true", matchIfMissing = true)
    public void syncRedisToDatabase() {
        if (!syncEnabled) {           
            return;
        }
        log.debug("Starting scheduled Redis-DB sync...");
        
        try {
            // Find dirty keys (only these need syncing)
            String dirtyPlayerPattern = "dirty:s:" + currentSeason.replace('/', '_') + ":p:*";
            String dirtyTeamPattern = "dirty:s:" + currentSeason.replace('/', '_') + ":t:*";
            
            Set<String> dirtyPlayerKeys = redisRepository.getKeysByPattern(dirtyPlayerPattern);
            Set<String> dirtyTeamKeys = redisRepository.getKeysByPattern(dirtyTeamPattern);
            
            if (dirtyPlayerKeys.isEmpty() && dirtyTeamKeys.isEmpty()) {
                log.debug("No dirty keys found - nothing to sync");
                return;
            }
            
            log.info("Syncing {} dirty player and {} dirty team keys to database", 
                     dirtyPlayerKeys.size(), dirtyTeamKeys.size());
            
            // Sync dirty player stats
            for (String dirtyKey : dirtyPlayerKeys) {
                try {
                    // Extract original season key: "dirty:s:2024_25:p:123" -> "s:2024_25:p:123"
                    String seasonKey = dirtyKey.substring("dirty:".length());
                    int playerId = extractPlayerIdFromKey(seasonKey);
                    
                    syncPlayerSeasonStats(playerId);
                    
                    // Remove dirty flag after successful sync
                    redisRepository.deleteDirtyFlag(dirtyKey);
                    
                } catch (Exception e) {
                    log.error("Failed to sync dirty player key {}", dirtyKey, e);
                }
            }
            
            // Sync dirty team stats  
            for (String dirtyKey : dirtyTeamKeys) {
                try {
                    // Extract original season key: "dirty:s:2024_25:t:456" -> "s:2024_25:t:456"
                    String seasonKey = dirtyKey.substring("dirty:".length());
                    int teamId = extractTeamIdFromKey(seasonKey);
                    
                    syncTeamSeasonStats(teamId);
                    
                    // Remove dirty flag after successful sync
                    redisRepository.deleteDirtyFlag(dirtyKey);
                    
                } catch (Exception e) {
                    log.error("Failed to sync dirty team key {}", dirtyKey, e);
                }
            }
            
            log.debug("Completed scheduled Redis-DB sync - processed {} player and {} team dirty keys", 
                     dirtyPlayerKeys.size(), dirtyTeamKeys.size());
                     
        } catch (Exception e) {
            log.error("Error during scheduled Redis-DB sync", e);
        }
    }

    /**
     * Sync specific player's season stats from Redis to Database
     */
    @Transactional
    private void syncPlayerSeasonStats(int playerId) {
        String seasonKey = buildPlayerSeasonKey(playerId);
        
        Map<String, Object> redisStats = redisRepository.getSeasonStats(seasonKey);
        if (redisStats == null || redisStats.isEmpty()) {
            log.debug("No Redis stats found for player {} current season", playerId);
            return;
        }

        try {
            dbRepository.upsertPlayerSeasonStats(playerId, currentSeason, redisStats);
            log.debug("Synced player {} current season stats to database", playerId);
        } catch (Exception e) {
            log.error("Failed to sync player {} current season stats", playerId, e);
            throw e;
        }
    }

    /**
     * Sync specific team's season stats from Redis to Database
     */
    @Transactional
    private void syncTeamSeasonStats(int teamId) {
        String seasonKey = buildTeamSeasonKey(teamId);
        
        Map<String, Object> redisStats = redisRepository.getSeasonStats(seasonKey);
        if (redisStats == null || redisStats.isEmpty()) {
            log.debug("No Redis stats found for team {} current season", teamId);
            return;
        }

        try {
            dbRepository.upsertTeamSeasonStats(teamId, currentSeason, redisStats);
            log.debug("Synced team {} current season stats to database", teamId);
        } catch (Exception e) {
            log.error("Failed to sync team {} current season stats", teamId, e);
            throw e;
        }
    }

    // ========== HELPER METHODS ==========
    
    private String buildPlayerSeasonKey(int playerId) {
        return "s:%s:p:%d".formatted(currentSeason.replace('/', '_'), playerId);
    }
    
    private String buildTeamSeasonKey(int teamId) {
        return "s:%s:t:%d".formatted(currentSeason.replace('/', '_'), teamId);
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