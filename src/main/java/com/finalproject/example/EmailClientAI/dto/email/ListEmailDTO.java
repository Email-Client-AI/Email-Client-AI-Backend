package com.finalproject.example.EmailClientAI.dto.email;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListEmailDTO {
    private Long total;
    private Integer totalPages;
    private Integer currentPage;
    private Long totalOverDue;
    List<EmailDTO> emails;
}
