import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Helper class to contain email data
 */
public class Mail {

    private String _fromAddress = "";
    private List<String> _toAddress = null;
    private String _messageBody = "";
    private String _timeStamp = "";

    public Mail() {
        _toAddress = new ArrayList<>();
        _fromAddress = "";
        _timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    }

    // Sets an email
    public void setEmail(String From, ArrayList<String> To, String messageBody) {
        _fromAddress = From;
        _toAddress = To;
        _messageBody = messageBody;
    }

    // Returns an email
    public String getEmail() {
        String recipients = "";
        for (String f : _toAddress) {
            recipients = f + ", ";
        }
        String email = "\tFrom: " + _fromAddress + "\n" + "\tTo: " + recipients + "\n" + _timeStamp + "\n\n"
                + _messageBody;
        return email;
    }

    public void printMsg() {
        System.out.println("\tFrom: " + _fromAddress + "\n");

        System.out.println("\tTo: ");
        for (String f : _toAddress) {
            System.out.println(f + ", ");
        }
        System.out.println(_timeStamp);
        System.out.println("\tData");
        String[] data = _messageBody.split("\n");
        for (String f : data) {
            System.out.println("\t\t" + f);
        }
    }
}
