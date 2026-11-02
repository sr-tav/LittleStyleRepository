package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

/**
 * Resultado que el cliente reporta tras intentar abrir, guardar o imprimir el archivo exportado.
 *
 * <p>El contrato HTTP usa minúsculas ({@code ok}, {@code error_red}, {@code blob_vacio},
 * {@code error_cliente}, {@code cancelado}). El enum conserva nombres Java en mayúsculas y
 * {@link #desde(String)} acepta ambas formas sin distinguir mayúsculas. Un valor desconocido
 * produce {@code 400} al deserializar el cuerpo.</p>
 */
public enum ResultadoExportacion {
    OK,
    ERROR_RED,
    BLOB_VACIO,
    ERROR_CLIENTE,
    CANCELADO;

    /** Convierte el texto recibido del navegador; lanza {@link IllegalArgumentException} si no existe. */
    @JsonCreator
    public static ResultadoExportacion desde(String valor) {
        String v = valor == null ? "" : valor.trim();
        return Arrays.stream(values())
                .filter(r -> r.name().equalsIgnoreCase(v))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Resultado de telemetría no válido. Usa: ok, error_red, blob_vacio, "
                                + "error_cliente o cancelado"));
    }

    /** Representación de cable en minúsculas, alineada con las etiquetas de Micrometer del RNF-24. */
    @JsonValue
    public String valor() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Una exportación solo es exitosa si el cliente confirma que pudo abrir o guardar el archivo. */
    public boolean esExito() {
        return this == OK;
    }

    /** La cancelación del usuario no es un fallo del sistema y queda fuera del denominador. */
    public boolean entraEnDenominador() {
        return this != CANCELADO;
    }
}
