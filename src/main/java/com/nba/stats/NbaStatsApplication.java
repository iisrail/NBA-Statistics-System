package com.nba.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class NbaStatsApplication {

	public static void main(String[] args) {
		SpringApplication.run(NbaStatsApplication.class, args);
	}

}
