package co.edu.uniquindio.littlestyle.config;

import co.edu.uniquindio.littlestyle.config.security.JwtAuthenticationFilter;
import co.edu.uniquindio.littlestyle.config.security.JwtProperties;
import co.edu.uniquindio.littlestyle.config.security.JwtService;
import co.edu.uniquindio.littlestyle.config.security.RestAccessDeniedHandler;
import co.edu.uniquindio.littlestyle.config.security.RestAuthenticationEntryPoint;
import co.edu.uniquindio.littlestyle.modules.auth.service.EstadoCuentaService;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.controller.WompiWebhookController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad sin estado basada en JWT (ADR-04) usando la cadena de filtros de Spring Security (ADR-17).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private List<String> allowedOrigins;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    /**
     * Define la cadena de seguridad: sin sesión ni CSRF, rutas públicas (login, registro, webhook de Wompi,
     * imágenes locales y consola H2 si está activa), acceso por rol a {@code /api/admin}, {@code
     * /api/vendedor} y {@code /api/cliente}, y el filtro JWT antes del filtro de usuario y contraseña.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   EstadoCuentaService estadoCuentaService,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll();
                    auth.requestMatchers("/error").permitAll();
                    // Webhook de Wompi (US-11): sin JWT, se autentica verificando la firma SHA-256 del evento
                    auth.requestMatchers(HttpMethod.POST, WompiWebhookController.RUTA).permitAll();
                    // Imágenes del almacenamiento local (desarrollo); en producción se sirven desde S3
                    auth.requestMatchers(HttpMethod.GET, AlmacenamientoConfig.RUTA_MEDIA).permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers("/h2-console/**").permitAll();
                    }
                    auth.requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR");
                    auth.requestMatchers("/api/vendedor/**").hasRole("VENDEDOR");
                    auth.requestMatchers("/api/cliente/**").hasRole("CLIENTE");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, estadoCuentaService), UsernamePasswordAuthenticationFilter.class);

        if (h2ConsoleEnabled) {
            // La consola H2 se renderiza en frames
            http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        }
        return http.build();
    }

    /** Cifrador BCrypt para las contraseñas de los usuarios. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Gestor de autenticación que valida correo y contraseña contra los usuarios de la base de datos. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Permite al frontend de los orígenes configurados llamar a {@code /api/**} con los métodos y cabeceras
     * necesarios.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
