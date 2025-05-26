package com.nba.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class PlayerStatsDelta {
    int points;
    int rebounds;
    int assists;
    int steals;
    int blocks;
    int fouls;
    int turnovers;  
    double minutesPlayed;
    int gamesPlayed;

}

