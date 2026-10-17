# Fase 3: CI/CD con Jenkins

```
push / merge a main ──webhook──> https://littlestyle.duckdns.org/jenkins/github-webhook/
                                          │ (Caddy)
                                          ▼
EC2 c7i-flex.large ── Jenkins (contenedor) ── usa el Docker del host ──> contenedores backend + web
```

Jenkins corre en la misma EC2, en Docker. Cada push a `main` ejecuta el `Jenkinsfile`:

| Etapa | Qué hace |
|-------|----------|
| Pruebas backend | `docker build --target reportes .`: `mvnw test` con H2 y exporta los JUnit |
| Pruebas frontend | `docker build --target reportes frontend`: `ng test` (Vitest) y exporta el JUnit |
| Resultados | Publica los reportes en Jenkins. **Si hay una prueba fallida, se detiene y no despliega** |
| Construir imágenes | Guarda la versión actual como `:anterior` y construye las nuevas |
| Desplegar | `docker compose up -d backend web` con el `.env` de `~/littlestyle` |
| Verificar | Espera a que la API responda 401 sin token. **Si no responde en 3 minutos, vuelve a `:anterior` (rollback)** |

Jenkins se configura solo con *Configuration as Code* (`deploy/jenkins/casc/jenkins.yaml`): crea el
usuario admin y el job `littlestyle`, sin el asistente de instalación.

Se probó en local: 524 pruebas (344 backend + 180 frontend) en verde, despliegue y verificación correctos.

## Archivos

| Archivo | Propósito |
|---------|-----------|
| `Jenkinsfile` | Pipeline |
| `deploy/jenkins/Dockerfile` | Jenkins LTS + Docker CLI, Compose y Buildx + plugins |
| `deploy/jenkins/plugins.txt` | Plugins: pipeline, git, github, junit, JCasC, job-dsl… |
| `deploy/jenkins/casc/jenkins.yaml` | Usuario admin, seguridad, credencial de GitHub y job |
| `deploy/jenkins/docker-compose.yml` | Contenedor Jenkins en la red de la app, con el socket de Docker |
| `deploy/jenkins/.env.example` | Variables de Jenkins (se copia a `deploy/jenkins/.env`, no se versiona) |

## Instalación en la EC2 (una sola vez)

### 1. Subir la instancia a c7i-flex.large (4 GB) y el disco a 30 GB

Jenkins, los builds y la app no caben en 2 GB, y las imágenes y la caché de build ocupan disco.

1. EC2 → la instancia → **Instance state → Stop**. Espera a que diga *Stopped*.
2. **Actions → Instance settings → Change instance type** → `c7i-flex.large` (en el plan gratuito `t3.medium` no aparece) → **Apply**.
3. En la pestaña **Storage**, haz clic en el volumen → **Actions → Modify volume** → Size `30` → **Modify**.
4. **Instance state → Start**.
5. Por SSH, confirma que el disco creció (Amazon Linux lo expande al arrancar):
   ```bash
   df -h /          # debe mostrar ~30G
   ```
   Si sigue en 20G: `sudo growpart /dev/nvme0n1 1 && sudo xfs_growfs -d /`

### 2. Traer el código nuevo a `~/littlestyle`

Jenkins despliega la rama `main`, así que antes hay que mergear el PR de US-13 a `main`.

```bash
cd ~/littlestyle
git fetch && git checkout main && git pull
docker compose up -d --build        # aplica el Caddyfile nuevo (ruta /jenkins)
```

### 3. Levantar Jenkins

```bash
cd ~/littlestyle/deploy/jenkins
cp .env.example .env
sed -i "s|^JENKINS_ADMIN_PASSWORD=.*|JENKINS_ADMIN_PASSWORD=$(openssl rand -base64 18)|" .env
sed -i "s|^DOCKER_GID=.*|DOCKER_GID=$(getent group docker | cut -d: -f3)|" .env
grep JENKINS_ADMIN_PASSWORD .env     # anota la contraseña
docker compose up -d --build         # la primera vez tarda ~3 min
```

Si el repositorio es **privado**, llena también `GITHUB_USER` y `GITHUB_TOKEN` en ese `.env`. El token
se crea en GitHub → Settings → Developer settings → Fine-grained token, solo para este repo y con
*Contents: Read-only*.

Abre **https://littlestyle.duckdns.org/jenkins** y entra con `admin` y la contraseña.

### 4. Webhook en GitHub

Lo hace alguien con permisos de administrador del repositorio:
GitHub → repo → **Settings → Webhooks → Add webhook**

| Campo | Valor |
|-------|-------|
| Payload URL | `https://littlestyle.duckdns.org/jenkins/github-webhook/` (con la `/` final) |
| Content type | `application/json` |
| Which events | *Just the push event* |

Al guardarlo, GitHub envía un *ping*. En **Recent Deliveries** debe aparecer con ✓ verde.

### 5. Primer build

En Jenkins → `littlestyle` → **Build Now**. Desde ahí, cada push o merge a `main` lo dispara solo.
En **Stage View** se ve el avance por etapa y en **Test Result**, las pruebas.

## Uso diario

- El equipo trabaja en sus ramas y hace PR a `main`. Al mergear, Jenkins prueba y despliega en ~10 minutos.
- **No ejecutes `docker compose up --build` a mano en `~/littlestyle`.** Esa carpeta solo guarda el `.env`
  y, si tiene código viejo, pisaría lo que desplegó Jenkins.
- Si un cambio agrega columnas o tablas, el pipeline falla en *Verificar* (`Schema validation`) y hace
  rollback. Hay que aplicar la migración una vez, a mano, en `~/littlestyle`:
  ```bash
  printf '\nSPRING_PROFILES_ACTIVE=prod,perfiles-migracion\n' >> .env
  # Build Now y esperar el verde. Luego quitar la línea sin cambiar el archivo de lugar
  # (Jenkins lo monta como archivo; con `sed -i` seguiría viendo la versión vieja):
  grep -v '^SPRING_PROFILES_ACTIVE=' .env > /tmp/env.tmp && cat /tmp/env.tmp > .env && rm /tmp/env.tmp
  # Build Now otra vez
  ```
  Si la migración trae datos que rellenar (por ejemplo, `docs/us-12/migracion-despachos.sql`), esa parte
  se ejecuta después en MySQL.
- Variables nuevas: se agregan al `.env` de `~/littlestyle` antes de mergear.

## Probar Jenkins en local (opcional)

Con el stack local arriba (`docker compose -f docker-compose.yml -f docker-compose.local.yml up -d`):

```bash
cd deploy/jenkins && cp .env.example .env
# JENKINS_URL=http://localhost/jenkins/   DOCKER_GID=0
# LITTLESTYLE_COMPOSE_FILE=docker-compose.yml:docker-compose.local.yml
docker compose up -d --build
```

Jenkins queda en http://localhost/jenkins. Despliega en el stack local con MySQL en contenedor.
