package com.sitionix.forgeit.application.executor.sql;

import com.sitionix.forgeit.domain.executor.SqlScriptExecutor;
import com.sitionix.forgeit.domain.loader.SqlLoader;
import com.sitionix.forgeit.domain.model.sql.RelationalFeatureMarker;
import com.sitionix.forgeit.domain.model.sql.SqlScriptDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(RelationalFeatureMarker.class)
public class SqlScriptExecutorImpl implements SqlScriptExecutor {

    private final SqlLoader sqlLoader;
    private final ResourceLoader resourceLoader;

    @Override
    public void executeAllForDataSource(final DataSource dataSource, final String basePath) {
        try(final Connection connection = dataSource.getConnection()) {

            this.sqlLoader.setBasePath(basePath);
            final List<SqlScriptDescriptor> scripts = this.sqlLoader.loadOrderedScripts();

            scripts.forEach(script -> {
                        final Resource resource = this.resourceLoader.getResource(script.path());
                        ScriptUtils.executeSqlScript(connection, resource);
                    });

        } catch (final Exception e) {
            throw new IllegalStateException("Failed to execute SQL scripts", e);
        }
    }
}
