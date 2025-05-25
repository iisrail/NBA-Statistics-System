package com.nba.stats.service;

import org.springframework.stereotype.Service;

import com.nba.stats.entity.Player;
import com.nba.stats.repository.RosterRepository;

import java.util.List;

@Service
public class PlayerService {

    private final RosterRepository repository;

    public PlayerService(RosterRepository repository) {
        this.repository = repository;
    }

    public List<Player> getAllPlayers() {
        return repository.findAllPlayers();
    }

    public void addPlayer(Player player) {
        repository.save(player);
    }
}