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
                sh 'docker compose down || true'
                sh 'docker compose up -d'
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