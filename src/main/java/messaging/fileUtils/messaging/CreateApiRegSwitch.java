package messaging.fileUtils.messaging;

import messaging.fileUtils.dao.ApiRegistryDao;
import messaging.fileUtils.exceptions.CustomException;
import messaging.fileUtils.util.BaseBean;
import messaging.fileUtils.util.ResponseUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;

public class CreateApiRegSwitch {//extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(CreateApiRegSwitch.class);
    ApiRegistryDao apiRegistryDao;

    public CreateApiRegSwitch() {
        apiRegistryDao = new ApiRegistryDao();
    }

  //  @Override
    public String execute(BaseBean bean, String request) {
        BaseBean requestBean = new BaseBean();

        boolean result = false;

        if (validateRequest(request)) {
            result = apiRegistryDao.insertApiRegistry(bean);
            LOG.info("Created successfully: {}", result);
        }
        return createReply(result);
    }

    public boolean validateRequest(String request) {
        if (request.isEmpty()) {
            return false;
        }

        return true;
    }

    private String createReply(boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(new BaseBean());
        }

        String message = "Request submitted";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .build();
        return jsonResp.toString();
    }
}

