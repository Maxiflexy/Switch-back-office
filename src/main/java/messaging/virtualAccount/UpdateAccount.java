package messaging.virtualAccount;

import constants.AppConstants;
import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.AccountDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.CustomUtil;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;

public class UpdateAccount extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(UpdateAccount.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("action", AppConstants.AppActions.UPDATE);
        boolean result = false;
        if (validateRequest(request, requestBean) && !"01".equals(requestBean.getString("validationcode"))) {
            result = AccountDbHelper.createAccountRequest(requestBean);
            LOG.info("Virtual Account Config request created successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(String request, BaseBean requestBean) {
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateOptionalParameter(jsonRequest, requestBean, "acct-prefix", true);
            validateOptionalParameter(jsonRequest, requestBean, "acct-enq-url", true);
            validateOptionalParameter(jsonRequest, requestBean, "credt-trf-url", true);
            validateOptionalParameter(jsonRequest, requestBean, "debt-trf-url", true);
            validateOptionalParameter(jsonRequest, requestBean, "service-provider", true);
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
                        .add("service-provider", requestBean.getString("service-provider"))
                        .build())
                .build();
        return jsonResp.toString();
    }
}