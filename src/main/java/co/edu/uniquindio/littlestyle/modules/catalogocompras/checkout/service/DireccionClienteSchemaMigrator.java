package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Elimina la restricción histórica de una dirección por cliente sin modificar las filas existentes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DireccionClienteSchemaMigrator implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String database = metadata.getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (database.contains("mysql") || database.contains("mariadb")) {
                eliminarIndicesMySql(metadata);
                return;
            }

            String tabla = nombreTabla(metadata);
            if (tabla == null) return;
            List<String> indices = indicesUnicosDeCliente(metadata, tabla);
            if (indices.isEmpty()) return;

            boolean migracionCompleta = true;
            for (String indice : indices) {
                if (database.contains("h2")) {
                    String restriccion = restriccionUnicaH2(tabla, indice);
                    if (restriccion == null) {
                        try {
                            jdbcTemplate.execute("DROP INDEX IF EXISTS " + identificar(metadata, indice));
                        } catch (DataAccessException e) {
                            migracionCompleta = false;
                            log.warn("No se pudo eliminar el índice único {} de {} durante la migración",
                                    indice, tabla, e);
                        }
                    } else {
                        jdbcTemplate.execute("ALTER TABLE " + identificar(metadata, tabla)
                                + " DROP CONSTRAINT " + identificar(metadata, restriccion));
                    }
                } else {
                    throw new IllegalStateException(
                            "No hay migración para la restricción de direcciones_cliente en " + database);
                }
            }
            if (migracionCompleta) {
                log.info("Restricción de una dirección por cliente eliminada; se conservaron las direcciones existentes");
            }
        }
    }

    private void eliminarIndicesMySql(DatabaseMetaData metadata) throws SQLException {
        List<String> indices = indicesUnicosClienteMySql();
        if (indices.isEmpty()) return;

        crearIndiceNoUnicoMySql(metadata);
        for (String indice : indices) {
            jdbcTemplate.execute("ALTER TABLE " + identificar(metadata, "direcciones_cliente")
                    + " DROP INDEX " + identificar(metadata, indice));
        }
        if (!indicesUnicosClienteMySql().isEmpty()) {
            throw new IllegalStateException(
                    "No se pudo eliminar la restricción única de direcciones_cliente.cliente_id");
        }
        log.info("Restricción de una dirección por cliente eliminada; se conservaron las direcciones existentes");
    }

    private List<String> indicesUnicosClienteMySql() {
        return jdbcTemplate.queryForList("""
                SELECT INDEX_NAME
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND LOWER(TABLE_NAME) = 'direcciones_cliente'
                  AND NON_UNIQUE = 0
                GROUP BY INDEX_NAME
                HAVING COUNT(*) = 1
                   AND SUM(CASE WHEN LOWER(COLUMN_NAME) = 'cliente_id' THEN 1 ELSE 0 END) = 1
                """, String.class);
    }

    private void crearIndiceNoUnicoMySql(DatabaseMetaData metadata) throws SQLException {
        Integer existente = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND LOWER(TABLE_NAME) = 'direcciones_cliente'
                  AND NON_UNIQUE = 1
                  AND LOWER(COLUMN_NAME) = 'cliente_id'
                  AND SEQ_IN_INDEX = 1
                """, Integer.class);
        if (existente != null && existente > 0) return;

        String base = "idx_direcciones_cliente_cliente_id";
        String indice = base;
        int sufijo = 2;
        while (indiceMySqlExiste(indice)) {
            indice = base + "_" + sufijo++;
        }
        jdbcTemplate.execute("CREATE INDEX " + identificar(metadata, indice)
                + " ON " + identificar(metadata, "direcciones_cliente")
                + " (" + identificar(metadata, "cliente_id") + ")");
    }

    private boolean indiceMySqlExiste(String nombre) {
        Integer cantidad = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND LOWER(TABLE_NAME) = 'direcciones_cliente'
                  AND INDEX_NAME = ?
                """, Integer.class, nombre);
        return cantidad != null && cantidad > 0;
    }

    private String nombreTabla(DatabaseMetaData metadata) throws SQLException {
        try (ResultSet tablas = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
            while (tablas.next()) {
                String nombre = tablas.getString("TABLE_NAME");
                if ("direcciones_cliente".equalsIgnoreCase(nombre)) return nombre;
            }
        }
        return null;
    }

    private List<String> indicesUnicosDeCliente(DatabaseMetaData metadata, String tabla) throws SQLException {
        Map<String, IndexInfo> indices = new LinkedHashMap<>();
        try (ResultSet resultado = metadata.getIndexInfo(null, null, tabla, true, false)) {
            while (resultado.next()) {
                String nombre = resultado.getString("INDEX_NAME");
                String columna = resultado.getString("COLUMN_NAME");
                if (nombre == null || columna == null || resultado.getBoolean("NON_UNIQUE")) continue;
                indices.computeIfAbsent(nombre, ignorado -> new IndexInfo())
                        .columnas().add(columna.toLowerCase(Locale.ROOT));
            }
        }
        List<String> resultado = new ArrayList<>();
        indices.forEach((nombre, info) -> {
            if (info.columnas().size() == 1 && info.columnas().contains("cliente_id")) resultado.add(nombre);
        });
        return resultado;
    }

    private String restriccionUnicaH2(String tabla, String indice) {
        List<String> restricciones = jdbcTemplate.queryForList("""
                SELECT CONSTRAINT_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE LOWER(TABLE_NAME) = LOWER(?)
                  AND INDEX_NAME = ?
                  AND CONSTRAINT_TYPE = 'UNIQUE'
                """, String.class, tabla, indice);
        return restricciones.isEmpty() ? null : restricciones.get(0);
    }

    private String identificar(DatabaseMetaData metadata, String identificador) throws SQLException {
        String comilla = metadata.getIdentifierQuoteString().trim();
        if (comilla.isEmpty()) return identificador;
        return comilla + identificador.replace(comilla, comilla + comilla) + comilla;
    }

    private static final class IndexInfo {
        private final Set<String> columnas = new LinkedHashSet<>();

        private Set<String> columnas() {
            return columnas;
        }
    }
}
