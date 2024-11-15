/*
 * This source file was generated by the Gradle 'init' task
 */
package utb.fai;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class App {

	public static void main(String[] args) {
		URI uri;
		int maxDepth = 2;
		int debugLevel = 0;

		ConcurrentLinkedQueue<URIinfo> foundURIs = new ConcurrentLinkedQueue<>();
		ConcurrentSkipListSet<URI> visitedURIs = new ConcurrentSkipListSet<>();
		HashMap<String, Integer> wordFrequency = new HashMap<>();

		List<Future<?>> futures = new ArrayList<>();
		ExecutorService executor = Executors.newFixedThreadPool(10);

		try {
			if (args.length < 1) {
				System.err.println("Missing parameter - start URL");
				return;
			}
			uri = new URI(args[0] + "/");

			// uri = new URI("https://wokwi.com/");

			foundURIs.add(new URIinfo(uri, 0));
			visitedURIs.add(uri);

			if (args.length > 1) {
				try {
					maxDepth = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					System.err.println("Invalid maxDepth parameter, using default value: " + maxDepth);
				}
			}

			if (args.length > 2) {
				try {
					debugLevel = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					System.err.println("Invalid debugLevel parameter, using default value: " + debugLevel);
				}
			}

			ParserCallback callBack = new ParserCallback(visitedURIs, foundURIs, maxDepth, debugLevel, wordFrequency);

			while (!foundURIs.isEmpty() || !futures.isEmpty()) {
				URIinfo URIinfo = foundURIs.poll();
				if (URIinfo != null) {
					callBack.depth = URIinfo.depth;
					callBack.pageURI = URIinfo.uri;
					URI current_uri = URIinfo.uri;
					System.err.println("Analyzing " + current_uri);

					Future<?> future = executor.submit(() -> {
						try {
							Document doc = Jsoup.connect(current_uri.toString()).get();
							callBack.parse(doc);
						} catch (Exception e) {
							System.err.println("Error loading page: " + e.getMessage());
						}
					});
					futures.add(future);
				}

				futures.removeIf(Future::isDone);

				if (foundURIs.isEmpty()) {
					Thread.sleep(50);
					if (debugLevel > 0)
						System.err.println("Waiting...");
				} else if (debugLevel > 0)
					System.err.println("Not empty, analyzing next.");
			}

			System.err.println("Shutdown");
			executor.shutdown();
            executor.awaitTermination(7, TimeUnit.SECONDS);
			
			wordFrequency.entrySet().stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.limit(20)
				.forEach(entry -> System.out.println(entry.getKey() + ";" + entry.getValue()));

		} catch (Exception e) {
			System.err.println("Zachycena neošetřená výjimka, končíme...");
			e.printStackTrace();
		}
	}

}
