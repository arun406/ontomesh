package io.ontomesh.onerecord.client.service;

import org.eclipse.rdf4j.model.IRI;

public interface INotificationClient {

    void sendNotification(IRI client, String notification);
}
