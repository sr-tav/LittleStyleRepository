package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/** Asigna consecutivos visibles a los pedidos existentes, agrupados por cliente. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoNumeroClienteSchemaMigrator implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String database = metadata.getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (!database.contains("mysql") && !database.contains("mariadb")) {
                log.debug("Migración de pedidos.numero_cliente omitida para {}", database);
                return;
            }
            if (!tablaPedidosExiste(metadata) || columnaNumeroClienteExiste(metadata)) {
                return;
            }
        }

        ejecutarPaso("agregar la columna numero_cliente",
                "ALTER TABLE pedidos ADD COLUMN numero_cliente INT NULL");
        ejecutarPaso("asignar los consecutivos por cliente", """
                UPDATE pedidos p
                JOIN (
                    SELECT id, ROW_NUMBER() OVER (PARTITION BY cliente_id ORDER BY id) AS numero
                    FROM pedidos
                ) t ON t.id = p.id
                SET p.numero_cliente = t.numero
                """);
        ejecutarPaso("crear el índice único de pedidos por cliente",
                "ALTER TABLE pedidos ADD UNIQUE INDEX uk_pedidos_cliente_numero "
                        + "(cliente_id, numero_cliente)");
    }

    private void ejecutarPaso(String descripcion, String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException e) {
            log.warn("No se pudo completar la migración de pedidos: {}", descripcion, e);
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

    private boolean columnaNumeroClienteExiste(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet columnas = metadata.getColumns(null, null, null, null)) {
            while (columnas.next()) {
                if ("pedidos".equalsIgnoreCase(columnas.getString("TABLE_NAME"))
                        && "numero_cliente".equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
