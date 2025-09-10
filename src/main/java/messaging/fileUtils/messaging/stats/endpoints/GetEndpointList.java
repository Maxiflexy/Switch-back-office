package messaging.fileUtils.messaging.stats.endpoints;

import exceptions.CustomException;
import persistence.PerformanceDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.CustomUtil;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class GetEndpointList implements RequestExecutor {

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        boolean error = PerformanceDbHelper.getApprovedEndpoints(requestBean, JsonUtil.toJsonObject(request));
        return createReply(requestBean, error);
    }

    public String createReply(BaseBean requestBean, boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(CustomUtil.createErrorBean(requestBean));
        }

        String message = "Successfully fetched endpoints";
        JsonArray switchList = JsonUtil.toJsonArray(requestBean.getString("db_response"));
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", switchList)
                .build();
        return jsonResp.toString();
    }


}
