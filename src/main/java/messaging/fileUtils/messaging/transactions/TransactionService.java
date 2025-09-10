package messaging.fileUtils.messaging.transactions;


import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.TransactionsDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;

public class TransactionService extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(TransactionService.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        boolean result = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            result = TransactionsDbHelper.getTransactionStatistics(requestBean);
            LOG.info("Transactions fetched successfully: {}", result);
        }
        return createReply(requestBean, result);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateOptionalParameter(jsonRequest, requestBean, "start-date", true);
            validateOptionalParameter(jsonRequest, requestBean, "end-date", true);
            validateOptionalParameter(jsonRequest, requestBean, "session-id", true);
            validateParameter(jsonRequest, requestBean, "switch-type", true);
            validateOptionalParameter(jsonRequest, requestBean, "account-number", true);
            validateOptionalParameter(jsonRequest, requestBean, "isSuccessful", false);
//            validateOptionalParameter(jsonRequest, requestBean, "page", true);
//            validateOptionalParameter(jsonRequest, requestBean, "size", true);

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

        String message = "Transactions fetched";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", Json.createObjectBuilder()

//                        .add("page", requestBean.getString("page"))
//                        .add("size", requestBean.getString("size"))
//                        .add("total-pages", divide(requestBean.getString("transaction_count"), requestBean.getString("size")))
                        .add("transactions", JsonUtil.toJsonArray(requestBean.getString("transaction_response")))
                        .build())
                .build();
        return jsonResp.toString();
    }

    private String divide(String divider, String divisor) {
        return String.valueOf(Math.floor(Double.parseDouble(divider)/Double.parseDouble(divisor)));
    }
}
