import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * 
 */

/**
 * @author Shiwei Dong
 * 
 */
public class ProcessTestDrive {

	public void testGrep() {
		String test[] = { "gig", "in.txt", "out.txt" };
		try {
			GrepProcess gp = new GrepProcess(test);
			Thread testdrive = new Thread((Runnable) gp);
			testdrive.start();
			Thread.sleep(500);
			int i = 0;
			while (i < 10) {
				gp.suspend();
				Thread.sleep(500);
				
				FileOutputStream fout=new FileOutputStream("serial");
				ObjectOutputStream out=new ObjectOutputStream(fout);
				out.writeObject(gp);
				
				FileInputStream fin=new FileInputStream("serial");
				ObjectInputStream in=new ObjectInputStream(fin);
				MigratableProcess job=(MigratableProcess)in.readObject();
				
				Thread testdrive2 = new Thread((Runnable) job);
				testdrive2.start();
				Thread.sleep(500);
				i++;
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
			e.printStackTrace();
		}
	}

	private void testZip() {
		String test[] = {"ziptest.txt", "zip1.zip" };
		
		try {
			ZipFileProcess zfp = new ZipFileProcess(test);
			Thread testdrive = new Thread((Runnable)zfp);
			testdrive.start();
			Thread.sleep(500);
			int i = 0;
			while (i < 100) {
				zfp.suspend();
				Thread.sleep(500);
				// testdrive.stop();
				Thread testdrive2 = new Thread((Runnable) zfp);
				testdrive2.start();
				Thread.sleep(500);
				i++;
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
			e.printStackTrace();
		}

	}
	
	public void testSerializable() {
		String test[] = { "gig", "in.txt", "out.txt" };
		try {
			MigratableProcess job=new TestSerializableJob();
			Thread testdrive = new Thread((Runnable) job);
			testdrive.start();
			Thread.sleep(500);
			int i = 0;
			while (i < 10) {
				job.suspend();
				Thread.sleep(500);
				
				FileOutputStream fout=new FileOutputStream("serial");
				ObjectOutputStream out=new ObjectOutputStream(fout);
				out.writeObject(job);
				
				FileInputStream fin=new FileInputStream("serial");
				ObjectInputStream in=new ObjectInputStream(fin);
				job=(MigratableProcess)in.readObject();
				
				Thread testdrive2 = new Thread((Runnable) job);
				testdrive2.start();
				Thread.sleep(500);
				i++;
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
			e.printStackTrace();
		}
	}

	/**
	 * Sep 12, 2013
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ProcessTestDrive ptd = new ProcessTestDrive();
		ptd.testGrep();
		//ptd.testZip();
		//ptd.testSerializable();
	}

}
