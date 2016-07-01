package com.flatironschool.javacs;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;


public class WikiCrawler {
	// keeps track of where we started
	private final String source;
	
	// the index where the results go
	private JedisIndex index;
	
	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();
	
	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}

	/**
	 * Gets a URL from the queue and indexes it.
	 * @param boolean
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {

        // FILL THIS IN!
		if(queue.isEmpty()) return null;

		String url = queue.poll();
		Elements contents ;

		if(testing) {
			//read the contents of the page using wikifetcher.readwikipedia
			contents = wf.readWikipedia(url);
		}else {

			if (index.isIndexed(url)) {
				System.out.println("Already indexed");
				return null;
			} else {
				contents = wf.fetchWikipedia(url);
			}

		}

		index.indexPage(url, contents);
		queueInternalLinks(contents);

		return url;
	}


	private static void fillQueue(Queue<String> queue, Element paragraph) {

		Elements elets = paragraph.select("a[href]");

		for(Element elem : elets) {

			if (elem.hasAttr("href")) {

				String newURL = elem.attr("href");
				if(newURL.startsWith("/wiki/")) {
					String addedURL = "https://en.wikipedia.org" + newURL;
					queue.add(addedURL);
				}

			}
		}
	}



	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
        // FILL THIS IN!
		for(Element paragraph : paragraphs) {
			fillQueue(queue, paragraph);
		}
	}

	public static void main(String[] args) throws IOException {
		
		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);

            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
            break;
		} while (res == null);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}
}
