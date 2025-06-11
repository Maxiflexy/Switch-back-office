package messaging;

import util.BaseBean;
import util.MessageBodyUtil;
import util.MessageBuilder;
import util.ProcessUtil;

public class NotificationService {

    public static String generateUserCreationNotificationTemplate(BaseBean requestBean) {
        MessageBuilder msgBuilder = new MessageBuilder(MessageBodyUtil.getBodyTempl(ProcessUtil.USER_CREATION.MESSAGE_TYPE));
        msgBuilder.replace("${firstName}", requestBean.getString("firstName"));
        msgBuilder.replace("${lastName}", requestBean.getString("lastName"));
        msgBuilder.replace("${url}", requestBean.getString("token"));
        return msgBuilder.getMainStr();
    }


}
