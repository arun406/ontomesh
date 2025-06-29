#!/bin/bash

# GraphDB Server Details
GDB_HOST="http://graphdb:7200"
ONTOMESH_REPO_ID="${ONTOMESH_REPO_ID:-ontomesh}"

# Function to import the IATA ontology
import_ontology() {
    echo "Starting IATA ontology import..."
    
    # Wait a bit for repository to be fully ready
    sleep 5
    
    # Import the ontology file
    import_response=$(curl -X POST "$GDB_HOST/repositories/$ONTOMESH_REPO_ID/statements" \
        -H 'Content-Type: text/turtle' \
        -H 'Accept: application/json' \
        --data-binary @/opt/ontomesh/graphdb/IATA-1R-DM-Ontology.ttl \
        -w "%{http_code}" \
        -s)
    
    if [ "$import_response" -eq 204 ] || [ "$import_response" -eq 200 ]; then
        echo "IATA ontology imported successfully!"
        
        # Verify the import by checking for ontology triples
        echo "Verifying ontology import..."
        verify_response=$(curl -s -G "$GDB_HOST/repositories/$ONTOMESH_REPO_ID/statements" \
            --data-urlencode "query=SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }" \
            -H "Accept: application/sparql-results+json")
        
        if echo "$verify_response" | grep -q '"count":"[0-9]*"'; then
            count=$(echo "$verify_response" | grep -o '"count":"[0-9]*"' | grep -o '[0-9]*')
            echo "Ontology import verified. Total triples in repository: $count"
        else
            echo "Warning: Could not verify ontology import"
        fi
        
    else
        echo "ERROR: Failed to import IATA ontology. HTTP response: $import_response"
        exit 1
    fi
}

echo "Starting GraphDB repository initialization..."
echo "GraphDB Host: $GDB_HOST"
echo "Repository ID: $ONTOMESH_REPO_ID"

# Wait for GraphDB to be ready
echo "Waiting for GraphDB to be ready..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s -f "$GDB_HOST/rest/repositories" > /dev/null 2>&1; then
        echo "GraphDB is ready!"
        break
    fi
    echo "Attempt $attempt/$max_attempts: GraphDB not ready yet, waiting..."
    sleep 5
    attempt=$((attempt + 1))
done

if [ $attempt -gt $max_attempts ]; then
    echo "ERROR: GraphDB failed to start within the expected time"
    exit 1
fi

# Check if the repository exists
echo "Checking if repository '$ONTOMESH_REPO_ID' exists..."
response=$(curl -s -o /dev/null -w "%{http_code}" "$GDB_HOST/rest/repositories/$ONTOMESH_REPO_ID")

if [ "$response" -eq 200 ]; then
    echo "Repository '$ONTOMESH_REPO_ID' already exists."
    
    # Check if ontology is already imported
    echo "Checking if IATA ontology is already imported..."
    ontology_check=$(curl -s -G "$GDB_HOST/repositories/$ONTOMESH_REPO_ID/statements" \
        --data-urlencode "query=ASK { <https://onerecord.iata.org/ns/cargo> a <http://www.w3.org/2002/07/owl#Ontology> }" \
        -H "Accept: application/sparql-results+json")
    
    if echo "$ontology_check" | grep -q '"boolean":true'; then
        echo "IATA ontology is already imported."
        exit 0
    else
        echo "IATA ontology not found, importing..."
        import_ontology
    fi
else
    echo "Repository '$ONTOMESH_REPO_ID' not found. Creating a new repository..."

    # Create the repository using CURL
    create_response=$(curl -X POST "$GDB_HOST/rest/repositories" \
        -H 'Content-Type: multipart/form-data' \
        -F "config=@/opt/ontomesh/graphdb/repo-ontomesh.ttl" \
        -w "%{http_code}" \
        -s)

    if [ "$create_response" -eq 201 ] || [ "$create_response" -eq 200 ]; then
        echo "Repository '$ONTOMESH_REPO_ID' created successfully."
        
        # Import the IATA ontology
        echo "Importing IATA ONE Record ontology..."
        import_ontology
        
    else
        echo "ERROR: Failed to create repository. HTTP response: $create_response"
        exit 1
    fi
fi
