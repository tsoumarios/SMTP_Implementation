import java.io.DataOutputStream;

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

    public ServerConnectionHandler(ArrayList<socketManager> inArrayListVar, socketManager inSocMngVar) {
        _socketMngObjVar = inSocMngVar;
        _active_clients = inArrayListVar;
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

                ArrayList<String> CommandStack = new ArrayList<String>(); // List of user commands
                ArrayList<String> mail_data_buffer = new ArrayList<String>();
                ArrayList<String> forward_path_buffer = new ArrayList<String>();
                ArrayList<String> reverse_path_buffer = new ArrayList<String>();
                ArrayList<String> Receipters = new ArrayList<String>(); // List to include the receipters of the emails

                while (!_socketMngObjVar.soc.isClosed()) { // While a user is connected
                    if (!isLoggedIn) {
                        String userEmail = _socketMngObjVar.input.readUTF();
                        if (KnownEmails.contains(userEmail)) {
                            isLoggedIn = true;
                            System.out.println("Client " + userEmail + " is connected.");
                            _socketMngObjVar.output.writeUTF("250 Client " + userEmail + "is connected.");
                            _socketMngObjVar.output.flush();
                        } else {
                            System.out.println(
                                    "550 " + userEmail + " is a wrong email, please try again. Type a valid email. ");
                        }
                    } else {

                        String clientMSG = _socketMngObjVar.input.readUTF();
                        System.out.println("SERVER : message FROM CLIENT : " + _socketMngObjVar.soc.getPort() + " --> "
                                + clientMSG);

                        if (!(clientMSG == "")) {
                            System.out.println("SERVER : message FROM CLIENT : " + _socketMngObjVar.soc.getPort()
                                    + " --> " + clientMSG);
                        } else {
                            System.out.println("No message from client");

                        }

                        Server_SMTP_Handler(_socketMngObjVar, clientMSG, CommandStack, Receipters);
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
            ArrayList<String> Receipters) {

        boolean REQUESTED_DOMAIN_NOT_AVAILABLE = false;
        String ServerDomainName = "ServerDomain.gr";
        boolean SMTP_OUT_OF_STORAGE = false;
        boolean SMTP_INSUFFICIENT_STORAGE = false;
        boolean SMTP_LOCAL_PROCESSING_ERROR = false;
        boolean SUCCESS_STATE = false;
        boolean WAIT_STATE = true;
        String sResponceToClient = "";
        String ClientDomainName = "";
        String ClientEmail = "";
        String ClientMsgToSend = "";
        String ckeck1 = "";
        String check2 = "";
        String helpCmd = "";

        String allCmdsList = "\tHELP\tAsks for help from the mail server.\n"
                + "\tHELO \tSpecify your domain name so that the mail server knows who you are.\n"
                + "\tRCPT \tSpecify the recipient. Issue this command multiple times if you have more than one recipient.\n"
                + "\tMAIL \tSpecify the sender email.\n"
                + "\tDATA \t Issue this command before sending the body of the message.\n"
                + "\tNOOP \tDoes nothing except to get a response from the server.\n"
                + "\tRSET \tAborts the current conversation and start a new conversation.\n"
                + "\tQUIT \tTerminates the conversation with the server.";

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
                    _socketMngObjVar.output.writeUTF(
                            "221" + LF + ServerDomainName + LF + " Service closing transmission channel" + CRLF);
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
                    sResponceToClient = "500" + CRLF;
                    System.out.println("error 500 -> Line too long");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 501 -> Syntax error in parameters or arguments
                else if (clientMSG.split(" ").length < 1 && GO_ON_CHECKS) {
                    sResponceToClient = "501" + CRLF;
                    System.out.println("error 501 -> Syntax error in parameters or arguments");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 504 -> Command parameter not implemented
                else if (clientMSG.length() < 4 && GO_ON_CHECKS) {
                    sResponceToClient = "504" + CRLF;
                    System.out.println("error 504 -> Command parameter not implemented");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 421 -> <domain> Service not available
                else if (REQUESTED_DOMAIN_NOT_AVAILABLE && GO_ON_CHECKS) {
                    sResponceToClient = "421" + CRLF;
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

                        _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream

                        SUCCESS_STATE = true;
                        GO_ON_CHECKS = false;
                    } else {
                        System.out.println("Client Domain Name is not in Domain List");
                        ResponceToClient = ClientDomainName + "is not in the Domain List" + CRLF; // Make the response
                        _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
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

                            _socketMngObjVar.output.writeUTF("250 OK" + CRLF);
                            _socketMngObjVar.output.flush();

                            SUCCESS_STATE = true;
                            GO_ON_CHECKS = false;
                            CommandStack.add("MAIL FROM");
                            System.out.println(CommandStack);
                        } else {
                            System.out.println("Client Email is not in Email List");
                            ResponceToClient = ClientEmail + " is not in the EMAIL List" + CRLF; // Makes the response
                            _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
                            _socketMngObjVar.output.flush(); // Flushes the data output stream to Client
                        }
                    } else { // if HELO is not implemented
                        System.out.println("451 MAIL FROM command faild. Implement HELO command before MAIL FROM.");

                        // Makes the response to Client
                        ResponceToClient = "451 MAIL FROM command faild. Implement HELO command before MAIL FROM.";
                        _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
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
                        Receipters.add(ClientEmail);

                        if (KnownEmails.containsAll(Receipters)) { // Check if the client email is in the Known email
                                                                   // list
                            System.out.println("SERVER : RCPT TO from client");

                            _socketMngObjVar.output.writeUTF("250 OK" + CRLF);
                            _socketMngObjVar.output.flush();

                            SUCCESS_STATE = true;
                            GO_ON_CHECKS = false;
                            CommandStack.add("RCPT TO");
                            System.out.println(CommandStack);
                        } else {
                            System.out.println("Client Email is not in Email List");
                            ResponceToClient = ClientEmail + " is not in the Known Emails List" + CRLF; // Make the
                                                                                                        // response
                            _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
                            _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
                        }
                    } else { // if MAIL FROM or SEND FROM command is not implemented
                        System.out.println(
                                "451 RCPT TO command faild. Implement MAIL FROM or SEND FROM command before RCPT TO.");

                        // Makes the response to Client
                        ResponceToClient = "451 RCPT TO command faild. Implement MAIL FROM or SEND FROM command before RCPT TO.";
                        _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
                    }
                }

                // END RCPT TO
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // DATA start
                else if (clientMSG.contains("DATA") && GO_ON_CHECKS) {

                    if (CommandStack.contains("RCPT TO")) { // Check if RCPT TO is done

                        _socketMngObjVar.output.writeUTF("354 End data with <CRLF>.<CRLF>");
                        _socketMngObjVar.output.flush();

                        System.out.println("SERVER : DATA from client");
                        if (clientMSG.endsWith(CRLF + "." + CRLF)) {
                            if (!Receipters.isEmpty()) {

                                ClientMsgToSend = clientMSG.substring(clientMSG.indexOf(CRLF));
                                System.out.println(ClientMsgToSend);

                                _socketMngObjVar.output.writeUTF("250 OK" + CRLF);
                                _socketMngObjVar.output.flush();

                                SUCCESS_STATE = true;
                                GO_ON_CHECKS = false;
                                CommandStack.add("DATA");
                                System.out.println(CommandStack);
                            } else {
                                _socketMngObjVar.output.writeUTF("451 Error: the message have no recipients.");
                                _socketMngObjVar.output.flush();
                                System.out.println("451 Error: the message have no recipients.");
                            }
                        } else {
                            _socketMngObjVar.output.writeUTF("Error: the message does not end with <CRLF>.<CRLF>");
                            _socketMngObjVar.output.flush();
                            System.out.println("451 Error: the message does not end with <CRLF>.<CRLF>");
                        }
                    } else { // if RCPT TO is not implemented
                        System.out.println("451 DATA command faild. Implement RCPT TO command before DATA.");

                        // Makes the response to Client
                        ResponceToClient = "451 DATA command faild. Implement RCPT TO command before DATA.";
                        _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
                    }
                }

                // END DATA
                ////////////////////////////////////////////////////////////////////
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // EXPN start
                else if (clientMSG.contains("EXPN") && GO_ON_CHECKS) {

                    System.out.println("SERVER : EXPN from client");
                    if (CommandStack.contains("RCPT TO")) { // Check if RCPT TO is done

                        if (KnownEmails.containsAll(Receipters)) { // Check if Receipters is in the Known email List

                            _socketMngObjVar.output.writeUTF("250 The recipients are on the list.");
                            _socketMngObjVar.output.flush();

                            SUCCESS_STATE = true;
                            GO_ON_CHECKS = false;
                            CommandStack.add("EXPN");
                            System.out.println(CommandStack);
                        } else {

                            _socketMngObjVar.output.writeUTF("550: The recipient are not on the list");
                            System.out.println("SERVER : The recipient are not on the list");

                        }
                    } else {// Check if RCPT TO is done
                        System.out.println("EXPN command faild. Implement RCPT TO command before EXPN.");

                        // Makes the response to Client
                        ResponceToClient = "451 EXPN command faild. Implement RCPT TO command before EXPN.";
                        _socketMngObjVar.output.writeUTF(ResponceToClient); // Send the response to Client
                        _socketMngObjVar.output.flush(); // Flushes the data output stream // to Client
                    }
                }

                // END EXPN
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // HELP start
                else if (clientMSG.contains("HELP") && GO_ON_CHECKS) {

                    System.out.println("SERVER : HELP from client");

                    _socketMngObjVar.output.writeUTF(
                            "214" + CRLF + "Table of command name and description" + CRLF + CRLF + allCmdsList);
                    _socketMngObjVar.output.flush();

                    // **** Notes for alternative implementation
                    // .writeUTF("For more information on a specific command, type HELP
                    // command-name" + CRLF + CRLF
                    // + allCmdsList);

                }
                // END HELP
                ////////////////////////////////////////////////////////////////////

                ////////////////////////////////////////////////////////////////////
                // NOOP start
                else if (clientMSG.contains("NOOP") && GO_ON_CHECKS) {

                    System.out.println("SERVER : NOOP from client");

                    _socketMngObjVar.output.writeUTF("250 OK + CRLF");
                    _socketMngObjVar.output.flush();

                }
                // END NOOP
                ////////////////////////////////////////////////////////////////////

                /************************************************
                 * MUST CONTINUE HERE
                 ***********************************************/
                // Check for RSET message for client
                else if (clientMSG.contains("RSET") && GO_ON_CHECKS) {
                    System.out.println("2 SERVER : RSET from client");

                    _socketMngObjVar.output.writeUTF("RSET" + CRLF);
                    _socketMngObjVar.output.flush();

                    return; // RSET FROM thread
                }

                /************************************************
                 * END CONTINUE
                 ***********************************************/

                clientMSG = ""; // empty buffer after CRLF
            } // if CRLF

            sm.output.writeUTF(sResponceToClient);
        } catch (

        Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error --> " + except.getMessage());
        }
    }
}
