package com.sitionix.forgeit.domain.loader;

import com.sitionix.forgeit.domain.model.sql.SqlScriptDescriptor;

import java.util.List;

public interface SqlLoader {

    List<SqlScriptDescriptor> loadOrderedScripts();

    void setBasePath(String basePath);
}
