package com.nba.stats.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.nba.stats.service.GameSubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles game-related events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameEventListener {
    
    private final GameSubscriptionService gameSubscriptionService;
    
    /**
     * Handle first player stat event by subscribing player to game completion
     */
    @EventListener
    public void handleFirstPlayerStat(FirstPlayerStatEvent event) {
        log.debug("First stat received for player {} in game {} - subscribing to completion", 
                 event.getPlayerId(), event.getGameId());
                 
        gameSubscriptionService.subscribePlayerToGame(
            event.getPlayerId(), 
            event.getGameId()
        );
    }
}