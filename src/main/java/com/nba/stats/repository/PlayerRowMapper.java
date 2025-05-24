package com.nba.stats.repository;

import org.springframework.jdbc.core.RowMapper;

import com.nba.stats.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerRowMapper implements RowMapper<Player> {
    @Override
    public Player mapRow(ResultSet rs, int rowNum) throws SQLException {
        Player player = new Player();
        player.setId(rs.getInt("id"));
        player.setName(rs.getString("name"));
        return player;
    }
}
