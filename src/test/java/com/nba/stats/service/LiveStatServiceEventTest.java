package com.nba.stats.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.event.FirstPlayerStatEvent;
import com.nba.stats.repository.DbStatsRepository;
import com.nba.stats.repository.RedisStatsRepository;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class LiveStatServiceEventTest {

    @Mock
    private DbStatsRepository playerStatsRepository;
    
    @Mock
    private RedisStatsRepository redisStatsRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    
    private LiveStatServiceImpl liveStatService;
    
    @BeforeEach
    void setUp() {
        // Manually create the service with currentSeason
        liveStatService = new LiveStatServiceImpl(
            playerStatsRepository,
            redisStatsRepository, 
            eventPublisher,
            "2024/25" // ← Provide currentSeason directly
        );
    }
    
    @Test
    void shouldPublishFirstPlayerStatEventWhenNoPreviousStats() {
        // Given: First stat for player using your actual DTO structure
        LiveStatDto liveStat = LiveStatDto.builder()
            .gameId(1001)
            .playerId(23)
            .teamId(10)
            .points(25)
            .rebounds(8)
            .assists(6)
            .steals(2)
            .blocks(1)
            .fouls(3)
            .turnovers(2)
            .minutesPlayed(35.5)
            .build();
            
        when(redisStatsRepository.getPreviousGameStats(anyString())).thenReturn(null);
        when(playerStatsRepository.getPlayerSeasonStats(anyInt(), anyString())).thenReturn(Map.of());
        
        // When: Process the stat
        liveStatService.processLiveStat(liveStat);
        
        // Then: Event should be published
        ArgumentCaptor<FirstPlayerStatEvent> eventCaptor = ArgumentCaptor.forClass(FirstPlayerStatEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        FirstPlayerStatEvent publishedEvent = eventCaptor.getValue();
        assertEquals(23, publishedEvent.getPlayerId());
        assertEquals(1001, publishedEvent.getGameId());
    }
    
    @Test
    void shouldNotPublishEventWhenPreviousStatsExist() {
        // Given: Player already has stats for this game
        LiveStatDto liveStat = LiveStatDto.builder()
            .gameId(1001)
            .playerId(23)
            .teamId(10)
            .points(30)
            .rebounds(10)
            .assists(7)
            .steals(3)
            .blocks(2)
            .fouls(4)
            .turnovers(1)
            .minutesPlayed(38.0)
            .build();
            
        LiveStatDto previousStats = LiveStatDto.builder()
            .gameId(1001)
            .playerId(23)
            .teamId(10)
            .points(25)
            .rebounds(8)
            .assists(6)
            .steals(2)
            .blocks(1)
            .fouls(3)
            .turnovers(2)
            .minutesPlayed(35.5)
            .build();
            
        when(redisStatsRepository.getPreviousGameStats(anyString())).thenReturn(previousStats);
        
        // When: Process the stat
        liveStatService.processLiveStat(liveStat);
        
        // Then: No event should be published
        verify(eventPublisher, never()).publishEvent(any(FirstPlayerStatEvent.class));
    }
}
