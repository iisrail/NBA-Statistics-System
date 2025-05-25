-- Drop tables if they exist (for clean test runs)
DROP TABLE IF EXISTS stat_team_sum;
DROP TABLE IF EXISTS stat_player_sum;
DROP TABLE IF EXISTS player;
DROP TABLE IF EXISTS team;

-- Roster tables (for names)
CREATE TABLE player (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE team (
    id INTEGER PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Stats tables (for historical season data)
CREATE TABLE stat_player_sum (
    player_id INTEGER NOT NULL,
    season VARCHAR(10) NOT NULL,
    games_played INTEGER DEFAULT 0,
    sum_points INTEGER DEFAULT 0,
    sum_rebounds INTEGER DEFAULT 0,
    sum_assists INTEGER DEFAULT 0,
    sum_steals INTEGER DEFAULT 0,
    sum_blocks INTEGER DEFAULT 0,
    sum_fouls INTEGER DEFAULT 0,
    sum_turnovers INTEGER DEFAULT 0,
    sum_minutes DECIMAL(5,1) DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_id, season)
);

CREATE TABLE stat_team_sum (
    team_id INTEGER NOT NULL,
    season VARCHAR(10) NOT NULL,
    games_played INTEGER DEFAULT 0,
    sum_points INTEGER DEFAULT 0,
    sum_rebounds INTEGER DEFAULT 0,
    sum_assists INTEGER DEFAULT 0,
    sum_steals INTEGER DEFAULT 0,
    sum_blocks INTEGER DEFAULT 0,
    sum_fouls INTEGER DEFAULT 0,
    sum_turnovers INTEGER DEFAULT 0,
    sum_minutes DECIMAL(5,1) DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (team_id, season)
);

-- Insert test roster data
INSERT INTO player (id, name) VALUES 
    (23, 'LeBron James'),
    (30, 'Stephen Curry'),
    (40, 'Giannis Antetokounmpo'),
    (41, 'Jayson Tatum'),
    (50, 'Luka Doncic'),
    (60, 'Nikola Jokic');

INSERT INTO team (id, name) VALUES 
    (10, 'Los Angeles Lakers'),
    (15, 'Golden State Warriors'),
    (25, 'Boston Celtics'),
    (30, 'Dallas Mavericks'),
    (35, 'Denver Nuggets');

-- Insert some test season data
INSERT INTO stat_player_sum (player_id, season, games_played, sum_points, sum_rebounds, sum_assists, sum_steals, sum_blocks, sum_fouls, sum_turnovers, sum_minutes) VALUES 
    (23, '2023/24', 71, 1800, 550, 600, 120, 80, 180, 200, 2500.0),
    (30, '2023/24', 74, 2100, 320, 420, 100, 40, 160, 180, 2650.0);