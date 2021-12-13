# SMTP_Implementation

This is a simple implementation of SMTP protocol with JAVA.

List of user emails and passwords provided to logging the app:

##########################################

jack@ServerDomain.gr - 123456
alice@ThatDomain.gr - 123456
bob@MyTestDomain.gr - 123456
james_bond@ThatDomain.gr - 123456

##########################################

# This implemetation can execute the following SMTP commands:

HELO, MAIL FROM, RCPT TO, DATA, NOOP, RSET, VRFY, EXPN, HELP, QUIT

# In order to start run this implementation, make the following steps:

1. Run the Server.java (Server/src/Server.java).

2. Run the Client.java (Client/src/Client.java).

3. Enter an email address and a password from the above list.

4. Start the SMTP command sequence

This implementation supports Mailbox functionality only for recipients. To access this,
type "mailbox". User should be logged in to the system to receive the mailbox.

# Explanation of the program

--------- Client Side -----------

1. HELO command uses as domain name, the given from the user email address,
   cuts the domain, and then sends it to the server.

2. MAIL FROM takes as email address, the given from the user email address.

3. RCPT TO takes an Arralist items and sends them to the server.

4. DATA sends to the server an hard coded message and send it to the server.

5. NOOP just sends NOOP to Server.

6. RSET just sends RSET to Server.

7. VRFY sends VRFY Bob Marley to Server to verify the user Bob Marley.

8. EXPN request to server to return a list with other users.

9. HELP ask for help about commands from the server

10. QUIT sends a request for exit.

--------- Server Side -----------

1. HELO grabs the domain name and checks if exists in servers known domain list.
   If exists response with 250 OK else response 451 error.

2. MAIL FROM takes the email and checks if exists in servers known email list and if exists response with 250 OK else response 451 error.

3. RCPT TO takes recipients emails, check if they exist in known email list
   and added them to the recipient list.
   If exists response with 250 OK else response 451 error.

4. DATA returns 354 End data with <CRLF>.<CRLF> . When the message is saved returns
   250 OK.

5. NOOP just returns 250 ok response.

6. VRFY veryfies if given name exists in a map with users and returns the name and the email address of the user.
   If exists responed with 250 OK else response 550 error.

7. EXPN returns an list with users and emails.

8. RSET clears all buffer lists which contains data such us recipients messages and implemented commands.
   When is done returns a response with 250 OK.

9. HELP returns details for each SMTP command.

10. QUIT shutting down the client connection.
