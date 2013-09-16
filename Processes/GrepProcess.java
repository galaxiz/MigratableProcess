package Processes;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;

import IOlib.TransactionalFileInputStream;
import IOlib.TransactionalFileOutputStream;

public class GrepProcess implements MigratableProcess {
	private TransactionalFileInputStream inFile;
	private TransactionalFileOutputStream outFile;
	private String query;
	private String[] args;

	private volatile boolean running;
	private volatile boolean suspending;

	public GrepProcess(String args[]) throws Exception {
		this.args = args;

		if (args.length != 3) {
			System.out
					.println("usage: GrepProcess <queryString> <inputFile> <outputFile>");
			throw new Exception("Invalid Arguments");
		}

		query = args[0];
		inFile = new TransactionalFileInputStream(args[1]);
		outFile = new TransactionalFileOutputStream(args[2], false);
	}

	public void run() {
		running = true;
		PrintStream out = new PrintStream(outFile);
		DataInputStream in = new DataInputStream(inFile);

		try {
			while (!suspending) {
				String line = in.readLine();

				if (line == null)
					break;

				if (line.contains(query)) {
					System.out.println(line);
					out.println(line);
				}

				// Make grep take longer so that we don't require extremely
				// large files for interesting results
				try {
					Thread.sleep(1 * 1000);
				} catch (InterruptedException e) {
					// ignore it
				}
			}
		} catch (EOFException e) {
			// End of File
		} catch (IOException e) {
			System.out.println("GrepProcess: Error: " + e);
		}

		if (suspending) {
			System.out.println("GrepProcess suspended!");
		} else {
			System.out.println("GrepProcess complete!");
			running = false;
		}

		suspending = false;
	}

	public void suspend() {
		while (running) {
			suspending = true;
			inFile.setMigrated(true);
			outFile.setMigrated(true);
			while (suspending)
				;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("GrepProcess");

		for (String argv : args) {
			sb.append(" " + argv);
		}
		return sb.toString();
	}

}
