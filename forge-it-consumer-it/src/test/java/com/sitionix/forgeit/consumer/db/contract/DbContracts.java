package com.sitionix.forgeit.consumer.db.contract;


import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import com.sitionix.forgeit.consumer.db.entity.UserStatusEntity;
import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbContractsDsl;

public class DbContracts {

    public static final DbContract<UserStatusEntity> USER_STATUS_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(UserStatusEntity.class)
                    .build();

    public static final DbContract<UserEntity> USER_ENTITY_DB_CONTRACT =
            DbContractsDsl.entity(UserEntity.class)
                    .dependsOn(USER_STATUS_ENTITY_DB_CONTRACT, UserEntity::setStatus)
                    .withDefaultBody("default_user_entity.json")
                    .build();
}
