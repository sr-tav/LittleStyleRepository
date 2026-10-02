# LittleStyle

Plataforma e-commerce de ropa infantil. Backend Spring Boot 4 (Java 25) + frontend Angular 21 (`frontend/`).

## Requisitos

- JDK 25 (`JAVA_HOME` debe apuntar al JDK 25, no a uno anterior)
- Node.js 24 y Angular CLI 21

## Ejecutar en local

**1. Backend** (perfil `local`: base H2 en `./data`, sin MySQL; crea un administrador inicial)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Queda en `http://localhost:8080`. Consola H2: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:file:./data/littlestyle-local`, usuario `sa`, sin contraseña).

Administrador inicial: `admin@littlestyle.com` / `Admin12345` (definido en `application-local.properties`).
Clientes y vendedores se crean desde la pantalla de registro.

Con el perfil `dev` (por defecto) se usa MySQL y se requieren `DB_HOST`, `DB_USER` y `DB_PASSWORD`.

**2. Frontend**

```bash
cd frontend
npm install
ng serve
```

Abrir `http://localhost:4200`. Las llamadas a `/api` se redirigen al backend mediante `proxy.conf.json`.

## Pruebas

```bash
./mvnw test              # backend: JUnit 5 + Mockito + MockMvc
cd frontend && ng test   # frontend: Vitest
```

## Módulo de autenticación (US05)

| Endpoint | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/register` | público | Registro de cliente o vendedor; devuelve JWT |
| `POST /api/auth/login` | público | Login; devuelve JWT |
| `GET /api/auth/me` | autenticado | Perfil del usuario del token |
| `/api/cliente/**`, `/api/vendedor/**`, `/api/admin/**` | por rol | Reservados para los módulos de cada rol |

- JWT HS512 sin estado (ADR-04): el filtro `JwtAuthenticationFilter` arma la autenticación desde los claims, sin consultar la base de datos.
- Variables: `JWT_SECRET` (Base64, obligatoria en `prod`), `JWT_EXPIRATION_MS` (por defecto 8 h), `CORS_ALLOWED_ORIGINS`.
- Frontend: `authInterceptor` adjunta el token y maneja 401/403; `roleGuard` (`data.roles`), `authGuard` y `guestGuard` protegen las rutas.
