# Lessons Learned — Local AWS EC2 CI/CD Lab

## 1. Introduction

This project provided practical experience designing, implementing, troubleshooting, and completing an end-to-end CI/CD pipeline.

Rather than relying entirely on a cloud provider, VMware Workstation was used to simulate an AWS EC2 environment with two Ubuntu virtual machines.

The project demonstrated that the core principles of a cloud-based DevOps workflow can be practiced locally while still working with real tools and real deployment patterns.

---

# 2. Understanding CI/CD as a Complete Workflow

One of the most important lessons was understanding that CI/CD is not simply a Jenkins job.

The completed workflow connects several independent technologies:

```text
GitHub
   |
   v
Jenkins
   |
   v
Maven
   |
   v
Docker
   |
   v
Docker Hub
   |
   v
SSH
   |
   v
VM2
   |
   v
Docker Container
   |
   v
Spring Boot Application
```

Each stage has a specific responsibility.

This helped reinforce the importance of understanding how individual DevOps tools work together rather than learning each tool in isolation.

---

# 3. Separating Build and Deployment Responsibilities

The two-VM architecture provided an important lesson in environment separation.

## VM1

VM1 acts as the CI/CD server.

It hosts Jenkins and performs:

* Source code checkout
* Maven testing
* Application packaging
* Docker image creation
* Docker Hub publishing
* Remote deployment commands

## VM2

VM2 acts as the application server.

It receives the Docker image and runs the application.

This separation resembles the distinction between a CI/build environment and a remote application environment in a cloud infrastructure.

---

# 4. Jenkins Is an Execution Environment

A major lesson was that Jenkins should be treated as its own environment.

Jenkins was running inside a Docker container on VM1.

Therefore, having Maven or Docker installed on the Ubuntu host did not automatically mean that Jenkins could use those tools.

This became particularly clear when the pipeline initially failed with:

```text
mvn: not found
```

The solution was to configure Maven as a Jenkins-managed tool.

This reinforced the principle:

> Always verify tools from the environment in which the pipeline actually executes.

---

# 5. Jenkins Pipeline Configuration

The project provided practical experience writing a Declarative Jenkins Pipeline.

The pipeline evolved from a simple test pipeline into a complete CI/CD workflow.

The major stages became:

```text
Checkout
   ↓
Test
   ↓
Package
   ↓
Docker Build
   ↓
Docker Push
   ↓
Deploy to dev-server
```

This demonstrated how a pipeline can progressively automate the complete software delivery process.

---

# 6. Git as the Source of Truth

Git and GitHub became the source of truth for the application and Jenkins pipeline.

The Jenkins job retrieves the Jenkinsfile directly from the Git repository.

This means that pipeline changes can be version-controlled alongside the application.

The project also introduced Git tags.

The first complete working CI/CD implementation was frozen as:

```text
v1.0.0
```

This provides a stable baseline that can be referenced when future changes introduce problems.

---

# 7. Importance of Version Control

One important lesson was the value of preserving known-good versions.

During development, the Jenkinsfile changed several times as new functionality was added.

Rather than treating every change as permanent, the working implementation was preserved with:

```bash
git tag -a v1.0.0 -m "First complete CI/CD deployment"
```

and pushed to GitHub.

This establishes a reference point for future development.

If a later modification breaks the pipeline, the known-good baseline can be inspected or restored.

---

# 8. Maven Build Lifecycle

The project provided hands-on experience with the Maven lifecycle.

The pipeline separates testing from packaging:

```bash
mvn test
```

followed by:

```bash
mvn package -DskipTests
```

This demonstrates an important CI/CD principle.

Testing should occur before the application is packaged into the deployable artifact.

The successful test stage produced:

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The package stage then generated:

```text
target/devops-status-app-1.0.0.jar
```

---

# 9. Docker Image Creation

The project demonstrated how a Java application can be transformed into a portable container image.

The Dockerfile uses Java 17 and packages the Spring Boot JAR into the container.

The resulting image is:

```text
azubuike1/devops-status-app:1.0.0
```

This separates the application from the underlying server environment.

Instead of installing the Java application directly onto VM2, the server only needs Docker to run the packaged application.

---

# 10. Docker Registry as an Artifact Distribution Mechanism

Docker Hub became the bridge between the CI environment and the deployment environment.

The workflow is:

```text
Jenkins VM1
     |
     | Docker Push
     v
Docker Hub
     |
     | Docker Pull
     v
VM2
```

This is an important architectural concept.

The application server does not need access to the Jenkins workspace or Maven build artifacts.

Instead, it receives the versioned container image from the registry.

---

# 11. Secure Credential Management

The project provided practical experience using Jenkins Credentials Manager.

Docker Hub credentials were stored under:

```text
docker-hub-repo
```

SSH credentials were stored under:

```text
dev-server-ssh-key
```

The credentials were not placed directly into the Jenkinsfile.

This reinforced the importance of separating:

```text
Application configuration
        from
Secrets and credentials
```

Credentials should be injected into pipelines only when required.

---

# 12. SSH Remote Deployment

The project demonstrated how Jenkins can remotely deploy an application using SSH.

The deployment flow is:

```text
Jenkins VM1
     |
     | SSH
     v
VM2 dev-server
     |
     | Docker commands
     v
Application Container
```

The pipeline uses the Jenkins SSH agent to provide the required key.

This is conceptually similar to deploying to a remote EC2 instance using SSH.

---

# 13. Environment-Based Configuration

The application uses environment variables for deployment-specific information.

For example:

```text
DEPLOYMENT_ENV=dev-server
DEPLOYMENT_PLATFORM=docker
```

The application can therefore report information about its deployment environment without hardcoding that information into the application source code.

This is an important DevOps principle:

