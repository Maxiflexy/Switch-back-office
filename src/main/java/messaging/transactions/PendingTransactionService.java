package messaging.transactions;

import constants.AppModules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.MessageDbHelper;
import persistence.PendingTransactionDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class PendingTransactionService implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(PendingTransactionService.class);

    // Define date formatters
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        JsonObject jsonRequest = JsonUtil.toJsonObject(request);

        try {
            // Extract parameters from request
            if (jsonRequest != null) {
                requestBean.setString("batch_id", JsonUtil.getJsonObjValue2(jsonRequest, "batch_id"));

                // Format dates for Oracle compatibility
                String startDate = JsonUtil.getJsonObjValue2(jsonRequest, "start_date");
                String endDate = JsonUtil.getJsonObjValue2(jsonRequest, "end_date");

                requestBean.setString("start_date", formatDateForOracle(startDate));
                requestBean.setString("end_date", formatDateForOracle(endDate));

                //requestBean.setString("start_date", JsonUtil.getJsonObjValue2(jsonRequest, "start_date"));
                //requestBean.setString("end_date", JsonUtil.getJsonObjValue2(jsonRequest, "end_date"));

                requestBean.setString("request_type", JsonUtil.getJsonObjValue2(jsonRequest, "request_type"));
                requestBean.setString("page", JsonUtil.getJsonObjValue2(jsonRequest, "page"));
                requestBean.setString("size", JsonUtil.getJsonObjValue2(jsonRequest, "size"));
            }

            // Validate required parameters
            if (requestBean.getString("batch_id").trim().isEmpty()) {
                return createErrorResponse("400", "batch_id parameter is required");
            }

            // Validate date format
            if (!requestBean.getString("start_date").trim().isEmpty()) {
                String originalStartDate = JsonUtil.getJsonObjValue2(jsonRequest, "start_date");
                if (!isValidDateFormat(originalStartDate)) {
                    return createErrorResponse("400", "Invalid start_date format. Expected: yyyy-MM-ddTHH:mm:ss");
                }
                LOG.info("Formatted start date: {}", requestBean.getString("start_date"));
            }

            if (!requestBean.getString("end_date").trim().isEmpty()) {
                String originalEndDate = JsonUtil.getJsonObjValue2(jsonRequest, "end_date");
                if (!isValidDateFormat(originalEndDate)) {
                    return createErrorResponse("400", "Invalid end_date format. Expected: yyyy-MM-ddTHH:mm:ss");
                }
                LOG.info("Formatted end date: {}", requestBean.getString("end_date"));
            }

            // Validate and log date parameters for debugging
//            if (!requestBean.getString("start_date").trim().isEmpty()) {
//                LOG.info("Start date parameter received: {}", requestBean.getString("start_date"));
//            }
//            if (!requestBean.getString("end_date").trim().isEmpty()) {
//                LOG.info("End date parameter received: {}", requestBean.getString("end_date"));
//            }

            // Set the user for audit trail
            requestBean.setString("current_user", currentUser);
            requestBean.setString("action_id", actionId);

            LOG.info("Fetching pending transactions for batch_id: {}", requestBean.getString("batch_id"));

            // Call the database helper
            boolean success = PendingTransactionDbHelper.getPendingTransactions(requestBean);

            if (success) {
                // Create success response
                return createSuccessResponse(requestBean);
            } else {
                // Create audit log for failed operation
                String message = requestBean.getString("message").isEmpty() ?
                        "Failed to fetch pending transactions" : requestBean.getString("message");
                MessageDbHelper.createMessageHelper(
                        "Pending Transaction Fetch Failed",
                        "Failed to fetch pending transactions: " + message,
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createErrorResponse("500", message);
            }

        } catch (Exception e) {
            LOG.error("Error processing pending transaction request", e);

            // Create audit log for exception
            MessageDbHelper.createMessageHelper(
                    "Pending Transaction Fetch Error",
                    "Error occurred while fetching pending transactions: " + e.getMessage(),
                    currentUser,
                    AppModules.TRANSACTION_MANAGEMENT,
                    currentUser
            );

            return createErrorResponse("500", "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Format date string from ISO format to Oracle-compatible format
     * @param dateStr Input date string in format: yyyy-MM-ddTHH:mm:ss
     * @return Formatted date string in format: yyyy-MM-dd HH:mm:ss, or original string if empty/null
     */
    private String formatDateForOracle(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return dateStr;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateStr.trim(), INPUT_FORMATTER);
            return dateTime.format(OUTPUT_FORMATTER);
        } catch (DateTimeParseException e) {
            LOG.warn("Failed to parse date: {}. Using original value.", dateStr);
            return dateStr; // Return original if parsing fails
        }
    }

    /**
     * Validate if the date string is in the expected format
     * @param dateStr Date string to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidDateFormat(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return true; // Empty dates are considered valid (optional)
        }

        try {
            LocalDateTime.parse(dateStr.trim(), INPUT_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String createSuccessResponse(BaseBean requestBean) {
        try {
            JsonArray dataArray = JsonUtil.toJsonArray(requestBean.getString("pending_transactions"));
            JsonArrayBuilder responseDataBuilder = Json.createArrayBuilder();

            // Transform data to match required response format
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject item = dataArray.getJsonObject(i);
                JsonObject responseItem = Json.createObjectBuilder()
                        .add("tran_ref", JsonUtil.getJsonObjValue2(item, "tran_ref"))
                        .add("tran_date", JsonUtil.getJsonObjValue2(item, "tran_date"))
                        .add("acct_no", JsonUtil.getJsonObjValue2(item, "acct_no"))
                        .add("tran_amt", JsonUtil.getJsonObjValue2(item, "tran_amt"))
                        .add("batch_id", JsonUtil.getJsonObjValue2(item, "batch_id"))
                        .add("tran_narration", JsonUtil.getJsonObjValue2(item, "tran_narration"))
                        .add("response_code", JsonUtil.getJsonObjValue2(item, "response_code"))
                        .add("response_desc", JsonUtil.getJsonObjValue2(item, "response_desc"))
                        .build();
                responseDataBuilder.add(responseItem);
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", "Transactions fetched")
                    .add("page", requestBean.getString("current_page"))
                    .add("size", requestBean.getString("page_size"))
                    .add("total_rows", requestBean.getString("total_rows"))
                    .add("total_pages", requestBean.getString("total_pages"))
                    .add("data", responseDataBuilder.build())
                    .build();

            return JsonUtil.toStr(response);

        } catch (Exception e) {
            LOG.error("Error creating success response", e);
            return createErrorResponse("500", "Error formatting response");
        }
    }

    private String createErrorResponse(String statusCode, String message) {
        JsonObject response = Json.createObjectBuilder()
                .add("status", statusCode)
                .add("message", message)
                .build();

        return JsonUtil.toStr(response);
    }
}