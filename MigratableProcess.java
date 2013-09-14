import java.io.Serializable;

/**
 * This interface defines the actions of Migratable Processes
 * Migratable Processes should be able to suspend and are serializable 
 * in order to migrate through networks. 
 * This interface also extends java.lang.Runnable in order to be run as multi-threaded
 */

/**
 * @author Shiwei Dong
 * 
 */
public interface MigratableProcess extends Serializable, Runnable {
	
	//suspend the current process
	void suspend();
	
	String toString();
	
}
