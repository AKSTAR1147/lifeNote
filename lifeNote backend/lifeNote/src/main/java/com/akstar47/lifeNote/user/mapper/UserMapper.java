package com.akstar47.lifeNote.user.mapper;

import com.akstar47.lifeNote.user.dto.UserProfileResponse;
import com.akstar47.lifeNote.user.entity.AppUser;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserProfileResponse toProfileResponse(AppUser user);
}
