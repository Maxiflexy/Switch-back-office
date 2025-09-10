package messaging.fileUtils.messaging;

import messaging.fileUtils.util.BaseBean;
import messaging.fileUtils.util.MessageBodyUtil;
import messaging.fileUtils.util.MessageBuilder;
import messaging.fileUtils.util.ProcessUtil;

public class NotificationService {

    public static String generateUserCreationNotificationTemplate(BaseBean requestBean) {
        MessageBuilder msgBuilder = new MessageBuilder(MessageBodyUtil.getBodyTempl(ProcessUtil.USER_CREATION.MESSAGE_TYPE));
        msgBuilder.replace("${firstName}", requestBean.getString("firstName"));
        msgBuilder.replace("${lastName}", requestBean.getString("lastName"));
        msgBuilder.replace("${url}", requestBean.getString("token"));
        return msgBuilder.getMainStr();
    }


}
