package co.edu.uniquindio.littlestyle.modules.admin.service;

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

/**
 * Migra al arrancar {@code auditoria_admin.accion} de ENUM a VARCHAR en bases creadas con versiones
 * anteriores, para que admita todas las acciones de
 * {@link co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria} (p. ej. ELIMINAR_USUARIO).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccionAuditoriaSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /** Si la columna sigue siendo ENUM, la convierte a VARCHAR(30) con la sintaxis de H2 o MySQL/MariaDB. */
    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!accionEsEnum(connection.getMetaData())) {
                return;
            }
            String database = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (database.contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE auditoria_admin ALTER COLUMN accion SET DATA TYPE VARCHAR(30)");
            } else if (database.contains("mysql") || database.contains("mariadb")) {
                jdbcTemplate.execute("ALTER TABLE auditoria_admin MODIFY COLUMN accion VARCHAR(30) NOT NULL");
            } else {
                throw new IllegalStateException(
                        "No hay migración para la columna enum auditoria_admin.accion en " + database);
            }
            log.info("Columna auditoria_admin.accion migrada a VARCHAR para admitir las acciones vigentes");
        }
    }

    /** Indica si la columna {@code auditoria_admin.accion} existe y es de tipo ENUM. */
    private boolean accionEsEnum(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet columnas = metadata.getColumns(null, null, null, null)) {
            while (columnas.next()) {
                if ("auditoria_admin".equalsIgnoreCase(columnas.getString("TABLE_NAME"))
                        && "accion".equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) {
                    return columnas.getString("TYPE_NAME").toUpperCase(Locale.ROOT).startsWith("ENUM");
                }
            }
        }
        return false;
    }
}