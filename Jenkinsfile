pipeline {
    agent any

    stages {

        // 1. 소스 체크아웃
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* 2. 시크릿(.env.production 등) 주입 */
        stage('Prepare secrets') {
            steps {
                withCredentials([
                    file(credentialsId: 'fe-prod-env', variable: 'FE_ENV'),
                    file(credentialsId: 'be-prod-env', variable: 'BE_ENV')
                ]) {
                    sh '''
                      # 권한 문제 해결을 위한 디렉토리 권한 설정
                      mkdir -p FE
                      mkdir -p BE
                      chmod -R 777 FE
                      chmod -R 777 BE
                      chmod 777 .
                      
                      # FRONTEND
                      cp $FE_ENV FE/.env.production
                      # BACKEND
                      cp $BE_ENV .env
                    '''
                }
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