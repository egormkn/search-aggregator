package io.github.egormkn.aggregator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import io.github.egormkn.aggregator.engine.Engine;
import io.github.egormkn.aggregator.engine.WikipediaEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class AkkaQuickstartTest {
    static ActorSystem system;

    @BeforeAll
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterAll
    public static void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testRealRequests() {
        final TestKit testKit = new TestKit(system);
        final ActorRef aggregator = system.actorOf(Props.create(Aggregator.class));
        aggregator.tell(new Engine.Request("AKKA"), testKit.getRef());
        Aggregator.Batch response = testKit.expectMsgClass(Aggregator.Batch.class);
        Assertions.assertTrue(!response.getResponses().isEmpty());
    }

    @Test
    public void testTimeout() {
        final TestKit testKit = new TestKit(system);
        final ActorRef aggregator = system.actorOf(Props.create(Aggregator.class, () ->
                new Aggregator(testKit.getRef()) {
                    @Override
                    protected void processRequest(Engine.Request request) {
                        getTimers().startSingleTimer("TIMEOUT", new Aggregator.Timeout(), Duration.ofSeconds(5));
                    }
                }));
        aggregator.tell(new Engine.Request("AKKA"), testKit.getRef());
        testKit.within(
                Duration.ZERO,
                Duration.ofSeconds(6),
                () -> {
                    Assertions.assertTrue(testKit.expectMsgClass(Aggregator.Batch.class).getResponses().isEmpty());
                    return null;
                });
    }

    @Test
    public void testWikipedia() {
        final TestKit testKit = new TestKit(system);
        final ActorRef wikipediaEngine = system.actorOf(Props.create(WikipediaEngine.class));
        wikipediaEngine.tell(new Engine.Request("AKKA"), testKit.getRef());
        Engine.Response response = testKit.expectMsgClass(Engine.Response.class);
        Assertions.assertEquals(response.getTag(), "Wikipedia");
        Assertions.assertTrue(!response.getLinks().isEmpty());
    }
}
