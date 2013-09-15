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
	/*
	 * Job info
	 */
	ArrayList<JobInfo> jobInfoList;
	/*
	 * Job Info Mutex
	 */
	Semaphore jobInfoMutex;

	/*
	 * host address or name of master node.
	 */
	String host;

	/*
	 * command listener port of this slave node.
	 */
	int listenerPort;

	/*
	 * heartbeat period.
	 */
	final int SlavePeriod = 3000;

	public ProcessManagerSlave() {
		jobInfoList = new ArrayList<JobInfo>();
		jobInfoMutex = new Semaphore(1);

		/*
		 * Randomly choose a port number for command listener. This enables
		 * multiple slave nodes to run on the same machine.
		 */
		listenerPort = 9001 + new Random().nextInt(100);
	}

	/*
	 * slave register this slave node to master node, create a command listener
	 * and create a heartbeat sender which sends heartbeat periodically.
	 */
	public void slave(String host) {
		this.host = host;

		while (registerSlave(host) == false) {
			try {
				Thread.sleep(SlavePeriod);
			} catch (InterruptedException e) {
				System.out.println("Retrying to register...");
			}
		}

		new Thread(new Runnable() {
			public void run() {
				commandListener();
			}
		}).start();

		new Timer(true).scheduleAtFixedRate(new TimerTask() {
			public void run() {
				heartbeatSender();
			}
		}, 0, SlavePeriod);
	}

	// private functions
	boolean registerSlave(String host) {
		HeartbeatMsg hmsg = new HeartbeatMsg();
		hmsg.type = HeartbeatMsg.Type.reg;
		hmsg.port = listenerPort;
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

			if (response.equals("ok")) {
				System.out
						.println("Registration succeed. Starting to serve...");
				return true;
			}
		} catch (IOException e) {
			System.out.println("Network error occurred when registering.");
			return false;
		}

		return false;
	}

	void commandListener() {
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
			System.out.println("Error occurred in execute command.");
		}
	}

	void heartbeatSender() {
		HeartbeatMsg beat = new HeartbeatMsg();
		beat.type = HeartbeatMsg.Type.normal;
		beat.port = listenerPort;

		/*
		 * poll to check whether jobs are done.
		 */
		try {
			jobInfoMutex.acquire();
		} catch (InterruptedException e) {
			System.out
					.println("Retrying to acquire mutex to check status of jobs");
		}

		for (Iterator<JobInfo> it = jobInfoList.iterator(); it.hasNext();) {
			JobInfo jobInfop = it.next();
			if (jobInfop.thread.isAlive() == false) {
				System.out.println("Job: " + jobInfop.job.toString()
						+ " has finished.");
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

			Class<?> jobClass = Class.forName(command);
			MigratableProcess job = (MigratableProcess) jobClass
					.getConstructor(String[].class).newInstance((Object) args);

			jobInfo.job = job;
			jobInfo.thread = new Thread((Runnable) job);

			jobInfoMutex.acquire();
			jobInfoList.add(jobInfo);
			jobInfo.thread.start();
			jobInfoMutex.release();
		} catch (ClassNotFoundException e) {
			System.out.println("command not found.");
			return false;
		} catch (NoSuchMethodException e) {
			System.out.println("Not a standard job.");
			return false;
		} catch (Exception e) {
			System.out.println("Create job failed.");
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
				// TODO: kill job feature.
				break;
			case requestJob:
				CommandMsg cm = new CommandMsg();
				cm.type = CommandMsg.Type.waitJob;
				cm.args = "";

				String[] info = cmsg.args.trim().split(":");

				Socket jobSocket = new Socket(info[0],
						Integer.parseInt(info[1]));
				ObjectOutputStream out = new ObjectOutputStream(
						jobSocket.getOutputStream());
				out.writeObject(cm);
				out.flush();

				System.out.println("Reading job...");
				ObjectInputStream jobIn = new ObjectInputStream(
						jobSocket.getInputStream());
				MigratableProcess job;

				try {
					job = (MigratableProcess) jobIn.readObject();

					System.out.println("Resuming job...");
					resumeJob(job);
				} catch (ClassNotFoundException e1) {
					System.out.println("Receiving job error.");
				}

				break;
			case waitJob:
				try {
					jobInfoMutex.acquire();
				} catch (InterruptedException e) {
					System.out
							.println("Retrying to acquire mutex to send job.");
				}

				JobInfo jobInfo = jobInfoList.get(new Random()
						.nextInt(jobInfoList.size()));

				System.out.println("Sending job...");
				if (sendJob(socket, jobInfo)) {
					jobInfoList.remove(jobInfo);
				}
				System.out.println("Sending job done.");
				jobInfoMutex.release();
				break;
			default:
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e2) {
			System.out.println("Command message error.");
		}
	}

	boolean sendJob(Socket socket, JobInfo jobInfo) {
		/*
		 * Suspend job
		 */
		jobInfo.job.suspend();

		/*
		 * Serialize to receiver.
		 */
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					socket.getOutputStream());
			out.writeObject(jobInfo.job);
			out.flush();
		} catch (IOException e) {
			System.out.println("Error in sending job to other slave node.");
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
		} catch (InterruptedException e) {
			System.out
					.println("Retrying to acquire mutex to resume job.");
		}

		jobInfoList.add(jobInfo);
		jobInfo.thread.start();
		jobInfoMutex.release();

		return true;
	}
}
