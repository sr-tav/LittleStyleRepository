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

/** Composición: materiales sin repetir y suma exacta de 100 %. */
export const composicionValida: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const filas = (control as FormArray).getRawValue() as { material: MaterialTextil; porcentaje: number | null }[];
  const materiales = filas.map((f) => f.material);
  if (new Set(materiales).size !== materiales.length) return { materialRepetido: true };
  const suma = filas.reduce((total, f) => total + (Number(f.porcentaje) || 0), 0);
  return suma === 100 ? null : { suma };
};

/** Tabla de tallas sin nombres repetidos (sin distinguir mayúsculas ni espacios). */
export const tallasValidas: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const nombres = ((control as FormArray).getRawValue() as { talla: string }[])
    .map((t) => normalizarTalla(t.talla))
    .filter(Boolean);
  const repetida = nombres.find((n, i) => nombres.indexOf(n) !== i);
  return repetida ? { tallaRepetida: repetida } : null;
};

/** Mínimos que no superan a los máximos dentro de una fila de talla. */
const rangoValido: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const v = (grupo as FormGroup).getRawValue();
  const errores: ValidationErrors = {};
  if (v.estaturaMinCm != null && v.estaturaMaxCm != null && +v.estaturaMinCm > +v.estaturaMaxCm) {
    errores['rangoEstatura'] = true;
  }
  if (v.pesoMinKg != null && v.pesoMaxKg != null && +v.pesoMinKg > +v.pesoMaxKg) {
    errores['rangoPeso'] = true;
  }
  return Object.keys(errores).length ? errores : null;
};

export function normalizarTalla(valor: string | null | undefined): string {
  return (valor ?? '').trim().replace(/\s+/g, ' ').toUpperCase();
}

/** UI-20: Crear / editar producto (prenda), con tabla de tallas, composición y fotos. */
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

  protected readonly form = this.fb.group({
    nombre: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(120),
    ]),
    categoria: this.fb.nonNullable.control<CategoriaPrenda>('VESTIDOS', Validators.required),
    genero: this.fb.nonNullable.control<GeneroPrenda>('UNISEX', Validators.required),
    descripcion: this.fb.nonNullable.control('', Validators.maxLength(2000)),
    marca: this.fb.nonNullable.control('', Validators.maxLength(80)),
    precio: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(100),
      Validators.max(10_000_000),
    ]),
    stockMinimo: this.fb.control<number | null>(5, [
      Validators.required,
      Validators.min(0),
      Validators.max(10_000),
      ENTERO,
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

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isSafeInteger(id) && id > 0) {
      this.prendaId.set(id);
      this.cargar(id);
    }
  }

  ngOnDestroy(): void {
    this.pendientes().forEach((p) => URL.revokeObjectURL(p.vistaPrevia));
  }

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

  private aplicarPrenda(prenda: Prenda): void {
    this.form.patchValue({
      nombre: prenda.nombre,
      categoria: prenda.categoria,
      genero: prenda.genero ?? 'UNISEX',
      descripcion: prenda.descripcion ?? '',
      marca: prenda.marca ?? '',
      precio: prenda.precio,
      stockMinimo: prenda.stockMinimo,
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

  /** Las tallas ya guardadas muestran su stock sin editarlo: se gestiona en el inventario. */
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
        stock: this.fb.control<number | null>(existente?.stock ?? 0, [
          Validators.required,
          Validators.min(0),
          Validators.max(100_000),
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

  protected agregarComponente(): void {
    if (this.composicion.length >= this.limites.maxMateriales) return;
    const usados = new Set(this.composicion.getRawValue().map((f) => f['material']));
    const libre = this.materiales.find((m) => !usados.has(m.valor))?.valor ?? 'OTRO';
    this.composicion.push(this.componente(libre, Math.max(0, 100 - this.sumaComposicion()) || null));
  }

  protected quitarComponente(indice: number): void {
    if (this.composicion.length > 1) this.composicion.removeAt(indice);
  }

  protected agregarTalla(): void {
    if (this.tallas.length < this.limites.maxTallas) this.tallas.push(this.talla());
  }

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

  protected alternarColor(color: ColorPreferido): void {
    this.coloresSeleccionados.update((actuales) =>
      actuales.includes(color)
        ? actuales.filter((c) => c !== color)
        : actuales.length >= 10
          ? actuales
          : [...actuales, color],
    );
  }

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

  protected quitarPendiente(indice: number): void {
    const foto = this.pendientes()[indice];
    URL.revokeObjectURL(foto.vistaPrevia);
    this.pendientes.update((actuales) => actuales.filter((_, i) => i !== indice));
  }

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

  protected invalido(control: AbstractControl | null): boolean {
    return !!control && control.invalid && (control.touched || this.enviado());
  }

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

  /** Reintenta solo la carga de fotos cuando la prenda ya quedó guardada. */
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

  /** Mensaje de subida según el fallo: tamaño, servicio caído u otro error. */
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

  private finalizar(eraNueva: boolean): void {
    this.guardando.set(false);
    this.router.navigate(['/vendedor/productos'], {
      state: { mensaje: eraNueva ? 'Producto creado correctamente.' : 'Cambios guardados.' },
    });
  }

  private construirRequest(): PrendaRequest {
    const v = this.form.getRawValue();
    const texto = (valor: string) => (valor.trim() ? valor.trim() : null);
    return {
      nombre: v.nombre.trim(),
      categoria: v.categoria,
      genero: v.genero,
      descripcion: texto(v.descripcion),
      marca: texto(v.marca),
      precio: Number(v.precio),
      stockMinimo: Number(v.stockMinimo),
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
        stock: t['existente'] ? null : Number(t['stock']),
      })),
    };
  }
}
