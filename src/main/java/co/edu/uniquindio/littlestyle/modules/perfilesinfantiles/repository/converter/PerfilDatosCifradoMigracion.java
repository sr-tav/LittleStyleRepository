package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.converter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Component
public class PerfilDatosCifradoMigracion implements ApplicationRunner {

    private static final String VERSION_MIGRACION = "US06_DATOS_CIFRADOS_V1";
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.perfiles.cifrado.migrar-datos:false}")
    private boolean migracionHabilitada;

    public PerfilDatosCifradoMigracion(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!migracionHabilitada) {
            return;
        }
        Integer completada = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM perfil_datos_cifrado_version WHERE version = ?",
                Integer.class, VERSION_MIGRACION);
        if (completada != null && completada > 0) {
            return;
        }
        migrarDatosAnteriores(true);
        jdbcTemplate.update("""
                INSERT INTO perfil_datos_cifrado_version (version, completada_en)
                VALUES (?, CURRENT_TIMESTAMP)
                """, VERSION_MIGRACION);
    }

    @Transactional
    public void migrarDatosAnteriores() {
        migrarDatosAnteriores(false);
    }

    private void migrarDatosAnteriores(boolean nombresAnterioresEnClaro) {
        migrarPerfiles(nombresAnterioresEnClaro);
        migrarOpciones("perfil_colores_preferidos", "color");
        migrarOpciones("perfil_estampados_preferidos", "estampado");
        migrarMediciones();
    }

    private void migrarPerfiles(boolean nombresAnterioresEnClaro) {
        List<PerfilAnterior> perfiles = jdbcTemplate.query("""
                SELECT id, nombre, fecha_nacimiento, contextura, holgura, alergias_cifradas,
                       otra_alergia_cifrada, sin_alergias, otro_color_cifrado, otro_estampado_cifrado
                FROM perfiles_infantiles
                """, this::perfilAnterior);
        for (PerfilAnterior perfil : perfiles) {
            String nombre = nombresAnterioresEnClaro
                    ? cifrarNombreAnterior(perfil.nombre()) : cifrarTextoClaro(perfil.nombre());
            String fechaNacimiento = cifrarFecha(perfil.fechaNacimiento());
            String contextura = cifrarTextoClaro(perfil.contextura());
            String holgura = cifrarTextoClaro(perfil.holgura());
            String alergias = cifrarTextoCifradoLegado(perfil.alergias());
            String otraAlergia = cifrarTextoCifradoLegado(perfil.otraAlergia());
            String sinAlergias = cifrarBooleano(perfil.sinAlergias());
            String otroColor = cifrarTextoCifradoLegado(perfil.otroColor());
            String otroEstampado = cifrarTextoCifradoLegado(perfil.otroEstampado());
            if (nombresAnterioresEnClaro || !perfil.yaCifrado()) {
                int actualizados = jdbcTemplate.update("""
                        UPDATE perfiles_infantiles
                        SET nombre = ?, fecha_nacimiento = ?, contextura = ?, holgura = ?,
                            alergias_cifradas = ?, otra_alergia_cifrada = ?, sin_alergias = ?,
                            otro_color_cifrado = ?, otro_estampado_cifrado = ?
                        WHERE id = ?
                        """, nombre, fechaNacimiento, contextura, holgura, alergias,
                        otraAlergia, sinAlergias, otroColor, otroEstampado, perfil.id());
                exigirUnaFila(actualizados, "perfil");
            }
        }
    }

    private void migrarOpciones(String tabla, String columna) {
        List<OpcionAnterior> opciones = jdbcTemplate.query(
                "SELECT perfil_id, " + columna + " FROM " + tabla,
                (rs, row) -> new OpcionAnterior(rs.getLong("perfil_id"), rs.getString(columna)));
        for (OpcionAnterior opcion : opciones) {
            String cifrada = cifrarTextoClaro(opcion.valor());
            if (!cifrada.equals(opcion.valor())) {
                int actualizadas = jdbcTemplate.update(
                        "UPDATE " + tabla + " SET " + columna + " = ? WHERE perfil_id = ? AND "
                                + columna + " = ?",
                        cifrada, opcion.perfilId(), opcion.valor());
                exigirUnaFila(actualizadas, "preferencia");
            }
        }
    }

    private void migrarMediciones() {
        List<MedicionAnterior> mediciones = jdbcTemplate.query("""
                SELECT id, fecha_medicion, estatura_cifrada, peso_cifrado
                FROM mediciones_crecimiento
                """, (rs, row) -> new MedicionAnterior(rs.getLong("id"),
                rs.getString("fecha_medicion"), rs.getString("estatura_cifrada"),
                rs.getString("peso_cifrado")));
        for (MedicionAnterior medicion : mediciones) {
            String fecha = cifrarFecha(medicion.fecha());
            String estatura = cifrarTextoCifradoLegado(medicion.estatura());
            String peso = cifrarTextoCifradoLegado(medicion.peso());
            if (!medicion.yaCifrado()) {
                int actualizadas = jdbcTemplate.update("""
                        UPDATE mediciones_crecimiento
                        SET fecha_medicion = ?, estatura_cifrada = ?, peso_cifrado = ?
                        WHERE id = ?
                        """, fecha, estatura, peso, medicion.id());
                exigirUnaFila(actualizadas, "medición");
            }
        }
    }

    private String cifrarFecha(String valor) {
        if (valor == null || CifradoPerfil.estaCifradoEnFormatoActual(valor)) {
            return valor;
        }
        String fecha;
        try {
            fecha = LocalDate.parse(valor).toString();
        } catch (DateTimeParseException ex) {
            fecha = CifradoPerfil.descifrar(valor);
        }
        return CifradoPerfil.cifrar(fecha);
    }

    private String cifrarTextoClaro(String valor) {
        return valor == null || CifradoPerfil.estaCifradoEnFormatoActual(valor)
                ? valor : CifradoPerfil.cifrar(valor);
    }

    private String cifrarNombreAnterior(String valor) {
        return valor == null ? null : CifradoPerfil.cifrar(valor);
    }

    private String cifrarBooleano(String valor) {
        if (valor == null || CifradoPerfil.estaCifradoEnFormatoActual(valor)) {
            return valor;
        }
        String normalizado = switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "1", "true", "t", "yes" -> "true";
            case "0", "false", "f", "no" -> "false";
            default -> throw new IllegalStateException(
                    "No se pudo migrar el indicador heredado de alergias");
        };
        return CifradoPerfil.cifrar(normalizado);
    }

    private String cifrarTextoCifradoLegado(String valor) {
        if (valor == null || CifradoPerfil.estaCifradoEnFormatoActual(valor)) {
            return valor;
        }
        return CifradoPerfil.cifrar(CifradoPerfil.descifrar(valor));
    }

    private PerfilAnterior perfilAnterior(ResultSet rs, int row) throws SQLException {
        return new PerfilAnterior(rs.getLong("id"), rs.getString("nombre"),
                rs.getString("fecha_nacimiento"), rs.getString("contextura"),
                rs.getString("holgura"), rs.getString("alergias_cifradas"),
                rs.getString("otra_alergia_cifrada"), rs.getString("sin_alergias"),
                rs.getString("otro_color_cifrado"), rs.getString("otro_estampado_cifrado"));
    }

    private void exigirUnaFila(int filas, String entidad) {
        if (filas != 1) {
            throw new IllegalStateException("No se pudo migrar un registro de " + entidad);
        }
    }

    private record PerfilAnterior(Long id, String nombre, String fechaNacimiento, String contextura,
                                  String holgura, String alergias, String otraAlergia,
                                  String sinAlergias, String otroColor, String otroEstampado) {
        private boolean yaCifrado() {
            return java.util.stream.Stream.of(nombre, fechaNacimiento, contextura, holgura, alergias,
                            otraAlergia, sinAlergias, otroColor, otroEstampado)
                    .allMatch(campo -> campo == null || CifradoPerfil.estaCifradoEnFormatoActual(campo));
        }
    }

    private record OpcionAnterior(Long perfilId, String valor) {
    }

    private record MedicionAnterior(Long id, String fecha, String estatura, String peso) {
        private boolean yaCifrado() {
            return java.util.stream.Stream.of(fecha, estatura, peso)
                    .allMatch(campo -> campo == null || CifradoPerfil.estaCifradoEnFormatoActual(campo));
        }
    }
}
