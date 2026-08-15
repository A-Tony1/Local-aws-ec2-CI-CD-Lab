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
                sh 'docker build -t azubuike1/devops-status-app:1.0.0 .'
            }
        }

        stage('Docker Push') {
            steps {
                echo 'Pushing Docker image to Docker Hub...'

                withCredentials([usernamePassword(
                    credentialsId: 'docker-hub-repo',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                        docker push azubuike1/devops-status-app:1.0.0
                        docker logout
                    '''
                }
            }
        }

        stage('Deploy to dev server') {
            steps {
                echo 'Deploying application to VM2 dev-server...'

                sshagent(['dev-server-ssh-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no dev-server@192.168.146.138 "
                            docker pull azubuike1/devops-status-app:1.0.0 &&
                            docker stop devops-status-app || true &&
                            docker rm devops-status-app || true &&
                            docker run -d \
                                --name devops-status-app \
                                -p 8081:8080 \
                                -e DEPLOYMENT_ENV=dev-server \
                                -e DEPLOYMENT_PLATFORM=docker \
                                azubuike1/devops-status-app:1.0.0
                        "
                    '''
                }
            }
        }
    }
}
```
