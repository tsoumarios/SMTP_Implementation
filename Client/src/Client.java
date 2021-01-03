import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    // Main Method:- called when running the class file.
    public static void main(String[] args) {

        int portNumber = 5000;
        String serverIP = "localhost";

        try {
            // Create a new socket for communication
            Socket soc = new Socket(serverIP, portNumber);
            // use a semaphpre for thread synchronisation
            AtomicBoolean isDATA = new AtomicBoolean(false);
            // create new instance of the client writer thread, intialise it and start it
            // running
            ClientReader clientRead = new ClientReader(soc, isDATA);
            Thread clientReadThread = new Thread(clientRead);
            // Thread.start() is required to actually create a new thread
            // so that the runnable's run method is executed in parallel.
            // The difference is that Thread.start() starts a thread that calls the run()
            // method,
            // while Runnable.run() just calls the run() method on the current thread
            clientReadThread.start();

            // create new instance of the client writer thread, intialise it and start it
            // running
            ClientWriter clientWrite = new ClientWriter(soc, isDATA);
            Thread clientWriteThread = new Thread(clientWrite);
            clientWriteThread.start();
        } catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error in SMTP_Client --> " + except.getMessage());
        }
    }

}
