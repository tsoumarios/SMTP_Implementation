import java.util.*;

public class MailBox {
    private static Map<String, String> mailBoxAddresses = new HashMap<String, String>();
    private static Map<String, List<Mail>> mailBoxes;

    // Initialize the shared mail box addresses and mailbox
    static {
        mailBoxAddresses.put("1", "alice@ThatDomain.gr");
        mailBoxAddresses.put("2", "myEmail@MyTestDomain.gr");
        mailBoxAddresses.put("3", ",myEmail@ServerDomain.gr");
        mailBoxAddresses.put("4", "receip@MyTestDomain.gr");

        mailBoxes = new HashMap<>();
    }

    /**
     * Retrieve the shared mailbox id associated with an email address
     */
    public static String getMailBoxId(String emailAddress) {
        // Iterate through all the mailbox ids
        for (String mailboxId : mailBoxAddresses.keySet()) {
            if (Arrays.asList(mailBoxAddresses.get(mailboxId)).contains(emailAddress)) {
                // if we find the email address present in a mailbox return the corresponding
                // mailbox id
                return mailboxId;
            }
        }
        return null;
    }

    public static void saveMail(String mailBoxId, Mail mail) {
        // Save the mail
        if (!mailBoxes.containsKey(mailBoxId)) {
            mailBoxes.put(mailBoxId, new ArrayList<>());
        }
        mailBoxes.get(mailBoxId).add(mail);
    }
}
