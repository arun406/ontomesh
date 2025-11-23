#!/bin/bash

# Certificate verification script
# This script verifies that Keycloak certificates are properly configured

set -e

echo "Verifying Keycloak certificate configuration..."

# Check if certificates exist
if [ ! -f "certs/tls.crt" ]; then
    echo "ERROR: Certificate file not found at certs/tls.crt"
    echo "Run ./generate-certs.sh to generate certificates"
    exit 1
fi

if [ ! -f "certs/tls.key" ]; then
    echo "ERROR: Private key file not found at certs/tls.key"
    echo "Run ./generate-certs.sh to generate certificates"
    exit 1
fi

echo "✓ Certificate files found"

# Check certificate details
echo "Certificate details:"
openssl x509 -in certs/tls.crt -text -noout | grep -E "(Subject:|DNS:|IP Address:)"

# Check if Keycloak is accessible
echo ""
echo "Testing Keycloak connectivity..."

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -k -s -f "https://localhost:8443/health/ready" > /dev/null 2>&1; then
        echo "✓ Keycloak is accessible via HTTPS"
        break
    fi
    echo "Attempt $attempt/$max_attempts: Keycloak not ready yet..."
    sleep 5
    attempt=$((attempt + 1))
done

if [ $attempt -gt $max_attempts ]; then
    echo "ERROR: Keycloak is not accessible"
    echo "Check if Keycloak container is running: docker-compose ps keycloak"
    echo "Check Keycloak logs: docker-compose logs keycloak"
    exit 1
fi

# Test certificate validation
echo ""
echo "Testing certificate validation..."

# Test with curl using the certificate
if curl --cacert certs/tls.crt -s -f "https://keycloak.ontomesh.io:8443/health/ready" > /dev/null 2>&1; then
    echo "✓ Certificate validation successful"
else
    echo "⚠ Certificate validation failed (this is expected for self-signed certificates)"
    echo "  The certificate will be imported into application truststores at runtime"
fi

# Check if applications can access Keycloak
echo ""
echo "Testing application connectivity..."

# Test from within a container
if docker-compose exec -T 1r-server curl -k -s -f "https://keycloak:8443/health/ready" > /dev/null 2>&1; then
    echo "✓ 1r-server can access Keycloak"
else
    echo "⚠ 1r-server cannot access Keycloak (may need certificate import)"
fi

echo ""
echo "Certificate verification completed!"
echo ""
echo "Next steps:"
echo "1. Start the application stack: docker-compose up -d"
echo "2. Monitor certificate import: docker-compose logs 1r-server"
echo "3. Test OAuth2 flow: curl -k https://localhost:8085/actuator/health" 