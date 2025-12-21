package com.sitionix.forgeit.consumer.db.dto;

import com.sitionix.forgeit.consumer.db.entity.ProductEntity;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDTO {

    private Long id;
    private String name;
    private String description;

    public static ProductDTO asProductDTO(final ProductEntity entity) {
        return ProductDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}
