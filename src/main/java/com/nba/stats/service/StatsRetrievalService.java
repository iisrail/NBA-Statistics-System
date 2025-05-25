package com.nba.stats.service;

import java.util.Map;

public interface StatsRetrievalService {
    
    /**
     * Get season stats for a player
     */
    Map<String, Object> getPlayerSeasonStats(int playerId, String season);
    
    /**
     * Get season stats for a team
     */
    Map<String, Object> getTeamSeasonStats(int teamId, String season);
}
