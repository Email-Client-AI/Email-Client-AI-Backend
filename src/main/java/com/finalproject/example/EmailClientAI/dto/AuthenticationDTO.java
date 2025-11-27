package com.finalproject.example.EmailClientAI.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationDTO {
    @JsonIgnore
    String refreshToken;
    @JsonIgnore
    String deviceId;
    String accessToken;
    UserDTO user;
}