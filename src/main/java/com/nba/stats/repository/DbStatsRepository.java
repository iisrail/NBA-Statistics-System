package com.nba.stats.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class DbStatsRepository {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Retrieve season stats for a player from database
	 */
	public Map<String, Object> getPlayerSeasonStats(int playerId, String season) {		
		String sql = """
			SELECT player_id, season, games_played, sum_points, sum_rebounds, sum_assists, sum_steals, sum_blocks, sum_fouls, sum_turnovers, sum_minutes, updated_at
			FROM public.stat_player_sum
			WHERE player_id = ? and season = ?
		""";

		List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, playerId, season);

		if (results.isEmpty()) {
			return createEmptyPlayerStats();
		}

		return results.get(0);
	}

	/**
	 * Retrieve team stats from database
	 */
	public Map<String, Object> getTeamSeasonStats(int teamId, String season) {		
		String sql = """
			SELECT team_id, season, games_played, sum_points, sum_rebounds, sum_assists, sum_steals, sum_blocks, sum_fouls, sum_turnovers, sum_minutes, updated_at
			FROM public.stat_team_sum
			WHERE team_id = ?  and season = ?
		""";

		List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, teamId, season);

		if (results.isEmpty()) {
			return createEmptyTeamStats();
		}

		return results.get(0);
	}
	
	/**
	 * Upsert player season stats (INSERT or UPDATE if exists)
	 */
	public void upsertPlayerSeasonStats(int playerId, String season, Map<String, Object> stats) {
	    String sql = """
	        INSERT INTO stat_player_sum 
	        (player_id, season, games_played, sum_points, sum_rebounds, sum_assists, 
	         sum_steals, sum_blocks, sum_fouls, sum_turnovers, sum_minutes, updated_at)
	        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
	        ON CONFLICT (player_id, season) 
	        DO UPDATE SET
	            games_played = EXCLUDED.games_played,
	            sum_points = EXCLUDED.sum_points,
	            sum_rebounds = EXCLUDED.sum_rebounds,
	            sum_assists = EXCLUDED.sum_assists,
	            sum_steals = EXCLUDED.sum_steals,
	            sum_blocks = EXCLUDED.sum_blocks,
	            sum_fouls = EXCLUDED.sum_fouls,
	            sum_turnovers = EXCLUDED.sum_turnovers,
	            sum_minutes = EXCLUDED.sum_minutes,
	            updated_at = CURRENT_TIMESTAMP
	        """;

	    jdbcTemplate.update(sql,
	        playerId, season,
	        getIntValue(stats, "games_played"),
	        getIntValue(stats, "sum_points"),
	        getIntValue(stats, "sum_rebounds"),
	        getIntValue(stats, "sum_assists"),
	        getIntValue(stats, "sum_steals"),
	        getIntValue(stats, "sum_blocks"),
	        getIntValue(stats, "sum_fouls"),
	        getIntValue(stats, "sum_turnovers"),
	        getDoubleValue(stats, "sum_minutes")
	    );

	    log.debug("Upserted player {} season {} stats to database", playerId, season);
	}

	/**
	 * Upsert team season stats (INSERT or UPDATE if exists)
	 */
	public void upsertTeamSeasonStats(int teamId, String season, Map<String, Object> stats) {
	    String sql = """
	        INSERT INTO stat_team_sum 
	        (team_id, season, games_played, sum_points, sum_rebounds, sum_assists,
	         sum_steals, sum_blocks, sum_fouls, sum_turnovers, sum_minutes, updated_at)
	        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
	        ON CONFLICT (team_id, season)
	        DO UPDATE SET
	            games_played = EXCLUDED.games_played,
	            sum_points = EXCLUDED.sum_points,
	            sum_rebounds = EXCLUDED.sum_rebounds,
	            sum_assists = EXCLUDED.sum_assists,
	            sum_steals = EXCLUDED.sum_steals,
	            sum_blocks = EXCLUDED.sum_blocks,
	            sum_fouls = EXCLUDED.sum_fouls,
	            sum_turnovers = EXCLUDED.sum_turnovers,
	            sum_minutes = EXCLUDED.sum_minutes,
	            updated_at = CURRENT_TIMESTAMP
	        """;

	    jdbcTemplate.update(sql,
	        teamId, season,
	        getIntValue(stats, "games_played"),
	        getIntValue(stats, "sum_points"),
	        getIntValue(stats, "sum_rebounds"),
	        getIntValue(stats, "sum_assists"),
	        getIntValue(stats, "sum_steals"),
	        getIntValue(stats, "sum_blocks"),
	        getIntValue(stats, "sum_fouls"),
	        getIntValue(stats, "sum_turnovers"),
	        getDoubleValue(stats, "sum_minutes")
	    );

	    log.debug("Upserted team {} season {} stats to database", teamId, season);
	}

	/**
	 * Helper method to safely get integer values from Redis stats
	 */
	private int getIntValue(Map<String, Object> stats, String key) {
	    Object value = stats.get(key);
	    if (value instanceof Number num) {
	        return num.intValue();
	    }
	    if (value instanceof String str) {
	        try {
	            return Integer.parseInt(str);
	        } catch (NumberFormatException e) {
	            return 0;
	        }
	    }
	    return 0;
	}

	/**
	 * Helper method to safely get double values from Redis stats
	 */
	private double getDoubleValue(Map<String, Object> stats, String key) {
	    Object value = stats.get(key);
	    if (value instanceof Number num) {
	        return num.doubleValue();
	    }
	    if (value instanceof String str) {
	        try {
	            return Double.parseDouble(str);
	        } catch (NumberFormatException e) {
	            return 0.0;
	        }
	    }
	    return 0.0;
	}	

	/**
	 * Create empty stats map for new player
	 */
	private Map<String, Object> createEmptyPlayerStats() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("games_played", 0);
		stats.put("sum_points", 0);
		stats.put("sum_rebounds", 0);
		stats.put("sum_assists", 0);
		stats.put("sum_minutes", 0.0);
		return stats;
	}

	/**
	 * Create empty stats map for new team
	 */
	private Map<String, Object> createEmptyTeamStats() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("games_played", 0);
		stats.put("sum_points", 0);
		stats.put("sum_rebounds", 0);
		stats.put("sum_assists", 0);
		stats.put("sum_minutes", 0.0);
		return stats;
	}
}