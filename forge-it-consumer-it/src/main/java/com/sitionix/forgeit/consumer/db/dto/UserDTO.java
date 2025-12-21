package com.sitionix.forgeit.consumer.db.dto;

import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
public class UserDTO {

    private Long id;
    private String username;
    private String status;
    private List<ProductDTO> products;

    public static UserDTO asUserDTO(final UserEntity entity) {
        final List<ProductDTO> products = Optional.ofNullable(entity.getProducts())
                .orElse(List.of())
                .stream()
                .map(ProductDTO::asProductDTO)
                .collect(Collectors.toList());

        return UserDTO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .status(entity.getStatus().getDescription())
                .products(products)
                .build();
    }
}
