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
}
