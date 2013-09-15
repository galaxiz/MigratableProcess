import java.io.*;
import java.util.ArrayList;

/**
 * 
 * @author Xi Zhao
 *
 */
public class HeartbeatMsg implements Serializable{
    public enum Type{
        normal,reg;
    }

    Type type;

    public Integer port;
    public Integer jobCount;
    public ArrayList<String> jobs;
    
    public HeartbeatMsg(){
    	
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
