package com.example.feedbackplugin;

public enum FeedbackCategory {
    BUG("🐛 Bug Report", 0xFF0000),         // Red
    SUGGESTION("💡 Suggestion", 0x00BFFF),  // Blue
    GENERAL("💬 General", 0x57F287);        // Green

    private final String displayName;
    private final int color;

    FeedbackCategory(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
