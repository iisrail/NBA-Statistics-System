package com.nba.stats.service;

import java.util.Map;

public interface StatsRetrievalService {
    
    /**
     * Get current season stats for a player
     */
    Map<String, Object> getPlayerSeasonStats(int playerId);
    
    /**
     * Get current season stats for a team
     */
    Map<String, Object> getTeamSeasonStats(int teamId);
}
