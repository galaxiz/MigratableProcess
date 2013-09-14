import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;

public class ProcessManager{
    //master or slave
    boolean isMaster;

    //other jvm info
    ArrayList<HostInfo> hostInfoList;
    
    //process info
    ArrayList<JobInfo> jobInfoList;
    
    public ProcessManager(){
        hostInfoList=new ArrayList<HostInfo>();
        jobInfoList=new ArrayList<JobInfo>();
    }

    public void master(){
        isMaster=true;

        new Timer(true).scheduleAtFixedRate(new TimerTask(){
            public void run(){
                //host heartbeat and workload checker
                checkHeartbeatAndWorkload();
            }
        },0,15*1000);

        //to do master listener RPC method for slave
        new Thread(new Runnable(){
            public void run(){
                //master listener
                heartbeatListener();
            }
        }).start();

        new Thread(new Runnable(){
            public void run(){
                prompt();
            }
        }).start();
    }

    public void slave(String host){
        isMaster=false;

        registerSlave();

        new Thread(new Runnable(){
            public void run(){
                //slave listener
                commandListener();
            }
        }).start();

        new Timer(true).scheduleAtFixedRate(new TimerTask(){
            public void run(){
                //send heartbeat
                heartbeatSender();
            }
        },0,3*1000);
    }

    //static entry point
    
    public static void main(String args[]){
        ProcessManager pm=new ProcessManager();

        if(args.length!=0){
            //slave
            if(args.length==2 && args[0].equals("-c")){
                pm.slave(args[1]);
            }
            else usage();
        }
        else {
            //master
            pm.master();
        }
    }

    public static void usage(){
        System.out.println("Usage: ProcessManager [-c host]");
    }

    //private functions
    
    //for both
    boolean sendObjectTo(String host,int port, Serializable object){
        try{
            Socket s=new Socket(host,port);

            ObjectOutputStream out=new ObjectOutputStream(s.getOutputStream());
            out.writeObject(object);
            out.flush();
        }
        catch(IOException e){
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    //functions for master

    void checkHeartbeatAndWorkload(){
        //to do samophore

        //heartbeat
        long curTime=System.currentTimeMillis();

        for(Iterator<HostInfo> it=hostInfoList.iterator();it.hasNext();){
            HostInfo hostp=it.next();
            if(curTime-hostp.lastTime>15000){
                System.out.println(hostp.host+"(slave) is unreachable.");
                it.remove();
            }
        }
        
        //workload
        while(true){
            int minCount=9999,maxCount=-1;
            HostInfo minHost=null,maxHost=null;

            for(Iterator<HostInfo> it=hostInfoList.iterator();it.hasNext();){
                HostInfo hostp=it.next();

                if(hostp.jobCount>maxCount){
                    maxCount=hostp.jobCount;
                    maxHost=hostp;
                }
                if(hostp.jobCount<minCount){
                    minCount=hostp.jobCount;
                    minHost=hostp;
                }
            }

            if(maxCount-minCount>1){
                //Send command to minHost to request a job from maxHost
                CommandMsg cmsg=new CommandMsg();
                cmsg.type=CommandMsg.Type.requestJob;
                cmsg.args=maxHost.host;

                sendObjectTo(minHost.host,9001,cmsg);

                //modify 
                minHost.jobCount++;
                maxHost.jobCount--;
            }
            else break;
        }
    }
    
    void prompt(){
        String line;
        
        while(true){
            try{
                System.out.print("> ");
                BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
                line=br.readLine();

                System.out.print(line+"\n");

                if(line.equals("exit")){
                    break;
                }
                else if(line.equals("ps")){
                    //show ps info on all slaves?
                }
                else {
                    //try to create a new job
                }
            }catch(IOException e){
                System.out.println(e.toString());
            }
        }
    }

    void heartbeatListener(){
        //to do listening to a fixed port 9000
        try{
            ServerSocket socket=new ServerSocket(9000);
            while(true){
                final Socket insocket=socket.accept();

                new Thread(new Runnable(){
                    public void run(){
                        heartbeatHandler(insocket);
                    }
                }).start();
            }
        }catch(IOException e){
            System.out.println(e.toString());
        }

    }

    boolean heartbeatHandler(Socket socket){
        //to do mutex
        
        try{
            ObjectInputStream in=new ObjectInputStream(socket.getInputStream());
            HeartbeatMsg beat=(HeartbeatMsg)in.readObject();

            String ip=socket.getInetAddress().getHostAddress();

            if(beat.type==Heartbeat.Type.normal){
                //find HostInfo with ip;
            }
            else {
                //register the host
            }
        }
        catch(IOException e){
            System.out.println(e.toString());
        }
        catch(ClassNotFoundException e){
            System.out.println(e.toString());
        }

        return true;
    }

    //functions for slave
    
    void registerSlave(){
    }

    void commandListener(){
        //to do listening to a fixed port 9001
        try{
            ServerSocket socket=new ServerSocket(9000);

            while(true){
                final Socket insocket=socket.accept();

                new Thread(new Runnable(){
                    public void run(){
                        commandHandler(insocket);
                    }
                }).start();
            }
        }
        catch(IOException e){
            System.out.println(e.toString());
        }
    }

    void heartbeatSender(){
        //to do
    }

    boolean newJob(String command,String args[]){

        try{
            JobInfo jobInfo=new JobInfo();

            Class jobClass=Class.forName(command);
            MigratableProcess job=(MigratableProcess) jobClass.getConstructor(String[].class).newInstance(args);

            jobInfo.job=job;
            jobInfoList.add(jobInfo);

            new Thread((Runnable)job).start();
        }catch(Exception e){
            //to do different exceptions
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    void commandHandler(Socket socket){
    }

    boolean sendJob(JobInfo jobInfo){
        //suspend
        jobInfo.job.suspend();

        //serialize
        return false;
    }

    boolean resumeJob(){
        return false;
    }
}

class HeartbeatMsg implements Serializable{
    public enum Type{
        normal,reg;
    }

    Type type;

    public int jobCount;
}

class HeartbeatResponse implements Serializable{
    public String response;
}

class CommandMsg implements Serializable{
    public enum Type{
        newJob,killJob,requestJob,waitJob;
    }

    Type type;

    public String args;
}

class CommandResponse implements Serializable{
}
