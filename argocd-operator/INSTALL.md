# ArgoCD Operator Installation Guide

## Prerequisites

- Kubernetes cluster (kind, k3s, or production cluster)
- kubectl configured and connected to your cluster
- Admin access to the cluster

## Installation Steps

### Step 1: Backup Current ArgoCD Installation (if migrating)

```bash
# Save current applications
kubectl get applications -n argocd -o yaml > backup-applications.yaml

# Save current AppProjects
kubectl get appprojects -n argocd -o yaml > backup-appprojects.yaml

# Save repository configurations
kubectl get secrets -n argocd -l argocd.argoproj.io/secret-type=repository -o yaml > backup-repositories.yaml
```

### Step 2: Install ArgoCD Operator CRDs

```bash
# Install the ArgoCD CRDs
kubectl apply -f https://raw.githubusercontent.com/argoproj-labs/argocd-operator/master/config/crd/bases/argoproj.io_argocds.yaml
kubectl apply -f https://raw.githubusercontent.com/argoproj-labs/argocd-operator/master/config/crd/bases/argoproj.io_applications.yaml
kubectl apply -f https://raw.githubusercontent.com/argoproj-labs/argocd-operator/master/config/crd/bases/argoproj.io_applicationsets.yaml
kubectl apply -f https://raw.githubusercontent.com/argoproj-labs/argocd-operator/master/config/crd/bases/argoproj.io_appprojects.yaml
```

### Step 3: Install ArgoCD Operator

```bash
# Install the operator
kubectl apply -f install-operator.yaml

# Wait for operator to be ready
kubectl wait --for=condition=available --timeout=300s deployment/argocd-operator-controller-manager -n argocd-operator
```

### Step 4: Remove Old ArgoCD Installation (if migrating)

```bash
# Delete the old ArgoCD installation (WARNING: This will delete all running ArgoCD components)
# Make sure you've backed up your applications first!
kubectl delete deployment --all -n argocd
kubectl delete statefulset --all -n argocd
kubectl delete service --all -n argocd
kubectl delete ingress --all -n argocd

# Keep the namespace and CRDs
# DO NOT delete: applications, appprojects, secrets
```

### Step 5: Deploy ArgoCD Instance via Operator

```bash
# Create ArgoCD namespace if it doesn't exist
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

# Deploy the ArgoCD instance
kubectl apply -f argocd-instance.yaml

# Watch the operator create ArgoCD components
kubectl get pods -n argocd -w
```

### Step 6: Get Admin Password

```bash
# Get the initial admin password
kubectl get secret argocd-cluster -n argocd -o jsonpath='{.data.admin\.password}' | base64 -d
echo
```

### Step 7: Access ArgoCD UI

For kind cluster with ingress:
```bash
# Port forward to access UI
kubectl port-forward svc/argocd-server -n argocd 8080:80

# Open browser to: http://localhost:8080
# Username: admin
# Password: (from step 6)
```

For production with ingress:
```bash
# Update argocd-instance.yaml with your domain:
# spec:
#   server:
#     host: argocd.yourdomain.com

# Apply the change
kubectl apply -f argocd-instance.yaml
```

### Step 8: Verify Installation

```bash
# Check ArgoCD CR status
kubectl get argocd -n argocd

# Check all pods are running
kubectl get pods -n argocd

# Check services
kubectl get svc -n argocd
```

### Step 9: Restore Applications (if migrating)

```bash
# Your applications should still exist as CRDs
kubectl get applications -n argocd

# If you need to restore from backup:
kubectl apply -f backup-applications.yaml
```

## Post-Installation

### Change Admin Password

```bash
# Login with argocd CLI
argocd login localhost:8080 --insecure

# Change password
argocd account update-password
```

### Configure Repositories

If you backed up repositories, they should still be present. To add new ones:

```bash
argocd repo add https://github.com/your-org/your-repo --username <username> --password <password>
```

## Upgrading ArgoCD

To upgrade ArgoCD when using the operator:

```bash
# Edit argocd-instance.yaml and update the version
# spec:
#   version: v3.3.0

# Apply the change
kubectl apply -f argocd-instance.yaml

# The operator will handle the rolling update
kubectl get pods -n argocd -w
```

## Troubleshooting

### Check Operator Logs

```bash
kubectl logs -n argocd-operator deployment/argocd-operator-controller-manager -f
```

### Check ArgoCD Instance Status

```bash
kubectl describe argocd argocd -n argocd
```

### Operator Not Creating Resources

1. Check operator is running:
   ```bash
   kubectl get pods -n argocd-operator
   ```

2. Check operator logs for errors:
   ```bash
   kubectl logs -n argocd-operator deployment/argocd-operator-controller-manager
   ```

3. Verify CRDs are installed:
   ```bash
   kubectl get crd | grep argoproj
   ```

## Uninstall

To completely remove ArgoCD Operator and ArgoCD:

```bash
# Delete ArgoCD instance
kubectl delete argocd argocd -n argocd

# Delete operator
kubectl delete -f install-operator.yaml

# Delete CRDs (WARNING: This will delete all ArgoCD applications)
kubectl delete crd argocds.argoproj.io
kubectl delete crd applications.argoproj.io
kubectl delete crd applicationsets.argoproj.io
kubectl delete crd appprojects.argoproj.io
```

## Adding Prometheus/Grafana Later

We'll add monitoring in a future step. For now, ArgoCD exposes metrics at:
- `argocd-metrics:8082/metrics` (application controller)
- `argocd-server-metrics:8083/metrics` (API server)
- `argocd-repo-server:8084/metrics` (repo server)
