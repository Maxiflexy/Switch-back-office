package messaging.outflow;

import constants.OutflowSwitchModule;
import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SwitchAlgorithmDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.time.format.DateTimeFormatter;

public class FetchSwitchAlgorithmRequest extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(FetchSwitchAlgorithmRequest.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        requestBean.setString("module", OutflowSwitchModule.SWITCH_ALGORITHM_MODULE.getName());

        boolean result = false;
        if (validateRequestBody(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            result = SwitchAlgorithmDbHelper.fetchSwitchAlgorithmRequest(requestBean);
            LOG.info("Fetching switch algorithm: {}", result);
        }
        return createReply(requestBean, result);
    }

    protected boolean validateRequestBody(BaseBean requestBean, String request) {
        boolean response = false;
        JsonObject jsonRequest = null;
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateOptionalDateParameter(jsonRequest, requestBean, "start_date", format);
            validateOptionalDateParameter(jsonRequest, requestBean, "end_date", format);
            validateOptionalParameter(jsonRequest, requestBean, "page", true);
            validateOptionalParameter(jsonRequest, requestBean, "size", true);
            validateOptionalParameter(jsonRequest, requestBean, "id", true);
            validateOptionalParameter(jsonRequest, requestBean, "details", false);
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

    protected String createReply(BaseBean requestBean, Boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(requestBean);
        }
        double totalPages = Math.ceil(Integer.parseInt(requestBean.getString("total_count")) / Double.parseDouble(requestBean.getString("size")));
        String message = "fetch successful";
        jsonResp = Json.createObjectBuilder()
                .add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("page", requestBean.getString("page"))
                .add("size", requestBean.getString("size"))
                .add("total_rows", requestBean.getString("total_count"))
                .add("total_pages", totalPages)
                .add("data", JsonUtil.toJsonArray(requestBean.getString("jsonBean")))
                .build();
        return jsonResp.toString();
    }
}