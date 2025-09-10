package messaging.fileUtils.messaging.switchService;

import constants.AppConstants;
import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SwitchDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.CustomUtil;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;

public class CreateSwitch extends RequestValidator implements RequestExecutor {
    final static Logger LOG = LogManager.getLogger(CreateSwitch.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("mc-action", AppConstants.AppActions.CREATE);
        requestBean.setString("mc-id", UUID.randomUUID().toString());
        boolean result = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            result = SwitchDbHelper.createSwitchRequest(requestBean);
            LOG.info("Switch request created successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {
        //validate aggregator-name, aggregator-code
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "switch-name");
            validateParameter(jsonRequest, requestBean, "switch-code");
            response = true;
        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return response;
    }

    private String createReply(BaseBean requestBean, boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(CustomUtil.createErrorBean(requestBean));
        }

        String message = "Switch request submitted, pending approval from admin";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("id", requestBean.getString("mc-id"))
                        .build())
                .build();
        return jsonResp.toString();
    }
}
