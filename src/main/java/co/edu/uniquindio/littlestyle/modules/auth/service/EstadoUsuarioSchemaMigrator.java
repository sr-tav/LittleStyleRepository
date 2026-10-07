package co.edu.uniquindio.littlestyle.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class EstadoUsuarioSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!estadoEsEnum(connection.getMetaData())) {
                return;
            }

            String database = connection.getMetaData().getDatabaseProductName()
                    .toLowerCase(Locale.ROOT);
            if (database.contains("h2")) {
                jdbcTemplate.execute(
                        "ALTER TABLE usuarios ALTER COLUMN estado SET DATA TYPE VARCHAR(20)");
            } else if (database.contains("mysql") || database.contains("mariadb")) {
                jdbcTemplate.execute(
                        "ALTER TABLE usuarios MODIFY COLUMN estado VARCHAR(20) NOT NULL");
            } else {
                throw new IllegalStateException(
                        "No hay migración para la columna enum usuarios.estado en " + database);
            }
            log.info("Columna usuarios.estado migrada a VARCHAR para admitir los estados vigentes");
        }
    }

    private boolean estadoEsEnum(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet columnas = metadata.getColumns(null, null, null, null)) {
            while (columnas.next()) {
                if ("usuarios".equalsIgnoreCase(columnas.getString("TABLE_NAME"))
                        && "estado".equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) {
                    return columnas.getString("TYPE_NAME")
                            .toUpperCase(Locale.ROOT).startsWith("ENUM");
                }
            }
        }
        return false;
    }
}
