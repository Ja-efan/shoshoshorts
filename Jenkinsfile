pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('BE') {
                    sh 'docker build -t sss-backend:latest .'
                }
            }
        }

        stage('Build Frontend (Static Files)') {
            steps {
                sh '''
                    echo "[INFO] Building frontend using builder profile..."
                    
                    # 빌더 컨테이너로 빌드 실행
                    docker compose --profile builder run --name fe-builder frontend sh -c "npm run build"

                    # FE/dist 디렉토리 생성 및 결과물 복사
                    mkdir -p FE/dist
                    docker cp fe-builder:/app/dist ./FE/dist

                    # 빌더 컨테이너 제거
                    docker rm fe-builder
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    echo "[INFO] Stopping and removing any existing containers..."
                    docker compose -f docker-compose.yml down || true

                    echo "[INFO] Starting new containers..."
                    docker compose --env-file .env -f docker-compose.yml up -d --build --force-recreate
                '''
            }
        }

        stage('Cleanup') {
            steps {
                sh 'docker image prune -f'
            }
        }
    }

    post {
        success {
            echo '✅ Deployment successful!'
        }
        failure {
            echo '❌ Deployment failed!'
        }
    }
}