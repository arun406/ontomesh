package com.ontomesh.onerecord.server.services;

import com.ontomesh.onerecord.server.domain.LogisticsObject;

public interface ActionRequestProcessor {
    void process(LogisticsObject actionRequest);
}
