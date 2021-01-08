import java.util.*;
import java.util.Map.Entry;

public class MailBox {
    private Map<String, String> mailBoxAddresses = new HashMap<String, String>();
    private Map<String, List<Mail>> mailBoxes;

    public MailBox() {
    }

    // Initialize the mail box and addresses
    {
        mailBoxAddresses.put("1", "alice@ThatDomain.gr");
        mailBoxAddresses.put("2", "bob@MyTestDomain.gr");
        mailBoxAddresses.put("3", "jack@ServerDomain.gr");
        mailBoxAddresses.put("4", "james_bond@ThatDomain.gr");

        mailBoxes = new HashMap<>();
    }

    // Returns true if a user have emails
    public Boolean haveEmails(String mailBoxId) {

        if (mailBoxes.containsKey(mailBoxId)) {
            return true;
        } else {
            return false;
        }
    }

    // Returns the shared mailbox id associated with an email address
    public String getMailBoxId(String emailAddress) {
        String mailBoxId = getKey(mailBoxAddresses, emailAddress);
        return mailBoxId;
    }

    // Map Key search method
    public static <K, V> K getKey(Map<K, V> mailBoxAddresses, V value) {
        for (Entry<K, V> entry : mailBoxAddresses.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Saves the email with an id
    public void saveMail(String mailBoxId, Mail mail) {
        // Save the mail
        if (!mailBoxes.containsKey(mailBoxId)) {
            mailBoxes.put(mailBoxId, new ArrayList<>());
        }
        mailBoxes.get(mailBoxId).add(mail);

    }

    // Returns mail list of given mailbox id
    public String Getmailbox(String getMailBoxId) {
        String mailList = "";
        List<Mail> mails = mailBoxes.get(getMailBoxId);
        for (Mail mail : mails) {
            mailList += mail.getEmail();
        }
        return mailList;
    }
}
