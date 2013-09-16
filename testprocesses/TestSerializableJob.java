package testprocesses;
import java.io.Serializable;

import processinterface.MigratableProcess;

/**
 * 
 * @author Xi Zhao
 *
 */
public class TestSerializableJob implements MigratableProcess{
	volatile boolean suspending=false;
	int i;
	
	public TestSerializableJob(){
		suspending=false;
		i=0;
	}
	
	public TestSerializableJob(String args[]){
		this();
		
		if(args==null){
			System.out.println("No args");
		}
		else {
			for(String arg:args){
				System.out.println(args);
			}
		}
	}
	@Override
	public void run() {
		while(!suspending && i<20*5){
			System.out.println(i);
			i++;
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		
		suspending=false;
	}

	@Override
	public void suspend() {
		// TODO Auto-generated method stub
		suspending=true;
		
		while(suspending);
	}

}
