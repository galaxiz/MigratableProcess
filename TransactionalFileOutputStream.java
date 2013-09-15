import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * 
 */

/**
 * @author Shiwei Dong
 * 
 */
public class TransactionalFileOutputStream extends OutputStream implements Serializable{

	private String filename;
	private FileOutputStream outputStream;
	private boolean migrated = false;
	private boolean append = false;

	public TransactionalFileOutputStream(String string, boolean append) {
		this.filename = string;
		this.migrated = false;
		this.append = append;
	}

	/**
	 * @param migrated
	 *            the migrated to set
	 */
	public void setMigrated(boolean migrated) {
		this.migrated = migrated;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public synchronized void write(int arg0) throws IOException {
		/*
		 * 1. open the file 2. do the write job 3. close the file
		 */
		if (migrated == true) {
			if (outputStream != null) {
				outputStream.close();
				outputStream = null;
			}

			if (outputStream == null)
				System.out.println("out put stream test ok");
		}
		if (outputStream == null) {
			outputStream = new FileOutputStream(filename, append);
			migrated = false;
		}	
		
		this.append = true;

		outputStream.write(arg0);
	}

}
