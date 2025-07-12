#!/bin/bash

# Certificate initialization script for application containers
# This script imports the Keycloak certificate into the Java truststore

set -e

echo "Initializing certificates for application container..."

# Check if certificate exists
if [ ! -f "/usr/local/share/ca-certificates/keycloak.crt" ]; then
    echo "ERROR: Certificate file not found at /usr/local/share/ca-certificates/keycloak.crt"
    exit 1
fi

# Find Java home
JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which java))))}
if [ ! -d "$JAVA_HOME" ]; then
    # Try alternative locations
    JAVA_HOME="/usr/lib/jvm/java-21-amazon-corretto"
    if [ ! -d "$JAVA_HOME" ]; then
        JAVA_HOME="/usr/lib/jvm/java-21-openjdk"
    fi
    if [ ! -d "$JAVA_HOME" ]; then
        JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
    fi
fi

if [ ! -d "$JAVA_HOME" ]; then
    echo "ERROR: Could not find Java installation"
    exit 1
fi

echo "Using Java home: $JAVA_HOME"

# Import certificate into Java truststore
TRUSTSTORE="$JAVA_HOME/lib/security/cacerts"

if [ ! -f "$TRUSTSTORE" ]; then
    echo "ERROR: Truststore not found at $TRUSTSTORE"
    exit 1
fi

echo "Importing certificate into truststore: $TRUSTSTORE"

# Check if certificate is already imported
if keytool -list -keystore "$TRUSTSTORE" -storepass changeit -alias keycloak 2>/dev/null; then
    echo "Certificate already imported, removing old entry..."
    keytool -delete -keystore "$TRUSTSTORE" -storepass changeit -alias keycloak -noprompt
fi

# Import the certificate
keytool -importcert \
    -file /usr/local/share/ca-certificates/keycloak.crt \
    -keystore "$TRUSTSTORE" \
    -storepass changeit \
    -alias keycloak \
    -noprompt

echo "Certificate imported successfully!"

# Verify the import
if keytool -list -keystore "$TRUSTSTORE" -storepass changeit -alias keycloak; then
    echo "Certificate verification successful!"
else
    echo "ERROR: Certificate verification failed!"
    exit 1
fi

echo "Certificate initialization completed successfully!" 