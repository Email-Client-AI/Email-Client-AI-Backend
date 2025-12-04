package com.finalproject.example.EmailClientAI.mapper;

import com.finalproject.example.EmailClientAI.dto.email.AttachmentDTO;
import com.finalproject.example.EmailClientAI.entity.Attachment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {
    AttachmentDTO toDto(Attachment attachment);
    Attachment toEntity(AttachmentDTO attachmentDTO);
}