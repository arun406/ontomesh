package io.ontomesh.onerecord.server.domain;

import org.eclipse.rdf4j.model.Literal;

public interface IVersionedLogisticsObject extends ILogisticsObject {
    Literal revision();

}
