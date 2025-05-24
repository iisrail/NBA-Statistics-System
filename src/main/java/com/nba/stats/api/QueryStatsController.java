package com.nba.stats.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/player/{playerId}")
    public Map<String, Object> getPlayerStats(@PathVariable int playerId) {
        log.info("Retrieving stats for player {}", playerId);
        return statsService.getPlayerSeasonStats(playerId);
    }

    @GetMapping("/team/{teamId}")
    public Map<String, Object> getTeamStats(@PathVariable int teamId) {
        log.info("Retrieving stats for team {}", teamId);
        return statsService.getTeamSeasonStats(teamId);
    }
}
