package com.finalproject.example.EmailClientAI.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatusDTO {
    Long id;
    String name;
    Integer orderIndex;
}
