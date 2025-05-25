package com.nba.stats.constants;

/**
 * Constants for API response field names to avoid magic strings
 */
public final class ResponseFields {
    
    // Common response fields
    public static final String GAMES_PLAYED = "gamesPlayed";
    
    public static final String PLAYER_ID = "playerId";
    public static final String PLAYER_NAME = "playerName";
    public static final String TEAM_ID = "teamId";
    public static final String TEAM_NAME = "teamName";
    public static final String UNKNOWN_PLAYER = "Unknown Player";
    public static final String UNKNOWN_TEAM = "Unknown Team";
    public static final String PLATER_TYPE = "p";
    public static final String TEAM_TYPE = "t";
    public static final String HAS_LIVE_GAME = "hasLiveGame";
    
    // Average statistics fields
    public static final String AVG_POINTS = "avgPoints";
    public static final String AVG_REBOUNDS = "avgRebounds";
    public static final String AVG_ASSISTS = "avgAssists";
    public static final String AVG_STEALS = "avgSteals";
    public static final String AVG_BLOCKS = "avgBlocks";
    public static final String AVG_FOULS = "avgFouls";
    public static final String AVG_TURNOVERS = "avgTurnovers";
    public static final String AVG_MINUTES = "avgMinutes";
    
    // Prevent instantiation
    private ResponseFields() {
        throw new AssertionError("ResponseFields is a utility class and should not be instantiated");
    }
}