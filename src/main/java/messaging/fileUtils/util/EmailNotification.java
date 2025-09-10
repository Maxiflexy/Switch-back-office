package messaging.fileUtils.util;

import util.Email;
import util.EmailUtil;

public class EmailNotification extends Thread {

    private final Email email;

    public EmailNotification(Email email) {
        this.email = email;
    }

    @Override
    public void run() {
         EmailUtil.sendEmail(email);
    }
}
