# Actions Test Application

Test application for CI/CD pipeline development using GitHub Actions, Docker Hub, and ArgoCD.

## Overview

This is a Spring Boot 3.5.9 application built with Java 21, designed to test and develop CI/CD strategies. The build system mirrors the Intorqa platform's approach with custom Gradle Docker plugin, date-based versioning, and multi-platform image builds.

## Project Structure

```
actions/
├── buildSrc/                           # Custom Gradle plugins
│   └── src/main/kotlin/
│       └── DockerPlugin.kt            # Multi-platform Docker build plugin
├── k8s/                               # Kubernetes manifests
│   ├── namespace.yaml                 # Namespace definition
│   ├── deployment.yaml                # App deployment
│   ├── service.yaml                   # ClusterIP service
│   └── argocd-application.yaml        # ArgoCD Application definition
├── .github/workflows/
│   └── build-branch.yml               # CI/CD pipeline
├── src/                               # Application source code
├── Dockerfile                         # Multi-stage Docker build
└── build.gradle.kts                   # Gradle build configuration
```

## Version Management

**Pattern:** `YYYY.MM.NNN-BRANCH`

- **Current version:** `2025.12.001-DEV`
- **Examples:**
  - `2025.12.001-DEV` (dev branch)
  - `2025.12.001-MAIN` (main branch)
  - `2025.12.002-TEST` (test branch)

### Incrementing Versions

Manually edit `build.gradle.kts`:

```kotlin
val codeVersion = "2025.12.002"  // Increment the build number
```

## Build System

### Local Development

```bash
# Build JAR only
./gradlew build

# Build and push Docker image to Docker Hub
./gradlew docker

# Print current version
./gradlew printVersion

# Run all default tasks (clean, build, docker)
./gradlew
```

### Docker Image Details

- **Registry:** Docker Hub (shinobislayer)
- **Image:** `shinobislayer/actions`
- **Platforms:** linux/amd64, linux/arm64
- **Tags:**
  - `shinobislayer/actions:<version>` (e.g., `2025.12.001-DEV`)
  - `shinobislayer/actions:latest`

## CI/CD Pipeline

### GitHub Actions Workflow

**Triggers:** Push to `main`, `dev`, or `test` branches

**Steps:**
1. Checkout code
2. Set up JDK 21
3. Set up Docker buildx for multi-platform builds
4. Log in to Docker Hub
5. Build JAR with Gradle
6. Build and push multi-platform Docker image

### Required GitHub Secrets

Navigate to: https://github.com/ShinobiSlayer/actions/settings/secrets/actions

Add the following secrets:

| Secret Name | Value | Purpose |
|-------------|-------|---------|
| `DOCKER_HUB_USERNAME` | `shinobislayer` | Docker Hub login |
| `DOCKER_HUB_PASSWORD` | Your Docker Hub access token | Authentication |

**Creating Docker Hub Access Token:**

1. Go to https://hub.docker.com/settings/security
2. Click "New Access Token"
3. Name: "GitHub Actions"
4. Permissions: Read, Write, Delete
5. Copy the token and add to GitHub secrets

## Kubernetes Deployment

### Manual Deployment

```bash
# Set kubectl context to your cluster
kubectl config use-context arn:aws:eks:eu-west-1:446264842815:cluster/development

# Create namespace
kubectl apply -f k8s/namespace.yaml

# Deploy application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Verify deployment
kubectl get pods -n actions-dev
kubectl get svc -n actions-dev
```

### ArgoCD Deployment

#### Option 1: Apply ArgoCD Application

```bash
kubectl apply -f k8s/argocd-application.yaml
```

#### Option 2: Create via ArgoCD CLI

```bash
argocd app create actions-app \
  --repo https://github.com/ShinobiSlayer/actions.git \
  --path k8s \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace actions-dev \
  --sync-policy automated \
  --auto-prune \
  --self-heal
```

#### Option 3: Create via ArgoCD UI

