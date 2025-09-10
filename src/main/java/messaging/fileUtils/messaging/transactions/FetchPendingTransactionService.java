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
import java.time.format.DateTimeFormatter;

public class FetchPendingTransactionService extends RequestValidator implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(FetchPendingTransactionService.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        requestBean.setString("user", currentUser);
        boolean result = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            String module = requestBean.getString("module");
            if(module.equalsIgnoreCase("inflow")) {
                result = TransactionsDbHelper.fetchPendingInflowTransactions(requestBean);
            } else if (module.equalsIgnoreCase("outflow")) {
                result = TransactionsDbHelper.fetchPendingOutflowTransactions(requestBean);
            } else if (module.equalsIgnoreCase("airtime")) {
                result = TransactionsDbHelper.fetchPendingAirtimeTransactions(requestBean);
            } else {
                requestBean.setString("message", "Invalid module type");
            }
            LOG.info("Transactions fetched successfully: {}", result);
        }
        return createReply(requestBean, result);
    }


    private boolean validateRequest(BaseBean requestBean, String request) {
        boolean response = false;
        JsonObject jsonRequest = null;
        DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateDateParameter(jsonRequest, requestBean, "start_date", format);
            validateDateParameter(jsonRequest, requestBean, "end_date", format);
            validateOptionalParameter(jsonRequest, requestBean, "page", true);
            validateOptionalParameter(jsonRequest, requestBean, "size", true);
            validateParameter(jsonRequest, requestBean, "operation_type");
            validateParameter(jsonRequest, requestBean, "module");
            response = true;

        } catch (Exception e) {
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
            throw new CustomException(requestBean);
        }
        double totalPages = Math.ceil(Integer.parseInt(requestBean.getString("total_count")) / Double.parseDouble(requestBean.getString("size")));
        String message = "Transactions fetched";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
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
