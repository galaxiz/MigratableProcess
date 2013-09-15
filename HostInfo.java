import java.io.Serializable;

/**
 * @author Xi Zhao 
 */
public class HostInfo implements Serializable{
    public String host;
    public Integer port;
    public Integer jobCount;
    public long lastTime;
}
