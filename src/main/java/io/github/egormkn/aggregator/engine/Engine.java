package io.github.egormkn.aggregator.engine;

import akka.actor.AbstractActor;
import akka.actor.Status;

import java.util.List;

public abstract class Engine extends AbstractActor {

    protected abstract Response query(Request request) throws Exception;

    protected void processRequest(Request request) {
        try {
            Response response = query(request);
            getSender().tell(response, self());
        } catch (Exception e) {
            getSender().tell(new Status.Failure(e), self());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, this::processRequest)
                .build();
    }

    public static class Request {
        private final String query;

        public Request(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }

    public static class Response {
        private final List<String> links;
        private final String tag;

        public Response(String tag, List<String> links) {
            this.tag = tag;
            this.links = links;
        }

        public List<String> getLinks() {
            return links;
        }

        public String getTag() {
            return tag;
        }
    }
}
