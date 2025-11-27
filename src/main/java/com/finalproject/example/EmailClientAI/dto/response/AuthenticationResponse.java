package com.finalproject.example.EmailClientAI.dto.response;

import com.finalproject.example.EmailClientAI.dto.UserDTO;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    String accessToken;
    UserDTO user;
}
