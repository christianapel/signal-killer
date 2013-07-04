package hudson.plugins.signal_killer;

import hudson.Extension;
import hudson.util.ProcessKiller;
import hudson.util.ProcessTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class SignalKiller extends ProcessKiller {
	//standard signals, see signum.h (or man 7 signal)
	public static final int SIGABRT = 6; //Abnormal termination.
	public static final int SIGKILL = 9; //Kill (cannot be blocked, caught, or ignored).
	public static final int SIGTERM = 15; //Termination request.
	
	/**
	 * Default constructor.
	 * Does nothing, just logs that plugin instance was created on Hudson master.
	 */
	public SignalKiller() {
		LOGGER.fine("SignalKiller initialized");
	}
	
	@Override
	public boolean kill(ProcessTree.OSProcess process) throws IOException, InterruptedException {
		int retVal = sendSignal(process.getPid(), SIGKILL);
		if(retVal == 0)
			return true;
		return false;
	}
	
	/**
	 * Call GNU libc kill function to send kill signal to process, 
	 * see http://www.gnu.org/s/libc/manual/html_mono/libc.html#Signaling-Another-Process
     * 
     * On Windows, execute the taskkill command instead.
	 * 
	 * @param pid
	 * @param signal
	 * @return zero if the signal can be sent successfully. Otherwise, no signal is sent, and a value of -1 is returned.
	 */
	private int sendSignal(int pid, int signal) {
      int returnValue = -1;
      LOGGER.fine("Sending signal " + signal + " to process " + pid);

      String osName = System.getProperty("os.name");
      if (osName.startsWith("Windows")) {
        try {
          List<String> command = new ArrayList();
          command.add("taskkill");
          if (signal != SIGTERM) command.add("/F");
          command.add("/PID");
          command.add(Integer.toString(pid));
          
          ProcessBuilder pb = new ProcessBuilder(command);
          Process process = pb.start();
          process.waitFor();
          
          int exitCode = process.exitValue();
          if (exitCode == 0) returnValue = 0;
        } catch (IOException ex) {
          LOGGER.log(Level.SEVERE, "Couldn't kill process " + pid, ex);
        } catch (InterruptedException ex) {
          LOGGER.log(Level.SEVERE, "Couldn't kill process " + pid, ex);
        }
      } else {
        try {
          returnValue = hudson.util.jna.GNUCLibrary.LIBC.kill(pid, SIGKILL);
        } catch (LinkageError er) {
          LOGGER.log(Level.SEVERE, "Couldn't find native library!", er);
        }
      }
      
      if (returnValue == 0) {
        LOGGER.fine("Successfully killed process " + pid);
      } else {
        LOGGER.fine("Process " + pid + " wasn't killed!");
      }
      return returnValue;
	}
	
	private static final Logger LOGGER = Logger.getLogger(SignalKiller.class.getName());
}
