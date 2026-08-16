# Architecture — Local AWS EC2 CI/CD Lab

## Overview

This project implements an end-to-end CI/CD workflow using two Ubuntu VMware virtual machines to simulate a cloud-based AWS EC2 environment.

The architecture separates the **CI/CD responsibilities** on VM1 from the **application deployment responsibilities** on VM2.

The workflow is:

```text
Developer
    |
    | Git Push
    v
GitHub Repository
    |
    | Checkout
    v
+--------------------------------+
| VM1 - CI/CD Server             |
|                                |
| Jenkins                        |
| Maven                          |
| Docker                         |
| SSH Client                     |
+---------------+----------------+
                |
                | Docker Image
                | Push
                v
+-------------------------------+
| Docker Hub                    |
|                               |
| azubuike1/devops-status-app   |
| tag: 1.0.0                    |
+---------------+---------------+
                |
                | SSH deployment
                v
+--------------------------------+
| VM2 - dev-server               |
|                                |
| Ubuntu                         |
| Docker                         |
+---------------+----------------+
                |
                | docker pull
                v
+--------------------------------+
| devops-status-app Container    |
|                                |
| Container Port: 8080           |
| Host Port: 8081                |
+---------------+----------------+
                |
                v
       Spring Boot Application
                |
                v
       /api/health
       /api/status
```

---

## VM1 — CI/CD Server

VM1 is the primary automation server in the local laboratory.

VM1 performs the continuous integration and continuous delivery activities.

### Responsibilities

VM1 is responsible for:

* Hosting Jenkins
* Accessing the GitHub repository
* Checking out source code
* Running Maven tests
* Packaging the Spring Boot application
* Building the Docker image
* Authenticating with Docker Hub
* Pushing the Docker image to Docker Hub
* Establishing an SSH connection to VM2
* Triggering the remote deployment

The project source code and Jenkinsfile are maintained in the Git repository on VM1.

---

## Jenkins

Jenkins runs on VM1 and controls the CI/CD workflow.

The Jenkins pipeline performs the following stages:

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

Jenkins is exposed on:

```text
http://localhost:8080
```

Jenkins uses configured credentials for:

* GitHub access
* Docker Hub authentication
* SSH access to VM2

This allows the pipeline to perform automated operations without requiring manual authentication during each build.

---

## GitHub

GitHub serves as the source-code repository.

The repository is:

```text
Local-aws-ec2-CI-CD-Lab
```

The GitHub repository contains:

```text
Dockerfile
Jenkinsfile
docker-compose.yml
pom.xml
README.md
src/
docs/
```

Jenkins retrieves the source code from the GitHub repository during the pipeline execution.

---

## Maven

Maven is used to build and test the Java Spring Boot application.

The Jenkins pipeline executes:

```bash
mvn test
```

to run automated tests.

The application is then packaged with:

```bash
mvn package -DskipTests
```

The resulting Spring Boot JAR is used as the input for the Docker image build.

---

## Docker Build

After Maven successfully packages the application, Jenkins builds the Docker image.

The image is tagged as:

```text
azubuike1/devops-status-app:1.0.0
```

The Dockerfile uses Java 17 through Amazon Corretto.

The resulting image contains the packaged Spring Boot application and its runtime environment.

---

## Docker Hub

Docker Hub acts as the container image registry.

The image produced by Jenkins is pushed to:

```text
azubuike1/devops-status-app:1.0.0
```

This creates a separation between:

```text
Build Environment
        |
        v
Container Registry
        |
        v
Deployment Environment
```

The application server does not need the source code or Maven build environment.

It only needs access to Docker and the published image.

---

## SSH Deployment

After successfully publishing the Docker image, Jenkins connects to VM2 using SSH.

The deployment connection targets:

```text
dev-server@192.168.146.138
```

SSH key authentication is used for automated server access.

The deployment process is performed remotely from Jenkins.

Conceptually:

```text
Jenkins VM1
     |
     | SSH
     v
VM2 dev-server
```

This demonstrates the same general pattern used when deploying applications to remote cloud servers.

---

## VM2 — Application Server

