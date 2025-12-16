package com.sitionix.forgeit.domain.executor;

import javax.sql.DataSource;

public interface SqlScriptExecutor {

    void executeAllForDataSource(DataSource dataSource, String basePath);
}
