import java.io.*;

public class CommandMsg implements Serializable{
    public enum Type{
        newJob,killJob,requestJob,waitJob;
    }

    Type type;

    public String args;
}
