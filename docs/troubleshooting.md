# Troubleshooting Guide  - On-premise CI/CD Deployment Automation Project

## 1. Purpose

This document records the major issues encountered while building and operating the Local AWS EC2 CI/CD Lab.

The problems documented here are based on the actual implementation process and demonstrate practical troubleshooting across:

* Jenkins
* Git/GitHub
* Maven
* Docker
* Docker Hub
* SSH
* Docker Compose
* VMware Ubuntu virtual machines
* Remote application deployment

The purpose is not only to record errors, but also to document the diagnostic process and the solution applied.

---

# 2. Jenkins Could Not Find Maven

## Symptom

The Jenkins pipeline reached the Test stage but failed with:

```text
mvn: not found
```

The pipeline ended with:

```text
ERROR: script returned exit code 127
```

## Cause

Maven was available on the Ubuntu host, but Jenkins runs inside its own container.

Installing Maven on VM1 does not automatically make Maven available inside the Jenkins container.

## Solution

Maven was configured as a Jenkins-managed tool.

The Jenkinsfile was updated to request the configured Maven installation:

```groovy
tools {
    maven 'Maven-3.9'
}
```

The pipeline then successfully executed:

```bash
mvn test
```

## Verification

A successful build produced:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Lesson

A Jenkins container is an isolated execution environment. Tools required by a pipeline must either exist inside that environment or be provided through Jenkins tool configuration.

---

# 3. Jenkins Git Installation Warning

## Symptom

The Jenkins console displayed:

```text
Selected Git installation does not exist. Using Default
The recommended git tool is: NONE
```

## Diagnosis

Although Jenkins reported the warning, Git operations were still successful.

The pipeline successfully performed:

```text
git init
git fetch
git checkout
```

and checked out the required commit from GitHub.

## Result

The warning did not prevent the pipeline from running.

The GitHub SSH credential was successfully used:

```text
jenkins-github
```

### Lesson

Not every warning in a Jenkins console represents a pipeline failure.

The actual result of the Git operation should be examined before changing working configuration.

---

# 4. Jenkins Docker Access

## Requirement

The Jenkins pipeline needed to execute Docker commands such as:

```bash
docker build
docker push
```

Jenkins runs inside a container, so Docker access had to be verified.

## Verification

Docker was checked from inside the Jenkins container:

```bash
docker exec jenkins docker --version
```

The Jenkins container reported a Docker CLI version.

The Docker socket was also checked:

```bash
docker exec jenkins ls -l /var/run/docker.sock
```

The Jenkins user was inspected:

```bash
docker exec jenkins id
```

The Jenkins user belonged to the Docker socket group.

## Result

Jenkins was able to communicate with the Docker engine and successfully build Docker images.

### Lesson

When Jenkins runs inside Docker and needs to control Docker, both the Docker CLI and access to the Docker daemon must be available.

---

# 5. Docker Image Build Failure Risk

## Requirement

The Docker build depended on the Maven-generated JAR.

The Dockerfile copied:

```text
target/devops-status-app-1.0.0.jar
```

into the image.

## Correct Pipeline Order

The pipeline therefore needed to package the application before building the Docker image:

```text
Maven Test
    |
    v
Maven Package
    |
    v
Docker Build
```

The successful build produced:

```text
target/devops-status-app-1.0.0.jar
```

The Docker build then successfully copied the artifact into the image.

### Lesson

Build dependencies must be respected in CI/CD pipelines.

The Docker build cannot copy an artifact that has not yet been generated.

---

# 6. Docker Hub Authentication

## Symptom

Docker Hub publishing requires authentication.

The pipeline therefore needed a secure way to provide Docker Hub credentials.

## Solution

A Jenkins credential was configured with the ID:

```text
docker-hub-repo
```

The Jenkinsfile uses:

```groovy
withCredentials([usernamePassword(
    credentialsId: 'docker-hub-repo',
    usernameVariable: 'DOCKER_USERNAME',
    passwordVariable: 'DOCKER_PASSWORD'
)])
```

The credentials are then passed to Docker:

```bash
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
```

The image is pushed:

