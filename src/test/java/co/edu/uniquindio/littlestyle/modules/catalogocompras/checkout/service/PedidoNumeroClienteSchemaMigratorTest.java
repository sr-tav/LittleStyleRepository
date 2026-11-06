package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoNumeroClienteSchemaMigratorTest {

    @Test
    void omiteLaMigracionMySqlEnH2SinInterrumpirElArranque() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:pedido-numero-cliente-schema-migrator;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE pedidos (
                    id BIGINT PRIMARY KEY,
                    cliente_id BIGINT NOT NULL
                )
                """);

        PedidoNumeroClienteSchemaMigrator migrator =
                new PedidoNumeroClienteSchemaMigrator(dataSource, jdbcTemplate);

        migrator.run();

        assertThat(jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE LOWER(TABLE_NAME) = 'pedidos'
                """, String.class)).doesNotContain("numero_cliente");
    }
}
