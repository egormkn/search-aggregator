package io.github.egormkn.aggregator;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import io.github.egormkn.aggregator.engine.*;
import io.github.egormkn.aggregator.engine.Engine.Request;
import io.github.egormkn.aggregator.engine.Engine.Response;

import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

public class Aggregator extends AbstractActorWithTimers {

    private ActorRef parent;

    private Set<ActorRef> workers = new HashSet<>();

    private List<Response> responses = new ArrayList<>();

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final Map<String, Class<? extends Engine>> engines = new HashMap<>();

    static {
        engines.put("Yandex", YandexEngine.class);
        engines.put("Google", GoogleEngine.class);
        engines.put("Bing", BingEngine.class);
        engines.put("Wikipedia", WikipediaEngine.class);
        engines.put("Reddit", RedditEngine.class);
    }

    private SupervisorStrategy strategy = new OneForOneStrategy(0, Duration.ofSeconds(10), DeciderBuilder.
            match(RuntimeException.class, e -> {
                Logger.getAnonymousLogger().info("Evaluation of a top level expression failed, restarting.");
                return SupervisorStrategy.restart();
            }).
            match(ArithmeticException.class, e -> {
                Logger.getAnonymousLogger().info("Evaluation failed because of: " + e.getMessage());
                processFailure(sender(), e);
                return SupervisorStrategy.stop();
            }).
            match(Throwable.class, e -> {
                Logger.getAnonymousLogger().info("Unexpected failure: " + e.getMessage());
                processFailure(sender(), e);
                return SupervisorStrategy.stop();
            })
            //.matchAny(e -> SupervisorStrategy.escalate())
            .build());

    public Aggregator() {}

    public Aggregator(ActorRef parent) {
        this.parent = parent;
    }

    public static Props props(ActorRef parent) {
        return Props.create(Aggregator.class, parent);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    protected void processRequest(Request request) {
        Logger.getAnonymousLogger().info("Got request: " + request.getQuery());
        parent = getSender();

        engines.forEach((name, type) -> {
            ActorRef worker = getContext().actorOf(Props.create(type), name);
            worker.tell(request, getSelf());
            workers.add(worker);
        });

        getTimers().startSingleTimer("TIMEOUT", new Timeout(), TIMEOUT);
    }

    private void processResponse(Response response) {
        Logger.getAnonymousLogger().info("Got response: " + String.format("%s (%d results)", response.getTag(), response.getLinks().size()));

        responses.add(response);

        ActorRef worker = getSender();
        workers.remove(worker);
        getContext().stop(worker);

        if (workers.isEmpty()) {
            processTimeout(null);
        }
    }

    private void processTimeout(Timeout ignored) {
        for (ActorRef worker : workers) {
            getContext().stop(worker);
        }
        parent.tell(new Batch(responses), getSelf());
        getContext().stop(getSelf());
    }

    private void processFailure(ActorRef worker, Throwable failure) {
        parent.tell(new Status.Failure(failure), self());
        workers.remove(worker);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, this::processRequest)
                .match(Response.class, this::processResponse)
                .match(Timeout.class, this::processTimeout)
                .build();
    }

    protected static final class Timeout {}

    public static class Batch {
        private final List<Response> responses;

        public Batch(List<Response> responses) {
            this.responses = responses;
        }

        public List<Response> getResponses() {
            return responses;
        }
    }
}
