package com.finalproject.example.EmailClientAI.enumeration;

public enum MimeType {
    // --- STRUCTURAL (Containers) ---
    MULTIPART_MIXED("multipart/mixed"),           // Contains Body + Attachments
    MULTIPART_ALTERNATIVE("multipart/alternative"), // Contains Text + HTML options
    MULTIPART_RELATED("multipart/related"),       // Contains HTML + Inline Images
    MESSAGE_RFC822("message/rfc822"),             // An attached email (.eml file)

    // --- Documents ---
    PDF("application/pdf"),
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html"),
    CSV("text/csv"),

    // --- Microsoft Office ---
    DOC("application/msword"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLS("application/vnd.ms-excel"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPT("application/vnd.ms-powerpoint"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    // --- Images ---
    JPEG("image/jpeg"),
    PNG("image/png"),
    GIF("image/gif"),
    WEBP("image/webp"),
    SVG("image/svg+xml"),

    // --- Archives ---
    ZIP("application/zip"),
    RAR("application/vnd.rar"),
    SEVEN_Z("application/x-7z-compressed"),

    // --- Audio/Video ---
    MP3("audio/mpeg"),
    MP4("video/mp4");

    private final String value;

    MimeType(String value) {
        this.value = value;
    }

    // Static Lookup
    public static MimeType fromValue(String value) {
        for (MimeType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null; // Handle unknown types gracefully
    }
}