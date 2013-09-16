import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * @author Shiwei Dong
 * 
 */
public class TransactionalFileInputStream extends InputStream implements
		Serializable {

	private String filename;
	private int currentLocation = 0;
	private boolean migrated = false;
	private transient FileInputStream inputStream;

	/**
	 * @param filename
	 */
	public TransactionalFileInputStream(String filename) {
		this.filename = filename;
		this.currentLocation = 0;
		this.migrated = false;
	}

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
			if (inputStream != null){
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
