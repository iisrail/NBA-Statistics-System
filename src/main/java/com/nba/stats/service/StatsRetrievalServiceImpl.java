package com.nba.stats.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.nba.stats.constants.RedisFields;
import com.nba.stats.constants.ResponseFields;
import com.nba.stats.repository.DbStatsRepository;
import com.nba.stats.repository.RedisStatsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@RequiredArgsConstructor
@Service
@Slf4j
public class StatsRetrievalServiceImpl implements StatsRetrievalService {

    private final RedisStatsRepository redisStatsRepository;
    private final DbStatsRepository playerStatsRepository;
    private final RosterService rosterService;
    
    private static final Map<String, Object> EMPTY_PLAYER_STATS = Map.ofEntries(
    	    Map.entry(ResponseFields.GAMES_PLAYED, 0),
    	    Map.entry(ResponseFields.AVG_POINTS, 0.0),
    	    Map.entry(ResponseFields.AVG_REBOUNDS, 0.0),
    	    Map.entry(ResponseFields.AVG_ASSISTS, 0.0),
    	    Map.entry(ResponseFields.AVG_STEALS, 0.0),
    	    Map.entry(ResponseFields.AVG_BLOCKS, 0.0),
    	    Map.entry(ResponseFields.AVG_FOULS, 0.0),
    	    Map.entry(ResponseFields.AVG_TURNOVERS, 0.0),
    	    Map.entry(ResponseFields.AVG_MINUTES, 0.0)
    	);

    private static final Map<String, Object> EMPTY_TEAM_STATS = Map.ofEntries(  // Change to ofEntries
    	    Map.entry(ResponseFields.GAMES_PLAYED, 0),
    	    Map.entry(ResponseFields.AVG_POINTS, 0.0),
    	    Map.entry(ResponseFields.AVG_REBOUNDS, 0.0),
    	    Map.entry(ResponseFields.AVG_ASSISTS, 0.0),
    	    Map.entry(ResponseFields.AVG_STEALS, 0.0),
    	    Map.entry(ResponseFields.AVG_BLOCKS, 0.0),
    	    Map.entry(ResponseFields.AVG_FOULS, 0.0),
    	    Map.entry(ResponseFields.AVG_TURNOVERS, 0.0)
    	);


    
    @Override
    public Map<String, Object> getPlayerSeasonStats(int playerId, String season) {
    	log.info("TEST: Received season parameter: '{}'", season);
        String seasonKey = getRosterSeasonKey(playerId, season, ResponseFields.PLATER_TYPE);//s:2024_25:p:2
        
        // Try Redis first (hot path)
        Map<String, Object> stats = redisStatsRepository.getSeasonStats(seasonKey);
        
        if (stats == null) {
            // Fallback to database and load into Redis
            log.debug("Stats not in Redis, loading from DB for player {}", playerId);
            stats = playerStatsRepository.getPlayerSeasonStats(playerId, season);
            
            if (stats != null && !stats.isEmpty()) {
                redisStatsRepository.storeSeasonStats(seasonKey, stats);
            }
        }
        
        // Calculate averages for response
        return calculatePlayerAverages(stats,playerId);
    }

    @Override
    public Map<String, Object> getTeamSeasonStats(int teamId, String season) {
    	log.info("TEST: Received season parameter: '{}'", season);
        String seasonKey = getRosterSeasonKey(teamId, season, ResponseFields.TEAM_TYPE);
        
        // Try Redis first
        Map<String, Object> stats = redisStatsRepository.getSeasonStats(seasonKey);
        
        if (stats == null) {
            // Fallback to database
            log.debug("Stats not in Redis, loading from DB for team {}", teamId);
            stats = playerStatsRepository.getTeamSeasonStats(teamId, season);
            
            if (stats != null && !stats.isEmpty()) {
                redisStatsRepository.storeSeasonStats(seasonKey, stats);
            }
        }
        
        return calculateTeamAverages(stats,teamId);
    }

