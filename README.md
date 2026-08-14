# Local AWS EC2 CI/CD Lab

A production-style DevOps CI/CD laboratory that simulates an AWS EC2 deployment environment using two Ubuntu VMware virtual machines.

This project demonstrates how a DevOps Engineer can design and implement a CI/CD pipeline without requiring an AWS account, while maintaining the same core workflow used in a cloud-based environment.

## Project Objective

The objective is to build, test, containerize, publish, and deploy a Java Spring Boot application through an automated CI/CD pipeline.

The lab uses two virtual machines to simulate a real development and deployment environment:

- VM1 — CI/Build Server
- VM2 — Development/Application Server

VM1 runs Jenkins and performs the CI/CD automation.

VM2, named `dev-server`, represents the remote application server that would normally be an AWS EC2 instance.

## Architecture

```text
                    GitHub
                      |
                      v
              +---------------+
              |    Jenkins    |
              |      VM1      |
              +---------------+
                      |
             1. Checkout Code
                      |
             2. Run Maven Tests
                      |
             3. Build JAR
                      |
             4. Build Docker Image
                      |
             5. Push Image
                to Docker Hub
                      |
             6. SSH to VM2
                      |
                      v
              +---------------+
              |  dev-server   |
              |      VM2      |
              +---------------+
                      |
                Pull Docker Image
                      |
                Docker Compose
                      |
                      v
              Spring Boot App
                      |
                  Port 8081

| Technology         | Purpose                              |
| ------------------ | ------------------------------------ |
| Git                | Version control                      |
| GitHub             | Source code repository               |
| Jenkins            | CI/CD automation                     |
| Maven              | Java build and dependency management |
| Java 17            | Application runtime                  |
| Spring Boot        | Application framework                |
| Docker             | Application containerization         |
| Docker Compose     | Container deployment                 |
| Docker Hub         | Container image registry             |
| SSH                | Remote deployment                    |
| Ubuntu             | Server operating system              |
| VMware Workstation | Local AWS/EC2 simulation             |


Application

The project contains a Spring Boot application called:

devops-status-app

The application exposes health and deployment status endpoints.

Health Check
GET /api/health

Example response:
{
  "status": "UP"
}


Deployment Status
GET /api/status

Example response:
{
  "environment": "dev-server",
  "deployment": "docker",
  "application": "devops-status-app",
  "status": "UP"
}

The status endpoint demonstrates that deployment-specific configuration can be supplied through environment variables rather than being hardcoded into the application.

Local Environment
VM1 — Jenkins / CI Server

VM1 is responsible for:

Jenkins
CI/CD pipeline execution
Maven build
Automated testing
Docker image creation
Docker Hub publishing
SSH-based deployment

Jenkins is exposed on:

http://localhost:8080

VM2 — Development Server

VM2 is named:

dev-server

It simulates an AWS EC2 application server.

The application is deployed using Docker Compose and exposed through:

http://localhost:8081
Current Deployment Workflow

The application can currently be built with Maven:

mvn clean test package

A Docker image can then be created:

docker build -t devops-status-app:1.0.0 .

The application can be deployed using Docker Compose:

export IMAGE=devops-status-app:1.0.0
docker compose up -d

Verify the deployment:

docker compose ps

Test the application:

curl http://localhost:8081/api/health
curl http://localhost:8081/api/status
Docker

The application is packaged into a Docker image using a Java 17 runtime.

The Dockerfile:

Uses Amazon Corretto 17
Copies the packaged Spring Boot JAR
Exposes the application port
Starts the application using Java


Docker Compose
Docker Compose separates deployment configuration from the application image.

The image is supplied through the IMAGE environment variable:

image: ${IMAGE}

This allows different image versions to be deployed without modifying the Compose file.

For example:

export IMAGE=devops-status-app:1.0.0
docker compose up -d

This approach prepares the deployment configuration for CI/CD automation.


CI/CD Pipeline — Planned
The final automated pipeline will implement:

Git Push
   |
   v
Jenkins
   |
   v
Checkout
   |
   v
Maven Test
   |
   v
Maven Package
   |
   v
Docker Build
   |
   v
Docker Hub
   |
   v
SSH to dev-server
   |
   v
Docker Compose Deployment
   |
   v
Health Check


AWS EC2 Simulation
This laboratory intentionally replaces AWS EC2 instances with VMware Ubuntu virtual machines.
The mapping is:
AWS Environment	Local Lab
EC2 CI/Build Server	VM1
EC2 Development Server	VM2
AWS Security Groups	VMware/network configuration
EC2 Public/Private IP	VM IP address
SSH to EC2	SSH to dev-server
ECR/Docker Registry	Docker Hub

The purpose is to provide a realistic hands-on environment without requiring continuous AWS infrastructure costs.



DevOps Practices Demonstrated

This project demonstrates:
Infrastructure/environment simulation
Git-based development
CI/CD automation
Automated testing
Maven builds
Docker image creation
Containerized application deployment
Docker Compose
Environment-based configuration
Docker registry usage
SSH remote deployment
Health checks
Deployment verification
Separation of build and deployment environments

Project Status
Completed
 Spring Boot application
 Maven build
 Automated test
 Dockerfile
 Docker image
 Docker Compose deployment
 Environment-based image configuration
 VM1 / VM2 lab environment
 Git repository

In Progress
 Jenkins CI pipeline
 Docker Hub image publishing
 SSH automated deployment
 VM2 automated deployment
 Deployment health verification
 Pipeline documentation
 Architecture diagrams
 Deployment screenshots



Author

Anthony Abia

Junior DevOps / Cloud Engineer

GitHub: A-Tony1

This project is continuously evolving as part of my hands-on DevOps engineering portfolio.
