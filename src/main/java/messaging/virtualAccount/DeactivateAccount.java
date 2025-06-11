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
import java.util.UUID;

public class DeactivateAccount extends RequestValidator implements RequestExecutor {
    final static Logger LOG = LogManager.getLogger(CreateAccount.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("action", AppConstants.AppActions.DEACTIVATE);
        requestBean.setString("service-provider", UUID.randomUUID().toString());
        boolean result = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            result = AccountDbHelper.createAccountRequest(requestBean);
            LOG.info("Virtual Account config request created successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {
        //validate aggregator-name, aggregator-code
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "service-provider");
            validateParameter(jsonRequest, requestBean, "acct-prefix");
            validateParameter(jsonRequest, requestBean, "acct-enq-url");
            validateParameter(jsonRequest, requestBean, "credt-trf-url");
            validateParameter(jsonRequest, requestBean, "debt-trf-url");
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

    private String createReply(BaseBean requestBean, boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(CustomUtil.createErrorBean(requestBean));
        }

        String message = "Virtual Account config request submitted, pending approval from admin";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("service-provider", requestBean.getString("service-provider"))
                        .build())
                .build();
        return jsonResp.toString();
    }
}
