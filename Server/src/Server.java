
//import java.io.*;
import java.net.*;
import java.util.ArrayList;
// import ServerConnectionHandler.java;

public class Server {

    // Main Method:- called when running the class file.
    public static void main(String[] args) {

        // Portnumber:- number of the port we wish to connect on.
        int portNumber = 5000;
        try {
            ServerSocket serverSoc = new ServerSocket(portNumber);
            // Setup the socket for communication
            ArrayList<socketManager> clients = new ArrayList<socketManager>();
            try { // This try is used to finaly close the server socket instance
                while (true) {

                    // accept incoming communication
                    System.out.println("Waiting for client");
                    Socket soc = serverSoc.accept();
                    socketManager temp = new socketManager(soc);
                    clients.add(temp);
                    // create a new thread for the connection and start it.
                    ServerConnectionHandler sch = new ServerConnectionHandler(clients, temp);
                    Thread schThread = new Thread(sch);
                    schThread.start();
                }
            } finally {
                serverSoc.close();
            }

        } catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error --> " + except.getMessage());
        }

    }
}
