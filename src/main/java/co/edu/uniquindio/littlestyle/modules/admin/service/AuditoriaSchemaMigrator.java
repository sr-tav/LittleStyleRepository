package co.edu.uniquindio.littlestyle.modules.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Crea y siembra la fila usada para serializar la escritura de la cadena de auditoría (US-14, RNF-08).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /** Asegura que exista un candado persistente, incluso antes de registrar la primera acción. */
    @Override
    public void run(ApplicationArguments args) throws SQLException {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auditoria_admin_candado (
                    id INTEGER NOT NULL PRIMARY KEY
                )
                """);

        try (Connection connection = dataSource.getConnection()) {
            String database = connection.getMetaData().getDatabaseProductName()
                    .toLowerCase(Locale.ROOT);
            if (database.contains("mysql") || database.contains("mariadb")) {
                jdbcTemplate.update("INSERT IGNORE INTO auditoria_admin_candado (id) VALUES (1)");
            } else if (database.contains("h2")) {
                jdbcTemplate.update("MERGE INTO auditoria_admin_candado (id) KEY (id) VALUES (1)");
            } else if (database.contains("postgresql")) {
                jdbcTemplate.update("INSERT INTO auditoria_admin_candado (id) VALUES (1) "
                        + "ON CONFLICT (id) DO NOTHING");
            } else {
                throw new IllegalStateException(
                        "No hay inicialización del candado de auditoría para " + database);
            }
        }
        log.info("Candado de escritura de auditoría verificado");
    }
}
