import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.IvParameterSpec;

import java.util.ArrayList;

public class ClientHandler {

    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String EC = " ";
    public static String ServerDomainName = "ServerDomain.gr";
    private socketManager _socketMngObjVar = null;
    private String clientMSG = null;
    private String _mailToSend = null;
    private MailBox mailBox;
    private String userEmail;
    Boolean SUCCESS_STATE;

    // Encryption/decryption variables
    private static String secretKey = "kdfslksdnflsdfsd";
    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";

    // List of messages
    private ArrayList<String> mail_data_buffer = new ArrayList<String>();

    // List of user commands
    private ArrayList<String> CommandStack = new ArrayList<String>();

    // List to include the Recipients of the emails
    private ArrayList<String> forward_path_buffer = new ArrayList<String>();

    // List of Senders
    private ArrayList<String> reverse_path_buffer = new ArrayList<String>();

    // List of active clients
    ArrayList<socketManager> _active_clients = null;

    // Return the giving mail in order to store it
    public String GetMail() {
        return _mailToSend;
    }

    // Class constructor
    public ClientHandler(socketManager socMngOV, String clientMSG, ArrayList<socketManager> active_clients) {
        this._socketMngObjVar = socMngOV;
        this.clientMSG = clientMSG;
        this._active_clients = active_clients;
    }

    // Start Handling the active client
    public void Handle(String clientMSG, MailBox mailBox, String userEmail) {
        this.clientMSG = clientMSG;
        this.mailBox = mailBox;
        this.userEmail = userEmail;
        Server_SMTP_Handler(this._socketMngObjVar, this.clientMSG, this.CommandStack, this.forward_path_buffer,
                this.mail_data_buffer, this.reverse_path_buffer);

    }

