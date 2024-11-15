package utb.fai;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


class ParserCallback {

    /**
     * pageURI bude obsahovat URI aktuální parsované stránky. Budeme
     * jej využívat pro resolving všech URL, které v kódu stránky najdeme
     * - předtím, než nalezené URL uložíme do foundURLs, musíme z něj udělat
     * absolutní URL!
     */
    URI pageURI;

    /**
     * depth bude obsahovat aktuální hloubku zanoření
     */
    int depth = 0, maxDepth = 5;

    /**
     * visitedURLs je množina všech URL, které jsme již navštívili
     * (parsovali). Pokud najdeme na stránce URL, který je v této množině,
     * nebudeme jej už dále parsovat
     */
    ConcurrentSkipListSet<URI> visitedURIs;

    /**
     * foundURLs jsou všechna nová (zatím nenavšívená) URL, která na stránce
     * najdeme. Poté, co projdeme celou stránku, budeme z tohoto seznamu
     * jednotlivá URL brát a zpracovávat.
     */
    ConcurrentLinkedQueue<URIinfo> foundURIs;

    /** pokud debugLevel>1, budeme vypisovat debugovací hlášky na std. error */
    int debugLevel = 0;

    HashMap<String, Integer> wordFrequency = new HashMap<>();

    ParserCallback(ConcurrentSkipListSet<URI> visitedURIs, ConcurrentLinkedQueue<URIinfo> foundURIs, int maxDepth, int debugLevel, HashMap<String, Integer> wordFrequency) {
        this.foundURIs = foundURIs;
        this.visitedURIs = visitedURIs;
        this.maxDepth = maxDepth;
        this.debugLevel = debugLevel;
        this.wordFrequency = wordFrequency;
    }

    public void parse(Document doc) {
        try {
            if (depth < maxDepth) {
                Elements links = doc.select("a[href], frame[src]");
                for (Element link : links) {
                    String href = link.is("a") ? link.attr("abs:href") : link.attr("abs:src");
                    if (href != null) {
                        try {
                            URI uri = pageURI.resolve(href.replace(" ", "%20"));
                            if (!uri.isOpaque() && !visitedURIs.contains(uri)) {
                                visitedURIs.add(uri);
                                foundURIs.add(new URIinfo(uri, depth + 1));
                                if (debugLevel > 0)
                                    System.err.println("Adding URI: " + uri.toString());
                            }
                        } catch (Exception e) {
                            System.err.println("Invalid URI: " + href);
                            e.printStackTrace();
                        }
                    }
                }
            }
    
            String visibleText = doc.body().text();
            handleText(visibleText);
        } catch (Exception e) {
            System.err.println("Error while parsing document: " + e.getMessage());
        }
    }

    /******************************************************************
     * V metodě handleText bude probíhat veškerá činnost, související se
     * zjišťováním četnosti slov v textovém obsahu HTML stránek.
     * IMPLEMENTACE TÉTO METODY JE V TÉTO ÚLOZE VAŠÍM ÚKOLEM !!!!
     * Možný postup:
     * Ve třídě Parser (klidně v její metodě main) si vytvořte vyhledávací tabulku
     * =instanci třídy HashMap<String,Integer> nebo TreeMap<String,Integer>.
     * Do této tabulky si ukládejte dvojice klíč-data, kde
     * klíčem jsou jednotlivá slova z textového obsahu HTML stránek,
     * data typu Integer bude dosavadní počet výskytu daného slova v
     * HTML stránkách.
     *******************************************************************/
    public void handleText(String text) {
        // String text = data.toLowerCase();
        if (debugLevel == 1)
            System.err.println("handleText: " + (text.length() > 20 ? text.substring(0, 20) : text) + "...");
        if (debugLevel > 1)
            System.err.println("handleText: " + text);
        String[] words = text.split("\\s+");

        for (String word : words) {
            if (!word.isBlank()) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                if (debugLevel > 1)
                    System.err.println("Vložení slova: " + word);
            }
        }
    }
}
