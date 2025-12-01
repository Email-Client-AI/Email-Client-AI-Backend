package com.finalproject.example.EmailClientAI.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    // Authentication & Authorization
    UNAUTHENTICATED(401, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(403, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(401, "Invalid or expired token", HttpStatus.UNAUTHORIZED),
    INVALID_EMAIL_PASSWORD(401, "Email or password is incorrect", HttpStatus.UNAUTHORIZED),

    // User
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS(409, "User already exists", HttpStatus.CONFLICT),

    // Email
    EMAIL_NOT_FOUND(404, "Email not found", HttpStatus.NOT_FOUND),
    MAILBOX_NOT_FOUND(404, "Mailbox not found", HttpStatus.NOT_FOUND),

    // Google OAuth
    INVALID_GOOGLE_TOKEN(401, "Invalid Google token", HttpStatus.UNAUTHORIZED),

    // General
    UNCATEGORIZED_EXCEPTION(500, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(400, "Validation error", HttpStatus.BAD_REQUEST),

    INVALID_LOGOUT_REQUEST(400, "Invalid logout request", HttpStatus.BAD_REQUEST),
    INVALID_REFRESH_REQUEST(400, "Invalid refresh token request", HttpStatus.BAD_REQUEST),
    SESSION_USER_MISMATCH(400, "Session does not belong to the user", HttpStatus.BAD_REQUEST),
    SESSION_EXPIRED(400, "Session has expired", HttpStatus.BAD_REQUEST);

    int code;
    String message;
    HttpStatus status;
}