    // Encryption method
    public static String encrypt(String value) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        byte[] values = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(values);
    }

    // Decryption method
    public static String decrypt(String value) throws Exception {
        byte[] values = Base64.getDecoder().decode(value);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        return new String(cipher.doFinal(values));
    }

    private void queueEMailsInInbox(MailBox mailBox) {

        // ************************************

        // Create the email based on client data
        Mail eMail = new Mail();
        eMail.setEmail(reverse_path_buffer.get(0), forward_path_buffer, String.join("\n", mail_data_buffer));
        System.out.println("Received Email:");
        eMail.printMsg();

        System.out.println("Email queued for delivery:");
        List<String> mailBoxesEmailForwardedTo = new ArrayList<>();
        // Go through each email receipient address, find the mailbox the address belong
        // and save the message to recipients mailbox
        for (String f : forward_path_buffer) {
            System.out.println(mailBox);
            String mailBoxId = this.mailBox.getMailBoxId(f);
            System.out.println(mailBoxId);
            if (!mailBoxesEmailForwardedTo.contains(mailBoxId)) {
                mailBoxesEmailForwardedTo.add(mailBoxId);
                mailBox.saveMail(mailBoxId, eMail);
                System.out.println("\tEmail saved in mail box " + mailBoxId + " for " + f);
            } else {
                System.out.println("\tEmail copy already present in shared mail box " + mailBoxId + " for " + f);
            }

        }
    }

    // Server_SMTP_Handler is responsible to Handle all possible SMTP requests
    public void Server_SMTP_Handler(socketManager sm, String clientMSG, ArrayList<String> CommandStack,
            ArrayList<String> Recipients, ArrayList<String> mail_data_buffer, ArrayList<String> reverse_path_buffer) {

        boolean REQUESTED_DOMAIN_NOT_AVAILABLE = false;
        String ServerDomainName = "ServerDomain.gr";
        String ResponceToClient = "";
        String ClientDomainName = "";
        String ClientEmail = "";
        String ckeck1 = "";
        String check2 = "";
        String ClientMsgToSend = "";
        Map<String, String> Usernames = new HashMap<String, String>();
        Usernames.put(" Alison Creck", "alice@ThatDomain.gr");
        Usernames.put(" Bob Marley", "bob@MyTestDomain.gr");
        Usernames.put(" Jack Madison", "jack@ServerDomain.gr");
        Usernames.put(" James Bond", "james_bond@ThatDomain.gr");
        // List of commands
        String allCmdsList = "\tHELO\n" + "\tRCPT \n" + "\tMAIL\n" + "\tDATA\n" + "\tNOOP\n" + "\tRSET\n" + "\tQUIT\n";

        // List of domains
        ArrayList<String> KnownDomains = new ArrayList<String>();
        KnownDomains.add("ThatDomain.gr");
        KnownDomains.add("MyTestDomain.gr");
        KnownDomains.add("ServerDomain.gr");

        // List of emails
        ArrayList<String> KnownEmails = new ArrayList<String>();
        KnownEmails.add("alice@ThatDomain.gr");
        KnownEmails.add("bob@MyTestDomain.gr");
        KnownEmails.add("jack@ServerDomain.gr");
        KnownEmails.add("james_bond@ThatDomain.gr");

        ArrayList<String> RecipientsList = new ArrayList<String>();

        boolean GO_ON_CHECKS = true;

        try {
            if (clientMSG.contains(CRLF)) {
                System.out.println("SERVER SIDE command RECEIVED--> " + clientMSG);
                ////////////////////////////////////////////////////////////////////
                // QUIT CMD

                if (clientMSG.contains("QUIT")) {
                    System.out.println("SERVER : QUIT client");

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt(
                            "221" + LF + ServerDomainName + LF + " Service closing transmission channel" + CRLF));
                    sm.output.flush();

                    // remove client from active client list
                    _active_clients.remove(sm);

                    System.out.print("SERVER : active clients : " + _active_clients.size());
                    GO_ON_CHECKS = false;

                    // Clear the Command List
                    CommandStack.clear();

                    return; // QUIT thread
                }
                ////////////////////////////////////////////////////////////////////
                // error 500 -> Line too long ! COMMAND CASE = 512
                else if (clientMSG.length() > 512 && GO_ON_CHECKS) {
                    ResponceToClient = encrypt("500" + CRLF);
                    System.out.println("error 500 -> Line too long");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt(ResponceToClient));
                    sm.output.flush();
                }
                // error 501 -> Syntax error in parameters or arguments
                else if (clientMSG.split(" ").length < 1 && GO_ON_CHECKS) {
                    ResponceToClient = encrypt("501" + CRLF);
                    System.out.println("error 501 -> Syntax error in parameters or arguments");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt(ResponceToClient));
                    sm.output.flush();
                }
                // error 504 -> Command parameter not implemented
                else if (clientMSG.length() < 4 && GO_ON_CHECKS) {
                    ResponceToClient = encrypt("504" + CRLF);
                    System.out.println("error 504 -> Command parameter not implemented");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt(ResponceToClient));
                    sm.output.flush();
                }
                // error 421 -> <domain> Service not available
                else if (REQUESTED_DOMAIN_NOT_AVAILABLE && GO_ON_CHECKS) {
                    ResponceToClient = encrypt("421" + CRLF);
                    String domain_not_found = clientMSG.replaceAll("HELO ", "");
                    domain_not_found = domain_not_found.replaceAll(CRLF, "");
                    System.out.println("error 421 -> " + domain_not_found + " Service not available");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt(ResponceToClient));
                    sm.output.flush();
                }

                ////////////////////////////////////////////////////////////////////
                // HELO Start
                else if (clientMSG.contains("HELO") && GO_ON_CHECKS) {
                    ckeck1 = clientMSG.replace("HELO", "");
                    check2 = ckeck1.replace(CRLF, "");
                    ClientDomainName = check2.replace(EC, ""); // Get client Domain

                    if (KnownDomains.contains(ClientDomainName)) { // Check if the client Domain is in the Known Domain
                                                                   // list
                        CommandStack.add("HELO");
                        System.out.println(CommandStack); // Print the command stack

                        ResponceToClient = "250" + LF + ServerDomainName + CRLF; // Make the response to Client
                        System.out.println("SERVER response: " + ResponceToClient); // Print client message

                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        sm.output.flush(); // Flushes the data output stream

                        SUCCESS_STATE = true;
                        GO_ON_CHECKS = false;
                    } else {
                        System.out.println("Client Domain Name is not in Domain List");
                        ResponceToClient = "451 " + ClientDomainName + " is not in the Domain List" + CRLF; // Make the
                                                                                                            // response

                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        sm.output.flush(); // Flushes the data output stream // to Client
                    }

                }

                // END HELO
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // MAIL FROM Start
                else if (clientMSG.contains("MAIL FROM") && GO_ON_CHECKS) {

                    if (CommandStack.contains("HELO")) { // Check if HELO is done

                        ckeck1 = clientMSG.replace("MAIL FROM", "");
                        check2 = ckeck1.replace(CRLF, "");
                        ClientEmail = check2.replace(EC, ""); // Get client email

                        if (KnownEmails.contains(ClientEmail)) { // Check if the client email is in the Known email list
                            System.out.println("SERVER : MAIL FROM from client");
                            reverse_path_buffer.add(ClientEmail);

                            // Message encryption and send to client
                            sm.output.writeUTF(encrypt("250 OK" + CRLF));
                            sm.output.flush();

                            SUCCESS_STATE = true;
                            GO_ON_CHECKS = false;
                            CommandStack.add("MAIL FROM");// Add command to the command stack
                            System.out.println(CommandStack);
                        } else {
                            System.out.println("Client Email is not in Email List");
                            ResponceToClient = ClientEmail + " is not in the EMAIL List" + CRLF; // Makes the response

                            // Message encryption and send to client
                            sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                            sm.output.flush(); // Flushes the data output stream to Client
                        }
                    } else { // if HELO is not implemented
                        System.out.println("451 MAIL FROM command faild. Implement HELO command before MAIL FROM.");

                        // Makes the response to Client
                        ResponceToClient = "451 MAIL FROM command faild. Implement HELO command before MAIL FROM.";

                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        sm.output.flush(); // Flushes the data output stream // to Client

                    }
                }

                // END MAIL FROM
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // RCPT TO Start
                else if (clientMSG.contains("RCPT TO") && GO_ON_CHECKS) {
                    // Check if MAIL FROM or SEND FROM is done
                    if (CommandStack.contains("MAIL FROM") || CommandStack.contains("SEND FROM")) {
                        ckeck1 = clientMSG.replace("RCPT TO", "");
                        check2 = ckeck1.replace(CRLF, "");
                        ClientEmail = check2.replace(EC, ""); // Get the client email
                        System.out.println(ClientEmail);
                        Recipients.add(ClientEmail);

                        if (KnownEmails.containsAll(Recipients)) { // Check if the client email is in the Known email
                                                                   // list
                            System.out.println("SERVER : RCPT TO from client");

                            // Message encryption and send to client
                            sm.output.writeUTF(encrypt("250 OK" + CRLF));
                            sm.output.flush();

                            SUCCESS_STATE = true;
                            GO_ON_CHECKS = false;
                            CommandStack.add("RCPT TO"); // Add command to the command stack
                            System.out.println(CommandStack);
                        } else {
                            System.out.println("Client Email is not in Email List");
                            ResponceToClient = ClientEmail + " is not in the Known Emails List" + CRLF; // Make the
                                                                                                        // response

                            // Message encryption and send to client
                            sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                            sm.output.flush(); // Flushes the data output stream // to Client
                        }
                    } else { // if MAIL FROM or SEND FROM command is not implemented
                        System.out.println(
                                "451 RCPT TO command faild. Implement MAIL FROM or SEND FROM command before RCPT TO.");

                        // Makes the response to Client
                        ResponceToClient = "451 RCPT TO command faild. Implement MAIL FROM or SEND FROM command before RCPT TO.";

                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        sm.output.flush(); // Flushes the data output stream // to Client
                    }
                }

                // END RCPT TO
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // DATA Start
                else if (clientMSG.contains("DATA") && GO_ON_CHECKS) {

                    if (CommandStack.contains("RCPT TO")) { // Check if RCPT TO is done

                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt("354 End data with <CRLF>.<CRLF>"));
                        sm.output.flush();

                        System.out.println("SERVER : DATA from client");
                        if (clientMSG.endsWith(CRLF + "." + CRLF)) {
                            if (!Recipients.isEmpty()) {

                                ClientMsgToSend = clientMSG.substring(clientMSG.indexOf(CRLF));
                                // Struct the message
                                System.out.println("From: " + reverse_path_buffer.get(0) + LF + "To: ");

                                // Loop through the recipient List and print the recipient email
                                for (String recip : Recipients) {
                                    System.out.println(recip + LF);
                                    RecipientsList.add(recip);
                                }
                                System.out.println(ClientMsgToSend);
                                // Print the message just for testing perposes
                                // Message encryption and send to client
                                sm.output.writeUTF(encrypt(ClientMsgToSend + CRLF));
                                sm.output.flush();

                                mail_data_buffer.add(ClientMsgToSend);// Save the client message to local list
                                queueEMailsInInbox(this.mailBox); // Save message to mailbox

                                // The message is saved and send success to client
                                sm.output.writeUTF(encrypt("250 OK" + CRLF)); // Message encryption and send to client
                                sm.output.flush();

                                SUCCESS_STATE = true;
                                GO_ON_CHECKS = false;
                                CommandStack.add("DATA"); // Add command to the command stack
                                System.out.println(CommandStack);
                            } else {

                                // Message encryption and send to client
                                sm.output.writeUTF(encrypt("451 Error: the message have no recipients."));
                                sm.output.flush();
                                System.out.println("451 Error: the message have no recipients.");
                            }
                        } else {

                            // Message encryption and send to client
                            sm.output.writeUTF(encrypt("Error: the message does not end with <CRLF>.<CRLF>"));
                            sm.output.flush();
                            System.out.println("451 Error: the message does not end with <CRLF>.<CRLF>");
                        }
                    } else { // if RCPT TO is not implemented
                        System.out.println("451 DATA command faild. Implement RCPT TO command before DATA.");

                        // Makes the response to Client
                        ResponceToClient = "451 DATA command faild. Implement RCPT TO command before DATA.";

                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        sm.output.flush(); // Flushes the data output stream // to Client
                    }
                }

                // END DATA
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // NOOP start
                else if (clientMSG.contains("NOOP") && GO_ON_CHECKS) {
                    CommandStack.add("NOOP"); // Add command to the command stack

                    System.out.println("SERVER : NOOP from client");

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt("250 OK" + CRLF)); // Just return success message
                    sm.output.flush();

                }
                // END NOOP
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // VRFY start
                else if (clientMSG.contains("VRFY") && GO_ON_CHECKS) {
                    CommandStack.add("VRFY"); // Add command to the command stack

                    System.out.println("SERVER : VRFY from client");
                    String UserToVRFY = clientMSG.substring(clientMSG.indexOf(EC));
                    UserToVRFY = UserToVRFY.replace(CRLF, "");

                    System.out.println("VRFY:" + UserToVRFY);

                    if (Usernames.containsKey(UserToVRFY)) {
                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt("250" + UserToVRFY + " " + Usernames.get(UserToVRFY) + CRLF)); // Just
                        // return success message and veryfication results
                        sm.output.flush();
                    } else {
                        System.out.println("550 Unknown user. String does not match anything.");

                        // Makes the response to Client
                        ResponceToClient = "550 Unknown user. String does not match anything.";

                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        sm.output.flush(); // Flushes the data output stream // to Client
                    }

                }
                // END VRFY
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // EXPN start
                else if (clientMSG.contains("EXPN") && GO_ON_CHECKS) {
                    CommandStack.add("EXPN"); // Add command to the command stack

                    System.out.println("SERVER : EXPN from client");
                    for (String KnownEmail : KnownEmails)
                        // Message encryption and send to client
                        sm.output.writeUTF(encrypt("250 " + KnownEmail + CRLF)); // Just return success message
                    sm.output.flush();

                }
                // END EXPN
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // RSET start
                // Check for RSET message for client
                else if (clientMSG.contains("RSET") && GO_ON_CHECKS) {
                    System.out.println("SERVER : RSET from client");

                    CommandStack.clear(); // Clear the Command stack
                    reverse_path_buffer.clear(); // Clear the Mail From List
                    Recipients.clear(); // Clear the Recipients List
                    mail_data_buffer.clear(); // Clear the Mail List

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt("250 OK" + CRLF));
                    sm.output.flush();
                    System.out.println("SERVER : All Lists are cleared!");
                }
                // END RSET
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // HELP start
                else if (clientMSG.contains("HELP") && GO_ON_CHECKS) {

                    System.out.println("SERVER : HELP from client");

                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt("214" + CRLF
                            + "Type HELP-<commandName> (without spaces) to receive informations about specific command."
                            + CRLF + CRLF + allCmdsList));
                    sm.output.flush();
                }

                // END HELP
                ////////////////////////////////////////////////////////////////////

                clientMSG = ""; // empty buffer after CRLF
            } // if CRLF

            ////////////////////////////////////////////////////////////////////////////////
            // ____________________HELP_INFORMATIONS_FOREACH_COMMAND______________________//

            if (clientMSG.contains("HELP -HELO") && GO_ON_CHECKS) {

                // Message encryption and send to client
                sm.output.writeUTF(
                        encrypt("\tHELO \t\tSpecify your domain name so that the mail server knows who you are.\n"));
                sm.output.flush();
            } else if (clientMSG.contains("HELP -MAIL") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {

                // Message encryption and send to client
                sm.output.writeUTF(encrypt("\tMAIL \t\tSpecify the sender email.\n"));
                sm.output.flush();
            } else if (clientMSG.contains("HELP -RCPT") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {

                // Message encryption and send to client
                sm.output.writeUTF(encrypt(
                        "\tRCPT \t\tSpecify the recipient. Issue this command multiple times if you have more than one recipient.\n"));
                sm.output.flush();
            } else if (clientMSG.contains("HELP -DATA") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {

                // Message encryption and send to client
                sm.output.writeUTF(encrypt("\tDATA \t\tIssue this command before sending the body of the message.\n"));
                sm.output.flush();
            } else if (clientMSG.contains("HELP -QUIT") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {

                // Message encryption and send to client
                sm.output.writeUTF(encrypt("\tQUIT \t\tTerminates the conversation with the server."));
                sm.output.flush();
            } else if (clientMSG.contains("HELP -RSET") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {

                // Message encryption and send to client
                sm.output.writeUTF(
                        encrypt("\tRSET \t\tAborts the current conversation and start a new conversation.\n"));
                sm.output.flush();
            } else if (clientMSG.contains("HELP -NOOP") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {

                // Message encryption and send to client
                sm.output.writeUTF(encrypt("\tNOOP \t\tDoes nothing except to get a response from the server.\n"));
                sm.output.flush();
            }

            // END HELP COMMANDS
            ////////////////////////////////////////////////////////////////////

            ////////////////////////////////////////////////////////////////////
            // MAILBOX start
            else if (clientMSG.contains("mailbox") && GO_ON_CHECKS) {

                String mailsList = "";
                System.out.println("SERVER : mailbox from client");

                String mailBoxId = this.mailBox.getMailBoxId(userEmail);

                if (mailBox.haveEmails(mailBoxId)) {
                    // Get the email List
                    mailsList = this.mailBox.Getmailbox(mailBoxId);
                    System.out.println("User " + userEmail + " have emails."); // Print in Server
                    // Message encryption and send to client
                    sm.output.writeUTF(encrypt("250 \n\n" + userEmail + " MAILBOX :" + CRLF + mailsList));
                    sm.output.flush();
                } else {
                    System.out.println("451 You have not any emails yet.");
                    sm.output.writeUTF(encrypt("451 You have not any emails yet."));
                    sm.output.flush();
                }
            }

            // END MAILBOX
            ////////////////////////////////////////////////////////////////////

        } catch (

        Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error --> " + except.getMessage());
        }
    }

}