    /**
     * Calculate per-game averages for player stats
     */
    private Map<String, Object> calculatePlayerAverages(Map<String, Object> stats, int playerId) {
    	log.info("TEST DEBUG - Retrieved stats: {}", stats); 
    	log.info("TEST DEBUG - games_played value: {}", stats.get(RedisFields.GAMES_PLAYED));
        if (stats == null || stats.isEmpty()) {
        	Map<String, Object> emptyStats = new HashMap<>(EMPTY_PLAYER_STATS);
            emptyStats.put(ResponseFields.PLAYER_ID, playerId);
            emptyStats.put(ResponseFields.PLAYER_NAME, rosterService.getPlayerName(playerId));        	
            return emptyStats;
        }
        int totalGames = (Integer) stats.getOrDefault(RedisFields.GAMES_PLAYED, 0);
        // Check if player has a live game
        boolean hasLiveGame = redisStatsRepository.hasLiveGame(playerId);
        int completedGames = hasLiveGame ? totalGames - 1 : totalGames;
        // Use completed games for average calculation
        int divisor = Math.max(completedGames, 1);
        log.debug("Player {}: totalGames={}, hasLiveGame={}, completedGames={}, divisor={}", 
                playerId, totalGames, hasLiveGame, completedGames, divisor);
        return Map.ofEntries(
        	    Map.entry(ResponseFields.PLAYER_ID, playerId),
        	    Map.entry(ResponseFields.PLAYER_NAME, rosterService.getPlayerName(playerId)),
        	    Map.entry(ResponseFields.GAMES_PLAYED, totalGames),
        	    Map.entry(ResponseFields.HAS_LIVE_GAME, hasLiveGame), // Boolean flag
        	    Map.entry(ResponseFields.AVG_POINTS, divide(stats.get(RedisFields.SUM_POINTS), divisor)),
        	    Map.entry(ResponseFields.AVG_REBOUNDS, divide(stats.get(RedisFields.SUM_REBOUNDS), divisor)),
        	    Map.entry(ResponseFields.AVG_ASSISTS, divide(stats.get(RedisFields.SUM_ASSISTS), divisor)),
        	    Map.entry(ResponseFields.AVG_STEALS, divide(stats.get(RedisFields.SUM_STEALS), divisor)),
        	    Map.entry(ResponseFields.AVG_BLOCKS, divide(stats.get(RedisFields.SUM_BLOCKS), divisor)),
        	    Map.entry(ResponseFields.AVG_FOULS, divide(stats.get(RedisFields.SUM_FOULS), divisor)),
        	    Map.entry(ResponseFields.AVG_TURNOVERS, divide(stats.get(RedisFields.SUM_TURNOVERS), divisor)),
        	    Map.entry(ResponseFields.AVG_MINUTES, divide(stats.get(RedisFields.SUM_MINUTES), divisor))
        	);
    }

    /**
     * Calculate per-game averages for team stats
     */
    private Map<String, Object> calculateTeamAverages(Map<String, Object> stats, int teamId) {
        if (stats == null || stats.isEmpty()) {
            Map<String, Object> emptyStats = new HashMap<>(EMPTY_TEAM_STATS);
            emptyStats.put(ResponseFields.TEAM_ID, teamId);
            emptyStats.put(ResponseFields.TEAM_NAME, rosterService.getTeamName(teamId));
            return emptyStats;
        }

        int totalGames = (Integer) stats.getOrDefault(RedisFields.GAMES_PLAYED, 0);
        int completedGames = totalGames - 1;  // Subtract current live game
        // Use completedGames for average if > 0, otherwise use totalGames
        int divisor = completedGames > 0 ? completedGames : totalGames;

        return Map.ofEntries(
        	    Map.entry(ResponseFields.TEAM_ID, teamId),
        	    Map.entry(ResponseFields.TEAM_NAME, rosterService.getTeamName(teamId)),
        	    Map.entry(ResponseFields.GAMES_PLAYED, totalGames),
        	    Map.entry(ResponseFields.AVG_POINTS, divide(stats.get(RedisFields.SUM_POINTS), divisor)),
        	    Map.entry(ResponseFields.AVG_REBOUNDS, divide(stats.get(RedisFields.SUM_REBOUNDS), divisor)),
        	    Map.entry(ResponseFields.AVG_ASSISTS, divide(stats.get(RedisFields.SUM_ASSISTS), divisor)),
        	    Map.entry(ResponseFields.AVG_STEALS, divide(stats.get(RedisFields.SUM_STEALS), divisor)),
        	    Map.entry(ResponseFields.AVG_BLOCKS, divide(stats.get(RedisFields.SUM_BLOCKS), divisor)),
        	    Map.entry(ResponseFields.AVG_FOULS, divide(stats.get(RedisFields.SUM_FOULS), divisor)),
        	    Map.entry(ResponseFields.AVG_TURNOVERS, divide(stats.get(RedisFields.SUM_TURNOVERS), divisor))
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
     * Generate Redis key for roaster member season stats
     */
    private String getRosterSeasonKey(int id, String season, String type) {
        return "s:%s:%s:%d".formatted(season.replace('/', '_'),type, id);
    }

}