package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.service;

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
 * Migra las columnas de moderación de {@code prendas} para US-14. Las prendas existentes quedan
 * APROBADAS para conservar la visibilidad que tenían; las nuevas nacen SIN_REVISAR. Compatible con H2
 * (MODE=MySQL) y MySQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstadoModeracionSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /** Agrega las columnas ausentes y normaliza los estados heredados sin ocultar prendas existentes. */
    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (!columnaExiste(metadata, "estado_moderacion")) {
                jdbcTemplate.execute(
                        "ALTER TABLE prendas ADD COLUMN estado_moderacion VARCHAR(20) "
                                + "NOT NULL DEFAULT 'APROBADA'");
            } else if (estadoEsEnum(metadata)) {
                String database = connection.getMetaData().getDatabaseProductName()
                        .toLowerCase(Locale.ROOT);
                if (database.contains("h2")) {
                    jdbcTemplate.execute(
                            "ALTER TABLE prendas ALTER COLUMN estado_moderacion SET DATA TYPE VARCHAR(20)");
                } else if (database.contains("mysql") || database.contains("mariadb")) {
                    jdbcTemplate.execute(
                            "ALTER TABLE prendas MODIFY COLUMN estado_moderacion VARCHAR(20) NULL");
                } else {
                    throw new IllegalStateException(
                            "No hay migración para la columna enum prendas.estado_moderacion en " + database);
                }
            }
            agregarColumnaSiFalta(metadata, "motivo_moderacion",
                    "ALTER TABLE prendas ADD COLUMN motivo_moderacion VARCHAR(500) NULL");
            agregarColumnaSiFalta(metadata, "fecha_moderacion",
                    "ALTER TABLE prendas ADD COLUMN fecha_moderacion TIMESTAMP NULL");
            agregarColumnaSiFalta(metadata, "admin_moderacion_id",
                    "ALTER TABLE prendas ADD COLUMN admin_moderacion_id BIGINT NULL");

            jdbcTemplate.update(
                    "UPDATE prendas SET estado_moderacion = 'APROBADA' WHERE estado_moderacion IS NULL");
            jdbcTemplate.update(
                    "UPDATE prendas SET estado_moderacion = 'SIN_REVISAR' WHERE estado_moderacion = 'PENDIENTE'");
            jdbcTemplate.update(
                    "UPDATE prendas SET estado_moderacion = 'DESACTIVADA' WHERE estado_moderacion = 'RECHAZADA'");
            log.info("Esquema de moderación de prendas verificado y actualizado");
        }
    }

    /** Agrega una columna de moderación solo si aún no existe. */
    private void agregarColumnaSiFalta(DatabaseMetaData metadata, String nombre, String sql)
            throws SQLException {
        if (!columnaExiste(metadata, nombre)) {
            jdbcTemplate.execute(sql);
        }
    }

    /** Indica si una columna de {@code prendas} ya existe. */
    private boolean columnaExiste(DatabaseMetaData metadata, String nombre) throws SQLException {
        try (ResultSet columnas = metadata.getColumns(null, null, null, null)) {
            while (columnas.next()) {
                if ("prendas".equalsIgnoreCase(columnas.getString("TABLE_NAME"))
                        && nombre.equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Indica si el esquema heredado todavía restringe los estados con un tipo SQL ENUM. */
    private boolean estadoEsEnum(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet columnas = metadata.getColumns(null, null, null, null)) {
            while (columnas.next()) {
                if ("prendas".equalsIgnoreCase(columnas.getString("TABLE_NAME"))
                        && "estado_moderacion".equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) {
                    return columnas.getString("TYPE_NAME").toUpperCase(Locale.ROOT).startsWith("ENUM");
                }
            }
        }
        return false;
    }
}