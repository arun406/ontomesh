#!/bin/bash

# GraphDB verification script
# This script verifies that the single-node GraphDB is properly configured

set -e

echo "Verifying GraphDB single-node configuration..."

# Check if GraphDB is running
echo "Checking GraphDB container status..."
if docker-compose ps graphdb | grep -q "Up"; then
    echo "✓ GraphDB container is running"
else
    echo "ERROR: GraphDB container is not running"
    echo "Start GraphDB: docker-compose up -d graphdb"
    exit 1
fi

# Check GraphDB health
echo ""
echo "Testing GraphDB health..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -s -f "http://localhost:7200/rest/repositories" > /dev/null 2>&1; then
        echo "✓ GraphDB is healthy and responding"
        break
    fi
    echo "Attempt $attempt/$max_attempts: GraphDB not ready yet..."
    sleep 5
    attempt=$((attempt + 1))
done

if [ $attempt -gt $max_attempts ]; then
    echo "ERROR: GraphDB is not responding"
    echo "Check GraphDB logs: docker-compose logs graphdb"
    exit 1
fi

# Check repository creation
echo ""
echo "Checking repository setup..."
if curl -s "http://localhost:7200/rest/repositories" | grep -q "ontomesh"; then
    echo "✓ OntoMesh repository exists"
else
    echo "⚠ OntoMesh repository not found, checking setup logs..."
    if docker-compose logs graph-db-setup | grep -q "Repository.*created successfully"; then
        echo "✓ Repository setup completed successfully"
    else
        echo "⚠ Repository setup may have failed"
        echo "Check setup logs: docker-compose logs graph-db-setup"
    fi
fi

# Check IATA ontology import
echo ""
echo "Checking IATA ontology import..."
ontology_check=$(curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
    --data-urlencode "query=ASK { <https://onerecord.iata.org/ns/cargo> a <http://www.w3.org/2002/07/owl#Ontology> }" \
    -H "Accept: application/sparql-results+json")

if echo "$ontology_check" | grep -q '"boolean":true'; then
    echo "✓ IATA ONE Record ontology is imported"
    
    # Get triple count
    count_response=$(curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
        --data-urlencode "query=SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }" \
        -H "Accept: application/sparql-results+json")
    
    if echo "$count_response" | grep -q '"count":"[0-9]*"'; then
        count=$(echo "$count_response" | grep -o '"count":"[0-9]*"' | grep -o '[0-9]*')
        echo "  Total triples in repository: $count"
    fi
else
    echo "⚠ IATA ontology not found"
    echo "Check setup logs: docker-compose logs graph-db-setup"
fi

# Check GraphDB workbench
echo ""
echo "Testing GraphDB workbench..."
if curl -s -f "http://localhost:7300" > /dev/null 2>&1; then
    echo "✓ GraphDB workbench is accessible"
else
    echo "⚠ GraphDB workbench not accessible"
fi

# Check memory configuration
echo ""
echo "Checking GraphDB memory configuration..."
if docker-compose exec graphdb env | grep -q "GRAPHDB_HEAP_SIZE=2G"; then
    echo "✓ GraphDB heap size configured to 2GB"
else
    echo "⚠ GraphDB heap size not properly configured"
fi

echo ""
echo "GraphDB verification completed!"
echo ""
echo "GraphDB URLs:"
echo "- REST API: http://localhost:7200"
echo "- Workbench: http://localhost:7300"
echo ""
echo "Next steps:"
echo "1. Access GraphDB workbench: http://localhost:7300"
echo "2. Login with default credentials (if configured)"
echo "3. Check OntoMesh repository status" 