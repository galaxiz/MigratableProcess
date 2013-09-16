package testprocesses;

import iolib.TransactionalFileInputStream;
import iolib.TransactionalFileOutputStream;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processinterface.MigratableProcess;

/**
 * This is a word count process which read from an input file and count the
 * number of each word in it.
 * 
 * @author Shiwei Dong
 * 
 */
public class WordcountProcess implements MigratableProcess {

	private TransactionalFileInputStream inFile;
	private TransactionalFileOutputStream outFile;
	// Each word extract from the input file will be added to this hashmap
	private HashMap<String, Integer> wordMap;
	private String[] args;

	private volatile boolean running;
	private volatile boolean suspending;

	/**
	 * WordcountProcess(String args[]) throws Exception
	 * 
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
				// Use sleep to make it run longer
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
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * Suspend the current running process and change it into a safe state for
	 * migration
	 * 
	 * @see MigratableProcess#suspend()
	 */
	@Override
	public void suspend() {
		suspending = true;
		inFile.setMigrated(true);
		outFile.setMigrated(true);
		while (suspending && running)
			;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * A toString method is handy for debugging
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder("WordcountProcess");

		for (String argv : args) {
			sb.append(" " + argv);
		}
		return sb.toString();
	}

}
