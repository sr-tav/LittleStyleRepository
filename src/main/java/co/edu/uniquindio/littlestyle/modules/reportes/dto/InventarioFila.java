// dto/InventarioFila.java
package co.edu.uniquindio.littlestyle.modules.reportes.dto;

public record InventarioFila(String producto, String tienda, String categoria, String talla,
                             int stock, Integer stockMinimo) {
}