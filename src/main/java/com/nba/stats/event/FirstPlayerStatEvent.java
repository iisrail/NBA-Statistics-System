package com.nba.stats.event;

import lombok.Data;

/**
 * Event published when a player sends their first stat for a game
 */
@Data
public class FirstPlayerStatEvent {
    private final int playerId;
    private final int gameId;
    private final long timestamp;
    
    public FirstPlayerStatEvent(int playerId, int gameId) {
        this.playerId = playerId;
        this.gameId = gameId;
        this.timestamp = System.currentTimeMillis();
    }
}
