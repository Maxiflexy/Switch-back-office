package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class EmailUtil {

    static final Logger LOG = LogManager.getLogger(EmailUtil.class);


    public static final int TRAN_LIMIT = 1000;

    public static boolean sendEmail(Email email) {

        String emailHost = System.getProperty("email-host");
        String password = System.getProperty("email-password");
        String user = System.getProperty("email-from");
        String port = System.getProperty("email-port");

        Properties props = new Properties();
        props.put("mail.smtp.host", emailHost);
        props.put("mail.smtp.port", port);
        props.put("mail.transport.protocol","smtp");

        Session session = Session.getDefaultInstance(props);
        session.setDebug(true);

        //Compose the message
        try {

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email.getTo()));
            if (email.getCc() != null && email.getCc().length != 0) {
                message.addRecipients(Message.RecipientType.CC, email.getCc());
            }
            if (!email.isHasAttachment()) {
                if (email.getContentType() != null && !email.getContentType().isEmpty()) {
                    message.setContent(email.getContent(), email.getContentType());
                } else {
                    message.setText(email.getContent());
                }
                message.setSubject(email.getTitle());
            } else {
                Message mimeMessahe = new MimeMessage(session);
                mimeMessahe.setFrom(new InternetAddress(user));
                mimeMessahe.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getTo()));
                mimeMessahe.setSubject("Transaction Data Excel File");
                BodyPart messageBodyPart = new MimeBodyPart();
                String htmlContent = email.getContent();
                messageBodyPart.setContent(htmlContent, email.getContentType());
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                for (String s : email.getFiles()) {
                    addAttachment(multipart, s);
                }

                mimeMessahe.setContent(multipart);

            }

            //send the message
            Transport.send(message);

            System.out.println("message sent successfully...");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void sendAsyncEmailNotification(Email email) {

        try {

            EmailNotification executeNotification = new EmailNotification(email);
            executeNotification.start();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }







    private static void addAttachment(Multipart multipart, String filename) throws MessagingException {
        DataSource source = new FileDataSource(filename);
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);
    }


}
