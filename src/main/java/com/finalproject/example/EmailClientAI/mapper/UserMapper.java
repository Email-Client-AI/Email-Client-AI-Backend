package com.finalproject.example.EmailClientAI.mapper;

import com.finalproject.example.EmailClientAI.dto.UserDTO;
import org.mapstruct.Mapper;
import com.finalproject.example.EmailClientAI.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper extends EntityMapper<UserDTO, User> {
}