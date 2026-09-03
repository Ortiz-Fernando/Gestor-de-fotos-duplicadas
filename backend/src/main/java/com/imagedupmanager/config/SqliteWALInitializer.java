package com.imagedupmanager.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Enables SQLite WAL journal mode once per database file. WAL is a persistent database
 * property; per-connection PRAGMAs such as busy_timeout are set through HikariCP
 * connection-init-sql in application.yml.
 */
@Component
public class SqliteWALInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public SqliteWALInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // WAL is a persistent database property; enable it once per file.
            try (ResultSet resultSet = statement.executeQuery("PRAGMA journal_mode=WAL")) {
                resultSet.next();
            }

            // Composite unique key: SQLiteDialect cannot emit @UniqueConstraint DDL,
            // so the unique index is created explicitly (see ImageRecord entity).
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_image_scan_path "
                    + "ON image_record (scan_id, absolute_path)");
        } catch (SQLException e) {
            throw new IllegalStateException("No se ha podido inicializar la base de datos SQLite.", e);
        }
    }
}
