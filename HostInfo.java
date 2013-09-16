import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Xi Zhao 
 */
public class HostInfo implements Serializable{
    public String host;
    public Integer port;
    public Integer jobCount;
    public ArrayList<String> jobs;
    public long lastTime;    
    
    public HostInfo(){
    	
    }
    
    public HostInfo(String host,Integer port,Integer jobCount,ArrayList<String> jobs, long lastTime){
    	this.host=host;
    	this.port=port;
    	this.jobCount=jobCount;
    	this.jobs=jobs;
    	this.lastTime=lastTime;
    }
}
