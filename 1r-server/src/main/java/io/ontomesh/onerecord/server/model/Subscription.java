package io.ontomesh.onerecord.server.model;

import io.ontomesh.onerecord.server.api.enums.NotificationEventType;
import io.ontomesh.onerecord.server.api.enums.SubscriptionEntityType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

@Slf4j
public record Subscription(
        IRI id,
        IRI subscriberId,
        SubscriptionEntityType subscriptionEntityType,
        List<NotificationEventType> eventTypes,
        IRI subscriptionEntityId) {
}