VM2 represents the remote application server.

Its hostname is:

```text
dev-server
```

VM2 is responsible for running the deployed Docker container.

Unlike VM1, VM2 does not perform the Maven build or Docker image creation.

Its primary responsibility is to run the application.

---

## Deployment on VM2

Jenkins performs the following operations on VM2:

```text
1. docker pull
       |
       v
2. Stop existing container
       |
       v
3. Remove existing container
       |
       v
4. docker run
```

The Docker image pulled from Docker Hub is:

```text
azubuike1/devops-status-app:1.0.0
```

The container is named:

```text
devops-status-app
```

---

## Port Mapping

The Spring Boot application listens on port:

```text
8080
```

The Docker container is exposed on VM2 using:

```text
8081:8080
```

This means:

```text
VM2 Host
  |
  | Port 8081
  v
Docker Container
  |
  | Port 8080
  v
Spring Boot Application
```

The application can therefore be tested on VM2 with:

```bash
curl http://localhost:8081/api/health
```

and:

```bash
curl http://localhost:8081/api/status
```

---

## Application Verification

The application exposes two important endpoints.

### Health Endpoint

```text
GET /api/health
```

Example response:

```json
{
  "status": "UP"
}
```

### Deployment Status Endpoint

```text
GET /api/status
```

Example response:

```json
{
  "environment": "dev-server",
  "status": "UP",
  "application": "devops-status-app",
  "deployment": "docker"
}
```

These endpoints provide a simple way to verify that the application has successfully started and is running in the expected deployment environment.

---

## Separation of Responsibilities

One of the key architectural principles demonstrated by this project is the separation between the CI/CD server and the application server.

### VM1

```text
Source
  ↓
Build
  ↓
Test
  ↓
Package
  ↓
Containerize
  ↓
Publish
  ↓
Deploy
```

### VM2

```text
Receive deployment
        ↓
Pull image
        ↓
Run container
        ↓
Serve application
```

This separation makes the architecture closer to a real-world deployment environment.

---

## AWS EC2 Simulation

The VMware environment provides a local simulation of an AWS-based architecture.

| Local Laboratory  | AWS Equivalent          |
| ----------------- | ----------------------- |
| VMware VM1        | EC2 CI/CD server        |
| VMware VM2        | EC2 application server  |
| VMware networking | AWS networking          |
| VM2 private IP    | EC2 private IP          |
| SSH to VM2        | SSH to EC2              |
| Docker Hub        | Container registry      |
| Jenkins           | CI/CD automation server |

The purpose is not to reproduce every AWS service, but to reproduce the **DevOps workflow and operational responsibilities** without requiring continuous AWS infrastructure costs.

---

## Architecture Benefits

This architecture provides several useful DevOps characteristics:

### Separation of Build and Runtime

The application is built on VM1 but executed on VM2.

### Immutable Application Artifact

The Docker image acts as the deployable artifact.

```text
Source Code
    ↓
Docker Image
    ↓
Deployment
```

### Versioned Deployment

The image is explicitly tagged:

```text
1.0.0
```

This allows the deployed version to be identified.

### Automated Deployment

Jenkins performs the deployment without requiring the engineer to manually log into VM2 and execute the deployment commands.

### Reproducibility

The same Docker image published to Docker Hub can be pulled and executed on another Docker-capable server.

---

## Current Baseline

The first complete working CI/CD implementation has been tagged in Git as:

```text
v1.0.0
```

Release description:

```text
First complete CI/CD deployment
```

This tag represents the known-good CI/CD baseline for the project.

Future improvements can be developed from this baseline without losing the working implementation.

---

## Future Architecture Improvements

The current architecture provides a strong foundation for additional DevOps capabilities.

Potential future improvements include:

```text
Automated Health Check
        ↓
Deployment Verification
        ↓
Rollback
        ↓
Monitoring
        ↓
Notifications
        ↓
Infrastructure as Code
        ↓
Cloud Deployment
        ↓
Kubernetes
```

The local VMware environment can therefore serve as the foundation for progressively introducing more advanced DevOps practices.
