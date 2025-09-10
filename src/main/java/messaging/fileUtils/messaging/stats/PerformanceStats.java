package messaging.fileUtils.messaging.stats;

import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.PerformanceDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.CustomUtil;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.format.DateTimeFormatter;

public class PerformanceStats extends RequestValidator implements RequestExecutor {
    final static Logger LOG = LogManager.getLogger(PerformanceStats.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        boolean result = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            result = PerformanceDbHelper.getPerformanceStatistics(requestBean);
            LOG.info("Fetching performance status: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {
        //validate aggregator-name, aggregator-code
        JsonObject jsonRequest = null;
        boolean response = false;
        DateTimeFormatter dateFormat = DateTimeFormatter.ISO_DATE_TIME;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateOptionalParameter(jsonRequest, requestBean, "total_tat", true);
            validateOptionalParameter(jsonRequest, requestBean, "endpoint", true);
            validateDateParameter(jsonRequest, requestBean, "start-date",dateFormat);
            validateDateParameter(jsonRequest, requestBean, "end-date", dateFormat);
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

        String message = "Performance statistics fetched";
        JsonArray result = JsonUtil.toJsonArray(requestBean.getString("db_response"));
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data",  result).build();
        return jsonResp.toString();
    }
}
