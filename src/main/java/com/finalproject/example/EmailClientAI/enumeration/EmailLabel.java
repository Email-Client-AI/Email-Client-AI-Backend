package com.finalproject.example.EmailClientAI.enumeration;

public enum EmailLabel {
    INBOX("INBOX"),
    UNREAD("UNREAD"),
    SENT("SENT"),
    TRASH("TRASH"),
    DRAFTS("DRAFTS"),
    SPAM("SPAM"),
    STARRED("STARRED"),
    IMPORTANT("IMPORTANT");

    private final String value;

    EmailLabel(String value) {
        this.value = value;
    }

    // Helper to find Enum from string (Safe lookup)
    public static EmailLabel fromId(String id) {
        for (EmailLabel label : values()) {
            if (label.value.equals(id)) {
                return label;
            }
        }
        return null; // Represents a Custom User Label (e.g., "Work", "Vacation")
    }
}
