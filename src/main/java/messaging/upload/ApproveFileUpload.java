package messaging.upload;

import exceptions.CustomException;
import messaging.RequestValidator;
import persistence.FileUploadDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;

public class ApproveFileUpload extends RequestValidator implements RequestExecutor {

    @Override
    public String execute(String request, String currentUser, String actionId) {

        BaseBean requestBean = new BaseBean();
        requestBean.setString("email", currentUser);
        JsonObject requestObject = JsonUtil.toJsonObject(request);
        boolean success = false;

        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            success = FileUploadDbHelper.approveUpload(requestBean, requestObject);
        }

        return createReply(requestBean, success);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {

        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateParameter(jsonRequest, requestBean, "document_id", true);
            validateParameter(jsonRequest, requestBean, "status", false);
            validateOptionalParameter(jsonRequest, requestBean, "message", true);
            response = true;

        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }

        return response;

    }

    public String createReply(BaseBean requestBean, Boolean error) {

        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(requestBean);
        }
        boolean status = requestBean.get("status").equals("true");
        String message = status ? "Request approved successfully" : "Request declined successfully";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()
                        .add("id", requestBean.getString("document_id"))
                        .build())
                .build();
        return jsonResp.toString();
    }

}
