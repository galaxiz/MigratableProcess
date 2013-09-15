import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

/*
 * Class ProcessManager
 * @author Xi Zhao
 *
 */
public class ProcessManager{
    //other jvm info
    ArrayList<HostInfo> hostInfoList;
    //jvm info mutex
    Semaphore hostInfoMutex;
    
    //Job info
    ArrayList<JobInfo> jobInfoList;
    //Job Info Mutex
    Semaphore jobInfoMutex;
    
    public ProcessManager(){
        hostInfoList=new ArrayList<HostInfo>();
        jobInfoList=new ArrayList<JobInfo>();

        hostInfoMutex=new Semaphore(1);
        jobInfoMutex=new Semaphore(1);
    }

    //static entry point
    public static void main(String args[]){
        if(args.length!=0){
            //slave
            if(args.length==2 && args[0].equals("-c")){
                ProcessManagerSlave pm=new ProcessManagerSlave();
                pm.slave(args[1]);
            }
            else usage();
        }
        else {
            //master
            ProcessManagerMaster pm=new ProcessManagerMaster();
            pm.master();
        }
    }

    static void usage(){
        System.out.println("Usage: ProcessManager [-c host]");
    }

    //private functions
    boolean sendObjectTo(String host,int port, Serializable object){
        try{
            Socket s=new Socket(host,port);

            ObjectOutputStream out=new ObjectOutputStream(s.getOutputStream());
            out.writeObject(object);
            out.flush();
        }
        catch(IOException e){
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
