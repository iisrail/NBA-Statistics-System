package com.nba.stats.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nba.stats.service.StatsRetrievalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/stat")
@RequiredArgsConstructor
@Slf4j
public class QueryStatsController {

    private final StatsRetrievalService statsService;
    @Value("${nba.current-season}")
    private String currentSeason;

    @GetMapping("/player/{playerId}")
    public Map<String, Object> getPlayerStats(
    		@PathVariable int playerId,
    		@RequestParam(required = false) String season) { 
        // Use currentSeason if season parameter is null
        String actualSeason = (season != null) ? season : currentSeason;
        log.info("Retrieving stats for player {} season {}", playerId, actualSeason);
        return statsService.getPlayerSeasonStats(playerId, actualSeason);
    }

    @GetMapping("/team/{teamId}")
    public Map<String, Object> getTeamStats(
    		@PathVariable int teamId,
    		@RequestParam(required = false) String season) {
    	String actualSeason = (season != null) ? season : currentSeason;
    	log.info("Retrieving stats for team {} season {}", teamId, actualSeason);
        return statsService.getTeamSeasonStats(teamId, actualSeason);
    }
}
