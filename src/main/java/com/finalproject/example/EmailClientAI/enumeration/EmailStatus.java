package com.finalproject.example.EmailClientAI.enumeration;

public enum EmailStatus {
    NEW("NEW"),
    READ("READ"),
    REMOVED("REMOVED"),
    STARRED("STARRED"),
    SNOOZED("SNOOZED"),
    TODO("TODO"),
    INPROGRESS("INPROGRESS"),
    DONE("DONE");



    private final String value;

    EmailStatus(String value) {
        this.value = value;
    }

    // Helper to find Enum from string (Safe lookup)
    public static EmailStatus fromId(String id) {
        for (EmailStatus status : values()) {
            if (status.value.equals(id)) {
                return status;
            }
        }
        return null; // Represents a Custom User Label (e.g., "Work", "Vacation")
    }
}
