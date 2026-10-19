import { Location } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Navbar } from '../../../shared/components/navbar/navbar';
import { procesarErrorApi } from '../../auth/auth.validators';
import { OPCIONES_COLORES, OPCIONES_ESTAMPADOS } from '../../perfilesInfantiles/models/perfil-opciones';
import { ColorPreferido, Estampado } from '../../perfilesInfantiles/models/perfil-infantil.models';
import { LIMITES_PRENDA, OPCIONES_CATEGORIAS, OPCIONES_GENERO, OPCIONES_MATERIALES } from '../models/prenda-opciones';
import {
  CategoriaPrenda,
  GeneroPrenda,
  ImagenPrenda,
  MaterialTextil,
  Prenda,
  PrendaRequest,
  Talla,
} from '../models/prenda.models';
import { PrendasService } from '../services/prendas.service';

interface FotoPendiente {
  archivo: File;
  vistaPrevia: string;
}

const ENTERO = Validators.pattern(/^\d+$/);

/**
 * Valida que los materiales de la composición no se repitan y sumen exactamente 100 %.
 * @param control FormArray de componentes textiles.
 * @returns Error `materialRepetido` o `suma` cuando la composición no es válida; si no, `null`.
 */
export const composicionValida: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const filas = (control as FormArray).getRawValue() as { material: MaterialTextil; porcentaje: number | null }[];
  const materiales = filas.map((f) => f.material);
  if (new Set(materiales).size !== materiales.length) return { materialRepetido: true };
  const suma = filas.reduce((total, f) => total + (Number(f.porcentaje) || 0), 0);
  return suma === 100 ? null : { suma };
};

/**
 * Valida que la tabla no contenga nombres de talla repetidos tras normalizarlos.
 * @param control FormArray de tallas del producto.
 * @returns Error `tallaRepetida` si hay un duplicado; si no, `null`.
 */
export const tallasValidas: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const nombres = ((control as FormArray).getRawValue() as { talla: string }[])
    .map((t) => normalizarTalla(t.talla))
    .filter(Boolean);
  const repetida = nombres.find((n, i) => nombres.indexOf(n) !== i);
  return repetida ? { tallaRepetida: repetida } : null;
};

/**
 * Comprueba que los mínimos no superen a los máximos en una fila de talla.
 * @param grupo Grupo con rangos de estatura, peso y edad.
 * @returns Errores de rango detectados o `null` si los intervalos son válidos.
 */
const rangoValido: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const v = (grupo as FormGroup).getRawValue();
  const errores: ValidationErrors = {};
  if (v.estaturaMinCm != null && v.estaturaMaxCm != null && +v.estaturaMinCm > +v.estaturaMaxCm) {
    errores['rangoEstatura'] = true;
  }
  if (v.pesoMinKg != null && v.pesoMaxKg != null && +v.pesoMinKg > +v.pesoMaxKg) {
    errores['rangoPeso'] = true;
  }
  if (v.edadMinMeses != null && v.edadMaxMeses != null && +v.edadMinMeses > +v.edadMaxMeses) {
    errores['rangoEdad'] = true;
  }
  return Object.keys(errores).length ? errores : null;
};

/**
 * Normaliza una talla para mostrarla o compararla sin diferencias de espacios y mayúsculas.
 * @param valor Texto original de la talla.
 * @returns Talla recortada, con espacios internos compactados y en mayúsculas.
 */
export function normalizarTalla(valor: string | null | undefined): string {
  return (valor ?? '').trim().replace(/\s+/g, ' ').toUpperCase();
}

/**
 * UI-20: Crear / editar producto (prenda), con tabla de tallas, composición y fotos.
 * @see UI-20
 */
