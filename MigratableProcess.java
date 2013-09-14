import java.lang.*;
import java.io.*;

interface MigratableProcess extends Runnable, Serializable{
    void suspend();
    String toString();
}
