import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ServerConnectionHandler implements Runnable {
    public static String CRLF = "\r\n";
    public static String LF = "\n";
    public static String ServerDomainName = "ServerDomain.gr";
    private static String CommandStack = "";
    socketManager _socketMngObjVar = null;
    ArrayList<socketManager> _active_clients = null;
    Socket _clientSocket = new Socket();

    public ServerConnectionHandler(ArrayList<socketManager> inArrayListVar, socketManager inSocMngVar) {
        _socketMngObjVar = inSocMngVar;
        _active_clients = inArrayListVar;
    }

    public void run() {
        try {
            System.out.println("0 Client " + _socketMngObjVar.soc.getPort() + " Connected");
            System.out.println("0 SERVER : active clients : " + _active_clients.size());

            while (!_socketMngObjVar.soc.isClosed()) {
                String clientMSG = _socketMngObjVar.input.readUTF();

                System.out.println(
                        "SERVER : message FROM CLIENT : " + _socketMngObjVar.soc.getPort() + " --> " + clientMSG);

                // Check for Quit message for client

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

                    System.out.print("5 SERVER : active clients : " + _active_clients.size());
                    CommandStack = "";
                    return; // QUIT thread
                }
                // Check for Hello message for client
                else if (clientMSG.contains("HELLO")) {
                    System.out.println("1 SERVER : HELLO client");

                    _socketMngObjVar.output.writeUTF(
                            "250" + LF + ServerDomainName + LF + "Hello, I am" + LF + ServerDomainName + CRLF);
                    _socketMngObjVar.output.flush();

                    return; // HELLO thread
                }

                Server_SMTP_Handler(_socketMngObjVar, clientMSG);
            } // while socket NOT CLOSED
        } catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing clientMSG to the
            // console
            System.out.println("Error in Server Connection Handler --> " + except.getMessage());
        }
    }

    private void Server_SMTP_Handler(socketManager sm, String clientMSG) {

        boolean REQUESTED_DOMAIN_NOT_AVAILABLE = false;
        String ServerDomainName = "ServerDomain.gr";
        boolean SMTP_OUT_OF_STORAGE = false;
        boolean SMTP_INSUFFICIENT_STORAGE = false;
        boolean SMTP_LOCAL_PROCESSING_ERROR = false;
        boolean SUCCESS_STATE = false;
        boolean WAIT_STATE = true;
        String sResponceToClient = "";

        ArrayList<String> UsersInServerDomain = new ArrayList<String>();
        UsersInServerDomain.add("Alice");
        UsersInServerDomain.add("Bob");
        UsersInServerDomain.add("Mike");

        ArrayList<String> KnownDomains = new ArrayList<String>();
        KnownDomains.add("ThatDomain.gr");
        KnownDomains.add("MyTestDomain.gr");
        KnownDomains.add("ServerDomain.gr");

        ArrayList<String> mail_data_buffer = new ArrayList<String>();
        ArrayList<String> forward_path_buffer = new ArrayList<String>();
        ArrayList<String> reverse_path_buffer = new ArrayList<String>();

        boolean GO_ON_CHECKS = true;

        try {
            if (clientMSG.contains(CRLF)) {
                // System.out.println("SERVER SIDE command RECEIVED--> " + clientMSG);
                ////////////////////////////////////////////////////////////////////
                // HELO CMD MESSSAGES PACK
                ////////////////////////////////////////////////////////////////////
                // error 500 -> Line too long ! COMMAND CASE = 512
                if (clientMSG.contains("QUIT")) {
                    GO_ON_CHECKS = false;
                    CommandStack = "";
                } else if (clientMSG.length() > 512 && GO_ON_CHECKS) {
                    sResponceToClient = "500" + CRLF;
                    System.out.println("error 500 -> Line too long");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 501 -> Syntax error in parameters or arguments
                else if (clientMSG.split(" ").length < 1 && GO_ON_CHECKS) {
                    sResponceToClient = "501" + CRLF;
                    // System.out.println("error 501 -> Syntax error in parameters or arguments");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 504 -> Command parameter not implemented
                else if (clientMSG.length() < 4 && GO_ON_CHECKS) {
                    sResponceToClient = "504" + CRLF;
                    // System.out.println("error 504 -> Command parameter not implemented");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // error 421 -> <domain> Service not available
                else if (REQUESTED_DOMAIN_NOT_AVAILABLE && GO_ON_CHECKS) {
                    sResponceToClient = "421" + CRLF;
                    String domain_not_found = clientMSG.replaceAll("HELO ", "");
                    domain_not_found = domain_not_found.replaceAll(CRLF, "");
                    // System.out.println("error 421 -> "+ domain_not_found +" Service not
                    // available");
                    SUCCESS_STATE = false;
                    GO_ON_CHECKS = false;
                }
                // Server received HELLO comand and respond with ok
                else if (clientMSG.contains("HELO") && GO_ON_CHECKS) {
                    sResponceToClient = "250" + LF + ServerDomainName + CRLF;
                    System.out.println("SERVER response: " + sResponceToClient);
                    SUCCESS_STATE = true;
                    GO_ON_CHECKS = false;
                    CommandStack = CommandStack + "HELO ";
                    System.out.println(CommandStack);
                }
                ////////////////////////////////////////////////////////////////////
                // END HELO
                ////////////////////////////////////////////////////////////////////

                // Server received MAIL FROM comand and respond with ok
                else if (clientMSG.contains("MAIL FROM") && GO_ON_CHECKS) {
                    sResponceToClient = "250" + LF + ServerDomainName + CRLF;
                    System.out.println("SERVER responce: " + sResponceToClient);
                    SUCCESS_STATE = true;
                    GO_ON_CHECKS = false;
                    CommandStack = CommandStack + "MAIL FROM ";
                    System.out.println(CommandStack);
                }
                ////////////////////////////////////////////////////////////////////
                // END MAIL FROM
                ////////////////////////////////////////////////////////////////////

                clientMSG = ""; // empty buffer after CRLF
            } // if CRLF

            sm.output.writeUTF(sResponceToClient);
        } catch (Exception except) {
            // Exception thrown (except) when something went wrong, pushing message to the
            // console
            System.out.println("Error --> " + except.getMessage());
        }
    }
}
