
/**
 * 
 */

/**
 * @author air
 *
 */
public class ProcessTestDrive {

	/**
	 * Sep 12, 2013
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		String test[] = {"hello","in.txt","out.txt"};
		try {
			GrepProcess gp = new GrepProcess(test);
			Thread testdrive = new Thread((Runnable)gp);
			testdrive.start();
			Thread.sleep(500);
			int i = 0;
			while (i < 10) {
			gp.suspend();
			Thread.sleep(500);
			testdrive.stop();
			Thread testdrive2 = new Thread((Runnable)gp);
			testdrive2.start();
			Thread.sleep(500);
			i++;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Exception "+e);
			e.printStackTrace();
		}
	}

}
