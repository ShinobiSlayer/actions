# ArgoCD Operator Migration Guide

## Overview

This directory contains the ArgoCD Operator setup for managing multiple clusters (dev, staging, production).

## Architecture

```
Management Cluster (kind or dedicated cluster)
├── ArgoCD Operator (manages ArgoCD instance)
├── ArgoCD Instance (GitOps control plane)
│   ├── Manages Dev Cluster
│   ├── Manages Staging Cluster
│   └── Manages Production Cluster
```

## Directory Structure

```
argocd-operator/
├── README.md                      # This file
├── operator/
│   └── install.yaml              # ArgoCD Operator installation
├── argocd-instance/
│   ├── base/
│   │   └── argocd.yaml           # Base ArgoCD CR
│   ├── overlays/
│   │   ├── dev/
│   │   │   └── kustomization.yaml
│   │   ├── staging/
│   │   │   └── kustomization.yaml
│   │   └── prod/
│   │       └── kustomization.yaml
└── clusters/
    ├── dev-cluster-secret.yaml
    ├── staging-cluster-secret.yaml
    └── prod-cluster-secret.yaml
```

## Installation Steps

### 1. Install ArgoCD Operator

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f operator/install.yaml
```

### 2. Wait for Operator to be Ready

```bash
kubectl wait --for=condition=ready pod -l name=argocd-operator -n argocd --timeout=300s
```

### 3. Deploy ArgoCD Instance

```bash
# For production
kubectl apply -k argocd-instance/overlays/prod
```

### 4. Register External Clusters

```bash
# Add cluster secrets for external clusters
kubectl apply -f clusters/dev-cluster-secret.yaml
kubectl apply -f clusters/staging-cluster-secret.yaml
kubectl apply -f clusters/prod-cluster-secret.yaml
```

## Benefits Over Standalone Installation

1. **Declarative Management**: ArgoCD itself is managed via GitOps
2. **Consistency**: Same configuration across all environments
3. **Automated Reconciliation**: Operator fixes drift automatically
4. **Easier Upgrades**: Update operator version, not individual manifests
5. **High Availability**: Built-in HA configuration options

## Migrating from Standalone

See `../scripts/backup-argocd.sh` to backup your current installation before migrating.

## Upgrading ArgoCD

To upgrade ArgoCD when using the operator:

1. Update the `spec.version` in the ArgoCD CR
2. Operator handles the rolling update automatically

```yaml
apiVersion: argoproj.io/v1beta1
kind: ArgoCD
metadata:
  name: argocd
spec:
  version: v3.3.0  # Update this
```

## Monitoring

The operator exposes metrics at:
- Operator metrics: `argocd-operator-metrics-service:8080/metrics`
- ArgoCD metrics: Standard ArgoCD metrics endpoints

## Troubleshooting

Check operator logs:
```bash
kubectl logs -n argocd deployment/argocd-operator -f
```

Check ArgoCD CR status:
```bash
kubectl get argocd -n argocd -o yaml
```
