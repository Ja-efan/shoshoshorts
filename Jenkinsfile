pipeline {
    agent any
    
    // environment {
    //     DB_PASSWORD = credentials('db-password')
    // }
    
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
        
        stage('Build Frontend') {
            steps {
                dir('FE') {
                    sh 'docker build -t sss-frontend:latest .'
                }
            }
        }
        
        stage('Deploy') {
            steps {
                sh '''
                    echo "[INFO] Copying .env..."
                    cp /Users/jaehwan/SSAFY/workspace/S12P21B106/.env docker-compose.env
                    echo "[INFO] Stopping and removing any existing containers..."
                    docker compose -f docker-compose.yml down || true
                    docker rm -f sss-mongo sss-postgres sss-backend sss-frontend || true

                    echo "[INFO] Starting new containers..."
                    docker compose --env-file docker-compose.env -f docker-compose.yml up -d
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
            echo 'Deployment successful!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
} 