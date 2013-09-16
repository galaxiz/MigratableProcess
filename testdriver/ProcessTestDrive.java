package testdriver;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processinterface.MigratableProcess;
import testprocesses.GrepProcess;
import testprocesses.WebCrawlerProcess;
import testprocesses.WordcountProcess;
import testprocesses.ZipFileProcess;


/**
 * Just for test purpose
 * 
 * @author Shiwei Dong
 * 
 */
public class ProcessTestDrive {

	public void testGrep() {
		String test[] = { "Larry", "in.txt", "out.txt" };
		try {
			GrepProcess gp = new GrepProcess(test);
			Thread testdrive = new Thread((Runnable) gp);
			testdrive.start();
			Thread.sleep(500);
			int i = 0;
			while (i < 1) {
				gp.suspend();
				Thread.sleep(500);

				FileOutputStream fout = new FileOutputStream("serial");
				ObjectOutputStream out = new ObjectOutputStream(fout);
				out.writeObject(gp);

				FileInputStream fin = new FileInputStream("serial");
				ObjectInputStream in = new ObjectInputStream(fin);
				MigratableProcess job = (MigratableProcess) in.readObject();

				Thread testdrive2 = new Thread((Runnable) job);
				testdrive2.start();
				Thread.sleep(50);
				i++;
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
			e.printStackTrace();
		}
	}

	private void testZip() {
		String test[] = { "zip", "test1zip.zip" };

		try {
			ZipFileProcess zfp = new ZipFileProcess(test);
			Thread testdrive = new Thread((Runnable) zfp);
			testdrive.start();
			System.out.println(zfp.toString());
			Thread.sleep(500);

			zfp.suspend();
			Thread.sleep(500);
			testdrive.stop();

			FileOutputStream fout = new FileOutputStream("serial");
			ObjectOutputStream out = new ObjectOutputStream(fout);
			out.writeObject(zfp);

			FileInputStream fin = new FileInputStream("serial");
			ObjectInputStream in = new ObjectInputStream(fin);
			zfp = (ZipFileProcess) in.readObject();

			Thread testdrive2 = new Thread((Runnable) zfp);
			testdrive2.start();
			Thread.sleep(500);

		} catch (Exception e) {
			System.out.println("Exception " + e);
			e.printStackTrace();
		}

	}

//	public void testSerializable() {
//		String test[] = { "gig", "in.txt", "out.txt" };
//		try {
//			MigratableProcess job = new TestSerializableJob();
//			Thread testdrive = new Thread((Runnable) job);
//			testdrive.start();
//			Thread.sleep(500);
//			int i = 0;
//			while (i < 10) {
//				job.suspend();
//				Thread.sleep(500);
//
//				FileOutputStream fout = new FileOutputStream("serial");
//				ObjectOutputStream out = new ObjectOutputStream(fout);
//				out.writeObject(job);
//
//				FileInputStream fin = new FileInputStream("serial");
//				ObjectInputStream in = new ObjectInputStream(fin);
//				job = (MigratableProcess) in.readObject();
//
//				Thread testdrive2 = new Thread((Runnable) job);
//				testdrive2.start();
//				Thread.sleep(500);
//				i++;
//			}
//		} catch (Exception e) {
//			System.out.println("Exception " + e);
//			e.printStackTrace();
//		}
//	}

	public void testPattern() {
		Pattern p = Pattern
				.compile("<a([\\s]*?href[\\s]*?=[\\s]*?\"(.+?)\")>.+?</a>");
		Matcher m = p
				.matcher("<a href   =   \"http://www.w3schools.com/\">Visit W3Schools</a>");
		if (m.find()) {
			System.out.println(m.group(1));
			System.out.println(m.group(2));
		}
	}

	public void testWebCrawler() {
		String test[] = { "http://www.cnn.com", "webcrawler.txt" };
		try {
			MigratableProcess wcp = new WebCrawlerProcess(test);
			Thread testdrive = new Thread((Runnable) wcp);
			testdrive.start();
			Thread.sleep(2000);

			wcp.suspend();
			System.out.println("suspended");
			Thread.sleep(500);
			testdrive.stop();

			FileOutputStream fout = new FileOutputStream("serial");
			ObjectOutputStream out = new ObjectOutputStream(fout);
			out.writeObject(wcp);

			FileInputStream fin = new FileInputStream("serial");
			ObjectInputStream in = new ObjectInputStream(fin);
			wcp = (WebCrawlerProcess) in.readObject();

			Thread testdrive2 = new Thread((Runnable) wcp);
			System.out.println("restarted");
			testdrive2.start();
			Thread.sleep(500);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void testWordcount() {
		String test[] = { "wc1.txt", "wcout2.txt" };
		try {
			WordcountProcess wcp = new WordcountProcess(test);
			Thread testdrive = new Thread((Runnable) wcp);
			testdrive.start();
			Thread.sleep(1000);

			wcp.suspend();
			System.out.println("suspended");
			Thread.sleep(500);
			testdrive.stop();

			FileOutputStream fout = new FileOutputStream("serial");
			ObjectOutputStream out = new ObjectOutputStream(fout);
			out.writeObject(wcp);

			FileInputStream fin = new FileInputStream("serial");
			ObjectInputStream in = new ObjectInputStream(fin);
			wcp = (WordcountProcess) in.readObject();

			Thread testdrive2 = new Thread((Runnable) wcp);
			System.out.println("restarted");
			testdrive2.start();
			Thread.sleep(500);
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
		 ptd.testZip();
		// ptd.testSerializable();
		// ptd.testPattern();
		 ptd.testWebCrawler();
		 ptd.testWordcount();
	}

}
