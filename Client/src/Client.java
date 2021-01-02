import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

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

    public static Boolean isLogedIn = false;

    public static Boolean isLogedIn() { // This static function return true if a user is veryfied
        return isLogedIn;
    }

    public ClientReader(Socket inputSoc, AtomicBoolean isDATA) {
        crSocket = inputSoc;
        this.isDATAflag = isDATA;
    }

    public void run() {

        while (!crSocket.isClosed()) {
            // while connection is open and NOT IN DATA exchange STATE
            try {
                DataInputStream dataIn = new DataInputStream(crSocket.getInputStream());

                BYTESin = dataIn.readUTF();

                /*****************************************************/
                // **** Must be implemented with Swith - case *********
                if (BYTESin.contains("221")) {
                    System.out.println("SERVER response: " + BYTESin);
                    System.out.println("...closing socket");
                    crSocket.close();
                    return;
                } else if (BYTESin.contains("101")) {
                    System.out.println("The server is unable to connect.");
                } else if (BYTESin.contains("111")) {
                    System.out.println("Connection refused or inability to open an SMTP stream.");
                    System.out.println("SERVER response: " + BYTESin);
                } else if (BYTESin.contains("211")) {
                    System.out.println("System status message or help reply.");
                    System.out.println("SERVER response: " + BYTESin);
                } else if (BYTESin.contains("214")) {
                    System.out.println("SERVER response: " + BYTESin);
                } else if (BYTESin.contains("220")) {
                    System.out.println(ConsoleColors.GREEN + "The server is ready." + ConsoleColors.RESET);
                    System.out.println("SERVER response: " + BYTESin);
                } else if (BYTESin.contains("250")) {
                    System.out.println("SERVER response: " + BYTESin);
                    System.out
                            .println(ConsoleColors.GREEN + "OK -> CLIENT going to state SUCCESS" + ConsoleColors.RESET);
                    isLogedIn = true;
                    isDATAflag.set(false);
                } else if (BYTESin.contains("250") && isDATAflag.get()) {
                    System.out.println("SERVER response: " + BYTESin);
                    System.out
                            .println(ConsoleColors.GREEN + "OK -> CLIENT going to state SUCCESS" + ConsoleColors.RESET);
                    isDATAflag.set(false);
                } else if (BYTESin.contains("251"))
                    System.out.println("SERVER Error--> User not local will forward");
                else if (BYTESin.contains("252"))
                    System.out.println(
                            "SERVER Error--> The server cannot verify the user, but it will try to deliver the message anyway.");
                else if (BYTESin.contains("354")) {
                    System.out.println("SERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.GREEN + "OK -> CLIENT going to state I (wait for data)"
                            + ConsoleColors.RESET);
                    isDATAflag.set(true);

                } else if (BYTESin.contains("420"))
                    System.out.println("SERVER Error--> Timeout connection problem.");
                else if (BYTESin.contains("421"))
                    System.out.println("SERVER Error-->Service not available, closing transmission channel");
                else if (BYTESin.contains("422"))
                    System.out.println("SERVER Error--> The recipient’s mailbox has exceeded its storage limit.");
                else if (BYTESin.contains("431"))
                    System.out.println("Not enough space on the disk, out of memory");
                else if (BYTESin.contains("432"))
                    System.out.println("The recipient’s Exchange Server incoming mail queue has been stopped.");
                else if (BYTESin.contains("441"))
                    System.out.println("The recipient’s server is not responding.");
                else if (BYTESin.contains("442"))
                    System.out.println("The connection was dropped during the transmission.");
                else if (BYTESin.contains("446"))
                    System.out.println(
                            "The maximum hop count was exceeded for the message: an internal loop has occurred.");
                else if (BYTESin.contains("447"))
                    System.out.println(
                            "Your outgoing message timed out because of issues concerning the incoming server.");
                else if (BYTESin.contains("449"))
                    System.out.println("A routing error.");
                else if (BYTESin.contains("450"))
                    System.out.println("Requested action not taken – The user’s mailbox is unavailable.");
                else if (BYTESin.contains("451")) {
                    System.out.println("SERVER response: " + BYTESin);
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error-->Requested action aborted – Local error in processing"
                                    + ConsoleColors.RESET);
                } else if (BYTESin.contains("452")) {
                    System.out.println("SERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED + "SERVER Error-->Too many emails sent or too many recipients."
                            + ConsoleColors.RESET);
                } else if (BYTESin.contains("471")) {
                    System.out.println("SERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error-->An error of your mail server, often due to an issue of the local anti-spam filter."
                            + ConsoleColors.RESET);
                } else if (BYTESin.contains("500"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Syntax error, command unrecognized."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("501"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Syntax error in parameters or arguments."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("502"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> The command is not implemented."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("503"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The server has encountered a bad sequence of commands, or it requires an authentication."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("504"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Command parameter not implemented"
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("510") || BYTESin.contains("511"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Bad email address." + ConsoleColors.RESET);
                else if (BYTESin.contains("512"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> A DNS error: the host server for the recipient’s domain name cannot be found."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("513"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error--> Address type is incorrect." + ConsoleColors.RESET);
                else if (BYTESin.contains("523"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The total size of your mailing exceeds the recipient server’s limits."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("530"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> Normally, an authentication problem. But sometimes it’s about the recipient’s server blacklisting yours, or an invalid email address."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("541"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The recipient address rejected your message: normally, it’s an error caused by an anti-spam filter."
                            + ConsoleColors.RESET);
                else if (BYTESin.contains("550")) {
                    isLogedIn = false;
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> It usually defines a non-existent email address on the remote side."
                            + ConsoleColors.RESET);
                } else if (BYTESin.contains("551"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error--> User not local or invalid address – Relay denied."
                                    + ConsoleColors.RESET);
                else if (BYTESin.contains("553"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error--> Requested action not taken – Mailbox name invalid."
                                    + ConsoleColors.RESET);
                else if (BYTESin.contains("554"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error--> the transaction has failed." + ConsoleColors.RESET);
                // PRINT HELP MESSAGES
                else if (BYTESin.contains("\tHELO") || BYTESin.contains("\tRCPT") || BYTESin.contains("\tMAIL")
                        || BYTESin.contains("\tDATA") || BYTESin.contains("\tNOOP") || BYTESin.contains("\tRSET")
                        || BYTESin.contains("\tQUIT")) {
                    System.out.println("=========================================" + ConsoleColors.CYAN
                            + "\n\nCommand: \t " + ConsoleColors.YELLOW + "Informations: \n" + ConsoleColors.RESET
                            + BYTESin + "\n=========================================");
                }
            } catch (Exception except) {
                // Exception thrown (except) when something went wrong, pushing message to the
                // console
                System.out.println(
                        ConsoleColors.RED + "Error in ClientReader --> " + except.getMessage() + ConsoleColors.RESET);
            }
        }
    }
}

class ClientWriter implements Runnable {
    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String EC = " ";
    public static String SPACE = "      ";
    public static String ClientDomainName = "MyTestDomain.gr";
    public static String ClientEmailAddress = "myEmail@" + ClientDomainName;
    public static String RCPTEmail = "receip@MyTestDomain.gr";
    public static String ClientMsg = "This is a simple message end with" + CRLF + "." + CRLF;
    public static Boolean isLogedIn = false;

    Socket cwSocket = null;
    AtomicBoolean isDATAflag;

    private void userChoice() { // Print the command board
        System.out.println("\nCLIENT WRITER: SELECT NUMBER CORRESPONDING TO SMTP COMMAND:" + CRLF + " 1...HELO" + SPACE
                + " 2...MAIL TO" + EC + EC + "  3...RECV FROM" + CRLF + " 4...DATA" + SPACE + " 5...NOOP" + SPACE
                + " 6...RSET" + CRLF + " 7...HELP" + SPACE + " 8...QUIT");
    }

    public ClientWriter(Socket outputSoc, AtomicBoolean isDATA) {
        cwSocket = outputSoc;
        this.isDATAflag = isDATA;
    }

    public void run() {

        String msgToServer = "";

        try {

            Scanner user_input = new Scanner(System.in);// Scanner for user input
            DataOutputStream dataOut = new DataOutputStream(cwSocket.getOutputStream()); // Initialize an output
            // stream
            System.out.println("Please type your email: ");

            String email = user_input.nextLine();
            // ecryption
            dataOut.writeUTF(email);
            dataOut.flush(); // Send user given email address in order to verify and connect

            while (!cwSocket.isClosed() && !isDATAflag.get()) { // While the user is connected

                TimeUnit.SECONDS.sleep(1); // Wait the response from the server

                if (ClientReader.isLogedIn()) { // if a user is veryfied -> Loged In
                    isLogedIn = true;
                } else {
                    isLogedIn = false;
                }

                // if user is connected
                if (isLogedIn) {

                    userChoice(); // Print the command board

                    switch (user_input.next()) { // User input cases

                        // HELO Command
                        case "1": {
                            System.out.println("CLIENT WRITER SENDING HELO");
                            System.out.println("--------------------------");
                            System.out.println(ConsoleColors.BLUE + "Sending..." + ConsoleColors.RESET + " HELO" + EC
                                    + ClientDomainName + CRLF);

                            msgToServer = ("HELO" + EC + ClientDomainName + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();

                            break;
                        }
                        // MAIL FROM Command
                        case "2": {

                            System.out.println("CLIENT WRITER SENDING MAIL FROM");
                            System.out.println("--------------------------");
                            System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET
                                    + "MAIL FROM:" + EC + ClientEmailAddress + CRLF);

                            msgToServer = ("MAIL FROM" + EC + ClientEmailAddress + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();

                            break;
                        }
                        // RCPT TO Command
                        case "3": {

                            System.out.println("CLIENT WRITER SENDING RCPT TO");
                            System.out.println("--------------------------");
                            System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "RCPT TO:"
                                    + EC + RCPTEmail + CRLF);

                            msgToServer = ("RCPT TO" + EC + RCPTEmail + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;
                        }
                        // DATA Command
                        case "4": {

                            System.out.println("CLIENT WRITER SENDING DATA");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "DATA" + CRLF);

                            msgToServer = ("DATA" + CRLF + "From:" + ClientEmailAddress + LF + "To" + RCPTEmail + LF
                                    + LF + ClientMsg);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;
                        }
                        // NOOP Command
                        case "5": {

                            System.out.println("CLIENT WRITER SET NOOP");
                            System.out.println("----------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "NOOP" + CRLF);

                            msgToServer = ("NOOP" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }

                        // RSET Command
                        case "6": {

                            System.out.println("CLIENT WRITER CONVERSATION RESET");
                            System.out.println("--------------------------------");

                            msgToServer = ("RSET" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();

                            break;

                        }

                        // HELP Command

                        case "7": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }
                        // QUIT Command
                        case "8": {

                            System.out.print("CLIENT : QUITing");

                            msgToServer = ("QUIT" + CRLF);
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();

                            System.out.println("...closing socket ");
                            user_input.close();
                            return;

                        }
                        case "HELP-HELO": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP HELO");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -HELO");
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }
                        case "HELP-MAIL": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP HELO");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -MAIL");
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }
                        case "HELP-RCPT": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP HELO");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -RCPT");
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }
                        case "HELP-DATA": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP HELO");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -DATA");
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }
                        case "HELP-RSET": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP RSET");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -HELO");
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }
                        case "HELP-QUIT": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP QUIT");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -QUIT");
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }
                        case "HELP-NOOP": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP HELO");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -NOOP");
                            dataOut.writeUTF(msgToServer);
                            dataOut.flush();
                            break;

                        }

                        default: {
                            System.out.println(ConsoleColors.RED + "Wrong input " + ConsoleColors.RESET + "\n\n");
                            userChoice();// Print the command board
                        }
                    } // switch

                } // else
                else { // If user types an unknown email

                    System.out.println(
                            ConsoleColors.RED + "Wrong email." + ConsoleColors.RESET + " Please type your email: ");

                    email = user_input.nextLine();
                    dataOut.writeUTF(email);
                    dataOut.flush();// Send user given email address in order to verify and connect

                }

                isLogedIn = false; // When use is logged out
            } // try
        } // while
        catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println(
                    ConsoleColors.RED + "Error in ClientWriter --> " + except.getMessage() + ConsoleColors.RESET);
        }
    }
}
