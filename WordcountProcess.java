import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 */

/**
 * @author air
 * 
 */
public class WordcountProcess implements MigratableProcess {

	private TransactionalFileInputStream inFile;
	private TransactionalFileOutputStream outFile;
	private HashMap<String, Integer> wordMap;
	private String[] args;
	private volatile boolean running;
	private volatile boolean suspending;

	/**
	 * @throws Exception
	 * 
	 */
	public WordcountProcess(String args[]) throws Exception {
		if (args.length != 2) {
			System.out
					.println("usage: WordcountProcess <inputFile> <ouputFile>");
			throw new Exception("Invalid Arguments");
		}

		this.args = args;
		inFile = new TransactionalFileInputStream(args[0]);
		outFile = new TransactionalFileOutputStream(args[1], false);
		wordMap = new HashMap<String, Integer>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		running = true;
		PrintStream out = new PrintStream(outFile);
		DataInputStream in = new DataInputStream(inFile);

		String curLine;
		try {
			while (!suspending && ((curLine = in.readLine()) != null)) {
				Pattern wordpattern = Pattern.compile("([\\w]+)");
				Matcher wordmatcher = wordpattern.matcher(curLine);
				while (wordmatcher.find()) {
					String word = wordmatcher.group(1);
					if (wordMap.containsKey(word)) {
						wordMap.put(word, wordMap.get(word) + 1);
					} else {
						wordMap.put(word, 1);
					}
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (suspending) {
				suspending = false;
				System.out.println("WordcountProcess suspended!");
			} else {
				running = false;
				for (String key : wordMap.keySet()) {
					out.println(key + " " + wordMap.get(key));
				}
				System.out.println("WordcountProcess completed");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see MigratableProcess#suspend()
	 */
	@Override
	public void suspend() {
		if (running) {
			suspending = true;
			inFile.setMigrated(true);
			outFile.setMigrated(true);
			while (suspending)
				;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("WordcountProcess");

		for (String argv : args) {
			sb.append(" " + argv);
		}
		return sb.toString();
	}

}
