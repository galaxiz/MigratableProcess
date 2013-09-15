import java.lang.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * @author Xi Zhao
 */
public class ProcessManagerMaster extends ProcessManager{
    //other jvm info
    ArrayList<HostInfo> hostInfoList;
    //jvm info mutex
    Semaphore hostInfoMutex;
    
    final int MasterPeriod=15000;

    public ProcessManagerMaster(){
        hostInfoList=new ArrayList<HostInfo>();
        hostInfoMutex=new Semaphore(1);
    }

    public void master(){
        new Timer(true).scheduleAtFixedRate(new TimerTask(){
            public void run(){
                //host heartbeat and workload checker
                checkHeartbeatAndWorkload();
            }
        },0,MasterPeriod);

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
        
    	System.out.println("Checker:"+curTime/1000);

		while (true) {
			try {
				hostInfoMutex.acquire();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        for(Iterator<HostInfo> it=hostInfoList.iterator();it.hasNext();){
            HostInfo hostp=it.next();
            if(curTime-hostp.lastTime>MasterPeriod){
                System.out.println(hostp.host+"(slave) is unreachable.");
                it.remove();
            }
        }
        hostInfoMutex.release();
        
        //workload
		while (true) {
			try {
				hostInfoMutex.acquire();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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

            if(maxCount-minCount>=2){
                //Send command to minHost to request a job from maxHost
                //to do sync between two slaves and master.
                CommandMsg cmsg=new CommandMsg();
                cmsg.type=CommandMsg.Type.requestJob;
                cmsg.args=maxHost.host+":"+maxHost.port.toString();

                sendObjectTo(minHost.host,minHost.port,cmsg);

                //to do do not modify jobCount until they heartbeat to master 
                //yes but I cannot always make it to request job.
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
                    while(true){
						try {
							hostInfoMutex.acquire();
							break;
						}
                    	catch(InterruptedException e){
                    		e.printStackTrace();
                    	}
                    }
                    for(HostInfo hostp:hostInfoList){
                        System.out.println(hostp.host+":"+hostp.port.toString()+" job count:"+hostp.jobCount.toString());
                    }
                    hostInfoMutex.release();
                }
                else {
                    //try to create a new job
                    //to do
                    CommandMsg cm=new CommandMsg();
                    cm.type=CommandMsg.Type.newJob;
                    cm.args=line;

                    while(true){
						try {
							hostInfoMutex.acquire();
							break;
						}
                    	catch(InterruptedException e){
                    		e.printStackTrace();
                    	}
                    }

                    //to do currently create job to first host
                    //HostInfo randHostInfo=hostInfoList.get(new Random().nextInt(hostInfoList.size()));
                    HostInfo randHostInfo=hostInfoList.get(0);
                    randHostInfo.jobCount++;
                    sendObjectTo(randHostInfo.host,randHostInfo.port,cm);

                    hostInfoMutex.release();
                }
            }catch(IOException e){
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
            int port=beat.port;
            
            //System.out.println("heartbeat type:"+beat.type.toString());

			switch (beat.type) {
			case normal:
				// find HostInfo with ip;
				while (true) {
					try {
						hostInfoMutex.acquire();
						break;
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				//to do change to hash instead of search
				for (Iterator<HostInfo> it = hostInfoList.iterator(); it
						.hasNext();) {
					HostInfo hostp = it.next();

					if (hostp.host.equals(ip) && hostp.port==port) {
						hostp.jobCount = beat.jobCount;
						hostp.lastTime = System.currentTimeMillis();
						break;
					}
				}
				hostInfoMutex.release();

				break;

			case reg:
				// register the host
				HostInfo hi = new HostInfo();
				hi.host = ip;
				hi.port = port; // to do
				hi.jobCount = 0;
				hi.lastTime = System.currentTimeMillis();

				hostInfoMutex.acquire();
				hostInfoList.add(hi);
				hostInfoMutex.release();
				
				//response ok to do
				PrintWriter out=new PrintWriter(socket.getOutputStream());
				out.write("ok\n");
				out.flush();
				
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
