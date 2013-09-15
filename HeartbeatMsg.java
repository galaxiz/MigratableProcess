import java.io.*;

public class HeartbeatMsg implements Serializable{
    public enum Type{
        normal,reg;
    }

    Type type;

    public Integer jobCount;
}
