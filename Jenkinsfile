pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Test') {
            steps {
                echo 'Running Maven tests...'
                sh 'mvn test'
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging application...'
                sh 'mvn package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                echo 'Building Docker image...'
                sh 'docker build -t devops-status-app:1.0.0 .'
            }
        }
    }
}
