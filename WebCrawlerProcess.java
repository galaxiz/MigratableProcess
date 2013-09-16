import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a simple web crawler which 
 */

/**
 * @author Shiwei Dong
 * 
 */
public class WebCrawlerProcess implements MigratableProcess {

	private Queue<URL> urlQueue;
	private HashMap<URL, Boolean> urlwithoutdup;
	private TransactionalFileOutputStream outFile;

	private volatile boolean suspending;

	/**
	 * @throws Exception
	 * 
	 */
	public WebCrawlerProcess(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("usage: MigratableProcess <URL> <ouputFile>");
			throw new Exception("Invalid Arguments");
		}

		urlQueue = new LinkedList<URL>();
		urlwithoutdup = new HashMap<URL, Boolean>();

		try {
			URL url = new URL(args[0]);
			urlQueue.add(url);
			urlwithoutdup.put(url, true);
		} catch (MalformedURLException e) {
			System.out.println("Exception: " + e);
			e.printStackTrace();
		}

		outFile = new TransactionalFileOutputStream(args[1], false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		PrintWriter out = new PrintWriter(outFile);

		while (!urlQueue.isEmpty() && !suspending) {
			try {
				URL currentURL = urlQueue.poll();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(currentURL.openStream()));
				String curLine;
				while ((curLine = reader.readLine()) != null) {
					// find if there are URLs. if exist and not duplicated add
					// to queue and print it to output file""
					Pattern linkPattern = Pattern
							.compile("(?i)<a([^>]+)>(.+?)</a>");
					Pattern urlPattern = Pattern
							.compile("\\s*(?i)href\\s*=\\s*(\"http://[^\"]*\"|http://'[^']*'|http://[^'\">\\s]+)");
					Matcher linkMatcher = linkPattern.matcher(curLine);

					if (linkMatcher.find()) {
						String link = linkMatcher.group(1);
						Matcher urlMatcher = urlPattern.matcher(link);
						if (urlMatcher.find()) {
							String urlAddress = urlMatcher.group(1);
							urlAddress = urlAddress.replace("\"", "");
							urlAddress = urlAddress.replace("'", "");
							URL tmpURL = new URL(urlAddress);
							if (urlwithoutdup.containsKey(tmpURL))
								continue;
							else
								urlwithoutdup.put(tmpURL, true);

							System.out.println("Found a new link, the URL is: "
									+ urlAddress);
							urlQueue.add(tmpURL);
							out.write(tmpURL.toString() + "\n");
						}
					}
				}
			} catch (MalformedURLException e) {
				// just ignore, some links just cannot be open
				//System.out.println("Exception: " + e);
				//e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		suspending = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see MigratableProcess#suspend()
	 */
	@Override
	public void suspend() {
		suspending = true;
		outFile.setMigrated(true);
		while (suspending)
			;

	}

	public String toString() {
		return null;
	}

}
