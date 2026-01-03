#!/bin/bash
set -e

BACKUP_DIR="argocd-backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo "Starting ArgoCD backup to $BACKUP_DIR..."

# Backup all ArgoCD applications
echo "Backing up Applications..."
kubectl get applications -n argocd -o yaml > "$BACKUP_DIR/applications.yaml"

# Backup all AppProjects
echo "Backing up AppProjects..."
kubectl get appprojects -n argocd -o yaml > "$BACKUP_DIR/appprojects.yaml"

# Backup ConfigMaps
echo "Backing up ConfigMaps..."
kubectl get configmap argocd-cm -n argocd -o yaml > "$BACKUP_DIR/argocd-cm.yaml"
kubectl get configmap argocd-cmd-params-cm -n argocd -o yaml > "$BACKUP_DIR/argocd-cmd-params-cm.yaml"
kubectl get configmap argocd-rbac-cm -n argocd -o yaml > "$BACKUP_DIR/argocd-rbac-cm.yaml"
kubectl get configmap argocd-ssh-known-hosts-cm -n argocd -o yaml > "$BACKUP_DIR/argocd-ssh-known-hosts-cm.yaml"
kubectl get configmap argocd-tls-certs-cm -n argocd -o yaml > "$BACKUP_DIR/argocd-tls-certs-cm.yaml"

# Backup Secrets (repositories, clusters, etc)
echo "Backing up Secrets..."
kubectl get secret argocd-secret -n argocd -o yaml > "$BACKUP_DIR/argocd-secret.yaml"
kubectl get secrets -n argocd -l argocd.argoproj.io/secret-type=repository -o yaml > "$BACKUP_DIR/repositories.yaml"
kubectl get secrets -n argocd -l argocd.argoproj.io/secret-type=cluster -o yaml > "$BACKUP_DIR/clusters.yaml"

# Backup notifications
echo "Backing up Notifications..."
kubectl get configmap argocd-notifications-cm -n argocd -o yaml > "$BACKUP_DIR/argocd-notifications-cm.yaml" 2>/dev/null || echo "No notifications ConfigMap"
kubectl get secret argocd-notifications-secret -n argocd -o yaml > "$BACKUP_DIR/argocd-notifications-secret.yaml" 2>/dev/null || echo "No notifications Secret"

# Backup ArgoCD Image Updater config
echo "Backing up ArgoCD Image Updater..."
kubectl get configmap argocd-image-updater-config -n argocd -o yaml > "$BACKUP_DIR/argocd-image-updater-config.yaml" 2>/dev/null || echo "No image updater ConfigMap"
kubectl get secret argocd-image-updater-secret -n argocd -o yaml > "$BACKUP_DIR/argocd-image-updater-secret.yaml" 2>/dev/null || echo "No image updater Secret"

# Backup Ingress
echo "Backing up Ingress..."
kubectl get ingress -n argocd -o yaml > "$BACKUP_DIR/ingress.yaml"

# Export current ArgoCD version
echo "Recording ArgoCD version..."
kubectl get deployment argocd-server -n argocd -o jsonpath='{.spec.template.spec.containers[0].image}' > "$BACKUP_DIR/version.txt"

echo ""
echo "Backup complete! Files saved to: $BACKUP_DIR"
echo ""
echo "To restore, review and apply the YAML files in this directory."
echo "Note: You may need to remove 'resourceVersion' and 'uid' fields before applying."

# Create tarball
tar -czf "${BACKUP_DIR}.tar.gz" "$BACKUP_DIR"
echo "Compressed backup: ${BACKUP_DIR}.tar.gz"
