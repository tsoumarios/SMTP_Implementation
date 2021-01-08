import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.crypto.spec.IvParameterSpec;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicBoolean;

class ClientWriter implements Runnable {
    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String EC = " ";
    public static String SPACE = "      ";
    public static String ClientDomainName = "";
    public static String ClientEmailAddress = "";
    ArrayList<String> RCPTEmail = new ArrayList<String>();

    public static String ClientMsg = "This is a simple message end with" + CRLF + "." + CRLF;
    public static Boolean isLogedIn = false;

    Socket cwSocket = null;
    AtomicBoolean isDATAflag;

    // Variables used in encryption
    private static String secretKey = "kdfslksdnflsdfsd";

    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";

    // Class construtor
    public ClientWriter(Socket outputSoc, AtomicBoolean isDATA) {
        cwSocket = outputSoc;
        this.isDATAflag = isDATA;
    }

    // Method for encription
    public static String encrypt(String value) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        byte[] values = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(values);
    }

    // Print the command board
    private void userChoice() {
        System.out.println("\nCLIENT WRITER: SELECT NUMBER CORRESPONDING TO SMTP COMMAND:" + CRLF + " 1...<HELO>"
                + SPACE + " 2...<MAIL FROM>" + EC + EC + "  3...<RCPT TO>" + CRLF + " 4...<DATA>" + SPACE
                + " 5...<NOOP>" + SPACE + "   6...<RSET>" + CRLF + " 7...<VRFY>" + SPACE + " 8...<EXPN>" + SPACE
                + "   9...<HELP>" + CRLF + " 10...<QUIT>" + CRLF + "\nType <mailbox> to receive your emails.");
    }

    public void run() {

        String msgToServer = "";

        try {

            Scanner user_input = new Scanner(System.in);// Scanner for user input
            DataOutputStream dataOut = new DataOutputStream(cwSocket.getOutputStream()); // Initialize an output
            // stream
            System.out.println( // Print the List of demo emails and ask user to type an email.
                    "\n--------------------------\n" + ConsoleColors.PURPLE_UNDERLINED + "Demo Emails:"
                            + ConsoleColors.RESET
                            + "\nalice@ThatDomain.gr\nbob@MyTestDomain.gr\njack@ServerDomain.gr\njames_bond@ThatDomain.gr \n"
                            + ConsoleColors.YELLOW_UNDERLINED + "Password for all users is:" + ConsoleColors.RESET
                            + " 123456\n" + "--------------------------\n\n");

            String email;

            // Ask for user's credentials while user is not loged in

            do {
                System.out.println("Please type your email: ");
                email = user_input.nextLine();
                // ecryption
                dataOut.writeUTF(encrypt(email)); // Data encryption and Send data to client
                dataOut.flush(); // Send user given email address in order to verify and connect

                System.out.println("Please type your password: ");
                String password = user_input.nextLine();
                // ecryption
                dataOut.writeUTF(encrypt(password)); // Data encryption and Send data to client
                dataOut.flush(); // Send user given email address in order to verify and connect
                TimeUnit.SECONDS.sleep(3); // Wait the response from the server
                isLogedIn = ClientReader.isLogedIn(); // True if a user is veryfied
            } while (!isLogedIn);

            while (!cwSocket.isClosed() && !isDATAflag.get()) { // While the user is connected
                isLogedIn = ClientReader.isLogedIn(); // True if a user is veryfied
                ClientDomainName = email.substring(email.indexOf("@"));
                ClientDomainName = ClientDomainName.replace("@", "");// Find the domain name
                ClientEmailAddress = email;

                // if user is Loged In
                TimeUnit.SECONDS.sleep(1); // Wait the response from the server
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();

                            break;
                        }
                        // MAIL FROM Command
                        case "2": {

                            System.out.println("CLIENT WRITER SENDING MAIL FROM");
                            System.out.println("--------------------------");
                            System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET
                                    + "MAIL FROM:" + EC + ClientEmailAddress + CRLF);
                            // The client email given hardcoded for testing purposes

                            msgToServer = ("MAIL FROM" + EC + ClientEmailAddress + CRLF);

                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();

                            break;
                        }
                        // RCPT TO Command
                        case "3": {

                            // Here for testing puporses added hardcoded two recipients
                            // to the RCPTEmail List for testing purposes
                            RCPTEmail.add("james_bond@ThatDomain.gr");
                            RCPTEmail.add("bob@MyTestDomain.gr");

                            System.out.println("CLIENT WRITER SENDING RCPT TO");
                            System.out.println("--------------------------");

                            // Loop through the Recipient list to print them all
                            for (String rcpt : RCPTEmail) {
                                System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET
                                        + "RCPT TO:" + EC + rcpt + CRLF);
                            }
                            // Loop through the Recipient list to send them all
                            for (String rcpt : RCPTEmail) {
                                msgToServer = ("RCPT TO" + EC + rcpt + CRLF);
                                dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                                dataOut.flush();
                            }

                            break;
                        }
                        // DATA Command
                        case "4": {

                            System.out.println("CLIENT WRITER SENDING DATA");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "DATA" + CRLF);

                            msgToServer = ("DATA" + CRLF + LF + ClientMsg);
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();
                            break;

                        }

                        // RSET Command
                        case "6": {

                            System.out.println("CLIENT WRITER CONVERSATION RESET");
                            System.out.println("--------------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "RSET" + CRLF);

                            msgToServer = ("RSET" + CRLF);
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();

                            break;

                        }

                        // VRFY Command
                        case "7": {

                            System.out.println("CLIENT SEND VRFY");
                            System.out.println("--------------------------------");
                            System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET
                                    + "VRFY Bob Marley" + CRLF);

                            msgToServer = ("VRFY" + EC + "Bob Marley" + CRLF);
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();

                            break;

                        }

                        // EXPN Command
                        case "8": {

                            System.out.println("CLIENT SEND EXPN");
                            System.out.println("--------------------------------");
                            System.out.println(ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET
                                    + "EXPN Userlist" + CRLF);

                            msgToServer = ("EXPN" + EC + "Userlist" + CRLF);
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();

                            break;

                        }

                        // HELP Command

                        case "9": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP" + CRLF);
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();
                            break;

                        }
                        // QUIT Command
                        case "10": {

                            System.out.print("CLIENT : QUITing");

                            msgToServer = ("QUIT" + CRLF);
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();
                            break;

                        }
                        case "HELP-RSET": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING HELP RSET");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "HELP" + CRLF);

                            msgToServer = ("HELP -RSET");
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
                            dataOut.flush();
                            break;

                        }
                        case "mailbox": {
                            // CLIENT WRITER SET HELP
                            System.out.println("CLIENT WRITER SENDING mailbox");
                            System.out.println("--------------------------");
                            System.out.println(
                                    ConsoleColors.BLUE + "Sending..." + EC + ConsoleColors.RESET + "mailbox" + CRLF);

                            msgToServer = ("mailbox\n");
                            dataOut.writeUTF(encrypt(msgToServer)); // Data encryption and Send data to clients
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
                    dataOut.writeUTF(encrypt(email)); // Data encryption and Send data to clients
                    dataOut.flush();// Send user given email address in order to verify and connect

                }

            } // while
            isLogedIn = false; // When use is logged out
            user_input.close();
        } // try

        catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println(
                    ConsoleColors.RED + "Error in ClientWriter --> " + except.getMessage() + ConsoleColors.RESET);
        }
    } // END run()
} // END of class
