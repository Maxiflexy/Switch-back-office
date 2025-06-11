package messaging.serviceProvider;

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

public class CreateService extends RequestValidator implements RequestExecutor {
    final static Logger LOG = LogManager.getLogger(CreateService.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("action", AppConstants.AppActions.CREATE);
        boolean result = false;
        if (validateRequest(request, requestBean) && !"01".equals(requestBean.getString("validationcode"))) {
            result = ServiceDbHelper.createServiceRequest(requestBean);
            LOG.info("Service Provider request created successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(String request, BaseBean requestBean) {
        boolean response = false;
        try {
            JsonObject jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "service-provider-id");
            validateParameter(jsonRequest, requestBean, "service-provider-name");
//            validateParameter(jsonRequest, requestBean, "user");
            response = true;
        } catch (Exception ex) {
            LOG.error("Validation error: ", ex);
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", "Bad request");
        }
        return response;
    }

//    private void validateParameter(JsonObject jsonRequest, BaseBean requestBean, String paramName) throws Exception {
//        if (!jsonRequest.containsKey(paramName) || jsonRequest.getString(paramName).isEmpty()) {
//            throw new Exception("Missing or empty parameter: " + paramName);
//        }
//        requestBean.setString(paramName, jsonRequest.getString(paramName));
//    }

    private String createReply(BaseBean requestBean, boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(CustomUtil.createErrorBean(requestBean));
        }

        String message = "Service request submitted, pending approval from admin";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("service-provider-id", requestBean.getString("service-provider-id"))
                        .build())
                .build();
        return jsonResp.toString();
    }
}