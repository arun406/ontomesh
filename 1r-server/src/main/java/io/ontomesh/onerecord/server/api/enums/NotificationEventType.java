package io.ontomesh.onerecord.server.api.enums;

import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;

import java.util.HashMap;
import java.util.Map;

public enum NotificationEventType implements Labeled {
    LOGISTICS_OBJECT_CREATED(API.LOGISTICS_OBJECT_CREATED, "Logistics object created"),
    LOGISTICS_OBJECT_UPDATED(API.LOGISTICS_OBJECT_UPDATED, "Logistics object updated"),
    LOGISTICS_EVENT_RECEIVED(API.LOGISTICS_EVENT_RECEIVED, "Logistics event received"),
    CHANGE_REQUEST_PENDING(API.CHANGE_REQUEST_PENDING, "Change request pending"),
    CHANGE_REQUEST_ACCEPTED(API.CHANGE_REQUEST_ACCEPTED, "Change request accepted"),
    CHANGE_REQUEST_REJECTED(API.CHANGE_REQUEST_REJECTED, "Change request rejected"),
    CHANGE_REQUEST_FAILED(API.CHANGE_REQUEST_FAILED, "Change request failed"),
    CHANGE_REQUEST_REVOKED(API.CHANGE_REQUEST_REVOKED, "Change request revoked"),
    ACCESS_DELEGATION_REQUEST_PENDING(API.ACCESS_DELEGATION_REQUEST_PENDING, "Access delegation request pending"),
    ACCESS_DELEGATION_REQUEST_ACCEPTED(API.ACCESS_DELEGATION_REQUEST_ACCEPTED, "Access delegation request accepted"),
    ACCESS_DELEGATION_REQUEST_REJECTED(API.ACCESS_DELEGATION_REQUEST_REJECTED, "Access delegation request rejected"),
    ACCESS_DELEGATION_REQUEST_FAILED(API.ACCESS_DELEGATION_REQUEST_FAILED, "Access delegation request failed"),
    ACCESS_DELEGATION_REQUEST_REVOKED(API.ACCESS_DELEGATION_REQUEST_REVOKED, "Access delegation request revoked"),
    SUBSCRIPTION_REQUEST_PENDING(API.SUBSCRIPTION_REQUEST_PENDING, "Subscription request pending"),
    SUBSCRIPTION_REQUEST_ACCEPTED(API.SUBSCRIPTION_REQUEST_ACCEPTED, "Subscription request accepted"),
    SUBSCRIPTION_REQUEST_REJECTED(API.SUBSCRIPTION_REQUEST_REJECTED, "Subscription request rejected"),
    SUBSCRIPTION_REQUEST_FAILED(API.SUBSCRIPTION_REQUEST_FAILED, "Subscription request failed"),
    SUBSCRIPTION_REQUEST_REVOKED(API.SUBSCRIPTION_REQUEST_REVOKED, "Subscription request revoked"),
    ;

    private static final Map<IRI, NotificationEventType> BY_NOTIFICATION_EVENT_TYPE = new HashMap<>();

    static {
        for (NotificationEventType e : values()) {
            BY_NOTIFICATION_EVENT_TYPE.put(e.notificationType, e);
        }
    }

    private final IRI notificationType;
    private final String codeListName;

    NotificationEventType(IRI notificationType, String codeListName) {
        this.notificationType = notificationType;
        this.codeListName = codeListName;
    }

    public static NotificationEventType valueOf(IRI iri) {
        return BY_NOTIFICATION_EVENT_TYPE.get(iri);
    }

    @Override
    public IRI label() {
        return this.notificationType;
    }

    public String getCodeListName() {
        return this.codeListName;
    }
}
