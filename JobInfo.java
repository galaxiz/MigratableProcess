import java.io.Serializable;

/**
 * 
 * @author Xi Zhao
 *
 */
public class JobInfo implements Serializable {
	public MigratableProcess job;
	public Thread thread;
	public String commandLine;

	public JobInfo(){
		
	}
	
	public JobInfo(MigratableProcess job, Thread thread, String commandLine) {
		this.job=job;
		this.thread=thread;
		this.commandLine=commandLine;
	}
}