@Component({
  selector: 'app-prenda-form',
  imports: [Navbar, RouterLink, ReactiveFormsModule],
  templateUrl: './prenda-form.html',
})
export class PrendaForm implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly prendasService = inject(PrendasService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  protected readonly categorias = OPCIONES_CATEGORIAS;
  protected readonly generos = OPCIONES_GENERO;
  protected readonly materiales = OPCIONES_MATERIALES;
  protected readonly colores = OPCIONES_COLORES;
  protected readonly estampados = OPCIONES_ESTAMPADOS;
  protected readonly limites = LIMITES_PRENDA;

  protected readonly prendaId = signal<number | null>(null);
  protected readonly esNueva = computed(() => this.prendaId() === null);
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly enviado = signal(false);
  protected readonly errorGeneral = signal<string | null>(null);
  protected readonly errorImagenes = signal<string | null>(null);
  protected readonly coloresSeleccionados = signal<ColorPreferido[]>([]);
  protected readonly imagenes = signal<ImagenPrenda[]>([]);
  protected readonly pendientes = signal<FotoPendiente[]>([]);
  protected readonly confirmandoImagen = signal<number | null>(null);
  protected readonly procesandoImagen = signal(false);
  protected readonly totalFotos = computed(() => this.imagenes().length + this.pendientes().length);

  /**
   * Formulario con límites de nombre, cuidado y precio; la composición debe sumar 100 %
   * sin materiales repetidos y las tallas deben tener rangos coherentes.
   * @see UI-20
   */
  protected readonly form = this.fb.group({
    nombre: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(120),
    ]),
    categoria: this.fb.nonNullable.control<CategoriaPrenda>('VESTIDOS', Validators.required),
    genero: this.fb.nonNullable.control<GeneroPrenda>('UNISEX', Validators.required),
    descripcion: this.fb.nonNullable.control('', Validators.maxLength(2000)),
    // Etiquetado obligatorio (Resolución Andina 2109): lavado, secado, planchado...
    instruccionesCuidado: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.maxLength(1000),
      Validators.pattern(/\S/),
    ]),
    marca: this.fb.nonNullable.control('', Validators.maxLength(80)),
    precio: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(100),
      Validators.max(10_000_000),
    ]),
    estampado: this.fb.nonNullable.control<Estampado | ''>(''),
    tintesSinteticos: this.fb.nonNullable.control(false),
    brochesMetalicos: this.fb.nonNullable.control(false),
    contieneNiquel: this.fb.nonNullable.control(false),
    tratamientoFormaldehido: this.fb.nonNullable.control(false),
    composicion: this.fb.array([this.componente('ALGODON', 100)], composicionValida),
    tallas: this.fb.array([this.talla()], tallasValidas),
  });

  protected get composicion(): FormArray<FormGroup> {
    return this.form.controls.composicion as FormArray<FormGroup>;
  }

  protected get tallas(): FormArray<FormGroup> {
    return this.form.controls.tallas as FormArray<FormGroup>;
  }

  protected sumaComposicion(): number {
    return this.composicion.getRawValue().reduce((t, f) => t + (Number(f['porcentaje']) || 0), 0);
  }

  /**
   * Detecta el identificador de la ruta y carga la prenda si se está editando.
   * @returns No devuelve un valor.
   */
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isSafeInteger(id) && id > 0) {
      this.prendaId.set(id);
      this.cargar(id);
    }
  }

  /**
   * Libera las URL temporales creadas para las vistas previas de fotos pendientes.
   * @returns No devuelve un valor.
   */
  ngOnDestroy(): void {
    this.pendientes().forEach((p) => URL.revokeObjectURL(p.vistaPrevia));
  }

  /**
   * Consulta la prenda que se va a editar y carga sus datos en el formulario.
   * @param id Identificador de la prenda.
   * @returns No devuelve un valor.
   */
  private cargar(id: number): void {
    this.cargando.set(true);
    this.prendasService.obtener(id).subscribe({
      next: (prenda) => {
        this.aplicarPrenda(prenda);
        this.cargando.set(false);
      },
      error: (err) => {
        this.errorGeneral.set(procesarErrorApi(err));
        this.cargando.set(false);
      },
    });
  }

  /**
   * Copia los atributos, composición, tallas, colores e imágenes al estado de edición.
   * @param prenda Datos de producto recibidos.
   * @returns No devuelve un valor.
   */
  private aplicarPrenda(prenda: Prenda): void {
    this.form.patchValue({
      nombre: prenda.nombre,
      categoria: prenda.categoria,
      genero: prenda.genero ?? 'UNISEX',
      descripcion: prenda.descripcion ?? '',
      instruccionesCuidado: prenda.instruccionesCuidado ?? '',
      marca: prenda.marca ?? '',
      precio: prenda.precio,
      estampado: prenda.estampado ?? '',
      tintesSinteticos: prenda.tintesSinteticos,
      brochesMetalicos: prenda.brochesMetalicos,
      contieneNiquel: prenda.contieneNiquel,
      tratamientoFormaldehido: prenda.tratamientoFormaldehido,
    });
    this.composicion.clear();
    prenda.composicion.forEach((c) => this.composicion.push(this.componente(c.material, c.porcentaje)));
    this.tallas.clear();
    prenda.tallas.forEach((t) => this.tallas.push(this.talla(t)));
    this.coloresSeleccionados.set([...prenda.colores]);
    this.imagenes.set(prenda.imagenes);
  }

  /**
   * Crea el grupo de formulario de un componente textil.
   * @param material Material seleccionado.
   * @param porcentaje Proporción porcentual del material.
   * @returns Grupo con validaciones para material y porcentaje.
   */
  private componente(material: MaterialTextil, porcentaje: number | null): FormGroup {
    return this.fb.group({
      material: this.fb.nonNullable.control<MaterialTextil>(material, Validators.required),
      porcentaje: this.fb.control<number | null>(porcentaje, [
        Validators.required,
        Validators.min(1),
        Validators.max(100),
        ENTERO,
      ]),
    });
  }

  /**
   * Las tallas ya guardadas muestran su stock sin editarlo: se gestiona en el inventario.
   * @param existente Datos de la talla guardada; si se omite, crea una nueva.
   * @returns Grupo de formulario con rangos y validaciones de stock.
   */
  private talla(existente?: Talla): FormGroup {
    const medida = (min: number, max: number) => [Validators.required, Validators.min(min), Validators.max(max)];
    const grupo = this.fb.group(
      {
        existente: this.fb.nonNullable.control(!!existente),
        talla: this.fb.nonNullable.control(existente?.talla ?? '', [
          Validators.required,
          Validators.maxLength(10),
          Validators.pattern(/^[\p{L}0-9 ./-]+$/u),
        ]),
        estaturaMinCm: this.fb.control<number | null>(existente?.estaturaMinCm ?? null, medida(40, 200)),
        estaturaMaxCm: this.fb.control<number | null>(existente?.estaturaMaxCm ?? null, medida(40, 200)),
        pesoMinKg: this.fb.control<number | null>(existente?.pesoMinKg ?? null, medida(2, 120)),
        pesoMaxKg: this.fb.control<number | null>(existente?.pesoMaxKg ?? null, medida(2, 120)),
        edadMinMeses: this.fb.control<number | null>(existente?.edadMinMeses ?? null, [
          Validators.min(0),
          Validators.max(216),
          ENTERO,
        ]),
        edadMaxMeses: this.fb.control<number | null>(existente?.edadMaxMeses ?? null, [
          Validators.min(0),
          Validators.max(216),
          ENTERO,
        ]),
        stock: this.fb.control<number | null>(existente?.stock ?? 0, [
          Validators.required,
          Validators.min(0),
          Validators.max(100_000),
          ENTERO,
        ]),
        stockMinimo: this.fb.control<number | null>(existente?.stockMinimo ?? null, [
          Validators.min(0),
          Validators.max(10_000),
          ENTERO,
        ]),
      },
      { validators: rangoValido },
    );
    if (existente) {
      grupo.controls.talla.disable();
      grupo.controls.stock.disable();
    }
    return grupo;
  }

  /**
   * Agrega un material libre sin superar el máximo configurado.
   * @returns No devuelve un valor.
   */
  protected agregarComponente(): void {
    if (this.composicion.length >= this.limites.maxMateriales) return;
    const usados = new Set(this.composicion.getRawValue().map((f) => f['material']));
    const libre = this.materiales.find((m) => !usados.has(m.valor))?.valor ?? 'OTRO';
    this.composicion.push(this.componente(libre, Math.max(0, 100 - this.sumaComposicion()) || null));
  }

  /**
   * Quita un material solo cuando queda al menos otro componente en la composición.
   * @param indice Índice del material que se quitará.
   * @returns No devuelve un valor.
   */
  protected quitarComponente(indice: number): void {
    if (this.composicion.length > 1) this.composicion.removeAt(indice);
  }

  /**
   * Agrega una fila de talla si no se ha alcanzado el máximo permitido.
   * @returns No devuelve un valor.
   */
  protected agregarTalla(): void {
    if (this.tallas.length < this.limites.maxTallas) this.tallas.push(this.talla());
  }

  /**
   * Quita una talla; una talla ya guardada con stock requiere dejar primero su stock en cero.
   * @param indice Índice de la talla que se quitará.
   * @returns No devuelve un valor.
   */
  protected quitarTalla(indice: number): void {
    const fila = this.tallas.at(indice);
    if (this.tallas.length <= 1) return;
    if (fila.getRawValue()['existente'] && Number(fila.getRawValue()['stock']) > 0) {
      this.errorGeneral.set(
        `La talla ${fila.getRawValue()['talla']} tiene unidades; deja su stock en 0 desde el inventario antes de quitarla.`,
      );
      return;
    }
    this.tallas.removeAt(indice);
  }

  /**
   * Alterna un color y limita la selección a diez valores.
   * @param color Color que se activa o desactiva.
   * @returns No devuelve un valor.
   */
  protected alternarColor(color: ColorPreferido): void {
    this.coloresSeleccionados.update((actuales) =>
      actuales.includes(color)
        ? actuales.filter((c) => c !== color)
        : actuales.length >= 10
          ? actuales
          : [...actuales, color],
    );
  }

  /**
   * Valida y prepara fotos JPG, PNG o WEBP según el tamaño y los máximos configurados.
   * @param evento Evento de selección del control de archivos.
   * @returns No devuelve un valor.
   */
  protected seleccionarFotos(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivos = Array.from(entrada.files ?? []);
    entrada.value = '';
    this.errorImagenes.set(null);
    if (archivos.length > this.limites.maxImagenesPorCarga) {
      this.errorImagenes.set(
        `Selecciona máximo ${this.limites.maxImagenesPorCarga} fotos a la vez (elegiste ${archivos.length}).`,
      );
      return;
    }
    const nuevas: FotoPendiente[] = [];
    for (const archivo of archivos) {
      if (!this.limites.tiposImagen.includes(archivo.type)) {
        this.errorImagenes.set(`${archivo.name} no es una imagen JPG, PNG o WEBP.`);
        continue;
      }
      if (archivo.size > this.limites.maxTamanoImagenMb * 1024 * 1024) {
        this.errorImagenes.set(`${archivo.name} supera ${this.limites.maxTamanoImagenMb} MB.`);
        continue;
      }
      if (this.totalFotos() + nuevas.length >= this.limites.maxImagenesPorPrenda) {
        this.errorImagenes.set(`Una prenda admite máximo ${this.limites.maxImagenesPorPrenda} fotos.`);
        break;
      }
      nuevas.push({ archivo, vistaPrevia: URL.createObjectURL(archivo) });
    }
    this.pendientes.update((actuales) => [...actuales, ...nuevas]);
  }

  /**
   * Descarta una foto pendiente y libera su URL de vista previa.
   * @param indice Índice de la foto pendiente.
   * @returns No devuelve un valor.
   */
  protected quitarPendiente(indice: number): void {
    const foto = this.pendientes()[indice];
    URL.revokeObjectURL(foto.vistaPrevia);
    this.pendientes.update((actuales) => actuales.filter((_, i) => i !== indice));
  }

  /**
   * Solicita establecer como principal una imagen ya guardada.
   * @param imagen Imagen que se marcará como principal.
   * @returns No devuelve un valor.
   */
  protected marcarPrincipal(imagen: ImagenPrenda): void {
    const id = this.prendaId();
    if (id === null || this.procesandoImagen()) return;
    this.procesandoImagen.set(true);
    this.prendasService.marcarPrincipal(id, imagen.id).subscribe({
      next: (imagenes) => {
        this.imagenes.set(imagenes);
        this.procesandoImagen.set(false);
      },
      error: (err) => {
        this.errorImagenes.set(procesarErrorApi(err));
        this.procesandoImagen.set(false);
      },
    });
  }

  /**
   * Elimina una imagen guardada y actualiza la lista local al completarse.
   * @param imagen Imagen que se eliminará.
   * @returns No devuelve un valor.
   */
  protected eliminarImagen(imagen: ImagenPrenda): void {
    const id = this.prendaId();
    if (id === null || this.procesandoImagen()) return;
    this.procesandoImagen.set(true);
    this.prendasService.eliminarImagen(id, imagen.id).subscribe({
      next: () => {
        this.imagenes.update((actuales) => actuales.filter((i) => i.id !== imagen.id));
        this.confirmandoImagen.set(null);
        this.procesandoImagen.set(false);
      },
      error: (err) => {
        this.errorImagenes.set(procesarErrorApi(err));
        this.procesandoImagen.set(false);
      },
    });
  }

  /**
   * Determina si el control debe presentarse como inválido tras interacción o envío.
   * @param control Control que se evalúa.
   * @returns `true` si es inválido y fue tocado o ya se intentó guardar.
   */
  protected invalido(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || this.enviado());
  }

  /**
   * Valida y guarda la prenda; después carga las fotos pendientes si existen.
   * @returns No devuelve un valor.
   * @see UI-20
   */
  protected guardar(): void {
    this.enviado.set(true);
    this.errorGeneral.set(null);
    if (this.guardando()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorGeneral.set('Revisa los campos marcados antes de guardar.');
      return;
    }
    this.guardando.set(true);
    const id = this.prendaId();
    const request = this.construirRequest();
    const operacion = id === null ? this.prendasService.crear(request) : this.prendasService.actualizar(id, request);
    operacion.subscribe({
      next: (prenda) => {
        const eraNueva = id === null;
        if (this.pendientes().length) {
          this.subirPendientes(prenda, eraNueva);
        } else {
          this.finalizar(eraNueva);
        }
      },
      error: (err) => {
        this.guardando.set(false);
        this.errorGeneral.set(procesarErrorApi(err, this.form));
      },
    });
  }

  /**
   * Reintenta solo la carga de fotos cuando la prenda ya quedó guardada.
   * @returns No devuelve un valor.
   */
  protected reintentarFotos(): void {
    const id = this.prendaId();
    if (id === null || this.guardando() || !this.pendientes().length) return;
    this.guardando.set(true);
    this.prendasService.obtener(id).subscribe({
      next: (prenda) => this.subirPendientes(prenda, false),
      error: (err) => {
        this.guardando.set(false);
        this.errorImagenes.set(procesarErrorApi(err));
      },
    });
  }

  /**
   * Carga las fotos pendientes y conserva el producto en modo edición si falla la carga.
   * @param prenda Producto al que se asociarán las fotos.
   * @param eraNueva Indica si el producto se creó en la operación actual.
   * @returns No devuelve un valor.
   */
  private subirPendientes(prenda: Prenda, eraNueva: boolean): void {
    const archivos = this.pendientes().map((p) => p.archivo);
    this.prendasService.subirImagenes(prenda.id, archivos).subscribe({
      next: () => {
        this.pendientes().forEach((p) => URL.revokeObjectURL(p.vistaPrevia));
        this.pendientes.set([]);
        this.finalizar(eraNueva);
      },
      error: (err) => {
        // La prenda ya existe: se pasa a modo edición sin perder las fotos pendientes
        this.guardando.set(false);
        if (eraNueva) {
          this.prendaId.set(prenda.id);
          this.aplicarPrenda(prenda);
          this.location.replaceState(`/vendedor/productos/${prenda.id}`);
        }
        this.errorImagenes.set(this.mensajeErrorSubida(err));
      },
    });
  }

  /**
   * Mensaje de subida según el fallo: tamaño, servicio caído u otro error.
   * @param err Error recibido durante la carga.
   * @returns Mensaje que explica el fallo al usuario.
   */
  private mensajeErrorSubida(err: unknown): string {
    const base = 'La prenda se guardó, pero las fotos no se pudieron subir';
    const estado = (err as { status?: number } | null)?.status;
    if (estado === 413) {
      return `${base}: las fotos superan el tamaño permitido. Reduce el tamaño e inténtalo de nuevo.`;
    }
    if (estado === 502) {
      return `${base}: el servicio de imágenes no responde. Reintenta en unos minutos.`;
    }
    return `${base}: ${procesarErrorApi(err)}`;
  }

  /**
   * Restablece el estado de guardado y vuelve al listado con un mensaje de resultado.
   * @param eraNueva Indica si se creó un producto o se actualizaron sus datos.
   * @returns No devuelve un valor.
   */
  private finalizar(eraNueva: boolean): void {
    this.guardando.set(false);
    this.router.navigate(['/vendedor/productos'], {
      state: { mensaje: eraNueva ? 'Producto creado correctamente.' : 'Cambios guardados.' },
    });
  }

  /**
   * Convierte los valores del formulario a la estructura enviada al servicio de prendas.
   * @returns Solicitud con valores normalizados y números convertidos.
   */
  private construirRequest(): PrendaRequest {
    const v = this.form.getRawValue();
    const texto = (valor: string) => (valor.trim() ? valor.trim() : null);
    return {
      nombre: v.nombre.trim(),
      categoria: v.categoria,
      genero: v.genero,
      descripcion: texto(v.descripcion),
      instruccionesCuidado: v.instruccionesCuidado.trim(),
      marca: texto(v.marca),
      precio: Number(v.precio),
      colores: this.coloresSeleccionados(),
      estampado: v.estampado || null,
      tintesSinteticos: v.tintesSinteticos,
      brochesMetalicos: v.brochesMetalicos,
      contieneNiquel: v.contieneNiquel,
      tratamientoFormaldehido: v.tratamientoFormaldehido,
      composicion: this.composicion.getRawValue().map((c) => ({
        material: c['material'],
        porcentaje: Number(c['porcentaje']),
      })),
      tallas: this.tallas.getRawValue().map((t) => ({
        talla: normalizarTalla(t['talla']),
        estaturaMinCm: Number(t['estaturaMinCm']),
        estaturaMaxCm: Number(t['estaturaMaxCm']),
        pesoMinKg: Number(t['pesoMinKg']),
        pesoMaxKg: Number(t['pesoMaxKg']),
        edadMinMeses: t['edadMinMeses'] === null || t['edadMinMeses'] === '' ? null : Number(t['edadMinMeses']),
        edadMaxMeses: t['edadMaxMeses'] === null || t['edadMaxMeses'] === '' ? null : Number(t['edadMaxMeses']),
        stock: t['existente'] ? null : Number(t['stock']),
        stockMinimo: t['stockMinimo'] === null || t['stockMinimo'] === '' ? null : Number(t['stockMinimo']),
      })),
    };
  }
}
