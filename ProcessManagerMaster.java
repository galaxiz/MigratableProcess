import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * @author Xi Zhao
 */
public class ProcessManagerMaster extends ProcessManager {
	/*
	 * slave nodes information
	 */
	ArrayList<HostInfo> hostInfoList;
	
	/*
	 * mutex for hostInfoList
	 */
	Semaphore hostInfoMutex;

	final int MasterPeriod = 12000;

	public ProcessManagerMaster() {
		hostInfoList = new ArrayList<HostInfo>();
		hostInfoMutex = new Semaphore(1);
	}

	/**
	 * Start the service of master, including: Prompt, HeartbeatListener,
	 * HeartbeatCheckerAndWorkloadBalancer
	 * 
	 * Prompt provides a prompt for administrator to type in command.
	 * 
	 * Heartbeat Listener will handle all heartbeat message from slave nodes.
	 * 
	 * HeartbeatCheckerAndWorkloadBalancer will check whether slaves are still
	 * alive and balance their work.
	 */
	public void master() {
		new Timer(true).scheduleAtFixedRate(new TimerTask() {
			public void run() {
				checkHeartbeatAndWorkload();
			}
		}, 0, MasterPeriod);

		// TODO: RPC methods
		new Thread(new Runnable() {
			public void run() {
				heartbeatListener();
			}
		}).start();

		// Prompt to receive administrators command
		new Thread(new Runnable() {
			public void run() {
				prompt();
			}
		}).start();
	}

	/**
	 * Private functions
	 */

	/**
	 * Check whether slave nodes are still alive, and balance the work between
	 * those alive slave nodes.
	 */
	void checkHeartbeatAndWorkload() {
		long curTime = System.currentTimeMillis();

		/*
		 * This function should not fail. So retry is the default behavior when
		 * encountering errors.
		 */
		while (true) {
			try {
				hostInfoMutex.acquire();
				break;
			} catch (InterruptedException e) {
				// TODO: log function
				System.out.println("\nRetrying to acquire mutex.");
			}
		}

		// check each host that how long has passed since last heartbeat came.
		for (Iterator<HostInfo> it = hostInfoList.iterator(); it.hasNext();) {
			HostInfo hostp = it.next();
			if (curTime - hostp.lastTime > MasterPeriod) {
				// TODO: log functions
				// Remove slave node if it times out because of lack of
				// heartbeat.
				System.out.println("\n" + hostp.host + ":" + hostp.port
						+ " (slave) is unreachable. Removing...");
				it.remove();
			}
		}
		hostInfoMutex.release();

		while (true) {
			try {
				hostInfoMutex.acquire();
				break;
			} catch (InterruptedException e) {
				// TODO: log function
				System.out.println("\nRetrying to acquire mutex.");
			}
		}

		/*
		 * Balancing one job per 15 seconds is enough. Balancing the nodes with
		 * heaviest workload and lightest workload.
		 * 
		 * Balancing multiple jobs could be much more complex, leave it to
		 * future work.
		 * 
		 * TODO: multiple job balancing.
		 */
		int minCount = 9999, maxCount = -1;
		HostInfo minHost = null, maxHost = null;

		for (Iterator<HostInfo> it = hostInfoList.iterator(); it.hasNext();) {
			HostInfo hostp = it.next();

			if (hostp.jobCount > maxCount) {
				maxCount = hostp.jobCount;
				maxHost = hostp;
			}
			if (hostp.jobCount < minCount) {
				minCount = hostp.jobCount;
				minHost = hostp;
			}
		}

		if (maxCount - minCount >= 2) {
			/*
			 * Send command to "minHost" slave node to ask it to request a job
			 * from maxHost.
			 * 
			 * Master doesn't actively change the workload information. It
			 * simply wait for the result from slave nodes.
			 */
			CommandMsg cmsg = new CommandMsg();
			cmsg.type = CommandMsg.Type.requestJob;
			cmsg.args = maxHost.host + ":" + maxHost.port.toString();

			sendObjectTo(minHost.host, minHost.port, cmsg);

			/*
			 * Do not modify jobCount until they heartbeat to master.
			 * 
			 * Warning: for multiple job balancing, this could cause it to try
			 * to balance for ever.
			 */
			// minHost.jobCount++;
			// maxHost.jobCount--;
		}

		hostInfoMutex.release();
	}

