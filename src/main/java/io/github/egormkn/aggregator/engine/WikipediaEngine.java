package io.github.egormkn.aggregator.engine;

import akka.actor.Props;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WikipediaEngine extends Engine {

    private static final String TAG = "Wikipedia";

    static public Props props() {
        return Props.create(WikipediaEngine.class);
    }

    @Override
    protected Response query(Request request) throws Exception {
        List<String> result = new ArrayList<>();
        Document doc = Jsoup.connect("https://ru.wikipedia.org/w/index.php?fulltext=1&search=" + request.getQuery()).get();
        Element content = doc.selectFirst(".mw-search-results");
        Elements items = content.select(".mw-search-result");
        for (Element item : items) {
            Element link = item.selectFirst(".mw-search-result-heading a");
            String title = link.text();
            String url = link.absUrl("href");
            result.add(String.format("%s [%s]", title, url));
        }
        return new Response(TAG, result);
    }
}
