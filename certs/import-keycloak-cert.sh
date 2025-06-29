#!/bin/bash

# Script to import Keycloak certificate to Java truststore
# This allows local development to connect to Keycloak without SSL errors

set -e

# Find Java home
JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
if [ -z "$JAVA_HOME" ]; then
    echo "Error: JAVA_HOME not set and could not find Java installation"
    exit 1
fi

echo "Using Java home: $JAVA_HOME"

# Check if certificate exists
CERT_FILE="$(dirname "$0")/tls.crt"
if [ ! -f "$CERT_FILE" ]; then
    echo "Error: Certificate not found at $CERT_FILE"
    echo "Looking for: $CERT_FILE"
    echo "Current directory: $(pwd)"
    echo "Available files in $(dirname "$0"):"
    ls -la "$(dirname "$0")"
    exit 1
fi

# Check if certificate already exists and delete it
echo "Checking if certificate with alias 'keycloak-ontomesh' already exists..."
if $JAVA_HOME/bin/keytool -list -alias keycloak-ontomesh -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit >/dev/null 2>&1; then
    echo "Certificate with alias 'keycloak-ontomesh' found. Deleting it..."
    $JAVA_HOME/bin/keytool -delete -alias keycloak-ontomesh -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit
    echo "Old certificate deleted successfully."
else
    echo "No existing certificate found with alias 'keycloak-ontomesh'."
fi

# Import certificate to Java truststore
echo "Importing Keycloak certificate to Java truststore..."
$JAVA_HOME/bin/keytool -import -alias keycloak-ontomesh -file "$CERT_FILE" \
    -keystore "$JAVA_HOME/lib/security/cacerts" \
    -storepass changeit -noprompt

echo "Keycloak certificate imported successfully!"
echo "You can now run the application in IntelliJ without SSL certificate errors."
echo ""
echo "To verify the certificate was imported:"
echo "$JAVA_HOME/bin/keytool -list -alias keycloak-ontomesh -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit" 