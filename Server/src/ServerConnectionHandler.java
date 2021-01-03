import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javax.crypto.spec.IvParameterSpec;

import java.util.ArrayList;

public class ServerConnectionHandler implements Runnable {
    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String EC = " ";
    public static String ServerDomainName = "ServerDomain.gr";
    socketManager _socketMngObjVar = null;
    ArrayList<socketManager> _active_clients = null;
    Boolean GO_ON_CHECKS;
    Boolean isLoggedIn = false;

    String ResponceToClient = "";
    Boolean SUCCESS_STATE;

    // Encryption/decryption variables
    private static String secretKey = "kdfslksdnflsdfsd";
    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";

    public ServerConnectionHandler(ArrayList<socketManager> inArrayListVar, socketManager inSocMngVar) {
        _socketMngObjVar = inSocMngVar;
        _active_clients = inArrayListVar;
    }

    public static String encrypt(String value) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        byte[] values = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(values);
    }

    public static String decrypt(String value) throws Exception {
        byte[] values = Base64.getDecoder().decode(value);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        return new String(cipher.doFinal(values));
    }

    public void run() {
        try {
            System.out.println("Client " + _socketMngObjVar.soc.getPort() + " Connected");
            System.out.println("SERVER: Active clients : " + _active_clients.size());

            if (!_socketMngObjVar.soc.isClosed()) { // Check if user is connected

                ArrayList<String> KnownEmails = new ArrayList<String>(); // List of Knowns Email addresses
                KnownEmails.add("ALICE@ThatDomain.gr");
                KnownEmails.add("myEmail@MyTestDomain.gr");
                KnownEmails.add("myEmail@ServerDomain.gr");
                KnownEmails.add("receip@MyTestDomain.gr");
                GO_ON_CHECKS = true;
                String userEmail = "";

                ArrayList<String> CommandStack = new ArrayList<String>(); // List of user commands
                ArrayList<String> mail_data_buffer = new ArrayList<String>();
                ArrayList<String> forward_path_buffer = new ArrayList<String>(); // List to include the Recipients of
                                                                                 // the emails
                ArrayList<String> reverse_path_buffer = new ArrayList<String>();
                String clientMSG = "";

                while (!_socketMngObjVar.soc.isClosed()) { // While a user is connected
                    if (!isLoggedIn) {

                        // Decryption of message
                        userEmail = decrypt(_socketMngObjVar.input.readUTF());

                        if (KnownEmails.contains(userEmail)) { // If the user's email exists in known email list
                            isLoggedIn = true;
                            // Print the response
                            System.out.println("Client " + userEmail + " is connected.");

                            // Send error response to client
                            _socketMngObjVar.output.writeUTF(encrypt("250 Client " + userEmail + " is connected."));
                            _socketMngObjVar.output.flush();
                        } else {
                            // Print the error response
                            System.out.println(
                                    "550 " + userEmail + " is a wrong email, please try again. Type a valid email. ");

                            // Send error response to client
                            _socketMngObjVar.output.writeUTF(encrypt(
                                    "550 " + userEmail + " is a wrong email, please try again. Type a valid email. "));
                            _socketMngObjVar.output.flush();
                        }
                    } else {

                        clientMSG = decrypt(_socketMngObjVar.input.readUTF());

                        System.out.println("SERVER : message FROM CLIENT : " + _socketMngObjVar.soc.getPort() + " --> "
                                + clientMSG);

                        if (!(clientMSG == "")) {
                            System.out.println("SERVER : message FROM CLIENT : " + _socketMngObjVar.soc.getPort()
                                    + " --> " + clientMSG);
                        } else {
                            System.out.println("No message from client");

                        }

                        Server_SMTP_Handler(_socketMngObjVar, clientMSG, CommandStack, forward_path_buffer,
                                mail_data_buffer, reverse_path_buffer);
                    }

                } // while socket NOT CLOSED
            }
        } catch (

        Exception except) {
            // Exception thrown (except) when something went wrong, pushing clientMSG to the
            // console
            System.out.println("Error in Server Connection Handler --> " + except.getMessage());
        }
    }

    private void Server_SMTP_Handler(socketManager sm, String clientMSG, ArrayList<String> CommandStack,
            ArrayList<String> Recipients, ArrayList<String> mail_data_buffer, ArrayList<String> reverse_path_buffer) {

        boolean REQUESTED_DOMAIN_NOT_AVAILABLE = false;
        String ServerDomainName = "ServerDomain.gr";
        // boolean SMTP_OUT_OF_STORAGE = false;
        // boolean SMTP_INSUFFICIENT_STORAGE = false;
        // boolean SMTP_LOCAL_PROCESSING_ERROR = false;
        // boolean SUCCESS_STATE = false;
        // boolean WAIT_STATE = true;
        String sResponceToClient = "";
        String ClientDomainName = "";
        String ClientEmail = "";
        String ckeck1 = "";
        String check2 = "";
        String ClientMsgToSend = "";

        String allCmdsList = "\tHELO\n" + "\tRCPT \n" + "\tMAIL\n" + "\tDATA\n" + "\tNOOP\n" + "\tRSET\n" + "\tQUIT\n";

        ArrayList<String> UsersInServerDomain = new ArrayList<String>();
        UsersInServerDomain.add("Alice");
        UsersInServerDomain.add("Bob");
        UsersInServerDomain.add("Mike");

        ArrayList<String> KnownDomains = new ArrayList<String>();
        KnownDomains.add("ThatDomain.gr");
        KnownDomains.add("MyTestDomain.gr");
        KnownDomains.add("ServerDomain.gr");

        ArrayList<String> KnownEmails = new ArrayList<String>();
        KnownEmails.add("ALICE@ThatDomain.gr");
        KnownEmails.add("myEmail@MyTestDomain.gr");
        KnownEmails.add("myEmail@ServerDomain.gr");
        KnownEmails.add("receip@MyTestDomain.gr");

        boolean GO_ON_CHECKS = true;

        try {
            if (clientMSG.contains(CRLF)) {
                System.out.println("SERVER SIDE command RECEIVED--> " + clientMSG);
                ////////////////////////////////////////////////////////////////////
                // QUIT CMD

                if (clientMSG.contains("QUIT")) {
                    System.out.println("1 SERVER : QUIT client");
                    //
                    // SYNTAX (page 12 RFC 821)
                    // QUIT <SP> <SERVER domain> <SP> Service closing transmission channel<CRLF>
                    //
                    _socketMngObjVar.output.writeUTF(encrypt(
                            "221" + LF + ServerDomainName + LF + " Service closing transmission channel" + CRLF));
                    _socketMngObjVar.output.flush();
                    _active_clients.remove(_socketMngObjVar);

                    System.out.print("6 SERVER : active clients : " + _active_clients.size());
                    GO_ON_CHECKS = false;
                    CommandStack.clear();
                    return; // QUIT thread
                }
                ////////////////////////////////////////////////////////////////////
                // error 500 -> Line too long ! COMMAND CASE = 512
                else if (clientMSG.length() > 512 && GO_ON_CHECKS) {
                    sResponceToClient = encrypt("500" + CRLF);
                    System.out.println("error 500 -> Line too long");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 501 -> Syntax error in parameters or arguments
                else if (clientMSG.split(" ").length < 1 && GO_ON_CHECKS) {
                    sResponceToClient = encrypt("501" + CRLF);
                    System.out.println("error 501 -> Syntax error in parameters or arguments");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 504 -> Command parameter not implemented
                else if (clientMSG.length() < 4 && GO_ON_CHECKS) {
                    sResponceToClient = encrypt("504" + CRLF);
                    System.out.println("error 504 -> Command parameter not implemented");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 421 -> <domain> Service not available
                else if (REQUESTED_DOMAIN_NOT_AVAILABLE && GO_ON_CHECKS) {
                    sResponceToClient = encrypt("421" + CRLF);
                    String domain_not_found = clientMSG.replaceAll("HELO ", "");
                    domain_not_found = domain_not_found.replaceAll(CRLF, "");
                    System.out.println("error 421 -> " + domain_not_found + " Service not available");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
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

                        _socketMngObjVar.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream

                        SUCCESS_STATE = true;
                        GO_ON_CHECKS = false;
                    } else {
                        System.out.println("Client Domain Name is not in Domain List");
                        ResponceToClient = "451" + ClientDomainName + "is not in the Domain List" + CRLF; // Make the
                                                                                                          // response
                        _socketMngObjVar.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
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
                            _socketMngObjVar.output.writeUTF(encrypt("250 OK" + CRLF));
                            _socketMngObjVar.output.flush();

                            SUCCESS_STATE = true;
                            GO_ON_CHECKS = false;
                            CommandStack.add("MAIL FROM");
                            System.out.println(CommandStack);
                        } else {
                            System.out.println("Client Email is not in Email List");
                            ResponceToClient = ClientEmail + " is not in the EMAIL List" + CRLF; // Makes the response
                            _socketMngObjVar.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                            _socketMngObjVar.output.flush(); // Flushes the data output stream to Client
                        }
                    } else { // if HELO is not implemented
                        System.out.println("451 MAIL FROM command faild. Implement HELO command before MAIL FROM.");

                        // Makes the response to Client
                        ResponceToClient = "451 MAIL FROM command faild. Implement HELO command before MAIL FROM.";
                        _socketMngObjVar.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client

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

                            _socketMngObjVar.output.writeUTF(encrypt("250 OK" + CRLF));
                            _socketMngObjVar.output.flush();

                            SUCCESS_STATE = true;
                            GO_ON_CHECKS = false;
                            CommandStack.add("RCPT TO");
                            System.out.println(CommandStack);
                        } else {
                            System.out.println("Client Email is not in Email List");
                            ResponceToClient = ClientEmail + " is not in the Known Emails List" + CRLF; // Make the
                                                                                                        // response
                            _socketMngObjVar.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                            _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
                        }
                    } else { // if MAIL FROM or SEND FROM command is not implemented
                        System.out.println(
                                "451 RCPT TO command faild. Implement MAIL FROM or SEND FROM command before RCPT TO.");

                        // Makes the response to Client
                        ResponceToClient = "451 RCPT TO command faild. Implement MAIL FROM or SEND FROM command before RCPT TO.";
                        _socketMngObjVar.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
                    }
                }

                // END RCPT TO
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // DATA start
                else if (clientMSG.contains("DATA") && GO_ON_CHECKS) {

                    if (CommandStack.contains("RCPT TO")) { // Check if RCPT TO is done

                        _socketMngObjVar.output.writeUTF(encrypt("354 End data with <CRLF>.<CRLF>"));
                        _socketMngObjVar.output.flush();

                        System.out.println("SERVER : DATA from client");
                        if (clientMSG.endsWith(CRLF + "." + CRLF)) {
                            if (!Recipients.isEmpty()) {

                                ClientMsgToSend = clientMSG.substring(clientMSG.indexOf(CRLF));
                                mail_data_buffer.add(ClientMsgToSend);
                                System.out.println(ClientMsgToSend);

                                _socketMngObjVar.output.writeUTF(encrypt("250 OK" + CRLF));
                                _socketMngObjVar.output.flush();

                                SUCCESS_STATE = true;
                                GO_ON_CHECKS = false;
                                CommandStack.add("DATA");
                                System.out.println(CommandStack);
                            } else {
                                _socketMngObjVar.output.writeUTF(encrypt("451 Error: the message have no recipients."));
                                _socketMngObjVar.output.flush();
                                System.out.println("451 Error: the message have no recipients.");
                            }
                        } else {
                            _socketMngObjVar.output
                                    .writeUTF(encrypt("Error: the message does not end with <CRLF>.<CRLF>"));
                            _socketMngObjVar.output.flush();
                            System.out.println("451 Error: the message does not end with <CRLF>.<CRLF>");
                        }
                    } else { // if RCPT TO is not implemented
                        System.out.println("451 DATA command faild. Implement RCPT TO command before DATA.");

                        // Makes the response to Client
                        ResponceToClient = "451 DATA command faild. Implement RCPT TO command before DATA.";
                        _socketMngObjVar.output.writeUTF(encrypt(ResponceToClient)); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
                    }
                }

                // END DATA
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // NOOP start
                else if (clientMSG.contains("NOOP") && GO_ON_CHECKS) {

                    System.out.println("SERVER : NOOP from client");

                    _socketMngObjVar.output.writeUTF(encrypt("250 OK" + CRLF));
                    _socketMngObjVar.output.flush();

                }
                // END NOOP
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

                    _socketMngObjVar.output.writeUTF(encrypt("250 OK" + CRLF));
                    _socketMngObjVar.output.flush();
                    System.out.println("SERVER : All Lists are cleared!");
                }
                // END RSET
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // HELP start
                else if (clientMSG.contains("HELP") && GO_ON_CHECKS) {

                    System.out.println("SERVER : HELP from client");

                    _socketMngObjVar.output.writeUTF(encrypt("214" + CRLF
                            + "Type HELP-<commandName> (without spaces) to receive informations about specific command."
                            + CRLF + CRLF + allCmdsList));
                    _socketMngObjVar.output.flush();
                }

                // END HELP
                ////////////////////////////////////////////////////////////////////

                clientMSG = ""; // empty buffer after CRLF
            } // if CRLF

            ////////////////////////////////////////////////////////////////////////////////
            // ____________________HELP_INFORMATIONS_FOREACH_COMMAND______________________//

            if (clientMSG.contains("HELP -HELO") && GO_ON_CHECKS) {
                _socketMngObjVar.output.writeUTF(
                        encrypt("\tHELO \t\tSpecify your domain name so that the mail server knows who you are.\n"));
                _socketMngObjVar.output.flush();
            } else if (clientMSG.contains("HELP -MAIL") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {
                _socketMngObjVar.output.writeUTF(encrypt("\tMAIL \t\tSpecify the sender email.\n"));
                _socketMngObjVar.output.flush();
            } else if (clientMSG.contains("HELP -RCPT") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {
                _socketMngObjVar.output.writeUTF(encrypt(
                        "\tRCPT \t\tSpecify the recipient. Issue this command multiple times if you have more than one recipient.\n"));
                _socketMngObjVar.output.flush();
            } else if (clientMSG.contains("HELP -DATA") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {
                _socketMngObjVar.output
                        .writeUTF(encrypt("\tDATA \t\tIssue this command before sending the body of the message.\n"));
                _socketMngObjVar.output.flush();
            } else if (clientMSG.contains("HELP -QUIT") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {
                _socketMngObjVar.output.writeUTF(encrypt("\tQUIT \t\tTerminates the conversation with the server."));
                _socketMngObjVar.output.flush();
            } else if (clientMSG.contains("HELP -RSET") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {
                _socketMngObjVar.output.writeUTF(
                        encrypt("\tRSET \t\tAborts the current conversation and start a new conversation.\n"));
                _socketMngObjVar.output.flush();
            } else if (clientMSG.contains("HELP -NOOP") && GO_ON_CHECKS && !clientMSG.contains(CRLF)) {
                _socketMngObjVar.output
                        .writeUTF(encrypt("\tNOOP \t\tDoes nothing except to get a response from the server.\n"));
                _socketMngObjVar.output.flush();
            }

            // END HELP COMMANDS
            ////////////////////////////////////////////////////////////////////
            sm.output.writeUTF(encrypt(sResponceToClient));
        } catch (

        Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error --> " + except.getMessage());
        }
    }
}
