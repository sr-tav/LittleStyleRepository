// CI/CD de LittleStyle (US-13). Corre en el Jenkins de la EC2 (deploy/jenkins) con el Docker del host:
// pruebas -> reportes JUnit y cobertura -> build de imágenes -> despliegue -> verificación, con rollback si falla.
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    triggers {
        githubPush()
    }

    environment {
        COMPOSE_PROJECT_NAME = 'littlestyle'
        DOCKER_BUILDKIT = '1'
    }

    stages {
        stage('Pruebas backend') {
            steps {
                sh 'rm -rf reportes && docker build --target reportes --output type=local,dest=reportes .'
            }
        }

        stage('Pruebas frontend') {
            steps {
                sh 'docker build --target reportes --output type=local,dest=reportes frontend'
            }
        }

        stage('Resultados') {
            steps {
                junit testResults: 'reportes/backend/*.xml, reportes/frontend/*.xml', allowEmptyResults: false
                recordCoverage id: 'cobertura', name: 'Cobertura', sourceCodeRetention: 'NEVER',
                    sourceDirectories: [[path: 'src/main/java'], [path: 'frontend']], tools: [
                    [parser: 'JACOCO', pattern: 'reportes/cobertura/backend/jacoco.xml'],
                    [parser: 'COBERTURA', pattern: 'reportes/cobertura/frontend/cobertura-coverage.xml']
                ]
                archiveArtifacts artifacts: 'reportes/cobertura/**', allowEmptyArchive: true
                script {
                    if (currentBuild.resultIsWorseOrEqualTo('UNSTABLE')) {
                        error('Hay pruebas fallidas: no se despliega')
                    }
                }
            }
        }

        stage('Construir imágenes') {
            steps {
                // Se conserva la versión en producción para poder volver a ella
                sh '''
                    docker image inspect littlestyle-backend:latest >/dev/null 2>&1 && docker tag littlestyle-backend:latest littlestyle-backend:anterior || true
                    docker image inspect littlestyle-web:latest >/dev/null 2>&1 && docker tag littlestyle-web:latest littlestyle-web:anterior || true
                    docker compose --env-file "$LITTLESTYLE_ENV_FILE" build
                '''
            }
        }

        stage('Desplegar') {
            steps {
                sh 'docker compose --env-file "$LITTLESTYLE_ENV_FILE" up -d backend web'
            }
        }

        stage('Verificar') {
            steps {
                // Sin token la API debe responder 401: el backend arrancó y la seguridad está activa
                sh '''
                    for i in $(seq 1 36); do
                        code=$(curl -s -o /dev/null -w "%{http_code}" http://backend:8080/api/cliente/perfiles || true)
                        if [ "$code" = "401" ]; then echo "Backend en línea (HTTP $code)"; exit 0; fi
                        sleep 5
                    done
                    echo "El backend no respondió en 3 minutos"
                    docker compose --env-file "$LITTLESTYLE_ENV_FILE" logs --tail 80 backend
                    exit 1
                '''
            }
            post {
                failure {
                    sh '''
                        if docker image inspect littlestyle-backend:anterior >/dev/null 2>&1; then
                            echo "Rollback a la versión anterior"
                            docker tag littlestyle-backend:anterior littlestyle-backend:latest
                            docker image inspect littlestyle-web:anterior >/dev/null 2>&1 && docker tag littlestyle-web:anterior littlestyle-web:latest || true
                            docker compose --env-file "$LITTLESTYLE_ENV_FILE" up -d backend web
                        fi
                    '''
                }
            }
        }
    }

    post {
        always {
            // El disco de la EC2 es pequeño: se limpian imágenes huérfanas y caché de build vieja
            sh 'docker image prune -f >/dev/null; docker builder prune -f --filter until=168h >/dev/null || true'
        }
    }
}
