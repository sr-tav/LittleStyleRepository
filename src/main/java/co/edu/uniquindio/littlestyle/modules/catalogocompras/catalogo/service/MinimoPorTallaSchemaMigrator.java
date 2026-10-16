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
 * Migración al mínimo por talla: reparte el antiguo mínimo del producto a las
 * tallas que no declaran uno y luego retira la columna. Idempotente: si la
 * columna del producto ya no existe, no hay nada que migrar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinimoPorTallaSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (!existeColumna(metadata, "prendas", "stock_minimo")) {
                return;
            }
            if (!existeColumna(metadata, "prenda_tallas", "stock_minimo")) {
                return;
            }
            jdbcTemplate.execute("""
                    UPDATE prenda_tallas
                    SET stock_minimo = (SELECT p.stock_minimo FROM prendas p WHERE p.id = prenda_tallas.prenda_id)
                    WHERE stock_minimo IS NULL
                    """);
            jdbcTemplate.execute("ALTER TABLE prendas DROP COLUMN stock_minimo");
            log.info("Mínimo migrado de prendas.stock_minimo a prenda_tallas.stock_minimo");
        }
    }

    private boolean existeColumna(DatabaseMetaData metadata, String tabla, String columna) throws SQLException {
        try (ResultSet columnas = metadata.getColumns(null, null, null, null)) {
            while (columnas.next()) {
                if (tabla.equalsIgnoreCase(columnas.getString("TABLE_NAME"))
                        && columna.equalsIgnoreCase(columnas.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
