package com.finalproject.example.EmailClientAI.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleOAuthDTO {
    String idToken;
    String accessToken;
    String refreshToken;
    Long accessTokenExpiresIn;
    Long refreshTokenExpiresIn;
    GoogleUserInfo userInfo;

    public record GoogleUserInfo(String sub, String email, String name, String picture) {}

}
