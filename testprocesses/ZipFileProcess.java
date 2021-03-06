package testprocesses;

import iolib.TransactionalFileOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import processinterface.MigratableProcess;

/**
 * This is a zip file process, which take in a file or directory and compress
 * into a zip file.
 * 
 * @author Shiwei Dong
 * 
 */
public class ZipFileProcess implements MigratableProcess {

	private List<File> fileList;
	private TransactionalFileOutputStream outFile;
	private String outFileName;
	private String inFileName;
	private String[] args;

	private volatile boolean running;
	private volatile boolean suspending;

	/**
	 * @throws Exception
	 */
	public ZipFileProcess(String[] args) throws Exception {
		if (args.length != 2) {
			System.out
					.println("usage: ZipFileProcess <inputFile> <outputFile>");
			throw new Exception("Invalid Arguments");
		}
		this.args = args;

		inFileName = args[0];
		outFileName = args[1];

		if (!outFileName.endsWith(".zip")) {
			System.out.println("OutputFile should take a \".zip\" suffix");
			throw new Exception("Invalid OutputFile Name");
		}

		outFile = new TransactionalFileOutputStream(outFileName, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		running = true;

		byte[] buffer = new byte[1024];

		try {
			ZipOutputStream zout = new ZipOutputStream(outFile);

			fileList = new LinkedList<File>();
			generateList(inFileName);

			for (Iterator<File> it = fileList.iterator(); it.hasNext()
					&& !suspending;) {
				File curFile = it.next();
				ZipEntry ze = new ZipEntry(curFile.getName());
				zout.putNextEntry(ze);
				FileInputStream inFile = new FileInputStream(
						curFile.getAbsolutePath());
				int len;
				while ((len = inFile.read(buffer)) > 0) {
					zout.write(buffer, 0, len);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				it.remove();
				zout.closeEntry();
				inFile.close();
			}

			if (suspending) {
				System.out.println("Zipping Suspended");
			} else {
				running = false;
				System.out.println("Zipping Compliete!");
			}

			suspending = false;

			zout.finish();
		} catch (IOException e) {
			System.out.println("ZipFileProcess Exception" + e);
			e.printStackTrace();
		}

	}

	/**
	 * generateList(String startFile)
	 * 
	 * generate a file list from directory
	 * 
	 * Sep 17, 2013
	 * 
	 * @param startFile
	 */
	public void generateList(String startFile) {
		File curFile = new File(startFile);
		if (curFile.isDirectory()) {
			for (File sub : curFile.listFiles()) {
				if (sub.isFile()) {
					fileList.add(sub);
				} else {
					generateList(sub.getName());
				}
			}
		}

		if (curFile.isFile()) {
			fileList.add(curFile);
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
		StringBuilder sb = new StringBuilder("ZipFileProcess");

		for (String argv : args) {
			sb.append(" " + argv);
		}
		return sb.toString();
	}

}
