import java.io.*;
import java.net.*;
import java.util.Scanner;

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

// This thread is responcible for writing messages
class ClientReader implements Runnable {
    public static String ClientDomainName = "MyTestDomain.gr";
    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String EC = " ";

    Socket crSocket = null;
    AtomicBoolean isDATAflag;
    String BYTESin = "";
    String sDataToServer;

    public ClientReader(Socket inputSoc, AtomicBoolean isDATA) {
        crSocket = inputSoc;
        this.isDATAflag = isDATA;
    }

    public void run() {

        while (!crSocket.isClosed() && !isDATAflag.get()) {
            // while connection is open and NOT IN DATA exchange STATE
            try {
                DataInputStream dataIn = new DataInputStream(crSocket.getInputStream());

                BYTESin = dataIn.readUTF();

                System.out.println("CLIENT : message FROM SERVER : " + BYTESin);

                if (BYTESin.contains("221")) {
                    System.out.println("...closing socket");
                    crSocket.close();
                    return;
                } else if (BYTESin.contains("250")) {
                    System.out.println("OK -> CLIENT going to state SUCCESS");
                } else if (BYTESin.contains("500"))
                    System.out.println("SERVER Error--> Syntax error, command unrecognized");
                else if (BYTESin.contains("501"))
                    System.out.println("SERVER Error--> Syntax error in parameters or arguments");
                else if (BYTESin.contains("504"))
                    System.out.println("SERVER Error--> Command parameter not implemented");
                else if (BYTESin.contains("421"))
                    System.out.println("SERVER Error-->Service not available, closing transmission channel");
                else if (BYTESin.contains("354")) {
                    System.out.println("OK -> CLIENT going to state I (wait for data)");
                    isDATAflag.set(true);
                }
            } catch (Exception except) {
                // Exception thrown (except) when something went wrong, pushing message to the
                // console
                System.out.println("Error in ClientReader --> " + except.getMessage());
            }
        }
    }
}

class ClientWriter implements Runnable {
    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String EC = " ";
    public static String ClientDomainName = "MyTestDomain.gr";
    public static String ClientEmailAddress = "myEmail@" + ClientDomainName;

    Socket cwSocket = null;
    AtomicBoolean isDATAflag;

    public ClientWriter(Socket outputSoc, AtomicBoolean isDATA) {
        cwSocket = outputSoc;
        this.isDATAflag = isDATA;
    }

    public void run() {
        String msgToServer = "";
        String BYTESin = "";
        String ClientDomainName = "MyTestDomain.gr";
        String RCPTEmail = "receip@gmail.com";
        String DataToSend = "This is the body of the message";
        try {
            System.out.println(
                    "CLIENT WRITER: SELECT NUMBER CORRESPONDING TO SMTP COMMAND 1...HELO 2...MAIL TO 3...RECV FROM 4...DATA 5...msg 6...QUIT");
            DataOutputStream dataOut = new DataOutputStream(cwSocket.getOutputStream());

            while (!cwSocket.isClosed()) {
                Scanner user_input = new Scanner(System.in);
                switch (user_input.nextInt()) {
                    case 1: {
                        System.out.println("CLIENT WRITER SENDING HELLO");
                        System.out.println("--------------------------");
                        System.out.println(ConsoleColors.BLUE + "Sending..." + ConsoleColors.RESET + " HELLO" + EC
                                + ClientDomainName + CRLF);

                        msgToServer = ("HELLO" + EC + ClientDomainName + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        break;
                    }
                    case 2: {
                        System.out.println("CLIENT WRITER SENDING MAIL FROM");
                        System.out.println("--------------------------");
                        System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "MAIL FROM:"
                                + EC + ClientEmailAddress + CRLF);

                        msgToServer = ("MAIL FROM:" + EC + ClientEmailAddress + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();

                        break;
                    }
                    case 3: {
                        System.out.println("CLIENT WRITER SENDING RCPT TO");
                        System.out.println("--------------------------");
                        System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "RCPT TO:"
                                + EC + RCPTEmail + CRLF);

                        msgToServer = ("RCPT TO:" + EC + RCPTEmail + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        break;
                    }
                    case 4: {
                        System.out.println("CLIENT WRITER SENDING DATA");
                        System.out.println("--------------------------");
                        System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "DATA:" + EC
                                + DataToSend + CRLF);

                        msgToServer = ("DATA:" + EC + DataToSend + CRLF + "." + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        break;
                    }
                    case 5: {

                        break;
                    }
                    case 6: {
                        System.out.print("CLIENT : QUITing");
                        //
                        // SYNTAX (page 12 RFC 821)
                        // QUIT <CRLF>
                        //
                        msgToServer = ("QUIT" + CRLF);
                        dataOut.writeUTF(msgToServer);
                        dataOut.flush();
                        System.out.println("...closing socket ");
                        return;
                    } // case
                } // switch
            } // while
        } // try
        catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error in ClientWriter --> " + except.getMessage());
        }
    }
}
