package messaging.fileUtils.messaging;

import messaging.fileUtils.util.BaseBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class CommonDB extends RequestValidator {

    final static private Logger LOG = LogManager.getLogger(Common.class);

    final static protected String INST_ID = "UBA";

    protected abstract String validateRequestBody(String request, BaseBean requestBean);

    protected abstract void processResponse(BaseBean requestBean);

    protected abstract String createReply(BaseBean requestBean, Boolean procErr);


    public boolean performDatabaseOperation() {
        return false;
    }

}

