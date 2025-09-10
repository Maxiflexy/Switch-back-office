package messaging.fileUtils.messaging.serviceProvider;

import constants.AppConstants;
import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.ServiceDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.CustomUtil;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;

public class UpdateService extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(UpdateService.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("action", AppConstants.AppActions.UPDATE);
        boolean result = false;
        if (validateRequest(request, requestBean) && !"01".equals(requestBean.getString("validationcode"))) {
            result = ServiceDbHelper.createServiceRequest(requestBean);
            LOG.info("Service Provider request created successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(String request, BaseBean requestBean) {
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateOptionalParameter(jsonRequest, requestBean, "service-provider-id", true);
            validateOptionalParameter(jsonRequest, requestBean, "service-provider-name", true);
            validateParameter(jsonRequest, requestBean, "update-id");
            response = true;
        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return response;
    }

    public String createReply(BaseBean requestBean, boolean error) {

        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(CustomUtil.createErrorBean(requestBean));
        }

        String message = "Service provider request submitted, pending approval from admin";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("service-provider-id", requestBean.getString("service-provider-id"))
                        .build())
                .build();
        return jsonResp.toString();
    }
}