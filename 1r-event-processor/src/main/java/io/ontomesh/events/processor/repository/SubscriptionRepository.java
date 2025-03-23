package io.ontomesh.events.processor.repository;


import io.ontomesh.core.rdf.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.springframework.stereotype.Component;

/**
 * Subscription repository.
 */
@Component
@Slf4j
public class SubscriptionRepository extends BaseRepository {

    /**
     * List all subscribers.
     *
     * @param loId   logistics object id.
     * @param loType logistics object type.
     * @return model.
     */
    public Model listAllSubscribers(IRI loId, IRI loType) {

        final String LIST_ALL_SUBSCRIBERS_QUERY = """
                PREFIX cargo: <https://onerecord.iata.org/ns/cargo#>
                PREFIX api:  <https://onerecord.iata.org/ns/api#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                CONSTRUCT  {
                    ?subscription api:hasTopic ?topic ;
                                  api:hasRequestStatus api:REQUEST_ACCEPTED ;
                                  api:SubscriptionRequest ?subject ;
                                  api:hasTopicType ?topicType ;
                                  api:hasSubscriber ?subscriber ;
                                  api:includeSubscriptionEventType ?subscriptionEventTypes
                }
                WHERE {
                    ?subject rdf:type api:SubscriptionRequest ;
                             api:hasRequestStatus api:REQUEST_ACCEPTED ;
                             api:hasSubscription ?subscription .
                    ?subscription rdf:type api:Subscription ;
                                  api:hasSubscriber ?subscriber ;
                                  api:hasTopic ?topic ;
                                  api:hasTopicType ?topicType .
                    OPTIONAL { ?subscription api:includeSubscriptionEventType ?subscriptionEventTypes } .
                    FILTER((?topicType = api:LOGISTICS_OBJECT_IDENTIFIER && IRI(?topic) = IRI("%s")) ||
                           (?topicType = api:LOGISTICS_OBJECT_TYPE && IRI(?topic) = IRI("%s")))
                }
                """.formatted(loId, loType);

        GraphQueryResult queryResult = getConnection().prepareGraphQuery(LIST_ALL_SUBSCRIBERS_QUERY).evaluate();
        return QueryResults.asModel(queryResult);
    }
}
