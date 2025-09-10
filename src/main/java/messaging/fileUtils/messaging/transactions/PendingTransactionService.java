package messaging.fileUtils.messaging.transactions;

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
import java.util.Arrays;
import java.util.List;

public class PendingTransactionService implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(PendingTransactionService.class);

    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final List<String> VALID_MODULE_TYPES = Arrays.asList("INFLOW", "OUTFLOW", "AIRTIME");

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        JsonObject jsonRequest = JsonUtil.toJsonObject(request);

        try {
            // Extract parameters from request
            if (jsonRequest != null) {
                requestBean.setString("batch_id", JsonUtil.getJsonObjValue2(jsonRequest, "batch_id"));
                requestBean.setString("tran_ref", JsonUtil.getJsonObjValue2(jsonRequest, "tran_ref"));
                requestBean.setString("start_date", JsonUtil.getJsonObjValue2(jsonRequest, "start_date"));
                requestBean.setString("end_date", JsonUtil.getJsonObjValue2(jsonRequest, "end_date"));
                requestBean.setString("request_type", JsonUtil.getJsonObjValue2(jsonRequest, "request_type"));
                requestBean.setString("module_type", JsonUtil.getJsonObjValue2(jsonRequest, "module_type"));
                requestBean.setString("page", JsonUtil.getJsonObjValue2(jsonRequest, "page"));
                requestBean.setString("size", JsonUtil.getJsonObjValue2(jsonRequest, "size"));
            }

            // Validate required parameters
            if (requestBean.getString("batch_id") == null || requestBean.getString("batch_id").trim().isEmpty()) {
                return createErrorResponse("400", "batch_id parameter is required");
            }

            // Validate module_type parameter (required)
            if (requestBean.getString("module_type") == null || requestBean.getString("module_type").trim().isEmpty()) {
                return createErrorResponse("400", "module_type parameter is required");
            }

            String moduleType = requestBean.getString("module_type").trim().toUpperCase();
            if (!VALID_MODULE_TYPES.contains(moduleType)) {
                return createErrorResponse("400", "Invalid module_type. Must be one of: " + String.join(", ", VALID_MODULE_TYPES));
            }
            requestBean.setString("module_type", moduleType);

            // Validate batch_id format (must be a valid number for NUMBER(28,0) column)
            try {
                Long.parseLong(requestBean.getString("batch_id").trim());
            } catch (NumberFormatException e) {
                return createErrorResponse("400", "Invalid batch_id format. Must be a valid number.");
            }

            // Validate tran_ref parameter (optional)
            if (requestBean.getString("tran_ref") != null && !requestBean.getString("tran_ref").trim().isEmpty()) {
                String tranRef = requestBean.getString("tran_ref").trim();
                int maxLength = getMaxTranRefLength(moduleType);
                if (tranRef.length() > maxLength) {
                    return createErrorResponse("400", "tran_ref parameter cannot exceed " + maxLength + " characters for " + moduleType);
                }
                LOG.info("Transaction reference parameter provided: {}", tranRef);
            }

            // Validate date format for start_date (optional)
            if (requestBean.getString("start_date") != null && !requestBean.getString("start_date").trim().isEmpty()) {
                String startDate = requestBean.getString("start_date").trim();
                if (!isValidDateFormat(startDate)) {
                    return createErrorResponse("400", "Invalid start_date format. Expected: yyyy-MM-ddTHH:mm:ss (e.g., 2025-06-16T07:38:25)");
                }
                LOG.info("Start date parameter validated: {}", startDate);
            }

            // Validate date format for end_date (optional)
            if (requestBean.getString("end_date") != null && !requestBean.getString("end_date").trim().isEmpty()) {
                String endDate = requestBean.getString("end_date").trim();
                if (!isValidDateFormat(endDate)) {
                    return createErrorResponse("400", "Invalid end_date format. Expected: yyyy-MM-ddTHH:mm:ss (e.g., 2025-06-17T07:38:25)");
                }
                LOG.info("End date parameter validated: {}", endDate);
            }

            // Validate pagination parameters
            if (requestBean.getString("page") != null && !requestBean.getString("page").trim().isEmpty()) {
                try {
                    int page = Integer.parseInt(requestBean.getString("page").trim());
                    if (page < 1) {
                        return createErrorResponse("400", "page parameter must be greater than 0");
                    }
                } catch (NumberFormatException e) {
                    return createErrorResponse("400", "Invalid page parameter. Must be a valid integer.");
                }
            }

            if (requestBean.getString("size") != null && !requestBean.getString("size").trim().isEmpty()) {
                try {
                    int size = Integer.parseInt(requestBean.getString("size").trim());
                    if (size < 1) {
                        return createErrorResponse("400", "size parameter must be greater than 0");
                    }
                    if (size > 100) {
                        return createErrorResponse("400", "size parameter cannot exceed 100");
                    }
                } catch (NumberFormatException e) {
                    return createErrorResponse("400", "Invalid size parameter. Must be a valid integer.");
                }
            }

            // Set the user for audit trail
            requestBean.setString("current_user", currentUser);
            requestBean.setString("action_id", actionId);

            LOG.info("Fetching pending transactions for module_type: {}, batch_id: {}, tran_ref: {}, start_date: {}, end_date: {}, request_type: {}",
                    moduleType,
                    requestBean.getString("batch_id"),
                    requestBean.getString("tran_ref"),
                    requestBean.getString("start_date"),
                    requestBean.getString("end_date"),
                    requestBean.getString("request_type"));

            boolean success = PendingTransactionDbHelper.getPendingTransactions(requestBean);

            if (success) {
                // Create success response
                return createSuccessResponse(requestBean);
            } else {
                // Create audit log for failed operation
                String message = (requestBean.getString("message") != null && !requestBean.getString("message").isEmpty()) ?
                        requestBean.getString("message") : "Failed to fetch pending transactions";

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
     * Get maximum length for tran_ref based on module type
     */
    private int getMaxTranRefLength(String moduleType) {
        switch (moduleType) {
            case "INFLOW":
            case "OUTFLOW":
                return 100; // PAYMENTREFERENCE is VARCHAR2(100)
            case "AIRTIME":
                return 30;  // TOPUP_REF_ID is VARCHAR2(50)
            default:
                return 100; // Default
        }
    }

    /**
     * Validate if the date string is in the expected ISO format
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
            LOG.warn("Invalid date format: {}. Expected format: yyyy-MM-ddTHH:mm:ss", dateStr);
            return false;
        }
    }

    /**
     * Create success response with properly formatted data
     */
    private String createSuccessResponse(BaseBean requestBean) {
        try {
            String pendingTransactionsStr = requestBean.getString("pending_transactions");
            if (pendingTransactionsStr == null || pendingTransactionsStr.trim().isEmpty()) {
                // Handle empty result set
                JsonObject response = Json.createObjectBuilder()
                        .add("status", "00")
                        .add("message", "Transactions fetched")
                        .add("page", requestBean.getString("current_page") != null ? requestBean.getString("current_page") : "1")
                        .add("size", requestBean.getString("page_size") != null ? requestBean.getString("page_size") : "10")
                        .add("total_rows", requestBean.getString("total_rows") != null ? requestBean.getString("total_rows") : "0")
                        .add("total_pages", requestBean.getString("total_pages") != null ? requestBean.getString("total_pages") : "0")
                        .add("data", Json.createArrayBuilder().build())
                        .build();

                return JsonUtil.toStr(response);
            }

            JsonArray dataArray = JsonUtil.toJsonArray(pendingTransactionsStr);

            // Fetch batch status once since all transactions have the same batch_id
            String batchStatus = "N/A";
            if (!dataArray.isEmpty()) {
                JsonObject firstTransaction = dataArray.getJsonObject(0);
                String batchId = JsonUtil.getJsonObjValue2(firstTransaction, "batch_id");
                if (!batchId.trim().isEmpty()) {
                    batchStatus = PendingTransactionDbHelper.getBatchStatus(batchId);
                    LOG.info("Retrieved batch status '{}' for batch_id: {}", batchStatus, batchId);
                }
            }

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
                        .add("status", batchStatus)
                        .build();
                responseDataBuilder.add(responseItem);
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", "Transactions fetched")
                    .add("page", requestBean.getString("current_page") != null ? requestBean.getString("current_page") : "1")
                    .add("size", requestBean.getString("page_size") != null ? requestBean.getString("page_size") : "10")
                    .add("total_rows", requestBean.getString("total_rows") != null ? requestBean.getString("total_rows") : "0")
                    .add("total_pages", requestBean.getString("total_pages") != null ? requestBean.getString("total_pages") : "0")
                    .add("data", responseDataBuilder.build())
                    .build();

            LOG.info("Successfully created response with {} transactions", dataArray.size());
            return JsonUtil.toStr(response);

        } catch (Exception e) {
            LOG.error("Error creating success response", e);
            return createErrorResponse("500", "Error formatting response: " + e.getMessage());
        }
    }

    /**
     * Create standardized error response
     */
    private String createErrorResponse(String statusCode, String message) {
        JsonObject response = Json.createObjectBuilder()
                .add("status", statusCode)
                .add("message", message)
                .build();

        LOG.warn("Returning error response - Status: {}, Message: {}", statusCode, message);
        return JsonUtil.toStr(response);
    }
}