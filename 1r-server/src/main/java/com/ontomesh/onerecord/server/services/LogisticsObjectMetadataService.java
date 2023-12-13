package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.model.LogisticsObjectMetadata;
import com.ontomesh.onerecord.server.repository.MetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author arun
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsObjectMetadataService {

    private final MetadataRepository metadataRepository;

    public Long createMetadata(LogisticsObjectMetadata metadata) {
        this.metadataRepository.save(metadata);
        return metadata.getId();
    }
}
