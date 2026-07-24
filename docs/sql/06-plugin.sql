-- Plugin runtime migration history.
-- Plugin-owned schema changes are executed by the startup plugin runtime and recorded here.

CREATE TABLE plugin_schema_history (
    plugin_id VARCHAR(128) NOT NULL,
    plugin_version VARCHAR(64) NOT NULL,
    migration_version VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    checksum CHAR(64) NOT NULL,
    executed_at DATETIME(6) NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (plugin_id, migration_version)
);
