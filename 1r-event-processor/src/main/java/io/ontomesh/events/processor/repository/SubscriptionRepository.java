package io.ontomesh.events.processor.repository;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionRepository extends BaseRepository {


    protected SubscriptionRepository(RepositoryConnection connection) {
        super(connection);
    }

    /**
     * List all Subscriptions for a lo id or lo type.
     *
     * @param loId
     * @param loType
     * @return
     */
    public Model listAllSubscribers(IRI loId, IRI loType) {

        final String LIST_ALL_SUBSCRIBERS_QUERY = StringTemplate.STR
                ."""
                            PREFIX cargo: <https://onerecord.iata.org/ns/cargo#>
                            PREFIX api:  <https://onerecord.iata.org/ns/api#>
                            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                            CONSTRUCT  { ?subscription 	api:hasTopic ?topic ;
                                                        api:hasRequestStatus api:REQUEST_ACCEPTED ;
                                                        api:SubscriptionRequest  ?subject ;
                                                        api:hasTopicType ?topicType ;
                                                        api:hasSubscriber ?subscriber ;
                                                        api:includeSubscriptionEventType ?subscriptionEventTypes }
                            WHERE {
                                ?subject rdf:type api:SubscriptionRequest ;
                                         api:hasRequestStatus api:REQUEST_ACCEPTED ;
                                         api:hasSubscription ?subscription .
                                ?subscription rdf:type api:Subscription ;
                                              api:hasSubscriber ?subscriber ;
                                              api:hasTopic ?topic ;
                                              api:hasTopicType ?topicType .
                                optional { ?subscription api:includeSubscriptionEventType ?subscriptionEventTypes } .
                                FILTER(( ?topicType = api:LOGISTICS_OBJECT_IDENTIFIER  &&  IRI(?topic) = IRI("\{loId}")) ||
                                       ( ?topicType = api:LOGISTICS_OBJECT_TYPE  &&  IRI(?topic) = IRI("\{loType}" )))
                            }
                """;

        GraphQueryResult queryResult = getConnection().prepareGraphQuery(LIST_ALL_SUBSCRIBERS_QUERY).evaluate();
        Model model = QueryResults.asModel(queryResult);
        return model;
    }
}
