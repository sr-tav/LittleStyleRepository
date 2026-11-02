// dto/ArchivoReporte.java
package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import org.springframework.http.MediaType;

public record ArchivoReporte(byte[] contenido, String nombre, MediaType tipoMedia) {
}