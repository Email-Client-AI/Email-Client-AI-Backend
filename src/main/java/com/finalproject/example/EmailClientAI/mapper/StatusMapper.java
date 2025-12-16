package com.finalproject.example.EmailClientAI.mapper;

import com.finalproject.example.EmailClientAI.dto.StatusDTO;
import com.finalproject.example.EmailClientAI.dto.UserDTO;
import com.finalproject.example.EmailClientAI.entity.Status;
import com.finalproject.example.EmailClientAI.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatusMapper extends EntityMapper<StatusDTO, Status> {
}