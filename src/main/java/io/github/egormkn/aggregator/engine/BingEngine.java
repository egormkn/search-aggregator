package io.github.egormkn.aggregator.engine;

import akka.actor.Props;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BingEngine extends Engine {

    private static final String TAG = "Bing";

    static public Props props() {
        return Props.create(BingEngine.class);
    }

    @Override
    protected Response query(Request request) throws Exception {
        List<String> result = new ArrayList<>();
        Document doc = Jsoup.connect("https://www.bing.com/search?q=" + request.getQuery()).get();
        Element content = doc.selectFirst("#b_results");
        Elements items = content.select(".b_algo");
        for (Element item : items) {
            Element link = item.selectFirst("a");
            String title = link.text();
            String url = link.absUrl("href");
            result.add(String.format("%s [%s]", title, url));
        }
        return new Response(TAG, result);
    }
}
