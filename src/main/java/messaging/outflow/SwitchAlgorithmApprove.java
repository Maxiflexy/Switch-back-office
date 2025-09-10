package messaging.outflow;

import constants.OutflowSwitchModule;
import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.OutflowSwitchDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.time.format.DateTimeFormatter;

public class SwitchAlgorithmApprove extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(SwitchAlgorithmApprove.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("module", OutflowSwitchModule.SWITCH_ALGORITHM_MODULE.getName());

        boolean result = false;
        if (validateRequestBody(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {

            result = OutflowSwitchDbHelper.approveSwitchRequest(requestBean);
            LOG.info("Switch Algorithm approved successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    protected boolean validateRequestBody(BaseBean requestBean, String request) {
        boolean response = false;
        JsonObject jsonRequest = null;
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "id", false);
            validateOptionalParameter(jsonRequest, requestBean, "status", true);
            response = true;

        } catch (Exception e) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return response;

    }

    protected String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObject jsonResp;
        if (!procErr) {
            throw new CustomException(requestBean);
        }
        String message = "Algorithm Request Created";
        jsonResp = Json.createObjectBuilder()
                .add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .build();
        return jsonResp.toString();
    }
}
