package com.sitionix.forgeit.consumer.db.dto;

import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {

    private Long id;
    private String username;
    private String status;

    public static UserDTO asUserDTO(final UserEntity entity) {
        return UserDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .status(entity.getStatus().getDescription())
                .build();
    }
}
