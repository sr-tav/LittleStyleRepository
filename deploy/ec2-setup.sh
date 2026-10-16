#!/bin/bash
# Prepara una EC2 Amazon Linux 2023 para LittleStyle (US-13): swap, Docker, Compose, Buildx
# y actualización automática de la IP en DuckDNS.
# Uso: bash ec2-setup.sh <subdominio-duckdns> <token-duckdns>
#   bash ec2-setup.sh littlestyle 1234abcd-....
set -euo pipefail

DOMINIO="${1:?Falta el subdominio de DuckDNS (sin .duckdns.org)}"
TOKEN="${2:?Falta el token de DuckDNS}"

# 2 GB de swap: el build de Maven + Angular no cabe solo en la RAM de una t3.small
if ! swapon --show | grep -q /swapfile; then
  sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 status=none
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile >/dev/null
  sudo swapon /swapfile
  echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

sudo dnf install -y docker git cronie
sudo systemctl enable --now docker crond
sudo usermod -aG docker ec2-user

# Amazon Linux no trae los plugins de Compose ni Buildx
PLUGINS=/usr/local/lib/docker/cli-plugins
sudo mkdir -p "$PLUGINS"
sudo curl -fsSL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o "$PLUGINS/docker-compose"
sudo curl -fsSL https://github.com/docker/buildx/releases/download/v0.25.0/buildx-v0.25.0.linux-amd64 \
  -o "$PLUGINS/docker-buildx"
sudo chmod +x "$PLUGINS/docker-compose" "$PLUGINS/docker-buildx"

# DuckDNS: la IP pública cambia en cada encendido de la instancia; se actualiza al arrancar y cada 5 min
DUCK="curl -fsS 'https://www.duckdns.org/update?domains=${DOMINIO}&token=${TOKEN}&ip=' >/dev/null"
( crontab -l 2>/dev/null | grep -v duckdns.org || true
  echo "@reboot sleep 20 && ${DUCK}"
  echo "*/5 * * * * ${DUCK}" ) | crontab -
eval "$DUCK"

echo "Listo. Cierra la sesión SSH y vuelve a entrar para usar docker sin sudo."
