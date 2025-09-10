package messaging.fileUtils.messaging;

import exceptions.CustomException;
import messaging.Common;
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

        requestBean.setString("batch_id", batchId);
        requestBean.setString("created_by", currentUser);

        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, false);
        }

        boolean recordSaved = false;
        String module = requestBean.getString("module");
        if (module.equalsIgnoreCase("inflow")) {
            recordSaved = DBHelper.writeInflowToRetrialTable(requestBean);
        } else if (module.equalsIgnoreCase("outflow")) {
            recordSaved = DBHelper.writeToRetrialTableOutflow(requestBean);
        } else if (module.equalsIgnoreCase("airtime")) {
            recordSaved = DBHelper.writeToRetrialTableAirtime(requestBean);
        }
//        if (DBHelper.writeToRetrialTable(requestBean)) {
//            recordSaved = true;
//        }

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
            validateParameter(authRequest, requestBean, "switch_type");
            validateParameter(authRequest, requestBean, "module");
            validateDateRange(requestBean.getString("start_date"), requestBean.getString("end_date"));

        } catch (Exception Ex) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("statusCode", "400");
                requestBean.setString("message", Ex.getMessage());
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
