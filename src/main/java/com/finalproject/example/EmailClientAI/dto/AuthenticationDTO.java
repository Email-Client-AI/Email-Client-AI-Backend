package com.finalproject.example.EmailClientAI.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.finalproject.example.EmailClientAI.dto.response.UserResponse;
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
    String accessToken;
    UserResponse user;
}