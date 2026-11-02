package co.edu.uniquindio.littlestyle.modules.reportes.factory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reglas compartidas de los formatos interoperables (XLSX y CSV).
 *
 * <p>Los textos que empiezan por un carácter peligroso se neutralizan con una comilla simple para
 * evitar inyección de fórmulas al abrir el archivo en una hoja de cálculo. Los números y fechas se
 * serializan como valores crudos, nunca como cadenas con símbolos de moneda.</p>
 */
final class ValoresReporte {

    /**
     * Caracteres que, al inicio de una celda, pueden abrir una fórmula o alterar la fila.
     *
     * <p>Guía OWASP «CSV Injection»: además de {@code = + - @}, un tabulador o un retorno de carro
     * inicial permiten escapar de la celda y colar contenido ejecutable al abrir el archivo en una
     * hoja de cálculo. Se neutralizan todos por igual.</p>
     */
    private static final String CARACTERES_PELIGROSOS = "=+-@\t\r";

    private ValoresReporte() {
    }

    /** Texto seguro para hoja de cálculo: antepone apóstrofo si parece una fórmula. */
    static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        if (!valor.isEmpty() && CARACTERES_PELIGROSOS.indexOf(valor.charAt(0)) >= 0) {
            return "'" + valor;
        }
        return valor;
    }

    /** Número decimal sin notación científica, símbolo de moneda ni separador de miles. */
    static String numero(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        BigDecimal plano = valor.stripTrailingZeros();
        return plano.scale() < 0 ? plano.setScale(0).toPlainString() : plano.toPlainString();
    }

    /** Fecha ISO 8601, la representación más interoperable entre aplicaciones. */
    static String fecha(LocalDate valor) {
        return valor == null ? "" : valor.toString();
    }

    static LocalDate aFecha(Object valor) {
        return switch (valor) {
            case null -> null;
            case LocalDate d -> d;
            case LocalDateTime d -> d.toLocalDate();
            case java.sql.Date d -> d.toLocalDate();
            case java.time.Instant i -> LocalDate.ofInstant(i, java.time.ZoneId.systemDefault());
            case String s -> s.isBlank() ? null : LocalDate.parse(s);
            default -> null;
        };
    }

    static BigDecimal aDecimal(Object valor) {
        return switch (valor) {
            case null -> null;
            case BigDecimal d -> d;
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            case String s -> s.isBlank() ? null : new BigDecimal(s);
            default -> null;
        };
    }
}
