package com.nba.stats.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Game {
	private int id;
	private int homeTeamId;
	private int awayTeamId;
	private LocalDateTime gameDate;
	private String score;


}
