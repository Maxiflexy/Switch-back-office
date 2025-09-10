package messaging.fileUtils.messaging.serviceProvider;

import constants.AppConstants;
import exceptions.CustomException;
import messaging.RequestValidator;
import messaging.serviceProvider.CreateService;
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

public class DeactivateService extends RequestValidator implements RequestExecutor {
    final static Logger LOG = LogManager.getLogger(CreateService.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("action", AppConstants.AppActions.DEACTIVATE);
        boolean result = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            result = ServiceDbHelper.createServiceRequest(requestBean);
            LOG.info("Service provider request created successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {
        //validate aggregator-name, aggregator-code
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "service-provider-id");
            validateParameter(jsonRequest, requestBean, "service-provider-name");
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

        String message = "Service provider request submitted, pending approval from admin";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("sp-id", requestBean.getString("service-provider-id"))
                        .build())
                .build();
        return jsonResp.toString();
    }
}