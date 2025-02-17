#!/bin/bash

# GraphDB Server Details
#GDB_HOST="http://graphdb:7200"
#ONTOMESH_REPO_ID="ontomesh"

# Check if the repository exists
response=$(curl -s -o /dev/null -w "%{http_code}" "http://graphdb:7200/rest/repositories/$ONTOMESH_REPO_ID")

if [ "$response" -eq 200 ]; then
    echo "Repository '$ONTOMESH_REPO_ID' already exists."
else
    echo "Repository '$ONTOMESH_REPO_ID' not found. Creating a new repository..."

    # Create the repository using CURL
    curl -X POST "http://graphdb:7200/rest/repositories" \
        -H 'Content-Type: multipart/form-data'\
        -F "config=@/opt/ontomesh/graphdb/repo-ontomesh.ttl"

    echo "Repository '$ONTOMESH_REPO_ID' created successfully."
fi
