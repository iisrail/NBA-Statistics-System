-- sql/init.sql
-- Database initialization for NBA Stats system

-- Create teams table
CREATE TABLE IF NOT EXISTS team (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    city VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create players table
CREATE TABLE IF NOT EXISTS player (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    team_id INTEGER REFERENCES team(id),
    position VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create player season stats summary table
CREATE TABLE IF NOT EXISTS stat_player_sum (
    player_id INTEGER NOT NULL,
    season VARCHAR(20) NOT NULL,
    games_played INTEGER DEFAULT 0,
    sum_points INTEGER DEFAULT 0,
    sum_rebounds INTEGER DEFAULT 0,
    sum_assists INTEGER DEFAULT 0,
    sum_steals INTEGER DEFAULT 0,
    sum_blocks INTEGER DEFAULT 0,
    sum_fouls INTEGER DEFAULT 0,
    sum_turnovers INTEGER DEFAULT 0,
    sum_minutes DECIMAL(10,2) DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_id, season),
    FOREIGN KEY (player_id) REFERENCES player(id)
);

-- Create team season stats summary table
CREATE TABLE IF NOT EXISTS stat_team_sum (
    team_id INTEGER NOT NULL,
    season VARCHAR(20) NOT NULL,
    games_played INTEGER DEFAULT 0,
    sum_points INTEGER DEFAULT 0,
    sum_rebounds INTEGER DEFAULT 0,
    sum_assists INTEGER DEFAULT 0,
    sum_steals INTEGER DEFAULT 0,
    sum_blocks INTEGER DEFAULT 0,
    sum_fouls INTEGER DEFAULT 0,
    sum_turnovers INTEGER DEFAULT 0,
    sum_minutes DECIMAL(10,2) DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, season),
    FOREIGN KEY (team_id) REFERENCES team(id)
);

-- Insert sample teams
INSERT INTO team (id, name, city) VALUES 
(10, 'Los Angeles Lakers', 'Los Angeles'),
(15, 'Golden State Warriors', 'San Francisco'),
(20, 'Miami Heat', 'Miami'),
(25, 'Boston Celtics', 'Boston'),
(30, 'Phoenix Suns', 'Phoenix')
ON CONFLICT (name) DO NOTHING;

-- Insert sample players
INSERT INTO player (id, name, team_id, position) VALUES 
(23, 'LeBron James', 10, 'SF'),
(30, 'Stephen Curry', 15, 'PG'),
(35, 'Kevin Durant', 30, 'SF'),
(40, 'Jayson Tatum', 25, 'SF'),
(41, 'Jaylen Brown', 25, 'SG'),
(50, 'Devin Booker', 30, 'SG')
ON CONFLICT DO NOTHING;

-- Insert sample historical data for 2023/24 season
INSERT INTO stat_player_sum (player_id, season, games_played, sum_points, sum_rebounds, sum_assists, sum_steals, sum_blocks, sum_fouls, sum_turnovers, sum_minutes) VALUES 
(23, '2023/24', 71, 1800, 550, 600, 90, 40, 180, 250, 2500.5),
(30, '2023/24', 74, 2100, 300, 400, 120, 20, 150, 220, 2600.0),
(35, '2023/24', 68, 1950, 450, 380, 85, 110, 160, 200, 2400.5)
ON CONFLICT (player_id, season) DO NOTHING;

-- Insert sample team stats for 2023/24 season
INSERT INTO stat_team_sum (team_id, season, games_played, sum_points, sum_rebounds, sum_assists, sum_steals, sum_blocks, sum_fouls, sum_turnovers, sum_minutes) VALUES 
(10, '2023/24', 82, 9200, 3400, 2100, 600, 400, 1800, 1200, 19680.0),
(15, '2023/24', 82, 9800, 3200, 2400, 700, 350, 1750, 1100, 19680.0),
(25, '2023/24', 82, 9500, 3500, 2200, 650, 450, 1900, 1300, 19680.0)
ON CONFLICT (team_id, season) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_stat_player_sum_season ON stat_player_sum(season);
CREATE INDEX IF NOT EXISTS idx_stat_team_sum_season ON stat_team_sum(season);
CREATE INDEX IF NOT EXISTS idx_player_team ON player(team_id);
