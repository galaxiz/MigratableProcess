package processmanager;

import java.io.*;

/**
 * class CommandMSg, sent over network. 
 * For slave nodes to execute.
 * 
 * @author Xi Zhao
 * 
 */
public class CommandMsg implements Serializable {
	public enum Type {
		/*
		 * requestJob: ask slave node to request a job from another slave node.
		 * 
		 * waitJob: some slave node is waiting for a job, this slave node should
		 * send a existing job to it.
		 */
		newJob, killJob, requestJob, waitJob;
	}

	Type type;

	public String args;

	public CommandMsg() {
	}

	public CommandMsg(Type type, String args) {
		super();
		this.type = type;
		this.args = args;
	}
}
