# Fase 2: despliegue manual en AWS (us-east-1)

```
littlestyle.duckdns.org ──HTTPS──> EC2 (sg-littlestyle-web) ── docker compose: web (Caddy) + backend
                                           │ rol IAM                   │ 3306
                                           ▼                           ▼
                                      S3 (imágenes)            RDS MySQL (sg de RDS: solo desde sg-littlestyle-web)
```

Todo se hace en la consola de AWS, en la región **us-east-1**, y en la **misma VPC de la RDS**.

## 1. Rol IAM para la EC2 (acceso a S3 sin llaves)

IAM → Roles → *Create role* → *AWS service* → **EC2**. No se asocia ninguna política administrada.
Nombre: `littlestyle-ec2-role`. Después, en el rol: *Add permissions* → *Create inline policy* → JSON
(reemplazar `NOMBRE-BUCKET`; el prefijo `catalogo` es el valor por defecto de `AWS_S3_PREFIJO`):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["s3:PutObject", "s3:DeleteObject"],
    "Resource": "arn:aws:s3:::NOMBRE-BUCKET/catalogo/*"
  }]
}
```

## 2. Security groups

| Security group | Regla de entrada | Origen |
|----------------|------------------|--------|
| `sg-littlestyle-web` (nuevo) | HTTP 80, HTTPS 443 | `0.0.0.0/0` |
| `sg-littlestyle-web` | SSH 22 | **My IP** |
| SG de la RDS (existente) | MySQL 3306 | `sg-littlestyle-web` (por ID de grupo, no por IP) |

La regla 3306 de tu IP en la RDS se puede mantener mientras sigas probando desde tu PC. Cuando el sistema
esté en producción conviene quitarla y dejar la RDS con *Public access = No*.

## 3. Lanzar la EC2

EC2 → *Launch instance*:

- AMI: **Amazon Linux 2023** (x86_64)
- Tipo: **t3.small** (2 GB). La t3.micro se queda corta para Spring Boot.
- Key pair: crear uno (`littlestyle-key.pem`) y guardarlo bien.
- Network: misma VPC de la RDS, subred pública, *Auto-assign public IP = Enable*, SG `sg-littlestyle-web`.
- Storage: 20 GB gp3.
- *Advanced details* → *IAM instance profile*: `littlestyle-ec2-role`.

## 4. Preparar la instancia

En duckdns.org copia el **token**. Desde tu PC:

```bash
scp -i littlestyle-key.pem deploy/ec2-setup.sh ec2-user@<IP-PUBLICA>:~
ssh -i littlestyle-key.pem ec2-user@<IP-PUBLICA>
bash ec2-setup.sh littlestyle <TOKEN-DUCKDNS>
exit   # volver a entrar para que aplique el grupo docker
```

El script instala Docker, Compose, Buildx y 2 GB de swap. También deja un cron que actualiza la IP en
DuckDNS en cada arranque, así no hace falta pagar una Elastic IP aunque apagues y prendas la instancia.

## 5. Desplegar

```bash
ssh -i littlestyle-key.pem ec2-user@littlestyle.duckdns.org
git clone https://github.com/sr-tav/LittleStyleRepository.git littlestyle   # si es privado, usar un token de GitHub
cd littlestyle
cp .env.example .env && nano .env
docker compose up -d --build
docker compose logs -f backend
```

Valores de `.env` en producción:

| Variable | Valor |
|----------|-------|
| `SITE_ADDRESS` | `littlestyle.duckdns.org` |
| `CORS_ALLOWED_ORIGINS` | `https://littlestyle.duckdns.org` |
| `DB_HOST` | endpoint de la RDS (`xxxx.us-east-1.rds.amazonaws.com`) |
| `DB_USER` / `DB_PASSWORD` | usuario maestro de la RDS (o uno propio de la app) |
| `JWT_SECRET` | `openssl rand -base64 64 \| tr -d '\r\n'` |
| `PERFIL_ENC_KEY` | ver la sección *Clave de cifrado* |
| `AWS_REGION` / `AWS_S3_BUCKET` / `AWS_S3_URL_PUBLICA` | `us-east-1`, nombre y URL pública del bucket |
| `WOMPI_REDIRECT_URL` | `https://littlestyle.duckdns.org/cliente/pago/resultado` |

No pongas `AWS_ACCESS_KEY_ID` ni `AWS_SECRET_ACCESS_KEY`: en la EC2 las credenciales vienen del rol IAM.

### Primer arranque contra una base vacía

`prod` usa `ddl-auto=validate` y no crea tablas. La primera vez se agrega temporalmente al `.env`:

```
SPRING_PROFILES_ACTIVE=prod,perfiles-migracion
```

Cuando arranque, se borra esa línea y se reinicia con `docker compose up -d`.

## 6. Verificación

| Prueba | Esperado |
|--------|----------|
| `https://littlestyle.duckdns.org` | Carga Angular con candado HTTPS válido |
| Registro y login | OK |
| Subir una imagen de prenda (vendedor) | Se guarda en S3 y se ve en el catálogo |
| Pago en sandbox de Wompi | El pedido cambia de estado (webhook a `/api/pagos/wompi/eventos`) |

En el panel de Wompi (sandbox) → URL de eventos: `https://littlestyle.duckdns.org/api/pagos/wompi/eventos`.

## Costos y apagado

- La EC2 detenida no cobra cómputo, solo el disco (~1.6 USD/mes por 20 GB).
- La RDS detenida se vuelve a encender sola a los 7 días.
- Al encender la EC2, `docker compose` arranca solo (`restart: unless-stopped`) y el cron actualiza DuckDNS.
