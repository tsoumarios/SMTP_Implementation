import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.crypto.spec.IvParameterSpec;

import java.util.concurrent.atomic.AtomicBoolean;

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

    private static String secretKey = "kdfslksdnflsdfsd";

    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";

    public static String encrypt(String value) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        byte[] values = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(values);
    }

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
            System.out.println( // Print the List of demo emails and ask user to type an email.
                    "\n--------------------------\n" + ConsoleColors.PURPLE_UNDERLINED + "Demo Emails:"
                            + ConsoleColors.RESET
                            + "\nALICE@ThatDomain.gr\nmyEmail@MyTestDomain.gr\nmyEmail@ServerDomain.gr\nreceip@MyTestDomain.gr \n"
                            + "--------------------------\n\n" + "Please type your email: ");

            String email = user_input.nextLine();
            // ecryption
            dataOut.writeUTF(encrypt(email));
            dataOut.flush(); // Send user given email address in order to verify and connect

            while (!cwSocket.isClosed() && !isDATAflag.get()) { // While the user is connected

                TimeUnit.SECONDS.sleep(1); // Wait the response from the server

                isLogedIn = ClientReader.isLogedIn(); // True if a user is veryfied

                // if user is Loged In
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
                            dataOut.writeUTF(encrypt(msgToServer));
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

                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
                            dataOut.flush();
                            break;

                        }

                        // RSET Command
                        case "6": {

                            System.out.println("CLIENT WRITER CONVERSATION RESET");
                            System.out.println("--------------------------------");

                            msgToServer = ("RSET" + CRLF);
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
                            dataOut.flush();
                            break;

                        }
                        // QUIT Command
                        case "8": {

                            System.out.print("CLIENT : QUITing");

                            msgToServer = ("QUIT" + CRLF);
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                            dataOut.writeUTF(encrypt(msgToServer));
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
                    dataOut.writeUTF(encrypt(email));
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
