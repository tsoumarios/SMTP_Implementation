import java.io.*;
import java.net.*;
import java.util.ArrayList;
// import ServerConnectionHandler.java;

public class Server {

    // Main Method:- called when running the class file.
    public static void main(String[] args) {

        // Portnumber:- number of the port we wish to connect on.
        int portNumber = 5000;
        try {
            // Setup the socket for communication
            ServerSocket serverSoc = new ServerSocket(portNumber);
            ArrayList<socketManager> clients = new ArrayList<socketManager>();

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

        } catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error --> " + except.getMessage());
        }
    }
}

// ***********************Wrong code***********************
// ********************************************************

// class ServerConnectionHandler implements Runnable {
// socketManager selfs = null;
// ArrayList<socketManager> clients = null;

// public ServerConnectionHandler(ArrayList<socketManager> l, socketManager
// inSoc) {
// selfs = inSoc;
// clients = l;
// }

// public void run() {
// try {
// // Catch the incoming data in a data stream, read a line and output it to the
// // console

// System.out.println("Client Connected");
// while (true) {
// // Print out message
// String message = selfs.input.readUTF();
// System.out.println("--> " + message);

// for (socketManager sm : clients) {
// sm.output.writeUTF(selfs.soc.getInetAddress().getHostAddress() + ":" +
// selfs.soc.getPort()
// + " wrote: " + message);
// }

// }
// // close the stream once we are done with it
// } catch (Exception except) {
// // Exception thrown (except) when something went wrong, pushing message to
// the
// // console
// System.out.println("Error in ServerHandler--> " + except.getMessage());
// }
// }
// }
