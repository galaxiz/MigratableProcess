import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;

/* Class ProcessManagerSlave
 * @author Xi Zhao
 */
public class ProcessManagerSlave extends ProcessManager{
    public void slave(String host){
        registerSlave(host);

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

    //private functions
    void registerSlave(String host){
        HeartbeatMsg hmsg=new HeartbeatMsg();
        hmsg.type=HeartbeatMsg.Type.reg;
        hmsg.jobCount=0;

        sendObjectTo(host,9000,hmsg);
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
            e.printStackTrace();
        }
    }

    void heartbeatSender(){
        //to do
        HeartbeatMsg beat=new HeartbeatMsg();
        beat.type=HeartbeatMsg.Type.normal;
        beat.jobCount=jobInfoList.size();

        sendObjectTo(hostInfoList.get(0).host,9000,beat);
    }

    boolean newJob(String command,String args[]){
        try{
            JobInfo jobInfo=new JobInfo();

            Class jobClass=Class.forName(command);
            MigratableProcess job=(MigratableProcess) jobClass.getConstructor(String[].class).newInstance(args);

            jobInfo.job=job;
            
            jobInfoMutex.acquire();
            jobInfoList.add(jobInfo);
            jobInfoMutex.release();

            new Thread((Runnable)job).start();
        }catch(Exception e){
            //to do different exceptions
            e.printStackTrace();
            return false;
        }

        return true;
    }

    void commandHandler(Socket socket){
        try{
            ObjectInputStream in=new ObjectInputStream(socket.getInputStream());
            CommandMsg cmsg=(CommandMsg) in.readObject();

            switch(cmsg.type){
                case newJob:
                    String[] infos=cmsg.args.trim().split(" +");
                    String[] args=new String[infos.length-1];

                    for(int i=1;i<infos.length;i++){
                        args[i-1]=infos[i];
                    }

                    newJob(infos[0],args);
                    break;
                case killJob:
                    //to do
                    break;
                case requestJob:
                    CommandMsg cm=new CommandMsg();
                    cm.type=CommandMsg.Type.waitJob;
                    cm.args="";

                    Socket jobSocket=new Socket(cmsg.args,9001);
                    ObjectOutputStream out=new ObjectOutputStream(jobSocket.getOutputStream());
                    out.writeObject(cm);
                    out.flush();

                    ObjectInputStream jobIn=new ObjectInputStream(jobSocket.getInputStream());
                    MigratableProcess job=(MigratableProcess)jobIn.readObject();
                    
                    resumeJob(job);
                    break;
                case waitJob:
                    JobInfo jobInfo=jobInfoList.get(new Random().nextInt(jobInfoList.size()));
                    sendJob(socket,jobInfo);
                    
                    jobInfoMutex.acquire();
                    jobInfoList.remove(jobInfo);
                    jobInfoMutex.release();
                    break;
                default:
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    boolean sendJob(Socket socket,JobInfo jobInfo){
        //suspend
        jobInfo.job.suspend();

        //serialize
        try{
            ObjectOutputStream out=new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(jobInfo);
            out.flush();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        return true;
    }

    boolean resumeJob(MigratableProcess job){
        JobInfo jobInfo=new JobInfo();

        jobInfo.job=job;

        try{
            jobInfoMutex.acquire();
            jobInfoList.add(jobInfo);
            jobInfoMutex.release();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }

        new Thread((Runnable)job).start();
        return true;
    }
}
