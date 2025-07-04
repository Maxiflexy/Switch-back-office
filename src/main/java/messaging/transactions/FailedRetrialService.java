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

    // Define date formatter for validation only
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        JsonObject jsonRequest = JsonUtil.toJsonObject(request);

        try {
            // Extract parameters from request
            if (jsonRequest != null) {
                requestBean.setString("service_type", JsonUtil.getJsonObjValue2(jsonRequest, "service_type"));
                requestBean.setString("request_status", JsonUtil.getJsonObjValue2(jsonRequest, "request_status"));

                // Pass dates directly to database layer (ISO format)
                requestBean.setString("retrial_start_date", JsonUtil.getJsonObjValue2(jsonRequest, "retrial_start_date"));
                requestBean.setString("retrial_end_date", JsonUtil.getJsonObjValue2(jsonRequest, "retrial_end_date"));

                // Add batch_id parameter (optional)
                requestBean.setString("batch_id", JsonUtil.getJsonObjValue2(jsonRequest, "batch_id"));

                requestBean.setString("page", JsonUtil.getJsonObjValue2(jsonRequest, "page"));
                requestBean.setString("size", JsonUtil.getJsonObjValue2(jsonRequest, "size"));
            }

            // Validate required parameters
            if (requestBean.getString("service_type") == null || requestBean.getString("service_type").trim().isEmpty()) {
                return createErrorResponse("400", "service_type parameter is required");
            }

            if (requestBean.getString("request_status") == null || requestBean.getString("request_status").trim().isEmpty()) {
                return createErrorResponse("400", "request_status parameter is required");
            }

            // Validate request_status values (case-insensitive)
            String requestStatus = requestBean.getString("request_status").trim().toLowerCase();
            if (!requestStatus.equals("approved") && !requestStatus.equals("pending") && !requestStatus.equals("rejected")) {
                return createErrorResponse("400", "request_status must be one of: approved, pending, rejected");
            }

            // Validate service_type values (optional validation - you can expand this)
            String serviceType = requestBean.getString("service_type").trim().toUpperCase();
            if (!serviceType.equals("POSTING") && !serviceType.equals("TSQ")) {
                LOG.warn("Unexpected service_type value: {}. Proceeding with query anyway.", serviceType);
            }

            boolean hasBatchId = requestBean.getString("batch_id") != null && !requestBean.getString("batch_id").trim().isEmpty();

            if (hasBatchId) {
                // When batch_id is provided, only validate batch_id (ignore date and pagination params)
                String batchId = requestBean.getString("batch_id").trim();
                LOG.info("Batch ID parameter provided: {} - Using single record mode", batchId);

            } else {
                // When batch_id is not provided, validate date format and pagination (existing logic)
                if (requestBean.getString("retrial_start_date") != null && !requestBean.getString("retrial_start_date").trim().isEmpty()) {
                    String startDate = requestBean.getString("retrial_start_date").trim();
                    if (!isValidDateFormat(startDate)) {
                        return createErrorResponse("400", "Invalid retrial_start_date format. Expected: yyyy-MM-ddTHH:mm:ss (e.g., 2025-06-16T07:38:25)");
                    }
                    LOG.info("Start date parameter validated: {}", startDate);
                }

                if (requestBean.getString("retrial_end_date") != null && !requestBean.getString("retrial_end_date").trim().isEmpty()) {
                    String endDate = requestBean.getString("retrial_end_date").trim();
                    if (!isValidDateFormat(endDate)) {
                        return createErrorResponse("400", "Invalid retrial_end_date format. Expected: yyyy-MM-ddTHH:mm:ss (e.g., 2025-06-17T07:38:25)");
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
            }


            // Set the user for audit trail
            requestBean.setString("current_user", currentUser);
            requestBean.setString("action_id", actionId);

            LOG.info("Fetching failed retrial requests for service_type: {}, status: {}, start_date: {}, end_date: {}, batch_id: {}",
                    requestBean.getString("service_type"),
                    requestBean.getString("request_status"),
                    requestBean.getString("retrial_start_date"),
                    requestBean.getString("retrial_end_date"),
                    requestBean.getString("batch_id"));

            // Call the database helper (database will handle TIMESTAMP conversion)
            boolean success = PostingRetrialDbHelper.getFailedRetrialRequests(requestBean);

            if (success) {
                // Create success response
                return createSuccessResponse(requestBean);
            } else {
                // Create audit log for failed operation
                String message = (requestBean.getString("message") != null && !requestBean.getString("message").isEmpty()) ?
                        requestBean.getString("message") : "Failed to fetch retrial requests";

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
            String retrialRequestsStr = requestBean.getString("retrial_requests");
            if (retrialRequestsStr == null || retrialRequestsStr.trim().isEmpty()) {
                // Handle empty result set
                JsonObject response = Json.createObjectBuilder()
                        .add("status", "00")
                        .add("message", "Success")
                        .add("page", requestBean.getString("current_page") != null ? requestBean.getString("current_page") : "1")
                        .add("size", requestBean.getString("page_size") != null ? requestBean.getString("page_size") : "10")
                        .add("total_rows", requestBean.getString("total_rows") != null ? requestBean.getString("total_rows") : "0")
                        .add("total_pages", requestBean.getString("total_pages") != null ? requestBean.getString("total_pages") : "0")
                        .add("data", Json.createArrayBuilder().build())
                        .build();

                return JsonUtil.toStr(response);
            }

            JsonArray dataArray = JsonUtil.toJsonArray(retrialRequestsStr);
            JsonArrayBuilder responseDataBuilder = Json.createArrayBuilder();

            boolean hasBatchId = requestBean.getString("batch_id") != null && !requestBean.getString("batch_id").trim().isEmpty();

            // Transform data to match required response format
            for (int i = 0; i < dataArray.size(); i++) {
                JsonObject item = dataArray.getJsonObject(i);

                if (hasBatchId) {
                    // Single record mode - return all 19 columns
                    JsonObject responseItem = Json.createObjectBuilder()
                            .add("sno", JsonUtil.getJsonObjValue2(item, "sno"))
                            .add("service_type", JsonUtil.getJsonObjValue2(item, "service_type"))
                            .add("retrial_start_date", JsonUtil.getJsonObjValue2(item, "retrial_start_date"))
                            .add("retrial_end_date", JsonUtil.getJsonObjValue2(item, "retrial_end_date"))
                            .add("created_by", JsonUtil.getJsonObjValue2(item, "created_by"))
                            .add("creation_date", JsonUtil.getJsonObjValue2(item, "creation_date"))
                            .add("batch_id", JsonUtil.getJsonObjValue2(item, "batch_id"))
                            .add("batch_count", JsonUtil.getJsonObjValue2(item, "batch_count"))
                            .add("status", JsonUtil.getJsonObjValue2(item, "status"))
                            .add("approved_by", JsonUtil.getJsonObjValue2(item, "approved_by"))
                            .add("approval_date", JsonUtil.getJsonObjValue2(item, "approval_date"))
                            .add("posting_date", JsonUtil.getJsonObjValue2(item, "posting_date"))
                            .add("posting_resp_flg", JsonUtil.getJsonObjValue2(item, "posting_resp_flg"))
                            .add("posting_resp_code", JsonUtil.getJsonObjValue2(item, "posting_resp_code"))
                            .add("posting_retrial_count", JsonUtil.getJsonObjValue2(item, "posting_retrial_count"))
                            .add("updated_by", JsonUtil.getJsonObjValue2(item, "updated_by"))
                            .add("updated_date", JsonUtil.getJsonObjValue2(item, "updated_date"))
                            .add("approval_message", JsonUtil.getJsonObjValue2(item, "approval_message"))
                            .add("switch_type", JsonUtil.getJsonObjValue2(item, "switch_type"))
                            .build();
                    responseDataBuilder.add(responseItem);
                } else {
                    // Paginated mode - return existing 15 columns
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
                            .add("sno", JsonUtil.getJsonObjValue2(item, "sno"))
                            .build();
                    responseDataBuilder.add(responseItem);
                }
            }

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", "Success")
                    .add("page", requestBean.getString("current_page") != null ? requestBean.getString("current_page") : "1")
                    .add("size", requestBean.getString("page_size") != null ? requestBean.getString("page_size") : "10")
                    .add("total_rows", requestBean.getString("total_rows") != null ? requestBean.getString("total_rows") : "0")
                    .add("total_pages", requestBean.getString("total_pages") != null ? requestBean.getString("total_pages") : "0")
                    .add("data", responseDataBuilder.build())
                    .build();

            LOG.info("Successfully created response with {} records", dataArray.size());
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