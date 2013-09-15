import java.io.Serializable;

/**
 * 
 * @author Xi Zhao
 *
 */
public class JobInfo implements Serializable{
    public MigratableProcess job;
    public Thread thread;
}
