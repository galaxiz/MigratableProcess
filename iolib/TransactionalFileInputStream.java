package iolib;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * When a read or write is requested via the library, it should open the file,
 * seek to the requisite location, perform the operation, and close the file
 * again. In this way, they will maintain all the information required in order
 * to continue performing operations on the file, even if the process is
 * transferred to another node
 * 
 * This class implement a migratable inputStream 
 * 
 * @author Shiwei Dong
 */
public class TransactionalFileInputStream extends InputStream implements
		Serializable {

	private String filename;
	private int currentLocation = 0;
	private boolean migrated = false;
	private transient FileInputStream inputStream;

	/** TransactionalFileInputStream(String filename)
	 * 
	 * @param filename
	 */
	public TransactionalFileInputStream(String filename) {
		this.filename = filename;
		this.currentLocation = 0;
		this.migrated = false;
	}

	/** setMigrated(boolean migrated)
	 * 
	 * Sep 16, 2013
	 * 
	 * @param migrated
	 */
	public void setMigrated(boolean migrated) {
		this.migrated = migrated;
	}

	/* 
	 * (non-Javadoc)
	 * 
	 * Read one byte at a time and the read method is thread safe If migrated is
	 * set to true, the inputStream will close and reopen. If migrated is false,
	 * the connection will be cached and no need to open that file again.
	 */
	public synchronized int read() throws IOException {
		/*
		 * 1. open the file 2. do the read job 3. close the file
		 */
		if (migrated == true) {
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			}
		}
		if (inputStream == null) {
			inputStream = new FileInputStream(filename);
			inputStream.skip(currentLocation);
			migrated = false;
		}
		int result = inputStream.read();
		if (result != -1)
			currentLocation++;

		return result;
	}

}
