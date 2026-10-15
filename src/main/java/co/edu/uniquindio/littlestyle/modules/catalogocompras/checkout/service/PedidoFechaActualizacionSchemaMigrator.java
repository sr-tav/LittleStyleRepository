package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

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
public class PedidoFechaActualizacionSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (!tablaPedidosExiste(metadata) || columnaFechaActualizacionExiste(metadata)) {
                return;
            }

            String database = metadata.getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (database.contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE pedidos ADD COLUMN fecha_actualizacion TIMESTAMP");
                jdbcTemplate.execute("""
                        UPDATE pedidos
                        SET fecha_actualizacion = fecha_creacion
                        WHERE fecha_actualizacion IS NULL
                        """);
                jdbcTemplate.execute("""
                        ALTER TABLE pedidos
                        ALTER COLUMN fecha_actualizacion SET NOT NULL
                        """);
            } else if (database.contains("mysql") || database.contains("mariadb")) {
                jdbcTemplate.execute("""
                        ALTER TABLE pedidos
                        ADD COLUMN fecha_actualizacion DATETIME(6) NULL
                        """);
                jdbcTemplate.execute("""
                        UPDATE pedidos
                        SET fecha_actualizacion = fecha_creacion
                        WHERE fecha_actualizacion IS NULL
                        """);
                jdbcTemplate.execute("""
                        ALTER TABLE pedidos
                        MODIFY COLUMN fecha_actualizacion DATETIME(6) NOT NULL
                        """);
            } else {
                throw new IllegalStateException(
                        "No hay migración para pedidos.fecha_actualizacion en " + database);
            }
            log.info("Columna pedidos.fecha_actualizacion migrada desde fecha_creacion");
        }
    }

    private boolean tablaPedidosExiste(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet tablas = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
            while (tablas.next()) {
                if ("pedidos".equalsIgnoreCase(tablas.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnaFechaActualizacionExiste(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet columnas = metadata.getColumns(null, null, null, null)) {
            while (columnas.next()) {
                if ("pedidos".equalsIgnoreCase(columnas.getString("TABLE_NAME"))
                        && "fecha_actualizacion".equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
