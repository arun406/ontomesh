package com.ontomesh.onerecord.server.model;

import com.ontomesh.onerecord.server.converter.IRIConverter;
import com.ontomesh.onerecord.server.converter.LocalDateTimeAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.eclipse.rdf4j.model.IRI;

import java.time.LocalDateTime;

/**
 * Represents the Metadata of a Logistics Object.
 *
 * @author arun
 */
@Entity
@Table(name = "LO_METADATA")
@Data
public class LogisticsObjectMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator")
    @SequenceGenerator(
            name = "id_generator",
            sequenceName = "METADATA_SEQ",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Long id;
    @Column(name = "LO_IRI", nullable = false)
    @Convert(converter = IRIConverter.class)
    private IRI loIRI;

    @Column(name = "LO_TYPE", nullable = false)
    @Convert(converter = IRIConverter.class)
    private IRI type;

    @Column(name = "LATEST_REVISION", nullable = false)
    private int latestRevision;
    
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;

    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime createdAt;
    private String createdBy;
}
