package messaging.messages;

import persistence.MessageDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class UpdateReadStatus implements RequestExecutor {


    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user-id", currentUser);
        boolean response = MessageDbHelper.updateReadStatus(requestBean);

        return createReply(requestBean, response);
    }

    private String createReply(BaseBean requestBean, boolean error) {
        JsonObject jsonResp = null;
        JsonObjectBuilder jObjBuil = Json.createObjectBuilder();
        if (!error) {
            requestBean.setString("status_type", ResponseUtil.FAIL);
            requestBean.setString("status_code", ResponseUtil.GENERIC_PROCESSING_ERROR);
        }
        if (requestBean.containsKey("status_type") &&
                requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
                    .add("message", requestBean.getString("message"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }
        String message = "Successfully updated read status";
//        JsonObject sequence = JsonUtil.toJsonObject(requestBean.getString("response"));
        jsonResp = jObjBuil.add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", "")
                .build();
        return jsonResp.toString();
    }

}
