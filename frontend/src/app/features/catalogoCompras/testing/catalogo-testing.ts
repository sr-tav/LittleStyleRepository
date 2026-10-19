import { AlertaInventario, InventarioItem, Prenda } from '../models/prenda.models';

/**
 * Construye una prenda de prueba combinando valores base con cambios opcionales.
 * @param cambios Propiedades que sustituyen los valores base.
 * @returns Prenda de prueba completa.
 */
export function crearPrenda(cambios: Partial<Prenda> = {}): Prenda {
  return {
    id: 7,
    nombre: 'Pantalón Jogger Azul',
    categoria: 'PANTALONES',
    genero: 'UNISEX',
    descripcion: null,
    instruccionesCuidado: 'Lavar a mano en agua fría',
    marca: null,
    precio: 32900,
    estado: 'ACTIVA',
    stockTotal: 3,
    nivelStock: 'STOCK_BAJO',
    colores: ['AZUL'],
    estampado: null,
    tintesSinteticos: false,
    brochesMetalicos: false,
    contieneNiquel: false,
    tratamientoFormaldehido: false,
    composicion: [
      { material: 'ALGODON', porcentaje: 80 },
      { material: 'POLIESTER', porcentaje: 20 },
    ],
    tallas: [
      { id: 1, talla: '4', estaturaMinCm: 100, estaturaMaxCm: 110, pesoMinKg: 15, pesoMaxKg: 20, stock: 2, stockMinimo: 5 },
      { id: 2, talla: '6', estaturaMinCm: 111, estaturaMaxCm: 122, pesoMinKg: 21, pesoMaxKg: 26, stock: 1, stockMinimo: 3 },
    ],
    imagenes: [{ id: 3, url: '/media/prendas/7/a.png', orden: 0 }],
    fechaCreacion: '2026-10-01T10:00:00',
    fechaActualizacion: '2026-10-01T10:00:00',
    ...cambios,
  };
}

/**
 * Construye una línea de inventario de prueba.
 * @param cambios Propiedades que sustituyen los valores base.
 * @returns Elemento de inventario de prueba.
 */
export function crearItemInventario(cambios: Partial<InventarioItem> = {}): InventarioItem {
  return {
    prendaId: 7,
    nombre: 'Pantalón Jogger Azul',
    categoria: 'PANTALONES',
    imagenUrl: null,
    estado: 'ACTIVA',
    stockTotal: 3,
    nivel: 'STOCK_BAJO',
    tallas: [
      { talla: '4', stock: 2, stockMinimo: 5 },
      { talla: '6', stock: 1, stockMinimo: 3 },
    ],
    ...cambios,
  };
}

/**
 * Construye una alerta de inventario de prueba.
 * @param cambios Propiedades que sustituyen los valores base.
 * @returns Alerta de inventario de prueba.
 */
export function crearAlerta(cambios: Partial<AlertaInventario> = {}): AlertaInventario {
  return {
    id: 1,
    prendaId: 7,
    nombre: 'Pantalón Jogger Azul',
    categoria: 'PANTALONES',
    imagenUrl: null,
    talla: '4',
    nivel: 'STOCK_BAJO',
    stockActual: 2,
    stockMinimo: 5,
    diferencia: -3,
    fechaEmision: '2026-10-01T10:00:00',
    ...cambios,
  };
}
