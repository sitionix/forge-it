package com.sitionix.forgeit.domain.model.sql;

public interface RelationalModuleProperties {

    Boolean getEnabled();

    Mode getMode();

    Connection getConnection();

    Paths getPaths();

    enum Mode {
        INTERNAL,
        EXTERNAL
    }

    interface Connection {
        String getDatabase();
        String getUsername();
        String getPassword();
        String getHost();
        Integer getPort();
        String getJdbcUrl();
    }

    interface Paths {
        Ddl getDdl();
        Entity getEntity();

        interface Ddl {
            String getPath();
        }

        interface Entity {
            String getDefaults();
            String getCustom();
        }
    }
}

