# Jenkins para el CI/CD de LittleStyle (US-13). Ejecuta las pruebas y los builds con el Docker
# del host (socket montado), así que no necesita Java 25 ni Node instalados.
FROM jenkins/jenkins:2.580.1-lts-jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
       > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*
USER jenkins

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Configuration as Code: usuario admin, seguridad y el job del pipeline
COPY casc/ /usr/share/jenkins/casc/
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/casc/jenkins.yaml
