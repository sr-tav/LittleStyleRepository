// dto/InventarioFila.java
package co.edu.uniquindio.littlestyle.modules.reportes.dto;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;

/**
 * Fila del reporte de inventario. El stock es por variante: cada combinación
 * prenda + talla + color es un SKU independiente, por lo que {@code color} es
 * parte de la identidad de la fila y no un atributo decorativo.
 */
public record InventarioFila(String producto, String tienda, String categoria, String talla,
                             ColorPreferido color, int stock, Integer stockMinimo) {
}
