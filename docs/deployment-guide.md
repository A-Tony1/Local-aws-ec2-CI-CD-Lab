# Deployment Guide — Local AWS EC2 CI/CD Lab

## 1. Introduction

This document describes the implementation and deployment process for the Local AWS EC2 CI/CD Lab.

The project uses two Ubuntu VMware virtual machines to simulate a cloud-based CI/CD environment.

The deployment architecture consists of:

```text
GitHub
   |
   v
Jenkins - VM1
   |
   +--> Maven Test
   |
   +--> Maven Package
   |
   +--> Docker Build
   |
   +--> Docker Push
   |
   v
Docker Hub
   |
   | SSH
   v
VM2 - dev-server
   |
   +--> Docker Pull
   |
   +--> Stop Previous Container
   |
   +--> Start New Container
   |
   v
Spring Boot Application
```

---

# 2. Environment

## VM1 — CI/CD Server

VM1 is the Jenkins automation server.

Primary responsibilities:

* GitHub source-code checkout
* Maven testing
* Maven packaging
* Docker image creation
* Docker Hub publishing
* SSH-based deployment to VM2

Jenkins is exposed on:

```text
http://localhost:8080
```

---

## VM2 — Application Server

VM2 represents the remote development/application server.

Hostname:

```text
dev-server
```

IP address:

```text
192.168.146.138
```

Primary responsibilities:

* Receive deployment commands from Jenkins
* Pull Docker images
* Run the application container
* Expose the application

---

# 3. Source Code Management

The application source code is stored in GitHub.

Repository:

```text
Local-aws-ec2-CI-CD-Lab
```

The local repository is maintained on VM1.

Check repository status:

```bash
git status
```

The repository uses the `master` branch.

The working CI/CD implementation was tagged:

```text
v1.0.0
```

Tag description:

```text
First complete CI/CD deployment
```

The tag provides a known-good baseline that can be used for recovery or comparison.

---

# 4. Application

The application is a Java Spring Boot application named:

```text
devops-status-app
```

The project uses Maven for dependency management, testing, and packaging.

The application exposes:

```text
/api/health
/api/status
```

The Spring Boot application listens internally on port:

```text
8080
```

---

# 5. Maven Build

The application can be tested and packaged with Maven.

Run the tests:

```bash
mvn test
```

A successful test run produces output similar to:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The application can then be packaged:

```bash
mvn package -DskipTests
```

The resulting JAR is:

```text
target/devops-status-app-1.0.0.jar
```

This JAR becomes the application artifact used by the Docker build.

---

# 6. Docker Image

The application is containerized using the project's Dockerfile.

The image is tagged:

```text
azubuike1/devops-status-app:1.0.0
```

A local Docker build can be performed with:

```bash
docker build -t azubuike1/devops-status-app:1.0.0 .
```

The Docker image contains:

* Java 17 runtime
* Spring Boot application
* Packaged application JAR

The container starts the application using:

```text
java -jar app.jar
```

---

# 7. Docker Hub

Docker Hub is used as the container image registry.

The repository is:

```text
azubuike1/devops-status-app
```

The deployment image is:

```text
azubuike1/devops-status-app:1.0.0
```

The CI pipeline authenticates to Docker Hub using Jenkins credentials.

The password/token is not stored directly in the Jenkinsfile.

Jenkins injects the credentials during pipeline execution.

The pipeline performs:

```bash
docker login
docker push
docker logout
```

This allows the Docker image to be securely published without exposing the credential in the pipeline source code.

---

# 8. Jenkins Configuration

Jenkins runs inside Docker on VM1.

The Jenkins container has access to the Docker socket so that Jenkins can execute Docker commands against the Docker engine.

The Jenkins environment was verified with:

```bash
docker exec jenkins docker --version
```

SSH support was also verified inside the Jenkins container:

```bash
docker exec jenkins ssh -V
```

Jenkins uses the following configured tools and credentials:

```text
Maven:
Maven-3.9

GitHub credential:
jenkins-github

Docker Hub credential:
docker-hub-repo

SSH credential:
dev-server-ssh-key
```

---

# 9. SSH Configuration

Jenkins deploys to VM2 using SSH.

The target server is:

```text
dev-server@192.168.146.138
```

SSH key-based authentication was configured between the CI/CD environment and VM2.

The VM1 SSH configuration allows the server to be referenced using:

```text
dev-server
```

The connection was verified from VM1 using:

```bash
ssh -o StrictHostKeyChecking=no dev-server "hostname"
```

The expected hostname is:

```text
dev-server-VMware-Virtual-Platform
```

Docker access on VM2 was also verified remotely:

```bash
ssh -o StrictHostKeyChecking=no dev-server "docker --version"
```

---

# 10. Jenkins Pipeline

The Jenkinsfile defines the CI/CD workflow.

The pipeline stages are:

```text
Checkout
   |
   v
Test
   |
   v
Package
   |
   v
Docker Build
   |
   v
Docker Push
   |
   v
Deploy to dev-server
```

---

## Stage 1 — Checkout

Jenkins retrieves the source code from GitHub.

```groovy
stage('Checkout') {
    steps {
        echo 'Checking out source code...'
        checkout scm
    }
}
```

---

## Stage 2 — Test

Maven tests are executed:

```groovy
stage('Test') {
    steps {
        echo 'Running Maven tests...'
        sh 'mvn test'
    }
}
```

