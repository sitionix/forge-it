package com.sitionix.forgeit.consumer.db.contract;


import com.sitionix.forgeit.consumer.db.entity.CategoryEntity;
import com.sitionix.forgeit.consumer.db.entity.ProductEntity;
import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.consumer.db.entity.UserStatusEntity;
import com.sitionix.forgeit.core.contract.ForgeDbContracts;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsDsl;
import com.sitionix.forgeit.domain.contract.clean.CleanupPolicy;

@ForgeDbContracts
public class DbContracts {

    public static final DbContract<UserStatusEntity> USER_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(UserStatusEntity.class)
                    .cleanupPolicy(CleanupPolicy.NONE)
                    .build();

    public static final DbContract<UserEntity> USER_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(UserEntity.class)
                    .dependsOn(USER_STATUS_ENTITY_DB_CONTRACT, UserEntity::setStatus)
                    .withDefaultBody("default_user_entity.json")
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();

    public static final DbContract<CategoryEntity> CATEGORY_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(CategoryEntity.class)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();

    public static final DbContract<ProductEntity> PRODUCT_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(ProductEntity.class)
                    .dependsOn(USER_ENTITY_DB_CONTRACT, ProductEntity::setUser)
                    .dependsOnOptional(CATEGORY_ENTITY_DB_CONTRACT, ProductEntity::setCategory)
                    .cleanupPolicy(CleanupPolicy.DELETE_ALL)
                    .build();
}
