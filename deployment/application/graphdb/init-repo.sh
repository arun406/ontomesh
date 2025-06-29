#!/bin/bash

# GraphDB Server Details
GDB_HOST="http://graphdb:7200"
ONTOMESH_REPO_ID="${ONTOMESH_REPO_ID:-ontomesh}"

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
    echo "Repository initialization completed successfully."
    exit 0
    
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
        echo "Repository initialization completed successfully."
        exit 0
        
    else
        echo "ERROR: Failed to create repository. HTTP response: $create_response"
        exit 1
    fi
fi
