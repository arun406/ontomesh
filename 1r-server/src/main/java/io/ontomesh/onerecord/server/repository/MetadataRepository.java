package io.ontomesh.onerecord.server.repository;

import io.ontomesh.onerecord.server.model.LogisticsObjectMetadata;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadataRepository extends JpaRepository<LogisticsObjectMetadata, Long> {
    public LogisticsObjectMetadata findByLoIRI(IRI loIRI);
}
