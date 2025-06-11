package messaging.serviceProvider;

import exceptions.CustomException;
import persistence.ServiceDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.CustomUtil;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class GetServiceList implements RequestExecutor {

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        boolean error = ServiceDbHelper.getApprovedServices(requestBean, JsonUtil.toJsonObject(request));
        return createReply(requestBean, error);
    }

    public String createReply(BaseBean requestBean, boolean success) {
        JsonObject jsonResp;
        if (!success) {
            throw new CustomException(CustomUtil.createErrorBean(requestBean));
        }

        String message = "Successfully fetched service providers";
        JsonArray serviceList = JsonUtil.toJsonArray(requestBean.getString("db_response"));
        jsonResp = Json.createObjectBuilder()
                .add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", serviceList)
                .build();

        return jsonResp.toString();
    }
}
