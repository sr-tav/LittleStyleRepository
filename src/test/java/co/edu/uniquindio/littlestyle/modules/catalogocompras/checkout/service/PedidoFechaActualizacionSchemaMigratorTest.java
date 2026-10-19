package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del migrador de esquema que agrega la columna fecha_actualizacion a pedidos.
 * Se corre contra una base H2 en memoria con datos previos para comprobar
 * que la migración es idempotente y no pierde la información existente.
 */
class PedidoFechaActualizacionSchemaMigratorTest {

    // Simula la tabla vieja sin fecha_actualizacion, inserta un registro,
    // ejecuta el migrador dos veces y verifica que la columna apareció
    // inicializada con la fecha de creación y que repetirla no causa error.
    @Test
    void agregaFechaActualizacionALaTablaPedidosExistenteSinPerderDatos() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:pedido-schema-migrator;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 10, 15, 12, 30);
        jdbcTemplate.execute("""
                CREATE TABLE pedidos (
                    id BIGINT PRIMARY KEY,
                    fecha_creacion TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.update("INSERT INTO pedidos (id, fecha_creacion) VALUES (?, ?)",
                1L, Timestamp.valueOf(fechaCreacion));

        PedidoFechaActualizacionSchemaMigrator migrator =
                new PedidoFechaActualizacionSchemaMigrator(dataSource, jdbcTemplate);
        DefaultApplicationArguments args = new DefaultApplicationArguments(new String[0]);

        migrator.run(args);
        migrator.run(args);

        LocalDateTime fechaActualizacion = jdbcTemplate.queryForObject(
                "SELECT fecha_actualizacion FROM pedidos WHERE id = 1", LocalDateTime.class);
        assertThat(fechaActualizacion).isEqualTo(fechaCreacion);
    }
}
