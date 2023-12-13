package io.ontomesh.onerecord.server.services;

import io.ontomesh.onerecord.server.domain.LogisticsObject;

public interface ActionRequestProcessor {
    void process(LogisticsObject actionRequest);
}
