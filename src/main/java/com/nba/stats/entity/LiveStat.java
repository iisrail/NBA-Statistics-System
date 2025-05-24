package com.nba.stats.entity;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class LiveStat {
    private final int gameId;
    private final int teamId;
    private final int playerId;
    private int points;
    private int rebounds;
    private int assists;
    private int steals;
    private int blocks;
    private int fouls;
    private int turnovers;    
    private int secPlayed;
    
    public String getRedisKey() {
        return String.format("live:game:%d:player:%d", gameId, playerId);
    }

}
