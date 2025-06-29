#!/bin/bash

# Certificate details
CERT_DIR="./certs"
DAYS_VALID=365

# Create certificate directory
mkdir -p $CERT_DIR

CERT_NAME=$1
COMMON_NAME=$2

generate_cert() {

    echo "Generating private key for $CERT_NAME..."
    openssl genpkey -algorithm RSA -out "$CERT_DIR/$CERT_NAME.key"

    echo "Creating certificate signing request (CSR) for $CERT_NAME..."
    openssl req -new -key "$CERT_DIR/$CERT_NAME.key" -out "$CERT_DIR/$CERT_NAME.csr" -subj "/C=US/ST=California/L=San Francisco/O=MyCompany/OU=IT/CN=$COMMON_NAME/emailAddress=ak.kandakatla@gmail.com"

    echo "Generating self-signed certificate for $CERT_NAME..."
    openssl x509 -req -in "$CERT_DIR/$CERT_NAME.csr" -signkey "$CERT_DIR/$CERT_NAME.key" -out "$CERT_DIR/$CERT_NAME.crt" -days $DAYS_VALID

    echo "$CERT_NAME certificate and key generated successfully!"
    echo " - Private Key: $CERT_DIR/$CERT_NAME.key"
    echo " - Certificate: $CERT_DIR/$CERT_NAME.crt"
    echo ""
}

# Generate certificates for both Application and Keycloak
generate_cert "$APP_NAME" "$APP_COMMON_NAME"

echo "All certificates generated successfully!"

