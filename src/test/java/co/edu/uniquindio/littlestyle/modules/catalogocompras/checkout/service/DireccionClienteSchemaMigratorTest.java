package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DireccionClienteSchemaMigratorTest {

    private boolean predeterminada(JdbcTemplate jdbcTemplate, long id) {
        Boolean valor = jdbcTemplate.queryForObject(
                "SELECT predeterminada FROM direcciones_cliente WHERE id = ?",
                Boolean.class,
                id);
        return Boolean.TRUE.equals(valor);
    }

    @Test
    void continuaElArranqueSiNoPuedeEliminarUnIndiceUnicoH2() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:direccion-cliente-schema-migrator-drop-error;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource) {
            @Override
            public void execute(String sql) {
                if (sql.startsWith("DROP INDEX IF EXISTS")) {
                    throw new DataIntegrityViolationException("El índice está ligado a una clave foránea");
                }
                super.execute(sql);
            }
        };
        jdbcTemplate.execute("""
                CREATE TABLE direcciones_cliente (
                    id BIGINT PRIMARY KEY,
                    cliente_id BIGINT NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX ux_direcciones_cliente_cliente_id "
                + "ON direcciones_cliente (cliente_id)");

        DireccionClienteSchemaMigrator migrator = new DireccionClienteSchemaMigrator(dataSource, jdbcTemplate);

        migrator.run(new DefaultApplicationArguments(new String[0]));
    }

    @Test
    void eliminaLaUnicidadHistoricaDeClienteSinPerderLaDireccionGuardada() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:direccion-cliente-schema-migrator;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE direcciones_cliente (
                    id BIGINT PRIMARY KEY,
                    cliente_id BIGINT NOT NULL UNIQUE,
                    direccion VARCHAR(100) NOT NULL
                )
                """);
        jdbcTemplate.update("INSERT INTO direcciones_cliente (id, cliente_id, direccion) VALUES (?, ?, ?)",
                1L, 10L, "Calle 1 # 2 - 3");

        DireccionClienteSchemaMigrator migrator = new DireccionClienteSchemaMigrator(dataSource, jdbcTemplate);
        DefaultApplicationArguments args = new DefaultApplicationArguments(new String[0]);
        migrator.run(args);
        migrator.run(args);

        jdbcTemplate.update(
                "INSERT INTO direcciones_cliente (id, cliente_id, direccion, predeterminada) VALUES (?, ?, ?, ?)",
                2L, 10L, "Carrera 4 # 5 - 6", false);

        assertThat(jdbcTemplate.queryForList(
                "SELECT direccion FROM direcciones_cliente WHERE cliente_id = 10 ORDER BY id", String.class))
                .containsExactly("Calle 1 # 2 - 3", "Carrera 4 # 5 - 6");
    }

    @Test
    void agregaPredeterminadaYMarcaLaDireccionMasAntiguaDeCadaCliente() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:direccion-cliente-predeterminada;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
            CREATE TABLE direcciones_cliente (
                id BIGINT PRIMARY KEY,
                cliente_id BIGINT NOT NULL,
                direccion VARCHAR(100) NOT NULL
            )
            """);
        jdbcTemplate.update(
                "INSERT INTO direcciones_cliente (id, cliente_id, direccion) VALUES (?, ?, ?)",
                1L, 10L, "Calle 1");
        jdbcTemplate.update(
                "INSERT INTO direcciones_cliente (id, cliente_id, direccion) VALUES (?, ?, ?)",
                2L, 10L, "Calle 2");
        jdbcTemplate.update(
                "INSERT INTO direcciones_cliente (id, cliente_id, direccion) VALUES (?, ?, ?)",
                3L, 20L, "Calle 3");

        DireccionClienteSchemaMigrator migrator =
                new DireccionClienteSchemaMigrator(dataSource, jdbcTemplate);
        DefaultApplicationArguments args = new DefaultApplicationArguments(new String[0]);

        migrator.run(args);
        migrator.run(args);

        assertThat(predeterminada(jdbcTemplate, 1L)).isTrue();
        assertThat(predeterminada(jdbcTemplate, 2L)).isFalse();
        assertThat(predeterminada(jdbcTemplate, 3L)).isTrue();
    }
}
