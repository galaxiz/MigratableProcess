package processmanager;

import java.io.Serializable;

import processinterface.MigratableProcess;

/**
 * class JobInfo Information about a job, mainly used by slave node to present a
 * running job.
 * 
 * Info including: 
 * MigratableProcess object, 
 * Running thread if it's running,
 * command line which creates this job, 
 * job id.
 * 
 * @author Xi Zhao
 * 
 */
public class JobInfo implements Serializable {
	public MigratableProcess job;
	public Thread thread;
	public String commandLine;
	public Integer id;

	public JobInfo() {

	}

	public JobInfo(MigratableProcess job, Thread thread, String commandLine,
			Integer id) {
		this.job = job;
		this.thread = thread;
		this.commandLine = commandLine;
		this.id = id;
	}
}
