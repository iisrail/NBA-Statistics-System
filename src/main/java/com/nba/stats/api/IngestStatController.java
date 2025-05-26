package com.nba.stats.api;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nba.stats.dto.LiveStatDto;
import com.nba.stats.service.LiveStatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/stat/live")
@RequiredArgsConstructor
@Slf4j
public class IngestStatController {
	private final LiveStatService service;	

	@PutMapping("/game")
	public void putLiveStat(@Valid @RequestBody LiveStatDto stat) {
	    log.info("Processing live stat for player {} in game {}", stat.getPlayerId(), stat.getGameId());
	    service.processLiveStat(stat);
	}	
		
}
