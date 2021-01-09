import java.io.*;
import java.net.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.crypto.spec.IvParameterSpec;

import java.util.concurrent.atomic.AtomicBoolean;

// This thread is responsible for writing messages
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

    // Class constructor
    public ClientReader(Socket inputSoc, AtomicBoolean isDATA) {
        crSocket = inputSoc;
        this.isDATAflag = isDATA;
    }

    // This static function return true if a user is veryfied
    public static Boolean isLogedIn() {
        return isLogedIn;
    }

    // Variables used for data decryption
    private static String secretKey = "kdfslksdnflsdfsd";

    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";

    // Decryption method
    public static String decrypt(String value) throws Exception {
        byte[] values = Base64.getDecoder().decode(value);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        return new String(cipher.doFinal(values));
    }

    public void run() {

        while (!crSocket.isClosed()) {
            // while connection is open and NOT IN DATA exchange STATE
            try {

                DataInputStream dataIn = new DataInputStream(crSocket.getInputStream());

                BYTESin = decrypt(dataIn.readUTF());
                // Check all SMTP response messages
                if (BYTESin.contains("221")) {
                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED + "...closing socket" + ConsoleColors.RESET + "\n");
                    crSocket.close();
                    return;
                } else if (BYTESin.contains("101")) {
                    System.out.println(
                            ConsoleColors.YELLOW + "The server is unable to connect." + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("111")) {
                    System.out.println(ConsoleColors.YELLOW + "Connection refused or inability to open an SMTP stream."
                            + ConsoleColors.RESET + "\n");
                    System.out.println("\nSERVER response: " + BYTESin);
                } else if (BYTESin.contains("211")) {
                    System.out.println(
                            ConsoleColors.YELLOW + "System status message or help reply." + ConsoleColors.RESET + "\n");
                    System.out.println("\nSERVER response: " + BYTESin);
                } else if (BYTESin.contains("214")) {
                    System.out.println(
                            "\nSERVER response: " + ConsoleColors.YELLOW + BYTESin + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("220")) {
                    System.out.println(ConsoleColors.GREEN + "The server is ready." + ConsoleColors.RESET + "\n");
                    System.out.println(
                            "\nSERVER response: " + ConsoleColors.YELLOW + BYTESin + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("250")) {
                    System.out
                            .println(ConsoleColors.YELLOW + "SERVER response: " + BYTESin + ConsoleColors.RESET + "\n");
                    System.out.println(
                            ConsoleColors.GREEN + "OK -> CLIENT going to state SUCCESS" + ConsoleColors.RESET + "\n");
                    isLogedIn = true;
                    isDATAflag.set(false);
                }
                // When response is 250 and data transmition state is true
                // (After execute success DATA command )
                else if (BYTESin.contains("250") && isDATAflag.get()) {
                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(
                            ConsoleColors.GREEN + "OK -> CLIENT going to state SUCCESS" + ConsoleColors.RESET + "\n");
                    isDATAflag.set(false); // Not in data transmition state
                } else if (BYTESin.contains("251"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> User not local will forward"
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("252"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The server cannot verify the user, but it will try to deliver the message anyway."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("354")) {
                    System.out.println(
                            "\nSERVER response: " + ConsoleColors.GREEN + BYTESin + ConsoleColors.RESET + "\n");
                    System.out.println(ConsoleColors.GREEN + "OK -> CLIENT going to state I (wait for data)"
                            + ConsoleColors.RESET + "\n");
                    isDATAflag.set(true);// In data transmition state

                } else if (BYTESin.contains("401")) {
                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Access denied. Unauthorized user"
                            + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("420"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Timeout connection problem."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("421"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error-->Service not available, closing transmission channel"
                                    + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("422"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The recipient’s mailbox has exceeded its storage limit."
                            + ConsoleColors.RESET + "\n");
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
                    System.out.println(ConsoleColors.RED + "A routing error." + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("450"))
                    System.out.println("Requested action not taken – The user’s mailbox is unavailable.");
                else if (BYTESin.contains("451")) {
                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error-->Requested action aborted – Local error in processing"
                                    + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("452")) {
                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED + "SERVER Error-->Too many emails sent or too many recipients."
                            + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("471")) {
                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error-->An error of your mail server, often due to an issue of the local anti-spam filter."
                            + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("500")) {
                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Syntax error, command unrecognized."
                            + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("501"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Syntax error in parameters or arguments."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("502"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> The command is not implemented."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("503"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The server has encountered a bad sequence of commands, or it requires an authentication."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("504"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Command parameter not implemented"
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("510") || BYTESin.contains("511"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error--> Bad email address." + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("512"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> A DNS error: the host server for the recipient’s domain name cannot be found."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("513"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> Address type is incorrect."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("523"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The total size of your mailing exceeds the recipient server’s limits."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("530"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> Normally, an authentication problem. But sometimes it’s about the recipient’s server blacklisting yours, or an invalid email address."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("541"))
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> The recipient address rejected your message: normally, it’s an error caused by an anti-spam filter."
                            + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("550")) {

                    System.out.println("\nSERVER response: " + BYTESin);
                    System.out.println(ConsoleColors.RED
                            + "SERVER Error--> It usually defines a non-existent email address on the remote side."
                            + ConsoleColors.RESET + "\n");
                } else if (BYTESin.contains("551"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error--> User not local or invalid address – Relay denied."
                                    + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("553"))
                    System.out.println(
                            ConsoleColors.RED + "SERVER Error--> Requested action not taken – Mailbox name invalid."
                                    + ConsoleColors.RESET + "\n");
                else if (BYTESin.contains("554"))
                    System.out.println(ConsoleColors.RED + "SERVER Error--> the transaction has failed."
                            + ConsoleColors.RESET + "\n");
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
