package com.sitionix.forgeit.postgresql.internal.domain;


import com.sitionix.forgeit.domain.contract.DbContract;
import com.sitionix.forgeit.domain.contract.DbEntityFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresDbEntityFactory implements DbEntityFactory {



    @Override
    public <E> E create(final DbContract<E> contract) {


        // TODO: тут ти:
        // 1) знаходиш відповідний JSON (пізніше – через DbContractInvocation)
        // 2) мапиш його в E
        // 3) робиш insert у Postgres
        // 4) повертаєш ентіті E
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
