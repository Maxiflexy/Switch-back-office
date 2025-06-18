package messaging;

import exceptions.CustomException;
import persistence.DBHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class RetrialService extends Common implements RequestExecutor {


    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        String batchId = generateBatchId();
        String requestString = validateRequestBody(request, requestBean);

        JsonObject jsonObject = JsonUtil.toJsonObject(request);
        System.out.println(jsonObject.toString());

        requestBean.setString("batch_id", batchId);
        requestBean.setString("created_by", currentUser);

        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, false);
        }

        boolean recordSaved = false;
        if (DBHelper.writeToRetrialTable(requestBean)) {
            recordSaved = true;
        }

        return createReply(requestBean, recordSaved);
    }


    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        JsonObject authRequest = null;
        try {
            authRequest = JsonUtil.toJsonObject(request);
            validateParameter(authRequest, requestBean, "service_type");
            validateParameter(authRequest, requestBean, "start_date");
            validateParameter(authRequest, requestBean, "end_date");
//            validateDateRange(requestBean.getString("start_date"), requestBean.getString("end_date"));

        } catch (Exception Ex) {
            if (requestBean.getString("service_type").isEmpty() || requestBean.getString("start_date").isEmpty()
                    || requestBean.getString("end_date").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return request;
    }

    @Override
    protected void processResponse(BaseBean requestBean) {

    }



    @Override
    protected String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObject jsonResp;
        if (!procErr) {
            throw new CustomException(requestBean);
        }
        String message = "Batch Created";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("batch_id", requestBean.getString("batch_id"))
                .build();
        return jsonResp.toString();
    }

    public static String generateBatchId() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return now.format(formatter);
    }



}
