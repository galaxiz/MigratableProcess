package IOlib;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * When a read or write is requested via the library, it should open the file,
 * seek to the requisite location, perform the operation, and close the file
 * again. In this way, they will maintain all the information required in order
 * to continue performing operations on the file, even if the process is
 * transferred to another node
 * 
 * This class implement a migratable outputStream
 * 
 * @author Shiwei Dong
 */
public class TransactionalFileOutputStream extends OutputStream implements
		Serializable {

	private String filename;
	private transient FileOutputStream outputStream;
	private boolean migrated = false;
	private boolean append = false;

	/** TransactionalFileOutputStream(String string, boolean append)
	 * 
	 * @param string
	 * @param append
	 */
	public TransactionalFileOutputStream(String string, boolean append) {
		this.filename = string;
		this.migrated = false;
		this.append = append;
	}

	/** setMigrated(boolean migrated)
	 * 
	 * @param migrated
	 *         
	 */
	public void setMigrated(boolean migrated) {
		this.migrated = migrated;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * If the migrate flag is true or it's the first time the object was used, use a new connection.
	 * Otherwise the connection will be cached.
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public synchronized void write(int arg0) throws IOException {
		if (migrated == true) {
			if (outputStream != null) {
				outputStream.close();
				outputStream = null;
			}
		}
		if (outputStream == null) {
			outputStream = new FileOutputStream(filename, append);
			migrated = false;
		}

		this.append = true;

		outputStream.write(arg0);
	}

}
