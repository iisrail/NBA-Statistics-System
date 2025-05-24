package com.nba.stats.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nba.stats.entity.Player;

import java.util.List;

@Repository
public class PlayerRepository {

    private final JdbcTemplate jdbcTemplate;

    public PlayerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Player> findAll() {
        return jdbcTemplate.query("SELECT * FROM player", new PlayerRowMapper());
    }

    public void save(Player player) {
        jdbcTemplate.update("INSERT INTO player(name) VALUES(?)", player.getName());
    }
}