1. Navigate to ArgoCD UI: http://192.168.1.231
2. Click "New App"
3. Fill in details:
   - **Application Name:** actions-app
   - **Project:** default
   - **Sync Policy:** Automatic
   - **Repository URL:** https://github.com/ShinobiSlayer/actions.git
   - **Path:** k8s
   - **Cluster:** https://kubernetes.default.svc
   - **Namespace:** actions-dev
4. Click "Create"

### Verify Deployment

```bash
# Check ArgoCD application status
argocd app get actions-app

# Check pods
kubectl get pods -n actions-dev

# Check service
kubectl get svc -n actions-dev

# Port-forward to test locally
kubectl port-forward -n actions-dev svc/actions-service 8080:80

# Test endpoint
curl http://localhost:8080/actuator/health
```

## Testing the Complete Pipeline

### End-to-End Test

1. **Make a code change**
   ```bash
   # Edit src/main/java/com/test/githubactions/ActionsApplication.java
   git add .
   git commit -m "Test CI/CD pipeline"
   git push origin main
   ```

2. **Monitor GitHub Actions**
   - Go to: https://github.com/ShinobiSlayer/actions/actions
   - Watch the workflow run
   - Verify Docker image is pushed to Docker Hub

3. **Check Docker Hub**
   - Go to: https://hub.docker.com/r/shinobislayer/actions
   - Verify new tag appears

4. **Watch ArgoCD sync**
   - ArgoCD will detect the change (if configured with automated sync)
   - Or manually sync: `argocd app sync actions-app`

5. **Verify deployment**
   ```bash
   kubectl get pods -n actions-dev -w
   kubectl logs -n actions-dev -l app=actions
   ```

## Troubleshooting

### Docker Build Issues

```bash
# Check Docker buildx builders
docker buildx ls

# Remove and recreate builder
docker buildx rm actionsBuilder
./gradlew docker
```

### GitHub Actions Failures

- Check secrets are correctly set
- Verify Docker Hub credentials
- Review workflow logs

### Kubernetes Deployment Issues

```bash
# Check pod status
kubectl describe pod -n actions-dev -l app=actions

# Check logs
kubectl logs -n actions-dev -l app=actions

# Check events
kubectl get events -n actions-dev --sort-by='.lastTimestamp'
```

### ArgoCD Sync Issues

```bash
# Force sync
argocd app sync actions-app

# Get sync status
argocd app get actions-app

# View application details
argocd app describe actions-app
```

## Development Workflow

### Making Changes

1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes
3. Test locally: `./gradlew build`
4. Push to branch: `git push origin feature/my-feature`
5. Create PR to `dev` branch
6. After merge, CI/CD automatically builds and deploys

### Version Increment Workflow

1. Edit `build.gradle.kts` - increment `codeVersion`
2. Commit: `git commit -m "Bump version to 2025.12.002"`
3. Push to trigger build

## Next Steps

### Planned Enhancements

1. **Automated Version Incrementing**
   - Auto-increment build numbers in CI/CD
   - Use commit SHA for version tags

2. **Advanced ArgoCD Integration**
   - Image Updater for automatic manifest updates
   - Separate config repo for GitOps
   - Environment promotion (dev → test → prod)

3. **Enhanced CI/CD**
   - Automated testing in pipeline
   - Code quality checks
   - Security scanning
   - Deployment rollback capabilities

4. **Multi-Environment Support**
   - dev, test, staging, production namespaces
   - Environment-specific configurations
   - Blue-green or canary deployments

## Resources

- **GitHub Repository:** https://github.com/ShinobiSlayer/actions
- **Docker Hub:** https://hub.docker.com/r/shinobislayer/actions
- **ArgoCD UI:** http://192.168.1.231
- **Kubernetes Cluster:** 192.168.1.231 (Kind cluster)

## Technology Stack

- **Java:** 21 (Eclipse Temurin)
- **Spring Boot:** 3.5.9
- **Build Tool:** Gradle 8.x with Kotlin DSL
- **Container:** Docker with multi-platform support
- **Orchestration:** Kubernetes (Kind)
- **GitOps:** ArgoCD
- **CI/CD:** GitHub Actions
# Testing webhook
# Testing webhook
