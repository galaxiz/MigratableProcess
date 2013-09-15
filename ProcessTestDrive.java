/**
 * 
 */

/**
 * @author Shiwei Dong
 * 
 */
public class ProcessTestDrive {

	public void testGrep() {
		String test[] = { "hello", "in.txt", "out.txt" };
		try {
			GrepProcess gp = new GrepProcess(test);
			Thread testdrive = new Thread((Runnable) gp);
			testdrive.start();
			Thread.sleep(500);
			int i = 0;
			while (i < 10) {
				gp.suspend();
				Thread.sleep(500);
				// testdrive.stop();
				Thread testdrive2 = new Thread((Runnable) gp);
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

	/**
	 * Sep 12, 2013
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ProcessTestDrive ptd = new ProcessTestDrive();
//		ptd.testGrep();
		ptd.testZip();
	}

}
