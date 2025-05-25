package com.nba.stats.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.nba.stats.repository.RosterRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class RosterService {
    private final RosterRepository rosterRepository;
    private Map<Integer, String> playerNames = new ConcurrentHashMap<>();
    private Map<Integer, String> teamNames = new ConcurrentHashMap<>();
    
    public RosterService(RosterRepository playerRepository) {
        this.rosterRepository = playerRepository;
    }
    
    @PostConstruct
    public void loadNamesFromDatabase() {
        log.info("Loading player and team names from database...");
        refreshPlayerNames();
        refreshTeamNames();
        log.info("Loaded {} players and {} teams", playerNames.size(), teamNames.size());
    }
    
    public String getPlayerName(int playerId) {
        return playerNames.getOrDefault(playerId, "Unknown Player");
    }
    
    public String getTeamName(int teamId) {
        return teamNames.getOrDefault(teamId, "Unknown Team");
    }
    
    @Scheduled(cron = "0 0 3 * * *") // Every day at 3:00 AM
    private void refreshPlayerNames() {
        Map<Integer, String> newPlayerNames = rosterRepository.getAllPlayerNames();
        if (!newPlayerNames.isEmpty()) {
            this.playerNames = new ConcurrentHashMap<>(newPlayerNames);
            log.info("Daily refresh: Updated {} player names", newPlayerNames.size());
        }
    }
    
    @Scheduled(cron = "0 0 3 * * *") // Every day at 3:00 AM
    private void refreshTeamNames() {
        Map<Integer, String> newTeamNames = rosterRepository.getAllTeamNames();
        if (!newTeamNames.isEmpty()) {
            this.teamNames = new ConcurrentHashMap<>(newTeamNames);
            log.debug("Refreshed {} team names", newTeamNames.size());
        }
    }
    

}
