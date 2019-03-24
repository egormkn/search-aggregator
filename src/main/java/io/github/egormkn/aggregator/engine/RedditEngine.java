package io.github.egormkn.aggregator.engine;

import akka.actor.Props;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class RedditEngine extends Engine {

    private static final String TAG = "Reddit";

    static public Props props() {
        return Props.create(RedditEngine.class);
    }

    @Override
    protected Response query(Request request) throws Exception {
        List<String> result = new ArrayList<>();
        Document doc = Jsoup.connect("https://www.reddit.com/search?q=" + request.getQuery()).get();
        Element content = doc.body();
        Elements items = content.select(".Post");
        for (Element item : items) {
            Element link = item.selectFirst("a");
            String title = link.text();
            String url = link.absUrl("href");
            result.add(String.format("%s [%s]", title, url));
        }
        return new Response(TAG, result);
    }
}