	/**
	 * Prompt to execute command from administrator.
	 */
	void prompt() {
		String line;

		while (true) {
			System.out.print("> ");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			try {
				line = br.readLine();

				if (line.equals("exit")) {
					break;
				} else if (line.equals("ps")) {
					// TODO: add more detail information from slave nodes.
					while (true) {
						try {
							hostInfoMutex.acquire();
							break;
						} catch (InterruptedException e) {
							System.out
									.println("\nRetrying to acquire mutex. (promp)");
						}
					}
					for (HostInfo hostp : hostInfoList) {
						System.out.println(hostp.host + ":"
								+ hostp.port.toString() + "--> JOB COUNT:"
								+ hostp.jobCount.toString());
						for(String commandLine:hostp.jobs){
							System.out.println("  "+commandLine);
						}
					}
					hostInfoMutex.release();
				} else {
					// default: create a new job with the command.
					//TODO: add more information about this job.
					CommandMsg cm = new CommandMsg();
					cm.type = CommandMsg.Type.newJob;
					cm.args = line;

					while (true) {
						try {
							hostInfoMutex.acquire();
							break;
						} catch (InterruptedException e) {
							System.out
									.println("\nRetrying to acquire mutex. (promp)");
						}
					}

					/*
					 * TODO: currently create job to first host, to see the
					 * effect of balancing.
					 */

					// HostInfo randHostInfo=hostInfoList.get(new
					// Random().nextInt(hostInfoList.size()));
					HostInfo randHostInfo = hostInfoList.get(0);
					sendObjectTo(randHostInfo.host, randHostInfo.port, cm);
					
					System.out.println("Try to create job on slave node: "+randHostInfo.host+":"+randHostInfo.port);

					hostInfoMutex.release();
				}
			} catch (IOException e1) {
				System.out.println("\nOops, this command cannot be processed.");
			}
		}
	}

	/**
	 * Listen to heartbeat from slave nodes.
	 */
	void heartbeatListener() {
		//TODO: currently Master always use port 9000.
		try {
			ServerSocket socket = new ServerSocket(9000);
			while (true) {
				final Socket insocket = socket.accept();

				new Thread(new Runnable() {
					public void run() {
						heartbeatHandler(insocket);
					}
				}).start();
			}
		} catch (IOException e) {
			System.out.println("Listener has encouner a unknown error.");
			e.printStackTrace();
		}
	}

	/**
	 * Handle each heartbeat messege from slave nodes.
	 * @param socket
	 * @return
	 */
	boolean heartbeatHandler(Socket socket) {
		try {
			ObjectInputStream in = new ObjectInputStream(
					socket.getInputStream());
			HeartbeatMsg beat = (HeartbeatMsg) in.readObject();

			String ip = socket.getInetAddress().getHostAddress();
			int port = beat.port;

			switch (beat.type) {
			case normal:
				/*
				 * normal heartbeat
				 * 
				 * TODO: try to locate HostInfo by hash, other than searching.
				 */
				while (true) {
					try {
						hostInfoMutex.acquire();
						break;
					} catch (InterruptedException e) {
						System.out.println("\nRetrying acquire mutex.");
					}
				}

				for (Iterator<HostInfo> it = hostInfoList.iterator(); it
						.hasNext();) {
					HostInfo hostp = it.next();

					if (hostp.host.equals(ip) && hostp.port == port) {
						//Find the right HostInfo object.
						hostp.jobCount = beat.jobCount;
						hostp.lastTime = System.currentTimeMillis();
						hostp.jobs=beat.jobs;
						break;
					}
				}
				hostInfoMutex.release();

				break;

			case reg:
				/*
				 * Register request from slave node.
				 */
				HostInfo hi = new HostInfo();
				hi.host = ip;
				hi.port = port; //port is the command listener port of slave node.
				hi.jobCount = 0;//initial job count
				hi.lastTime = System.currentTimeMillis();
				hi.jobs=new ArrayList<String>();

				hostInfoMutex.acquire();
				hostInfoList.add(hi);
				hostInfoMutex.release();

				/*
				 * Response ok to slave node.
				 * TODO: better response required.
				 */
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.write("ok\n");
				out.flush();

				break;
			}
		} catch (IOException e) {
			System.out.println("\nUnknow error encountered when handle heartbeat message.");
			return false;
		} catch (ClassNotFoundException e) {
			System.out.println("\nUnknow error encountered when handle heartbeat message.");
			return false;
		} catch (InterruptedException e) {
			System.out.println("\nUnknow error encountered when handle heartbeat message.");
			return false;
		}

		return true;
	}

}