```bash
docker push azubuike1/devops-status-app:1.0.0
```

Finally:

```bash
docker logout
```

## Result

The image was successfully published to:

```text
azubuike1/devops-status-app:1.0.0
```

### Lesson

Credentials should be stored in Jenkins Credentials Manager rather than hardcoded into the Jenkinsfile.

---

# 7. SSH Authentication to VM2

## Symptom

An SSH connection initially failed with:

```text
Permission denied (publickey,password).
```

## Diagnosis

The important distinction was between:

```text
VM1 → VM2
```

and:

```text
Jenkins container → VM2
```

SSH from the Ubuntu user on VM1 was working, but Jenkins operates inside its own container and therefore has its own execution environment and SSH configuration.

## Verification

SSH support was checked inside Jenkins:

```bash
docker exec jenkins ssh -V
```

Jenkins also contained an SSH configuration directory:

```bash
docker exec jenkins ls -la /var/jenkins_home/.ssh
```

## Working Configuration

The Jenkins pipeline uses the Jenkins credential:

```text
dev-server-ssh-key
```

and the SSH agent:

```groovy
sshagent(['dev-server-ssh-key'])
```

The deployment target is VM2:

```text
dev-server@192.168.146.138
```

### Lesson

Successful SSH from the host does not automatically prove that SSH from Jenkins will work.

Each execution environment must be considered separately.

---

# 8. VM1 Hostname vs VM2 Hostname

During troubleshooting, it was important to distinguish the two virtual machines.

## VM1

VM1 is the Jenkins/CI server.

The Jenkins container runs on VM1.

## VM2

VM2 is the application server.

Its hostname is:

```text
dev-server-VMware-Virtual-Platform
```

Its IP address is:

```text
192.168.146.138
```

SSH from VM1 to VM2 was verified with:

```bash
ssh -o StrictHostKeyChecking=no dev-server "hostname"
```

The command returned:

```text
dev-server-VMware-Virtual-Platform
```

### Lesson

Clear separation of the CI server and application server is important when troubleshooting remote deployments.

---

# 9. Docker Compose IMAGE Variable Error

## Symptom

Docker Compose produced:

```text
WARN The "IMAGE" variable is not set. Defaulting to a blank string.
```

followed by:

```text
service "devops-status-app" has neither an image nor a build context specified
```

## Cause

The Compose file used:

```yaml
image: ${IMAGE}
```

Docker Compose therefore expected an environment variable called:

```text
IMAGE
```

## Solution

The image variable must be supplied before running Compose:

```bash
export IMAGE=devops-status-app:1.0.0
```

Then:

```bash
docker compose up -d
```

## Verification

The deployment can be checked with:

```bash
docker compose ps
```

### Lesson

Environment-variable-based Compose configuration provides flexibility, but the required variables must be supplied before deployment.

---

# 10. Jenkinsfile Backslash Syntax Error

## Symptom

Jenkins reported:

```text
WorkflowScript: 67: unexpected char: '\' 
```

The problem occurred around a Docker command using shell line continuation:

```text
--name devops-status-app \
```

## Cause

The Jenkinsfile contained problematic escaping and whitespace around the shell continuation characters.

Groovy, Jenkins Pipeline syntax, and shell syntax were interacting in the same multiline string.

## Resolution

The deployment command was simplified and corrected so that Jenkins could pass the remote shell command reliably.

The final working deployment process uses:

```text
SSH
  |
  v
docker pull
  |
  v
stop existing container
  |
  v
remove existing container
  |
  v
docker run
```

### Lesson

When embedding shell commands inside Jenkins Pipeline Groovy strings, keep quoting and escaping as simple as possible.

---

# 11. Existing Container on VM2

## Symptom

A previous deployment left the container running:

```text
devops-status-app
```

A new deployment therefore needed to replace the existing container.

## Deployment Strategy

The deployment process checks for the existing application container and removes it before starting the new version.

The intended sequence is:

```text
docker pull
    |
    v
stop old container
    |
    v
remove old container
    |
    v
start new container
```

This prevents the deployment from failing because a container with the same name already exists.

### Lesson

