package com.nba.stats.api;

import org.springframework.web.bind.annotation.*;

import com.nba.stats.entity.Player;
import com.nba.stats.service.PlayerService;

import java.util.List;

@RestController
@RequestMapping("/players")
public class PlayerController {

    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping
    public List<Player> getAll() {
        return service.getAllPlayers();
    }

    @PostMapping
    public void addPlayer(@RequestBody Player player) {
        service.addPlayer(player);
    }
}