package com.finalproject.example.EmailClientAI.mapper;

import com.finalproject.example.EmailClientAI.dto.email.EmailDTO;
import com.finalproject.example.EmailClientAI.entity.Email;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses= {AttachmentMapper.class})
public interface EmailMapper extends EntityMapper<EmailDTO, Email> {

}
