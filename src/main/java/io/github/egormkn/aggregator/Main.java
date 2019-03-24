package io.github.egormkn.aggregator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.Util;
import akka.pattern.Patterns;
import akka.util.Timeout;
import io.github.egormkn.aggregator.engine.Engine;
import io.github.egormkn.aggregator.engine.Engine.Response;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("aggregator-system");

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        do {
            try {
                String query = in.readLine();
                if (query.equals("exit")) break;

                ActorRef aggregatorEngine =
                        system.actorOf(Props.create(Aggregator.class), "aggregator-engine");

                Engine.Request request = new Engine.Request(query);

                FiniteDuration duration = Duration.create(20, TimeUnit.SECONDS);
                Future<Aggregator.Batch> responseFuture = Patterns.ask(aggregatorEngine, request, new Timeout(duration)).mapTo(Util.classTag(Aggregator.Batch.class));
                List<Response> result = Await.result(responseFuture, duration).getResponses();
                System.err.flush();
                System.out.flush();
                System.out.format("Got result:%n");
                result.forEach(response -> {
                    System.out.println(response.getTag());
                    response.getLinks().forEach(System.out::println);
                    System.out.println();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (true);

        try {
            Await.ready(system.terminate(), Duration.Inf());
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
