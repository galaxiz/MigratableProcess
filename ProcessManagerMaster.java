import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;

/* Class ProcessManagerMaster
 * @author Xi Zhao
 */
public class ProcessManagerMaster extends ProcessManager{
    public void master(){
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

    //private functions
    
    void checkHeartbeatAndWorkload(){
        //to do samophore

        //heartbeat
        long curTime=System.currentTimeMillis();

        try{
            hostInfoMutex.acquire();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        for(Iterator<HostInfo> it=hostInfoList.iterator();it.hasNext();){
            HostInfo hostp=it.next();
            if(curTime-hostp.lastTime>15000){
                System.out.println(hostp.host+"(slave) is unreachable.");
                it.remove();
            }
        }
        hostInfoMutex.release();
        
        //workload
        try{
            hostInfoMutex.acquire();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
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
                //to do sync between two slaves and master.
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
        hostInfoMutex.release();
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
                    //to do
                    hostInfoMutex.acquire();
                    for(HostInfo hostp:hostInfoList){
                        System.out.println(hostp.host+"'s job count:"+hostp.jobCount.toString());
                    }
                    hostInfoMutex.release();
                }
                else {
                    //try to create a new job
                    //to do
                    CommandMsg cm=new CommandMsg();
                    cm.type=CommandMsg.Type.newJob;
                    cm.args=line;

                    hostInfoMutex.acquire();

                    HostInfo randHostInfo=hostInfoList.get(new Random().nextInt(hostInfoList.size()));
                    randHostInfo.jobCount++;
                    sendObjectTo(randHostInfo.host,9001,cm);

                    hostInfoMutex.release();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
            catch(InterruptedException e){
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    boolean heartbeatHandler(Socket socket){
        //to do mutex
        
        try{
            ObjectInputStream in=new ObjectInputStream(socket.getInputStream());
            HeartbeatMsg beat=(HeartbeatMsg)in.readObject();

            String ip=socket.getInetAddress().getHostAddress();

            switch(beat.type){
                case normal:
                    //find HostInfo with ip;
                    hostInfoMutex.acquire();
                    for(Iterator<HostInfo> it=hostInfoList.iterator();it.hasNext();){
                        HostInfo hostp=it.next();

                        if(hostp.host.equals(ip)){
                            hostp.jobCount=beat.jobCount;
                            break;
                        }
                    }
                    hostInfoMutex.release();

                    break;

                case reg:
                    //register the host
                    HostInfo hi=new HostInfo();
                    hi.host=ip;

                    hostInfoMutex.acquire();
                    hostInfoList.add(hi);
                    hostInfoMutex.release();
                    break;
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

        return true;
    }

}
