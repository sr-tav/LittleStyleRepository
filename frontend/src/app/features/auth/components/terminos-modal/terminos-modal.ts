import { AfterViewInit, Component, ElementRef, HostListener, output, viewChild } from '@angular/core';

/**
 * Versión del texto mostrado. Debe coincidir con AuthService.VERSION_TERMINOS_VIGENTE en el backend,
 * que es la versión que queda registrada como aceptada por el usuario.
 */
export const VERSION_TERMINOS = '1.1';

/** Términos y condiciones de uso y política de tratamiento de datos personales (Ley 1581 de 2012). */
@Component({
  selector: 'app-terminos-modal',
  template: `
    <div class="terms-backdrop" role="presentation" (click)="cerrar.emit()">
      <section
        #dialogo
        class="terms-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="terminos-titulo"
        tabindex="-1"
        (click)="$event.stopPropagation()"
      >
        <header class="terms-header">
          <h2 id="terminos-titulo">Términos y condiciones</h2>
          <small>Versión {{ version }} · Vigente desde octubre de 2026</small>
        </header>

        <div class="terms-body">
          <h3>1. Aceptación</h3>
          <p>
            Al crear una cuenta en LittleStyle declaras que eres mayor de edad, que la información que
            suministras es veraz y que aceptas estos términos y la política de tratamiento de datos
            personales descrita a continuación. Si no estás de acuerdo, no debes registrarte.
          </p>

          <h3>2. Objeto de la plataforma</h3>
          <p>
            LittleStyle es un marketplace de ropa infantil que conecta a padres, madres y acudientes
            (clientes) con tiendas y vendedores, y que ofrece recomendaciones de tallas a partir de los
            perfiles infantiles que registra el cliente.
          </p>

          <h3>3. Cuentas de usuario</h3>
          <ul>
            <li>La cuenta es personal e intransferible; eres responsable de la confidencialidad de tu contraseña.</li>
            <li>Los vendedores responden por la veracidad de la información, el precio, el etiquetado y la calidad de los productos que publican, conforme a la Ley 1480 de 2011 (Estatuto del Consumidor).</li>
            <li>LittleStyle puede suspender cuentas que incumplan estos términos, suministren información falsa o realicen actividades fraudulentas. La suspensión surte efecto de inmediato.</li>
          </ul>

          <h3>4. Uso adecuado</h3>
          <p>
            Te comprometes a no usar la plataforma para fines ilícitos, a no publicar contenido ofensivo
            o engañoso y a no intentar acceder a información de otros usuarios.
          </p>

          <h2 class="terms-subtitle">Política de tratamiento de datos personales</h2>

          <h3>5. Responsable del tratamiento</h3>
          <p>
            LittleStyle es responsable del tratamiento de los datos personales recolectados en la
            plataforma, en cumplimiento de la Ley 1581 de 2012 y el Decreto 1074 de 2015.
          </p>

          <h3>6. Datos que recolectamos</h3>
          <ul>
            <li><strong>Datos de la cuenta:</strong> nombre, apellido, correo electrónico, teléfono celular y, para vendedores, el nombre de la tienda.</li>
            <li><strong>Datos de perfiles infantiles</strong> (solo clientes): nombre o apodo, fecha de nacimiento, medidas corporales, historial de crecimiento, preferencias de estilo y alergias textiles.</li>
            <li><strong>Datos transaccionales:</strong> pedidos, direcciones de entrega e historial de compras.</li>
          </ul>

          <h3>7. Datos de niñas, niños y adolescentes</h3>
          <p>
            Los datos de los perfiles infantiles son suministrados por el padre, la madre o el acudiente,
            quien declara tener la representación legal del menor y autoriza su tratamiento. Se usan
            exclusivamente para recomendar tallas y prendas adecuadas, respetando el interés superior del
            menor y sus derechos fundamentales. Se almacenan cifrados y nunca se comparten con vendedores
            ni terceros de forma que permitan identificar al menor.
          </p>

          <h3>8. Finalidades</h3>
          <ul>
            <li>Crear y administrar tu cuenta y autenticarte en la plataforma.</li>
            <li>Recomendar tallas y productos según los perfiles infantiles registrados.</li>
            <li>Procesar pedidos, pagos, envíos y devoluciones.</li>
            <li>Atender peticiones, quejas y reclamos.</li>
            <li>Generar estadísticas agregadas y anónimas para mejorar el servicio.</li>
          </ul>

          <h3>9. Derechos del titular</h3>
          <p>Como titular de los datos puedes, en cualquier momento:</p>
          <ul>
            <li>Conocer, actualizar y rectificar tus datos personales.</li>
            <li>Solicitar prueba de la autorización otorgada.</li>
            <li>Ser informado sobre el uso que se ha dado a tus datos.</li>
            <li>Revocar la autorización y solicitar la supresión de tus datos personales, salvo que exista un deber legal o contractual de conservarlos.</li>
            <li>Presentar quejas ante la Superintendencia de Industria y Comercio.</li>
          </ul>

          <h3>10. Seguridad y conservación</h3>
          <p>
            Aplicamos medidas técnicas como el cifrado de los datos sensibles en la base de datos, el
            almacenamiento de contraseñas mediante funciones hash y la transmisión exclusiva por HTTPS.
            Si solicitas eliminar tu cuenta, eliminaremos los perfiles infantiles, las mediciones y las
            preferencias asociadas, y anonimizaremos los datos identificables de la cuenta. El correo
            electrónico original quedará disponible para un nuevo registro y la cuenta eliminada no podrá
            volver a iniciar sesión. La información que deba conservarse por obligaciones legales o
            contractuales se limitará y protegerá para esa finalidad. Los plazos y las categorías de
            conservación están pendientes de validación jurídica.
          </p>

          <h3>11. Cambios a estos términos</h3>
          <p>
            Cualquier cambio sustancial se publicará con una nueva versión y, cuando sea necesario, se
            solicitará nuevamente tu autorización.
          </p>
        </div>

        <footer class="terms-actions">
          <button type="button" class="btn-ghost" (click)="cerrar.emit()">Cerrar</button>
          <button type="button" class="btn-primary" (click)="aceptar.emit()">He leído y acepto</button>
        </footer>
      </section>
    </div>
  `,
})
export class TerminosModal implements AfterViewInit {
  readonly aceptar = output<void>();
  readonly cerrar = output<void>();

  protected readonly version = VERSION_TERMINOS;
  private readonly dialogo = viewChild.required<ElementRef<HTMLElement>>('dialogo');

  ngAfterViewInit(): void {
    this.dialogo().nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.cerrar.emit();
  }
}
