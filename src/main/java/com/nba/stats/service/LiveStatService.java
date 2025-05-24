package com.nba.stats.service;

import com.nba.stats.dto.LiveStatDto;

public interface LiveStatService {
    void processLiveStat(LiveStatDto stat);
}

