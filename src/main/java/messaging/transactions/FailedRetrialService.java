package messaging.transactions;

import constants.AppModules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.MessageDbHelper;
import persistence.PostingRetrialDbHelper;
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

public class FailedRetrialService implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(FailedRetrialService.class);

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
                requestBean.setString("service_type", JsonUtil.getJsonObjValue2(jsonRequest, "service_type"));
                requestBean.setString("request_status", JsonUtil.getJsonObjValue2(jsonRequest, "request_status"));

                // Format dates for Oracle compatibility
                String startDate = JsonUtil.getJsonObjValue2(jsonRequest, "retrial_start_date");
                String endDate = JsonUtil.getJsonObjValue2(jsonRequest, "retrial_end_date");

                requestBean.setString("retrial_start_date", formatDateForOracle(startDate));
                requestBean.setString("retrial_end_date", formatDateForOracle(endDate));
                //requestBean.setString("retrial_start_date", JsonUtil.getJsonObjValue2(jsonRequest, "retrial_start_date"));
                //requestBean.setString("retrial_end_date", JsonUtil.getJsonObjValue2(jsonRequest, "retrial_end_date"));

                requestBean.setString("page", JsonUtil.getJsonObjValue2(jsonRequest, "page"));
                requestBean.setString("size", JsonUtil.getJsonObjValue2(jsonRequest, "size"));
            }

            // Validate required parameters
            if (requestBean.getString("service_type").trim().isEmpty()) {
                return createErrorResponse("400", "service_type parameter is required");
            }

            if (requestBean.getString("request_status").trim().isEmpty()) {
                return createErrorResponse("400", "request_status parameter is required");
            }

            // Validate request_status values
            String requestStatus = requestBean.getString("request_status").trim().toLowerCase();
            if (!requestStatus.equals("approved") && !requestStatus.equals("pending") && !requestStatus.equals("rejected")) {
                return createErrorResponse("400", "request_status must be one of: approved, pending, rejected");
            }

            // Validate date format
            if (!requestBean.getString("retrial_start_date").trim().isEmpty()) {
                assert jsonRequest != null;
                String originalStartDate = JsonUtil.getJsonObjValue2(jsonRequest, "retrial_start_date");
                if (!isValidDateFormat(originalStartDate)) {
                    return createErrorResponse("400", "Invalid retrial_start_date format. Expected: yyyy-MM-ddTHH:mm:ss");
                }
                LOG.info("Formatted start date: {}", requestBean.getString("retrial_start_date"));
            }

            if (!requestBean.getString("retrial_end_date").trim().isEmpty()) {
                assert jsonRequest != null;
                String originalEndDate = JsonUtil.getJsonObjValue2(jsonRequest, "retrial_end_date");
                if (!isValidDateFormat(originalEndDate)) {
                    return createErrorResponse("400", "Invalid retrial_end_date format. Expected: yyyy-MM-ddTHH:mm:ss");
                }
                LOG.info("Formatted end date: {}", requestBean.getString("retrial_end_date"));
            }

            // Validate and log date parameters for debugging
//            if (!requestBean.getString("retrial_start_date").trim().isEmpty()) {
//                LOG.info("Start date parameter received: {}", requestBean.getString("retrial_start_date"));
//            }
//            if (!requestBean.getString("retrial_end_date").trim().isEmpty()) {
//                LOG.info("End date parameter received: {}", requestBean.getString("retrial_end_date"));
//            }

            // Set the user for audit trail
            requestBean.setString("current_user", currentUser);
            requestBean.setString("action_id", actionId);

            LOG.info("Fetching failed retrial requests for service_type: {} and status: {}",
                    requestBean.getString("service_type"), requestBean.getString("request_status"));

            // Call the database helper
            boolean success = PostingRetrialDbHelper.getFailedRetrialRequests(requestBean);

            if (success) {
                // Create success response
                return createSuccessResponse(requestBean);
            } else {
                // Create audit log for failed operation
                String message = requestBean.getString("message").isEmpty() ?
                        "Failed to fetch retrial requests" : requestBean.getString("message");
                MessageDbHelper.createMessageHelper(
                        "Failed Retrial Request Fetch Failed",
                        "Failed to fetch failed retrial requests: " + message,
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createErrorResponse("500", message);
            }

        } catch (Exception e) {
            LOG.error("Error processing failed retrial request", e);

            // Create audit log for exception
            MessageDbHelper.createMessageHelper(
                    "Failed Retrial Request Fetch Error",
                    "Error occurred while fetching failed retrial requests: " + e.getMessage(),
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
            JsonArray dataArray = JsonUtil.toJsonArray(requestBean.getString("retrial_requests"));
            JsonArrayBuilder responseDataBuilder = Json.createArrayBuilder();

            // Transform data to match required response format
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject item = dataArray.getJsonObject(i);
                JsonObject responseItem = Json.createObjectBuilder()
                        .add("batch_id", JsonUtil.getJsonObjValue2(item, "batch_id"))
                        .add("batch_count", JsonUtil.getJsonObjValue2(item, "batch_count"))
                        .add("creation_date", JsonUtil.getJsonObjValue2(item, "creation_date"))
                        .add("service_type", JsonUtil.getJsonObjValue2(item, "service_type"))
                        .add("status", JsonUtil.getJsonObjValue2(item, "status"))
                        .add("created_by", JsonUtil.getJsonObjValue2(item, "created_by"))
                        .add("retrial_start_date", JsonUtil.getJsonObjValue2(item, "retrial_start_date"))
                        .add("retrial_end_date", JsonUtil.getJsonObjValue2(item, "retrial_end_date"))
                        .add("approved_by", JsonUtil.getJsonObjValue2(item, "approved_by"))
                        .add("approval_date", JsonUtil.getJsonObjValue2(item, "approval_date"))
                        .add("posting_date", JsonUtil.getJsonObjValue2(item, "posting_date"))
                        .add("posting_resp_flg", JsonUtil.getJsonObjValue2(item, "posting_resp_flg"))
                        .add("posting_resp_code", JsonUtil.getJsonObjValue2(item, "posting_resp_code"))
                        .add("posting_retrial_count", JsonUtil.getJsonObjValue2(item, "posting_retrial_count"))
                        .build();
                responseDataBuilder.add(responseItem);
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", "Success")
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