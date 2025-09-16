package sbdad;

import constants.AppModules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.MessageDbHelper;
import persistence.TimetableDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.HashSet;
import java.util.Set;

public class CreateTimetableService implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(CreateTimetableService.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        JsonObject jsonRequest = JsonUtil.toJsonObject(request);

        try {
            // Store the request JSON for later use
            requestBean.setString("request_json", request);

            // Check if this is a fetch request (GET)
            if (jsonRequest != null && "fetch".equals(JsonUtil.getJsonObjValue2(jsonRequest, "action"))) {
                return handleFetchRequest(requestBean, currentUser, actionId);
            }

            // Handle POST request (create timetable)
            return handleCreateRequest(jsonRequest, requestBean, currentUser, actionId);

        } catch (Exception e) {
            LOG.error("Error processing timetable request for user: {}", currentUser, e);

            // Create audit log for exception
            MessageDbHelper.createMessageHelper(
                    "Timetable Request Error",
                    "Error occurred during timetable request: " + e.getMessage(),
                    currentUser,
                    AppModules.TRANSACTION_MANAGEMENT,
                    currentUser
            );

            return createErrorResponse("500", "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Handle GET request to fetch timetable data
     */
    private String handleFetchRequest(BaseBean requestBean, String currentUser, String actionId) {
        try {
            requestBean.setString("current_user", currentUser);
            requestBean.setString("action_id", actionId);

            // Extract query parameters from the JSON request
            String request = requestBean.getString("request_json");
            JsonObject jsonRequest = JsonUtil.toJsonObject(request);

            // Extract pagination parameters
            String page = JsonUtil.getJsonObjValue2(jsonRequest, "page");
            String size = JsonUtil.getJsonObjValue2(jsonRequest, "size");
            String id = JsonUtil.getJsonObjValue2(jsonRequest, "id");
            String channelCode = JsonUtil.getJsonObjValue2(jsonRequest, "channelCode");

            // Validate and set pagination parameters
            int pageNum = 1;
            int pageSize = 10;

            if (!page.trim().isEmpty()) {
                try {
                    pageNum = Integer.parseInt(page.trim());
                    if (pageNum < 1) {
                        return createErrorResponse("400", "page parameter must be greater than 0");
                    }
                } catch (NumberFormatException e) {
                    return createErrorResponse("400", "Invalid page parameter. Must be a valid integer.");
                }
            }

            if (!size.trim().isEmpty()) {
                try {
                    pageSize = Integer.parseInt(size.trim());
                    if (pageSize < 1) {
                        return createErrorResponse("400", "size parameter must be greater than 0");
                    }
                    if (pageSize > 100) {
                        return createErrorResponse("400", "size parameter cannot exceed 100");
                    }
                } catch (NumberFormatException e) {
                    return createErrorResponse("400", "Invalid size parameter. Must be a valid integer.");
                }
            }

            // Validate id parameter if provided
            Long idValue = null;
            if (!id.trim().isEmpty()) {
                try {
                    idValue = Long.parseLong(id.trim());
                    if (idValue < 1) {
                        return createErrorResponse("400", "id parameter must be greater than 0");
                    }
                } catch (NumberFormatException e) {
                    return createErrorResponse("400", "Invalid id parameter. Must be a valid number.");
                }
            }

            // Validate channelCode parameter if provided
            if (!channelCode.trim().isEmpty()) {
                channelCode = channelCode.trim().toUpperCase();
                if (channelCode.length() > 25) {
                    return createErrorResponse("400", "channelCode cannot exceed 25 characters");
                }
                if (!channelCode.matches("^[A-Z0-9]+$")) {
                    return createErrorResponse("400", "channelCode must contain only alphanumeric characters");
                }
            }

            // Set parameters in requestBean
            requestBean.setString("page", String.valueOf(pageNum));
            requestBean.setString("size", String.valueOf(pageSize));

            if (idValue != null) {
                requestBean.setString("id", String.valueOf(idValue));
            }
            if (!channelCode.trim().isEmpty()) {
                requestBean.setString("channel_code", channelCode);
            }

            LOG.info("Fetching timetable data for user: {}, page: {}, size: {}, id: {}, channelCode: {}",
                    currentUser, pageNum, pageSize, idValue, channelCode);

            boolean success = TimetableDbHelper.fetchTimetableData(requestBean);

            if (success) {
                LOG.info("Timetable data fetched successfully for user: {}", currentUser);
                return createFetchSuccessResponse(requestBean);
            } else {
                String message = requestBean.getString("message") != null ?
                        requestBean.getString("message") : "Failed to fetch timetable data";

                // Create audit log for failed fetch
                MessageDbHelper.createMessageHelper(
                        "Timetable Fetch Failed",
                        "Failed to fetch timetable data: " + message,
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createErrorResponse("500", "Algorithm fetch failed: " + message);
            }

        } catch (Exception e) {
            LOG.error("Error fetching timetable data for user: {}", currentUser, e);
            return createErrorResponse("500", "Algorithm fetch failed: " + e.getMessage());
        }
    }

    /**
     * Handle POST request to create timetable
     */
    private String handleCreateRequest(JsonObject jsonRequest, BaseBean requestBean, String currentUser, String actionId) {
        try {
            // Extract parameters from request
            if (jsonRequest != null) {
                requestBean.setString("channel_code", JsonUtil.getJsonObjValue2(jsonRequest, "channelCode"));
                requestBean.setString("switch_details", JsonUtil.toStr(jsonRequest.getJsonArray("switchDetails")));
            }

            // Validate required parameters
            if (requestBean.getString("channel_code") == null || requestBean.getString("channel_code").trim().isEmpty()) {
                return createErrorResponse("400", "channelCode parameter is required");
            }

            if (requestBean.getString("switch_details") == null || requestBean.getString("switch_details").trim().isEmpty()) {
                return createErrorResponse("400", "switchDetails parameter is required");
            }

            // Validate channel code format (alphanumeric, up to 25 characters)
            String channelCode = requestBean.getString("channel_code").trim().toUpperCase();
            if (channelCode.length() > 25) {
                return createErrorResponse("400", "channelCode cannot exceed 25 characters");
            }
            if (!channelCode.matches("^[A-Z0-9]+$")) {
                return createErrorResponse("400", "channelCode must contain only alphanumeric characters");
            }
            requestBean.setString("channel_code", channelCode);

            // Parse and validate switch details
            JsonArray switchDetailsArray = JsonUtil.toJsonArray(requestBean.getString("switch_details"));
            if (switchDetailsArray == null || switchDetailsArray.isEmpty()) {
                return createErrorResponse("400", "switchDetails array cannot be empty");
            }

            if (switchDetailsArray.size() > 24) {
                return createErrorResponse("400", "switchDetails cannot exceed 24 entries (maximum hours in a day)");
            }

            // Validate switch details and check for duplicate hours
            Set<Integer> usedHours = new HashSet<>();
            for (int i = 0; i < switchDetailsArray.size(); i++) {
                JsonValue jsonValue = switchDetailsArray.get(i);
                if (jsonValue.getValueType() != JsonValue.ValueType.OBJECT) {
                    return createErrorResponse("400", "Each switchDetails entry must be an object");
                }

                JsonObject switchDetail = (JsonObject) jsonValue;

                // Validate switchCode
                String switchCode = JsonUtil.getJsonObjValue2(switchDetail, "switchCode");
                if (switchCode == null || switchCode.trim().isEmpty()) {
                    return createErrorResponse("400", "switchCode is required for entry " + (i + 1));
                }
                switchCode = switchCode.trim().toUpperCase();
                if (switchCode.length() > 25) {
                    return createErrorResponse("400", "switchCode cannot exceed 25 characters for entry " + (i + 1));
                }
                if (!switchCode.matches("^[A-Z0-9]+$")) {
                    return createErrorResponse("400", "switchCode must contain only alphanumeric characters for entry " + (i + 1));
                }

                // Validate time (hour)
                String timeStr = JsonUtil.getJsonObjValue2(switchDetail, "time");
                if (timeStr == null || timeStr.trim().isEmpty()) {
                    return createErrorResponse("400", "time is required for entry " + (i + 1));
                }

                int hour;
                try {
                    hour = Integer.parseInt(timeStr.trim());
                } catch (NumberFormatException e) {
                    return createErrorResponse("400", "time must be a valid integer for entry " + (i + 1));
                }

                if (hour < 0 || hour > 23) {
                    return createErrorResponse("400", "time must be between 0 and 23 for entry " + (i + 1));
                }

                // Check for duplicate hours in this request
                if (usedHours.contains(hour)) {
                    return createErrorResponse("400", "Duplicate time hour " + hour + " found in request. Each hour can only be assigned once per channel.");
                }
                usedHours.add(hour);
            }

            // Set audit fields
            requestBean.setString("created_by", currentUser);
            requestBean.setString("action_id", actionId);

            LOG.info("Processing timetable creation request for channel: {}, entries: {}, user: {}",
                    channelCode, switchDetailsArray.size(), currentUser);

            // Check if channel already has existing timetable entries (optional business rule)
            boolean hasExistingEntries = TimetableDbHelper.checkExistingTimetable(requestBean);
            if (hasExistingEntries) {
                LOG.warn("Channel {} already has existing timetable entries", channelCode);
                // You can choose to either allow or reject this based on business rules
                // For now, we'll proceed but log a warning
            }

            // Save to maker-checker tables
            boolean success = TimetableDbHelper.createTimetableRequest(requestBean);

            if (success) {
                LOG.info("Timetable creation request submitted successfully for channel: {} by user: {}",
                        channelCode, currentUser);

                // Create audit log for successful submission
                MessageDbHelper.createMessageHelper(
                        "Timetable Creation Request Submitted",
                        "Timetable creation request submitted for channel: " + channelCode + " with " + switchDetailsArray.size() + " entries",
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createSuccessResponse("00", "Timetable creation request submitted successfully. Awaiting approval.");
            } else {
                String message = requestBean.getString("message") != null ?
                        requestBean.getString("message") : "Failed to submit timetable creation request";

                // Create audit log for failed submission
                MessageDbHelper.createMessageHelper(
                        "Timetable Creation Request Failed",
                        "Failed to submit timetable creation request for channel: " + channelCode + " - " + message,
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createErrorResponse("500", message);
            }

        } catch (Exception e) {
            LOG.error("Error processing timetable creation request for channel: {}, user: {}",
                    requestBean.getString("channel_code"), currentUser, e);

            // Create audit log for exception
            MessageDbHelper.createMessageHelper(
                    "Timetable Creation Request Error",
                    "Error occurred during timetable creation request: " + e.getMessage(),
                    currentUser,
                    AppModules.TRANSACTION_MANAGEMENT,
                    currentUser
            );

            return createErrorResponse("500", "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Create success response for fetch request with pagination
     */
    private String createFetchSuccessResponse(BaseBean requestBean) {
        try {
            String timetableDataStr = requestBean.getString("timetable_data");
            JsonArray dataArray = null;

            if (timetableDataStr != null && !timetableDataStr.trim().isEmpty()) {
                dataArray = JsonUtil.toJsonArray(timetableDataStr);
            }

            if (dataArray == null) {
                dataArray = Json.createArrayBuilder().build();
            }

            // Get pagination information
            String currentPage = requestBean.getString("current_page") != null ? requestBean.getString("current_page") : "1";
            String pageSize = requestBean.getString("page_size") != null ? requestBean.getString("page_size") : "10";
            String totalRows = requestBean.getString("total_rows") != null ? requestBean.getString("total_rows") : "0";
            String totalPages = requestBean.getString("total_pages") != null ? requestBean.getString("total_pages") : "0";

            JsonObject response = Json.createObjectBuilder()
                    .add("status", "00")
                    .add("message", "Algorithm fetch successful")
                    .add("page", currentPage)
                    .add("size", pageSize)
                    .add("total_rows", totalRows)
                    .add("total_pages", totalPages)
                    .add("data", dataArray)
                    .build();

            LOG.info("Successfully created fetch response with {} records, page: {}, total_rows: {}",
                    dataArray.size(), currentPage, totalRows);
            return JsonUtil.toStr(response);

        } catch (Exception e) {
            LOG.error("Error creating fetch success response", e);
            return createErrorResponse("500", "Algorithm fetch failed: Error formatting response - " + e.getMessage());
        }
    }

    /**
     * Create standardized success response
     */
    private String createSuccessResponse(String statusCode, String message) {
        JsonObject response = Json.createObjectBuilder()
                .add("status", statusCode)
                .add("message", message)
                .build();

        LOG.info("Returning success response - Status: {}, Message: {}", statusCode, message);
        return JsonUtil.toStr(response);
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