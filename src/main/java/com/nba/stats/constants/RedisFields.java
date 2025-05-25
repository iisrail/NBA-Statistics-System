package com.nba.stats.constants;

/**
 * Constants for Redis hash field names to avoid magic strings
 */
public final class RedisFields {

	// Season stats fields (aggregated totals)
	public static final String SUM_POINTS = "sum_points";
	public static final String SUM_REBOUNDS = "sum_rebounds";
	public static final String SUM_ASSISTS = "sum_assists";
	public static final String SUM_STEALS = "sum_steals";
	public static final String SUM_BLOCKS = "sum_blocks";
	public static final String SUM_FOULS = "sum_fouls";
	public static final String SUM_TURNOVERS = "sum_turnovers";
	public static final String SUM_MINUTES = "sum_minutes";
	public static final String GAMES_PLAYED = "games_played";

	// Individual game stats fields
	public static final String GAME_ID = "gameId";
	public static final String TEAM_ID = "teamId";
	public static final String PLAYER_ID = "playerId";
	public static final String POINTS = "points";
	public static final String REBOUNDS = "rebounds";
	public static final String ASSISTS = "assists";
	public static final String STEALS = "steals";
	public static final String BLOCKS = "blocks";
	public static final String FOULS = "fouls";
	public static final String TURNOVERS = "turnovers";
	public static final String MINUTES_PLAYED = "minutesPlayed";

	// Database/Entity fields
	public static final String PLAYER_ID_DB = "player_id";
	public static final String TEAM_ID_DB = "team_id";
	public static final String SEASON = "season";
	public static final String UPDATED_AT = "updated_at";

	// Prevent instantiation
	private RedisFields() {
		throw new AssertionError("RedisFields is a utility class and should not be instantiated");
	}
}
