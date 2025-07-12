#!/bin/bash

# Generate SSL certificates for Keycloak
# This script creates self-signed certificates for development use

set -e

echo "Generating SSL certificates for Keycloak..."

# Check for existing certificate files
if [ -f "certs/tls.key" ] || [ -f "certs/tls.crt" ]; then
    echo "Warning: Existing certificate files found:"
    [ -f "certs/tls.key" ] && echo "  - certs/tls.key"
    [ -f "certs/tls.crt" ] && echo "  - certs/tls.crt"
    echo ""
    read -p "Do you want to delete existing certificates and generate new ones? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Operation cancelled. Existing certificates will be preserved."
        exit 0
    fi
    
    echo "Removing existing certificate files..."
    rm -f certs/tls.key certs/tls.crt
    echo "Existing certificates removed."
    echo ""
fi

# Create certs directory if it doesn't exist
mkdir -p certs

# Generate self-signed certificate
openssl req -x509 -newkey rsa:4096 -sha256 -days 365 -nodes \
  -keyout certs/tls.key -out certs/tls.crt \
  -subj "/CN=keycloak.ontomesh.io" \
  -addext "subjectAltName=DNS:keycloak.ontomesh.io,DNS:localhost,IP:127.0.0.1"

# Set proper permissions
chmod 400 certs/tls.key
chmod 644 certs/tls.crt

echo "Certificates generated successfully!"
echo "Certificate: certs/tls.crt"
echo "Private Key: certs/tls.key"
echo ""
echo "Note: These are self-signed certificates for development use only."
echo "For production, use proper CA-signed certificates." 