import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 
 */

/**
 * @author air
 * 
 */
public class ZipFileProcess implements MigratableProcess {

	private TransactionalFileInputStream inFile;
	private TransactionalFileOutputStream outFile;
	private String outFileName;
	private String inFileName;

	private volatile boolean suspending;

	/**
	 * @throws Exception
	 * 
	 */
	public ZipFileProcess(String[] args) throws Exception {
		if (args.length != 2) {
			System.out
					.println("usage: ZipFileProcess <inputFile> <outputFile>");
			throw new Exception("Invalid Arguments");
		}
		inFileName = args[0];
		outFileName = args[1];

		if (!outFileName.endsWith(".zip")) {
			System.out.println("OutputFile should take a \".zip\" suffix");
			throw new Exception("Invalid OutputFile Name");
		}

		inFile = new TransactionalFileInputStream(inFileName);
		outFile = new TransactionalFileOutputStream(outFileName, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		byte[] buffer = new byte[32];

		try {
			ZipOutputStream zout = new ZipOutputStream(outFile);
			ZipEntry ze = new ZipEntry(inFileName);
			zout.putNextEntry(ze);

			while (!suspending) {
				int len;
				while ((len = inFile.read(buffer)) > 0) {
					zout.write(buffer, 0, len);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			suspending = false;

			inFile.close();
			zout.closeEntry();
			zout.close();
			outFile.close();

			System.out.println("Zipping complete!");
		} catch (IOException e) {
			System.out.println("Exception" + e);
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
		suspending = true;
		inFile.setMigrated(true);
		outFile.setMigrated(true);
		while (suspending)
			;
	}

}
