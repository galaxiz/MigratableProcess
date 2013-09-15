import java.io.*;

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
}
