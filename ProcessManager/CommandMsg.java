package ProcessManager;
import java.io.*;

/**
 * 
 * @author Xi Zhao
 *
 */
public class CommandMsg implements Serializable{
    public enum Type{
        newJob,killJob,requestJob,waitJob;
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
