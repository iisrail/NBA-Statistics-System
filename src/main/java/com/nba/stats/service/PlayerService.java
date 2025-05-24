package com.nba.stats.service;

import org.springframework.stereotype.Service;

import com.nba.stats.entity.Player;
import com.nba.stats.repository.PlayerRepository;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository repository;

    public PlayerService(PlayerRepository repository) {
        this.repository = repository;
    }

    public List<Player> getAllPlayers() {
        return repository.findAll();
    }

    public void addPlayer(Player player) {
        repository.save(player);
    }
}