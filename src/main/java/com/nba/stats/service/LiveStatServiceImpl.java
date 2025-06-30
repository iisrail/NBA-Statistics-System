package com.nba.stats.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.dto.PlayerStatsDelta;
import com.nba.stats.event.FirstPlayerStatEvent;
import com.nba.stats.repository.DbStatsRepository;
import com.nba.stats.repository.RedisStatsRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LiveStatServiceImpl implements LiveStatService {

    private final DbStatsRepository playerStatsRepository;
    private final RedisStatsRepository redisStatsRepository;
    private final ApplicationEventPublisher eventPublisher; // Spring event publisher
    private final String currentSeason;

    /** Constructor required because of `@Value` + final */
    public LiveStatServiceImpl(
            DbStatsRepository playerStatsRepository,
            RedisStatsRepository redisStatsRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${nba.current-season}") String currentSeason) {

        this.playerStatsRepository = playerStatsRepository;
        this.redisStatsRepository = redisStatsRepository;
        this.eventPublisher = eventPublisher;
        this.currentSeason = currentSeason; // final now safe
    }

    @Override
    public void processLiveStat(LiveStatDto liveStat) {
        // Step 1: Update player stats and get delta
        PlayerStatsDelta delta = updatePlayerStats(liveStat);

        // Step 2: Update team stats using the same delta
        updateTeamStats(liveStat.getTeamId(), delta, liveStat.getGameId());
        
        log.info("Processed live stat for player {} in game {}", liveStat.getPlayerId(), liveStat.getGameId());
    }

    /**
     * Update player statistics and return the delta
     */
    private PlayerStatsDelta updatePlayerStats(LiveStatDto liveStat) {
        String seasonKey = getPlayerSeasonKey(liveStat.getPlayerId());
        String gameKey = getPlayerGameKey(liveStat.getPlayerId(), liveStat.getGameId());

       
        ensurePlayerSeasonStatsLoaded(seasonKey, liveStat.getPlayerId());

        // Get previous game stats for delta calculation
        LiveStatDto previousStats = redisStatsRepository.getPreviousGameStats(gameKey);
        
     // Publish event if this is first stat for this player in this game
        if (previousStats == null) {
            eventPublisher.publishEvent(new FirstPlayerStatEvent(
                liveStat.getPlayerId(), 
                liveStat.getGameId()
            ));
            log.debug("Published FirstPlayerStatEvent for player {} in game {}", 
                     liveStat.getPlayerId(), liveStat.getGameId());
        }
        

        // Calculate delta
        PlayerStatsDelta delta = calculateDelta(previousStats, liveStat);

        // Update season aggregates with delta
        redisStatsRepository.updateSeasonAggregates(seasonKey, delta);
        // Store current game stats for next delta calculation
        redisStatsRepository.storeCurrentGameStats(gameKey, liveStat);

        return delta;
    }

    /**
     * Update team statistics using player delta with proper game counting
     */
    private void updateTeamStats(int teamId, PlayerStatsDelta delta, int gameId) {
        String teamSeasonKey = getTeamSeasonKey(teamId);
        String teamGameKey = "team_game:" + teamId + ":" + gameId;

        // Ensure team stats are loaded in Redis
        ensureTeamSeasonStatsLoaded(teamSeasonKey, teamId);

        // Check if this is the first player processed for this team in this game
        boolean isFirstPlayerInGame = !redisStatsRepository.seasonStatsExist(teamGameKey);

        // Create team delta
        PlayerStatsDelta teamDelta = new PlayerStatsDelta(
            delta.getPoints(),
            delta.getRebounds(),
            delta.getAssists(),
            delta.getSteals(),
            delta.getBlocks(),
            delta.getFouls(),
            delta.getTurnovers(),
            delta.getMinutesPlayed(),
            isFirstPlayerInGame ? 1 : 0  //Only count game once per team
        );

        // Update team aggregates
        redisStatsRepository.updateSeasonAggregates(teamSeasonKey, teamDelta);
        // Mark this team-game combination as processed
        if (isFirstPlayerInGame) {
        	redisStatsRepository.markTeamGameProcessed(teamId, gameId);
            log.debug("First player processed for team {} in game {} - incremented team games", teamId, gameId);
        } 
    }

    /**
     *  Ensure season stats are loaded in Redis, and if not retrieve from DB and load to Redis 
     */
    private void ensurePlayerSeasonStatsLoaded(String seasonKey, int playerId) {
        if (!redisStatsRepository.seasonStatsExist(seasonKey)) {
            Map<String, Object> stats = playerStatsRepository.getPlayerSeasonStats(playerId, currentSeason);
            redisStatsRepository.storeSeasonStats(seasonKey, stats);
            log.debug("Loaded season stats for player {} into Redis", playerId);
        }
    }

    /**
     * Ensure team season stats are loaded in Redis
     */
    private void ensureTeamSeasonStatsLoaded(String teamSeasonKey, int teamId) {
        if (!redisStatsRepository.seasonStatsExist(teamSeasonKey)) {
            // Assuming you have this method in PlayerStatsRepository
            Map<String, Object> stats = playerStatsRepository.getTeamSeasonStats(teamId,currentSeason);
            redisStatsRepository.storeSeasonStats(teamSeasonKey, stats);
            log.debug("Loaded season stats for team {} into Redis", teamId);
        }
    }

    /**
     * Calculate delta between previous and current stats 
     */
    private PlayerStatsDelta calculateDelta(LiveStatDto previous, LiveStatDto current) {
        if (previous == null) {
            // First update for this game - return current stats as delta
            return new PlayerStatsDelta(
                current.getPoints(),
                current.getRebounds(),
                current.getAssists(),
                current.getSteals(),       
                current.getBlocks(),       
                current.getFouls(),        
                current.getTurnovers(),   
                current.getMinutesPlayed(),
                1
            );
        }

        // Calculate difference between current and previous
        return new PlayerStatsDelta(
            current.getPoints() - previous.getPoints(),
            current.getRebounds() - previous.getRebounds(),
            current.getAssists() - previous.getAssists(),
            current.getSteals() - previous.getSteals(), 
            current.getBlocks() - previous.getBlocks(), 
            current.getFouls() - previous.getFouls(),   
            current.getTurnovers() - previous.getTurnovers(),
            current.getMinutesPlayed() - previous.getMinutesPlayed(),
            0
        );
    }

    /**
     * Generate Redis key for player season stats
     */
    private String getPlayerSeasonKey(int playerId) {
        // e.g. s:24_25:p:23 (slashes replaced)
        return "s:%s:p:%d".formatted(currentSeason.replace('/', '_'), playerId);
    }

    /**
     * Generate Redis key for team season stats
     */
    private String getTeamSeasonKey(int teamId) {
        return "s:%s:t:%d".formatted(currentSeason.replace('/', '_'), teamId);
    }

    /**
     * Generate Redis key for player game stats
     */
    private String getPlayerGameKey(int playerId, int gameId) {
        // e.g. g:8123:p:23
        return "g:%d:p:%d".formatted(gameId, playerId);
    }
}