package com.nba.stats.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nba.stats.entity.Player;
import com.nba.stats.entity.Team;

@Repository
public class RosterRepository {

    private final JdbcTemplate jdbcTemplate;

    public RosterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<Integer, String> getAllPlayerNames() {
        Map<Integer, String> playerMap = new HashMap<>();
        jdbcTemplate.query("SELECT id, name FROM player", 
            (rs) -> {
                playerMap.put(rs.getInt("id"), rs.getString("name"));
            });
        return playerMap;
    }
    
    public Map<Integer, String> getAllTeamNames() {
        Map<Integer, String> teamMap = new HashMap<>();
        jdbcTemplate.query("SELECT id, name FROM team", 
            (rs) -> {
                teamMap.put(rs.getInt("id"), rs.getString("name"));
            });
        return teamMap;
    }

    public void save(Player player) {
        jdbcTemplate.update("INSERT INTO player(name) VALUES(?)", player.getName());
    }
    
    public void save(Team team) {
        jdbcTemplate.update("INSERT INTO team(name) VALUES(?)", team.getName());
    }

    public List<Player> findAllPlayers() {
        return jdbcTemplate.query("SELECT * FROM player", 
            (rs, rowNum) -> {
                Player player = new Player();
                player.setId(rs.getInt("id"));
                player.setName(rs.getString("name"));
                return player;
            });
    }
   
}
