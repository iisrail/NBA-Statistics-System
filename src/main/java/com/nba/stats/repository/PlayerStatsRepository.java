package com.nba.stats.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PlayerStatsRepository {
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