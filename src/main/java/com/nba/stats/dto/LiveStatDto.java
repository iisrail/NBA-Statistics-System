package com.nba.stats.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@AllArgsConstructor
@Value
@Builder
public class LiveStatDto{
	    int gameId;
	    int teamId;
	    int playerId;
	    int points;
	    int rebounds;
	    int assists;
	    int steals;
	    int blocks;
	    @Min(0) @Max(6) int fouls;      // NBA rule: max 6 fouls
	    int turnovers;	    
	    @Builder.Default
	    @DecimalMin("0.0") @DecimalMax("48.0") 
	    BigDecimal minutesPlayed = BigDecimal.ZERO;
	}
