package com.devhabits.model.enums;

public enum GitHubEventType {
    COMMIT,         // Push commits
    PULL_REQUEST,   // Open/merge PR
    CODE_REVIEW,    // Review PR
    ISSUE           // Create/close issue
}