package messaging.fileUtils.messaging.virtualAccount;

import exceptions.CustomException;
import messaging.RequestValidator;
import messaging.virtualAccount.CreateAccount;
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

public class ApproveAccount extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(CreateAccount.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        boolean result = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            result = AccountDbHelper.approveAccountRequest(requestBean);
            LOG.info("Account config request created successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "mc-status", false);
            validateParameter(jsonRequest, requestBean, "approval-comment");
            validateParameter(jsonRequest, requestBean, "service-provider");
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

        String message = "Account config request ".concat(Boolean.parseBoolean(requestBean.getString("mc-status")) ? "approved" : "cancelled");
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", "")
                .build();
        return jsonResp.toString();
    }

}