-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    avatar_url VARCHAR(500),
    timezone VARCHAR(50) DEFAULT 'UTC',
    total_xp INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for users table
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- HABITS TABLE
CREATE TABLE habits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    target_count INTEGER DEFAULT 1,
    icon VARCHAR(50),
    color VARCHAR(20),
    github_auto_track BOOLEAN DEFAULT false,
    github_event_type VARCHAR(50),
    reminder_enabled BOOLEAN DEFAULT false,
    reminder_time TIME,
    is_active BOOLEAN DEFAULT true,
    archived_at TIMESTAMP,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    total_completions INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for habits
CREATE INDEX idx_habits_user_id ON habits(user_id);
CREATE INDEX idx_habits_user_active ON habits(user_id, is_active);
CREATE INDEX idx_habits_category ON habits(category);

-- Trigger for habits table
CREATE TRIGGER update_habits_updated_at
    BEFORE UPDATE ON habits
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- HABIT LOGS TABLE
CREATE TABLE habit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    habit_id UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    github_commit_sha VARCHAR(40),
    github_repo_name VARCHAR(255),
    xp_earned INTEGER DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for habit_logs
CREATE INDEX idx_habit_logs_habit_id ON habit_logs(habit_id, completed_at DESC);
CREATE INDEX idx_habit_logs_user_id ON habit_logs(user_id, completed_at DESC);
CREATE INDEX idx_habit_logs_completed_at ON habit_logs(completed_at DESC);

-- Unique constraint: one log per habit per day
CREATE UNIQUE INDEX idx_habit_logs_unique_day 
    ON habit_logs(habit_id, DATE(completed_at));

-- Comments for documentation
COMMENT ON TABLE users IS 'Main users table with authentication and profile data';
COMMENT ON TABLE habits IS 'User habits with tracking configuration';
COMMENT ON TABLE habit_logs IS 'Individual habit completion logs';

COMMENT ON COLUMN users.total_xp IS 'Total experience points earned by user';
COMMENT ON COLUMN users.level IS 'Calculated level based on total XP';
COMMENT ON COLUMN users.current_streak IS 'Current consecutive days streak across all habits';
COMMENT ON COLUMN users.longest_streak IS 'Historical longest streak achieved';

COMMENT ON COLUMN habits.frequency IS 'DAILY, WEEKLY, or CUSTOM frequency';
COMMENT ON COLUMN habits.target_count IS 'Number of times to complete per frequency period';
COMMENT ON COLUMN habits.github_auto_track IS 'Whether to automatically track from GitHub events';
COMMENT ON COLUMN habits.github_event_type IS 'COMMIT, PULL_REQUEST, CODE_REVIEW, or ISSUE';