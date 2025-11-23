#!/bin/bash

# Test script for IATA ontology import
# This script tests the IATA ONE Record ontology import and queries

set -e

echo "Testing IATA ONE Record ontology import..."

# Check if GraphDB is running
if ! curl -s -f "http://localhost:7200/rest/repositories" > /dev/null 2>&1; then
    echo "ERROR: GraphDB is not running"
    echo "Start GraphDB: docker-compose up -d graphdb"
    exit 1
fi

echo "✓ GraphDB is running"

# Check if ontology is imported
echo ""
echo "Checking IATA ontology import..."
ontology_check=$(curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
    --data-urlencode "query=ASK { <https://onerecord.iata.org/ns/cargo> a <http://www.w3.org/2002/07/owl#Ontology> }" \
    -H "Accept: application/sparql-results+json")

if echo "$ontology_check" | grep -q '"boolean":true'; then
    echo "✓ IATA ONE Record ontology is imported"
else
    echo "ERROR: IATA ontology not found"
    echo "Check setup logs: docker-compose logs graph-db-setup"
    exit 1
fi

# Get total triple count
echo ""
echo "Getting repository statistics..."
count_response=$(curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
    --data-urlencode "query=SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }" \
    -H "Accept: application/sparql-results+json")

if echo "$count_response" | grep -q '"count":"[0-9]*"'; then
    count=$(echo "$count_response" | grep -o '"count":"[0-9]*"' | grep -o '[0-9]*')
    echo "✓ Total triples in repository: $count"
else
    echo "⚠ Could not get triple count"
fi

# Test some basic ontology queries
echo ""
echo "Testing ontology queries..."

# Query 1: Get ontology information
echo "1. Ontology information:"
curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
    --data-urlencode "query=SELECT ?title ?description WHERE { <https://onerecord.iata.org/ns/cargo> <http://purl.org/dc/elements/1.1/title> ?title ; <http://purl.org/dc/elements/1.1/description> ?description }" \
    -H "Accept: application/sparql-results+json" | jq -r '.results.bindings[0] | .title.value + ": " + .description.value' 2>/dev/null || echo "  Could not retrieve ontology info"

# Query 2: Count classes
echo "2. Number of classes:"
class_count=$(curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
    --data-urlencode "query=SELECT (COUNT(DISTINCT ?class) AS ?count) WHERE { ?class a <http://www.w3.org/2002/07/owl#Class> }" \
    -H "Accept: application/sparql-results+json" | grep -o '"count":"[0-9]*"' | grep -o '[0-9]*' || echo "0")
echo "  Classes: $class_count"

# Query 3: Count properties
echo "3. Number of properties:"
prop_count=$(curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
    --data-urlencode "query=SELECT (COUNT(DISTINCT ?prop) AS ?count) WHERE { ?prop a <http://www.w3.org/2002/07/owl#ObjectProperty> }" \
    -H "Accept: application/sparql-results+json" | grep -o '"count":"[0-9]*"' | grep -o '[0-9]*' || echo "0")
echo "  Object Properties: $prop_count"

# Query 4: Sample classes
echo "4. Sample classes:"
curl -s -G "http://localhost:7200/repositories/ontomesh/statements" \
    --data-urlencode "query=SELECT ?class ?label WHERE { ?class a <http://www.w3.org/2002/07/owl#Class> ; <http://www.w3.org/2000/01/rdf-schema#label> ?label } LIMIT 5" \
    -H "Accept: application/sparql-results+json" | jq -r '.results.bindings[] | "  - " + .label.value + " (" + .class.value + ")"' 2>/dev/null || echo "  Could not retrieve sample classes"

echo ""
echo "Ontology test completed successfully!"
echo ""
echo "You can now:"
echo "1. Access GraphDB workbench: http://localhost:7300"
echo "2. Query the ontology using SPARQL"
echo "3. Use the ontology in your ONE Record applications" 