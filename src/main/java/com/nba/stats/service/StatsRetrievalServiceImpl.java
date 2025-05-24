package com.nba.stats.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nba.stats.repository.PlayerStatsRepository;
import com.nba.stats.repository.RedisStatsRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Service
@Slf4j
public class StatsRetrievalServiceImpl implements StatsRetrievalService {

    private final RedisStatsRepository redisStatsRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final String currentSeason;

    public StatsRetrievalServiceImpl(
            RedisStatsRepository redisStatsRepository,
            PlayerStatsRepository playerStatsRepository,
            @Value("${nba.current-season}") String currentSeason) {
        
        this.redisStatsRepository = redisStatsRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.currentSeason = currentSeason;
    }

    @Override
    public Map<String, Object> getPlayerSeasonStats(int playerId) {
        String seasonKey = getPlayerSeasonKey(playerId);
        
        // Try Redis first (hot path)
        Map<String, Object> stats = redisStatsRepository.getSeasonStats(seasonKey);
        
        if (stats == null) {
            // Fallback to database and load into Redis
            log.debug("Stats not in Redis, loading from DB for player {}", playerId);
            stats = playerStatsRepository.getPlayerSeasonStats(playerId, currentSeason);
            
            if (stats != null && !stats.isEmpty()) {
                redisStatsRepository.storeSeasonStats(seasonKey, stats);
            }
        }
        
        // Calculate averages for response
        return calculatePlayerAverages(stats);
    }

    @Override
    public Map<String, Object> getTeamSeasonStats(int teamId) {
        String seasonKey = getTeamSeasonKey(teamId);
        
        // Try Redis first
        Map<String, Object> stats = redisStatsRepository.getSeasonStats(seasonKey);
        
        if (stats == null) {
            // Fallback to database
            log.debug("Stats not in Redis, loading from DB for team {}", teamId);
            stats = playerStatsRepository.getTeamSeasonStats(teamId, currentSeason);
            
            if (stats != null && !stats.isEmpty()) {
                redisStatsRepository.storeSeasonStats(seasonKey, stats);
            }
        }
        
        return calculateTeamAverages(stats);
    }

    /**
     * Calculate per-game averages for player stats
     */
    private Map<String, Object> calculatePlayerAverages(Map<String, Object> stats) {
        if (stats == null || stats.isEmpty()) {
            return Map.of(
                "playerId", 0,
                "gamesPlayed", 0,
                "avgPoints", 0.0,
                "avgRebounds", 0.0,
                "avgAssists", 0.0,
                "avgSteals", 0.0,
                "avgBlocks", 0.0,
                "avgFouls", 0.0,
                "avgTurnovers", 0.0,
                "avgMinutes", 0.0
            );
        }

        int gamesPlayed = (Integer) stats.getOrDefault("games_played", 0);
        
        if (gamesPlayed == 0) {
            return Map.of(
                "playerId", stats.getOrDefault("player_id", 0),
                "gamesPlayed", 0,
                "avgPoints", 0.0,
                "avgRebounds", 0.0,
                "avgAssists", 0.0,
                "avgSteals", 0.0,
                "avgBlocks", 0.0,
                "avgFouls", 0.0,
                "avgTurnovers", 0.0,
                "avgMinutes", 0.0
            );
        }

        // Convert seconds back to minutes for user response
        double avgMinutes = convertSecondsToMinutes(stats.get("sum_seconds"), gamesPlayed);

        return Map.of(
            "playerId", stats.getOrDefault("player_id", 0),
            "gamesPlayed", gamesPlayed,
            "avgPoints", divide(stats.get("sum_points"), gamesPlayed),
            "avgRebounds", divide(stats.get("sum_rebounds"), gamesPlayed),
            "avgAssists", divide(stats.get("sum_assists"), gamesPlayed),
            "avgSteals", divide(stats.get("sum_steals"), gamesPlayed),
            "avgBlocks", divide(stats.get("sum_blocks"), gamesPlayed),
            "avgFouls", divide(stats.get("sum_fouls"), gamesPlayed),
            "avgTurnovers", divide(stats.get("sum_turnovers"), gamesPlayed),
            "avgMinutes", avgMinutes  // Converted from seconds
        );
    }

    /**
     * Calculate per-game averages for team stats
     */
    private Map<String, Object> calculateTeamAverages(Map<String, Object> stats) {
        if (stats == null || stats.isEmpty()) {
            return Map.of(
                "teamId", 0,
                "gamesPlayed", 0,
                "avgPoints", 0.0,
                "avgRebounds", 0.0,
                "avgAssists", 0.0,
                "avgSteals", 0.0,
                "avgBlocks", 0.0,
                "avgFouls", 0.0,
                "avgTurnovers", 0.0,
                "avgMinutes", 0.0
            );
        }

        int gamesPlayed = (Integer) stats.getOrDefault("games_played", 0);
        
        if (gamesPlayed == 0) {
            return Map.of(
                "teamId", stats.getOrDefault("team_id", 0),
                "gamesPlayed", 0,
                "avgPoints", 0.0,
                "avgRebounds", 0.0,
                "avgAssists", 0.0,
                "avgSteals", 0.0,
                "avgBlocks", 0.0,
                "avgFouls", 0.0,
                "avgTurnovers", 0.0,
                "avgMinutes", 0.0
            );
        }

        // Convert seconds back to minutes for user response
        double avgMinutes = convertSecondsToMinutes(stats.get("sum_seconds"), gamesPlayed);

        return Map.of(
            "teamId", stats.getOrDefault("team_id", 0),
            "gamesPlayed", gamesPlayed,
            "avgPoints", divide(stats.get("sum_points"), gamesPlayed),
            "avgRebounds", divide(stats.get("sum_rebounds"), gamesPlayed),
            "avgAssists", divide(stats.get("sum_assists"), gamesPlayed),
            "avgSteals", divide(stats.get("sum_steals"), gamesPlayed),
            "avgBlocks", divide(stats.get("sum_blocks"), gamesPlayed),
            "avgFouls", divide(stats.get("sum_fouls"), gamesPlayed),
            "avgTurnovers", divide(stats.get("sum_turnovers"), gamesPlayed),
            "avgMinutes", avgMinutes  // Converted from seconds
        );
    }

    /**
     * Safe division that handles nulls and zeros
     */
    private double divide(Object numerator, int denominator) {
        if (numerator == null || denominator == 0) {
            return 0.0;
        }
        
        if (numerator instanceof Number num) {
            return num.doubleValue() / denominator;
        }
        
        return 0.0;
    }

    /**
     * Convert total seconds to average minutes per game
     */
    private double convertSecondsToMinutes(Object totalSeconds, int gamesPlayed) {
        if (totalSeconds == null || gamesPlayed == 0) {
            return 0.0;
        }
        
        if (totalSeconds instanceof Number num) {
            double avgSeconds = num.doubleValue() / gamesPlayed;
            return Math.round((avgSeconds / 60.0) * 10.0) / 10.0;  // Round to 1 decimal place
        }
        
        return 0.0;
    }

    /**
     * Generate Redis key for player season stats
     */
    private String getPlayerSeasonKey(int playerId) {
        return "s:%s:p:%d".formatted(currentSeason.replace('/', '_'), playerId);
    }

    /**
     * Generate Redis key for team season stats
     */
    private String getTeamSeasonKey(int teamId) {
        return "s:%s:t:%d".formatted(currentSeason.replace('/', '_'), teamId);
    }
}