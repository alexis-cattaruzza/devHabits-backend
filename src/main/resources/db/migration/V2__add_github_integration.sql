-- GITHUB CONNECTIONS TABLE
-- Stores OAuth tokens and GitHub user information
CREATE TABLE github_connections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    github_user_id BIGINT NOT NULL,
    github_username VARCHAR(255) NOT NULL,
    github_email VARCHAR(255),
    github_avatar_url VARCHAR(500),
    access_token VARCHAR(500) NOT NULL,
    token_type VARCHAR(50) DEFAULT 'Bearer',
    scope TEXT,
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);

-- Indexes for github_connections
CREATE INDEX idx_github_connections_user_id ON github_connections(user_id);
CREATE INDEX idx_github_connections_github_user_id ON github_connections(github_user_id);
CREATE INDEX idx_github_connections_active ON github_connections(is_active);

-- GITHUB EVENTS TABLE
-- Stores GitHub events (commits, PRs, etc.) that triggered habit completions
CREATE TABLE github_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    habit_id UUID REFERENCES habits(id) ON DELETE SET NULL,
    habit_log_id UUID REFERENCES habit_logs(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL, -- COMMIT, PULL_REQUEST, CODE_REVIEW, ISSUE
    event_id VARCHAR(255) NOT NULL, -- GitHub event ID
    repository_name VARCHAR(255) NOT NULL,
    repository_full_name VARCHAR(500) NOT NULL,
    commit_sha VARCHAR(40),
    commit_message TEXT,
    pull_request_number INTEGER,
    pull_request_title TEXT,
    issue_number INTEGER,
    issue_title TEXT,
    event_data JSONB, -- Full GitHub event payload
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for github_events
CREATE INDEX idx_github_events_user_id ON github_events(user_id, created_at DESC);
CREATE INDEX idx_github_events_habit_id ON github_events(habit_id);
CREATE INDEX idx_github_events_event_id ON github_events(event_id);
CREATE INDEX idx_github_events_event_type ON github_events(event_type);
CREATE INDEX idx_github_events_repository ON github_events(repository_full_name);

-- Unique constraint: one event per GitHub event ID
CREATE UNIQUE INDEX idx_github_events_unique_event
    ON github_events(event_id, event_type);

-- GITHUB REPOSITORIES TABLE (Optional tracking of user repos)
CREATE TABLE github_repositories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    github_repo_id BIGINT NOT NULL,
    repository_name VARCHAR(255) NOT NULL,
    repository_full_name VARCHAR(500) NOT NULL,
    description TEXT,
    is_private BOOLEAN DEFAULT false,
    is_tracked BOOLEAN DEFAULT true,
    language VARCHAR(100),
    stargazers_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for github_repositories
CREATE INDEX idx_github_repositories_user_id ON github_repositories(user_id);
CREATE INDEX idx_github_repositories_tracked ON github_repositories(user_id, is_tracked);
CREATE UNIQUE INDEX idx_github_repositories_unique
    ON github_repositories(user_id, github_repo_id);

-- Trigger for github_repositories table
CREATE TRIGGER update_github_repositories_updated_at
    BEFORE UPDATE ON github_repositories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE github_connections IS 'GitHub OAuth connections and tokens for users';
COMMENT ON TABLE github_events IS 'GitHub events that triggered automatic habit completions';
COMMENT ON TABLE github_repositories IS 'Tracked GitHub repositories for users';

COMMENT ON COLUMN github_connections.access_token IS 'GitHub OAuth access token (encrypted in production)';
COMMENT ON COLUMN github_events.event_data IS 'Full GitHub webhook payload in JSON format';
COMMENT ON COLUMN github_repositories.is_tracked IS 'Whether to track events from this repository';
