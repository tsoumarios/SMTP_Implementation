import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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

    // This map is used for store emails
    // (params : <userEmailAddress>,<GivenMailMessage>)
    Map<String, String> sharedMailbox = new HashMap<String, String>();
    String mail = "";
    // Emails and passwords
    Map<String, String> Users = new HashMap<String, String>();

    // Encryption/decryption variables
    private static String secretKey = "kdfslksdnflsdfsd";
    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "abcdefgh";
    public String clientMSG = "";
    public String userEmail = "";

    // Class Constructor
    public ServerConnectionHandler(ArrayList<socketManager> inArrayListVar, socketManager inSocMngVar) {
        _active_clients = inArrayListVar;
        _socketMngObjVar = inSocMngVar;
    }

    // Encryption Method
    public static String encrypt(String value) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(IV.getBytes()));
        byte[] values = cipher.doFinal(value.getBytes());
        return Base64.getEncoder().encodeToString(values);
    }

    // Decryption Method
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

                // List of Knowns Email addresses
                ArrayList<String> KnownEmails = new ArrayList<String>();
                KnownEmails.add("alice@ThatDomain.gr");
                KnownEmails.add("myEmail@MyTestDomain.gr");
                KnownEmails.add("myEmail@ServerDomain.gr");
                KnownEmails.add("receip@MyTestDomain.gr");
                GO_ON_CHECKS = true;

                // List of users
                Users.put("alice@ThatDomain.gr", "123456");
                Users.put("myEmail@MyTestDomain.gr", "123456");
                Users.put("myEmail@ServerDomain.gr", "123456");
                Users.put("receip@MyTestDomain.gr", "123456");

                // Create an active client object to handle the active user
                ClientHandler activeClient = new ClientHandler(_socketMngObjVar, clientMSG, _active_clients);
                while (!_socketMngObjVar.soc.isClosed()) { // While a user is connected

                    // If user is NOT Logged in
                    if (!isLoggedIn) {

                        // Decryption of message
                        userEmail = decrypt(_socketMngObjVar.input.readUTF());

                        String userGivenPass = decrypt(_socketMngObjVar.input.readUTF());
                        String userActualPass = Users.get(userEmail);
                        System.out.println("Given pass: " + userGivenPass);
                        System.out.println("Actual pass: " + userActualPass);
                        // if user's password is corect

                        if (userActualPass.contains(userGivenPass)) { // If the user's
                            // KnownEmails.contains(userEmail) && // email exists in
                            // known email list

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

                            // Encrypt and Send error response to client
                            _socketMngObjVar.output.writeUTF(encrypt(
                                    "550 " + userEmail + " is a wrong email, please try again. Type a valid email. "));
                            _socketMngObjVar.output.flush();
                        }
                    }
                    // If user is Logged in
                    else {
                        clientMSG = decrypt(_socketMngObjVar.input.readUTF());
                        // If client message is not empty
                        if (!(clientMSG == "")) {

                            // Decryption of message

                            System.out.println("SERVER : message FROM CLIENT : " + _socketMngObjVar.soc.getPort()
                                    + " --> " + clientMSG);

                            activeClient.Handle(clientMSG); // Start handle the active user

                            // If user sent an email
                            if (!(activeClient.GetMail() == "")) {
                                mail = activeClient.GetMail(); // Get the given mail
                                sharedMailbox.put(userEmail, mail); // Store the mail
                            }

                        } else {
                            System.out.println("No message from client");
                            // Encrypt and Send error response to client
                            _socketMngObjVar.output
                                    .writeUTF(encrypt("550 No message received from client " + userEmail));
                            _socketMngObjVar.output.flush();
                        }
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

}