A deployment pipeline should account for the previous application instance.

---

# 12. Container Stopped After Host Shutdown

## Symptom

After the computer was shut down, the application container on VM2 was no longer running.

Docker showed the container with an exited status.

## Cause

The VMware host and virtual machines had been stopped.

The container therefore stopped along with the VM.

## Recovery

The container was started again with:

```bash
docker start devops-status-app
```

Then:

```bash
docker ps
```

confirmed that the application was running.

## Verification

The application was tested with:

```bash
curl http://localhost:8081/api/status
```

The application returned:

```json
{
  "environment": "dev-server",
  "status": "UP",
  "application": "devops-status-app",
  "deployment": "docker"
}
```

### Lesson

A container restart policy can improve recovery when Docker itself is running, but a stopped VM requires the infrastructure to be started first.

---

# 13. Application Port Mapping

The Spring Boot application listens on:

```text
8080
```

VM2 exposes it externally through:

```text
8081
```

The Docker mapping is:

```text
8081:8080
```

Therefore:

```text
VM2
Port 8081
   |
   v
Docker
   |
   v
Container
Port 8080
   |
   v
Spring Boot
```

## Verification

On VM2:

```bash
curl http://localhost:8081/api/health
```

Expected:

```json
{
  "status": "UP"
}
```

### Lesson

Port mapping allows the host and container to use different ports.

This was particularly important because Jenkins itself uses port `8080` on VM1.

---

# 14. Verifying the Final Deployment

The deployment should not be considered complete merely because the Docker container starts.

Verification should occur at multiple levels.

## Container Level

```bash
docker ps
```

## Application Health

```bash
curl http://localhost:8081/api/health
```

## Deployment Configuration

```bash
curl http://localhost:8081/api/status
```

Expected deployment information includes:

```text
environment: dev-server
deployment: docker
status: UP
```

### Lesson

Deployment verification should confirm both infrastructure state and application state.

---

# 15. General Troubleshooting Method Used

The project followed a practical troubleshooting pattern:

```text
1. Read the error
       |
       v
2. Identify which environment failed
       |
       v
3. Check the relevant component
       |
       v
4. Reproduce or verify the problem
       |
       v
5. Apply the smallest required change
       |
       v
6. Re-run the pipeline
       |
       v
7. Verify the result
```

This approach was used throughout the project instead of changing multiple components at once.

---

# 16. Key Troubleshooting Lessons

The major lessons from the implementation were:

### Jenkins is its own environment

A tool installed on VM1 is not automatically available inside the Jenkins container.

### Remote execution must be tested from the actual execution environment

SSH from VM1 does not automatically prove that Jenkins can SSH to VM2.

### CI/CD stages depend on one another

Testing and packaging must successfully produce the artifacts required by Docker.

### Credentials belong in Jenkins Credentials Manager

Secrets should not be committed to source control.

### Docker and Jenkins require correct integration

Jenkins must have access to the Docker CLI and Docker daemon.

### Deployment should be repeatable

The pipeline should be capable of replacing an existing application container.

### Always verify the deployed application

A successful Jenkins build is not enough. The application itself must be tested after deployment.

---

# 17. Final Working State

The troubleshooting process resulted in a functioning CI/CD pipeline:

```text
GitHub
   |
   v
Jenkins VM1
   |
   +--> Checkout
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
VM2 dev-server
   |
   +--> Docker Pull
   |
   +--> Container Deployment
   |
   v
Spring Boot
   |
   +--> /api/health
   |
   +--> /api/status
```

The first complete working implementation was preserved as:

```text
v1.0.0
```

This baseline provides a stable reference point for future improvements.

---

## Conclusion

The troubleshooting performed during this project demonstrates practical DevOps problem-solving rather than simply following a tutorial.

The implementation required diagnosing issues across multiple layers:

```text
Application
    ↓
Maven
    ↓
Docker
    ↓
Jenkins
    ↓
Docker Hub
    ↓
SSH
    ↓
VM2
    ↓
Container
    ↓
Application
```

Understanding how these layers interact is essential for designing, operating, and troubleshooting reliable CI/CD systems.