The pipeline should stop if the tests fail.

This prevents a failed application from progressing through the deployment pipeline.

---

## Stage 3 — Package

The Spring Boot application is packaged:

```groovy
stage('Package') {
    steps {
        echo 'Packaging application...'
        sh 'mvn package -DskipTests'
    }
}
```

This produces the deployable JAR.

---

## Stage 4 — Docker Build

Jenkins builds the Docker image:

```groovy
stage('Docker Build') {
    steps {
        echo 'Building Docker image...'
        sh 'docker build -t azubuike1/devops-status-app:1.0.0 .'
    }
}
```

The image is now available to the Docker engine used by Jenkins.

---

## Stage 5 — Docker Push

The image is published to Docker Hub:

```text
azubuike1/devops-status-app:1.0.0
```

Jenkins uses a credential stored in its credential manager rather than exposing the Docker Hub password in the Jenkinsfile.

The general workflow is:

```text
Docker Login
     |
     v
Docker Push
     |
     v
Docker Logout
```

---

# 11. Deployment to VM2

After the Docker image is successfully pushed, Jenkins connects to VM2 using SSH.

The deployment process performs the following operations:

```text
Jenkins
   |
   | SSH
   v
VM2
   |
   | docker pull
   v
Docker Hub Image
   |
   v
Stop Existing Container
   |
   v
Remove Existing Container
   |
   v
Start New Container
```

The image pulled by VM2 is:

```text
azubuike1/devops-status-app:1.0.0
```

The container is named:

```text
devops-status-app
```

---

# 12. Container Configuration

The application container is started with:

```text
--name devops-status-app
```

The port mapping is:

```text
8081:8080
```

This means:

```text
VM2 port 8081
      |
      v
Container port 8080
      |
      v
Spring Boot application
```

Deployment-specific environment variables are supplied to the container:

```text
DEPLOYMENT_ENV=dev-server
DEPLOYMENT_PLATFORM=docker
```

This allows the application to identify its deployment environment without hardcoding the values into the application.

---

# 13. Deployment Verification

After deployment, VM2 can be checked using:

```bash
docker ps
```

The expected container is:

```text
devops-status-app
```

The expected port mapping is:

```text
0.0.0.0:8081->8080/tcp
```

---

# 14. Health Check

The application health endpoint can be tested on VM2:

```bash
curl http://localhost:8081/api/health
```

Expected response:

```json
{
  "status": "UP"
}
```

---

# 15. Deployment Status Verification

The deployment status endpoint can be tested with:

```bash
curl http://localhost:8081/api/status
```

Expected response:

```json
{
  "environment": "dev-server",
  "status": "UP",
  "application": "devops-status-app",
  "deployment": "docker"
}
```

This confirms that:

* The application is running
* The application is running inside Docker
* The deployment environment is `dev-server`
* The container is responding to requests

---

# 16. Complete Deployment Flow

The complete automated process is:

```text
Developer
    |
    | git push
    v
GitHub
    |
    | Jenkins checkout
    v
Jenkins - VM1
    |
    +---- Maven Test
    |
    +---- Maven Package
    |
    +---- Docker Build
    |
    +---- Docker Push
    |
    v
Docker Hub
    |
    | SSH deployment
    v
VM2 - dev-server
    |
    +---- Docker Pull
    |
    +---- Stop old container
    |
    +---- Remove old container
    |
    +---- Start new container
    |
    v
Spring Boot Application
    |
    +---- /api/health
    |
    +---- /api/status
```

---

# 17. Manual Recovery

If the application container has stopped because VM2 was restarted or the host machine was shut down, the container can be started again with:

```bash
docker start devops-status-app
```

Verify:

```bash
docker ps
```

Then test:

```bash
curl http://localhost:8081/api/status
```

The deployment can also be recreated by running the Jenkins pipeline again.

---

# 18. CI/CD Baseline

The first complete working implementation was frozen as:

```text
v1.0.0
```

This version represents the first successful implementation of:

```text
GitHub
   ↓
Jenkins
   ↓
Maven Test
   ↓
Maven Package
   ↓
Docker Build
   ↓
Docker Hub
   ↓
SSH
   ↓
VM2
   ↓
Docker Container
   ↓
Spring Boot Application
```

Future pipeline improvements should be developed as new versions rather than modifying the known-good baseline without version control.

---

# 19. Future Improvements

The next planned improvements include:

* Automated deployment health checks
* Deployment verification from Jenkins
* Automatic rollback
* Improved image versioning
* Build metadata
* Jenkins notifications
* Better Docker layer caching
* Security improvements
* Infrastructure as Code
* Cloud deployment
* Kubernetes deployment

These improvements will allow the laboratory to evolve from a basic CI/CD implementation into a more production-oriented DevOps platform.

---

## Conclusion

This deployment demonstrates a complete CI/CD workflow using locally hosted infrastructure.

The project shows how source code can move automatically through:

```text
Source Control
      ↓
Continuous Integration
      ↓
Application Build
      ↓
Containerization
      ↓
Container Registry
      ↓
Remote Deployment
      ↓
Application Verification
```

The VMware environment provides an affordable AWS EC2-style laboratory while preserving the key DevOps concepts of separation of environments, automation, containerization, remote deployment, and repeatable delivery.
