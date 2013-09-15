import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * @author Xi Zhao
 */
public class ProcessManagerSlave extends ProcessManager {
	// Job info
	ArrayList<JobInfo> jobInfoList;
	// Job Info Mutex
	Semaphore jobInfoMutex;

	// host
	String host;

	int listenerPort;

	final int SlavePeriod = 3000;

	public ProcessManagerSlave() {
		jobInfoList = new ArrayList<JobInfo>();
		jobInfoMutex = new Semaphore(1);

		listenerPort = 9001 + new Random().nextInt(100);
	}

	public void slave(String host) {
		this.host = host;

		while (registerSlave(host) == false) {
			try {
				Thread.sleep(SlavePeriod);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		new Thread(new Runnable() {
			public void run() {
				// slave listener
				commandListener();
			}
		}).start();

		new Timer(true).scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// send heartbeat
				heartbeatSender();
			}
		}, 0, SlavePeriod);
	}

	// private functions
	boolean registerSlave(String host) {
		HeartbeatMsg hmsg = new HeartbeatMsg();
		hmsg.type = HeartbeatMsg.Type.reg;
		hmsg.port=listenerPort;
		hmsg.jobCount = 0;

		try {
			Socket socket = new Socket(host, 9000);

			ObjectOutputStream out = new ObjectOutputStream(
					socket.getOutputStream());
			out.writeObject(hmsg);
			out.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String response = br.readLine();
			
			System.out.println(response);
			if (response.equals("ok")) {
				System.out.println("Register succeed.");
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return false;
	}

	void commandListener() {
		// to do listening to a fixed port 9001
		try {
			ServerSocket socket = new ServerSocket(listenerPort);

			while (true) {
				final Socket insocket = socket.accept();

				new Thread(new Runnable() {
					public void run() {
						commandHandler(insocket);
					}
				}).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void heartbeatSender() {
		// to do

		// heartbeat
		HeartbeatMsg beat = new HeartbeatMsg();
		beat.type = HeartbeatMsg.Type.normal;
		beat.port = listenerPort;

		// check whether job is done.
		try {
			jobInfoMutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (Iterator<JobInfo> it = jobInfoList.iterator(); it.hasNext();) {
			JobInfo jobInfop = it.next();
			if (jobInfop.thread.isAlive() == false) {
				it.remove();
			}
		}
		
		beat.jobCount = jobInfoList.size();

		jobInfoMutex.release();
		
		sendObjectTo(host, 9000, beat);
	}

	boolean newJob(String command, String args[]) {
		try {
			JobInfo jobInfo = new JobInfo();

			Class jobClass = Class.forName(command);
			MigratableProcess job = (MigratableProcess) jobClass
					.getConstructor(String[].class).newInstance((Object) args);

			jobInfo.job = job;
			jobInfo.thread = new Thread((Runnable) job);

			jobInfoMutex.acquire();
			jobInfoList.add(jobInfo);
			jobInfo.thread.start();
			jobInfoMutex.release();
		} catch (Exception e) {
			// to do different exceptions
			e.printStackTrace();
			return false;
		}

		return true;
	}

	void commandHandler(Socket socket) {
		try {
			ObjectInputStream in = new ObjectInputStream(
					socket.getInputStream());
			CommandMsg cmsg = (CommandMsg) in.readObject();

			System.out.println(cmsg.type.toString() + ":" + cmsg.args);

			switch (cmsg.type) {
			case newJob:
				String[] infos = cmsg.args.trim().split(" +");
				String[] args = null;

				if (infos.length > 1) {
					args = new String[infos.length - 1];

					for (int i = 1; i < infos.length; i++) {
						args[i - 1] = infos[i];
					}
				}

				newJob(infos[0], args);
				break;
			case killJob:
				// to do
				break;
			case requestJob:
				CommandMsg cm = new CommandMsg();
				cm.type = CommandMsg.Type.waitJob;
				cm.args = "";
				
				String[] info=cmsg.args.trim().split(":");

				Socket jobSocket = new Socket(info[0],Integer.parseInt(info[1]));
				ObjectOutputStream out = new ObjectOutputStream(
						jobSocket.getOutputStream());
				out.writeObject(cm);
				out.flush();

				System.out.println("Starting to read job");
				ObjectInputStream jobIn = new ObjectInputStream(
						jobSocket.getInputStream());
				MigratableProcess job = (MigratableProcess) jobIn.readObject();
				
				System.out.println("Starting to resume job");
				resumeJob(job);
				break;
			case waitJob:
				jobInfoMutex.acquire();
				JobInfo jobInfo = jobInfoList.get(new Random()
						.nextInt(jobInfoList.size()));

				System.out.println("Starting to send job");
				if (sendJob(socket, jobInfo)) {
					jobInfoList.remove(jobInfo);
				}
				System.out.println("End of sending job");
				jobInfoMutex.release();
				break;
			default:
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	boolean sendJob(Socket socket, JobInfo jobInfo) {
		// suspend
		jobInfo.job.suspend();

		// serialize
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					socket.getOutputStream());
			out.writeObject(jobInfo.job);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	boolean resumeJob(MigratableProcess job) {
		JobInfo jobInfo = new JobInfo();

		jobInfo.job = job;
		jobInfo.thread = new Thread((Runnable) job);

		try {
			jobInfoMutex.acquire();
			jobInfoList.add(jobInfo);
			jobInfo.thread.start();
			jobInfoMutex.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return true;
	}
}
