package processmanager;

import java.io.*;
import java.util.ArrayList;

/**
 * class HeartbeatMsg, sent by slave nodes to the master node as a heartbeat.
 * Information including: 
 * Slave node command listener port, 
 * Current number of running jobs, 
 * Job information of all jobs.
 * 
 * @author Xi Zhao
 * 
 */
public class HeartbeatMsg implements Serializable {
	public enum Type {
		normal, reg, done;
	}

	Type type;

	public Integer port;
	public Integer jobCount;
	public ArrayList<String> jobs;

	public HeartbeatMsg() {

	}

	public HeartbeatMsg(Type type, Integer port, Integer jobCount,
			ArrayList<String> jobs) {
		super();
		this.type = type;
		this.port = port;
		this.jobCount = jobCount;
		this.jobs = jobs;
	}
}
