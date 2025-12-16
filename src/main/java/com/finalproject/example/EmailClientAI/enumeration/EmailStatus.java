package com.finalproject.example.EmailClientAI.enumeration;

public enum EmailStatus {
    NEW(0L),
    SNOOZED(1L);



    private final Long value;

    EmailStatus(Long value) {
        this.value = value;
    }

    // Helper to find Enum from string (Safe lookup)
    public static EmailStatus fromId(Long id) {
        for (EmailStatus status : values()) {
            if (status.value.equals(id)) {
                return status;
            }
        }
        return null; // Represents a Custom User Label (e.g., "Work", "Vacation")
    }
}