> Configuration should be separated from application code.

---

# 14. Docker Port Mapping

The application runs inside the container on port:

```text
8080
```

VM2 exposes the application through:

```text
8081
```

The mapping is:

```text
8081:8080
```

Therefore:

```text
Browser / Client
      |
      v
VM2:8081
      |
      v
Container:8080
      |
      v
Spring Boot
```

This also prevented a port conflict with Jenkins, which uses port `8080` on VM1.

---

# 15. Deployment Verification

Another important lesson was that a successful pipeline does not necessarily mean a successful application deployment.

The deployment needs to be verified.

The application was tested using:

```bash
curl http://localhost:8081/api/health
```

and:

```bash
curl http://localhost:8081/api/status
```

The status endpoint confirmed:

```json
{
  "environment": "dev-server",
  "status": "UP",
  "application": "devops-status-app",
  "deployment": "docker"
}
```

This demonstrated the importance of validating the application after deployment.

---

# 16. Troubleshooting Is Part of DevOps

The project showed that building a CI/CD pipeline involves much more than writing configuration files.

Several real issues were encountered, including:

* Maven unavailable to Jenkins
* Jenkins Docker access
* SSH authentication
* Docker Hub authentication
* Docker Compose environment variables
* Jenkinsfile shell escaping
* Existing Docker containers
* VM shutdown and container recovery
* Port mapping
* Remote deployment verification

These problems provided practical experience diagnosing issues across multiple layers.

---

# 17. Understanding Logs

Jenkins console output became one of the most important troubleshooting tools.

For example, a pipeline failure such as:

```text
mvn: not found
```

immediately identified the missing tool.

Similarly, successful output such as:

```text
BUILD SUCCESS
```

and:

```text
Finished: SUCCESS
```

provided evidence that a stage had completed successfully.

The project reinforced the habit of reading logs carefully rather than changing configuration based on assumptions.

---

# 18. Local Cloud Simulation

Using VMware Workstation instead of AWS provided an important lesson.

A cloud environment is not defined only by the provider.

Many cloud engineering concepts can be reproduced locally:

```text
AWS EC2
   ↓
Ubuntu VM

AWS Security / Network Configuration
   ↓
VMware Networking

EC2 Remote Access
   ↓
SSH

Container Registry
   ↓
Docker Hub
```

This makes it possible to practice realistic DevOps workflows without continuously paying for cloud infrastructure.

---

# 19. Documentation as an Engineering Practice

The project also demonstrated that documentation is part of a professional DevOps implementation.

The repository now contains dedicated documentation covering areas such as:

```text
docs/
├── architecture.md
├── deployment-guide.md
├── troubleshooting.md
└── lessons-learned.md
```

This makes the repository easier for another engineer or potential employer to understand and reproduce.

---

# 20. The Difference Between "It Works" and "It Is Automatable"

Initially, parts of the application could be deployed manually.

The project progressed toward automation.

The final CI/CD workflow allows Jenkins to:

```text
1. Retrieve source code
2. Run tests
3. Package the application
4. Build the Docker image
5. Push the image to Docker Hub
6. Connect to VM2
7. Pull the new image
8. Replace the previous container
9. Start the application
10. Verify the deployment
```

This is a major transition from manually operating infrastructure to automating software delivery.

---

# 21. Portfolio Value

The project demonstrates practical exposure to:

* Linux administration
* Git and GitHub
* Jenkins
* CI/CD
* Maven
* Java
* Spring Boot
* Docker
* Docker Compose
* Docker Hub
* SSH
* VMware networking
* Remote deployments
* Environment configuration
* Troubleshooting
* Deployment verification
* Technical documentation

More importantly, it demonstrates the ability to connect these technologies into a working delivery system.

---

# 22. Next Development Opportunities

The current implementation provides a strong baseline for further DevOps improvements.

Potential future enhancements include:

### Automated deployment health checks

Jenkins could automatically call:

```text
/api/health
```

after deployment and fail the pipeline if the application is unhealthy.

### Version-driven Docker tags

Instead of always using:

```text
1.0.0
```

the pipeline could automatically generate image versions.

### Rollback capability

A failed deployment could automatically return VM2 to the previous known-good image.

### Docker Compose deployment

The deployment stage could use the existing Compose configuration rather than directly using `docker run`.

### Branch-based CI/CD

Different Git branches could trigger different pipeline behaviors.

### Security improvements

The project could eventually introduce:

* Secret management
* Container image scanning
* Dependency scanning
* SSH hardening
* Least-privilege access

### Infrastructure as Code

VMware infrastructure concepts could eventually be extended into Terraform-managed infrastructure where practical.

### Cloud migration

The local environment can later be mapped to actual AWS services such as:

```text
VM1 → CI/CD infrastructure
VM2 → EC2
Docker Hub → ECR
VMware networking → AWS networking
```

---

# 23. Final Takeaway

The most important lesson from this project is that DevOps is about connecting development, infrastructure, automation, security, and operations into a repeatable system.

The completed laboratory demonstrates that workflow locally:

```text
Developer
    |
    v
GitHub
    |
    v
Jenkins
    |
    +---- Test
    |
    +---- Package
    |
    +---- Build Image
    |
    +---- Push Image
    |
    v
Docker Hub
    |
    v
SSH
    |
    v
VM2
    |
    v
Docker
    |
    v
Spring Boot Application
    |
    v
Health Check
```

The project therefore represents more than a collection of Docker and Jenkins commands.

It is a practical demonstration of designing, implementing, troubleshooting, documenting, and operating a complete CI/CD workflow.

The first complete working implementation is preserved as:

```text
v1.0.0
```

This baseline can now be used as the foundation for more advanced DevOps engineering improvements.
