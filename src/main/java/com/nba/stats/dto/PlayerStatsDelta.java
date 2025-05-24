package com.nba.stats.dto;

import java.math.BigDecimal;

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
    @Builder.Default
    BigDecimal minutesPlayed = BigDecimal.ZERO;
    
	/*
	 * public int convertMinToSeconds() {
	 * return minutesPlayed.multiply(BigDecimal.valueOf(60)).intValue();
	 * }
	 */
}

