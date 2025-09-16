package messaging.transactions;

import constants.AppModules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.MessageDbHelper;
import persistence.TimetableDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonObject;

public class ApproveTimetableService implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(ApproveTimetableService.class);

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        JsonObject jsonRequest = JsonUtil.toJsonObject(request);

        try {
            // Extract parameters from request
            if (jsonRequest != null) {
                requestBean.setString("request_id", JsonUtil.getJsonObjValue2(jsonRequest, "id"));
                requestBean.setString("approval_status", JsonUtil.getJsonObjValue2(jsonRequest, "status"));
                requestBean.setString("approval_message", JsonUtil.getJsonObjValue2(jsonRequest, "message"));
            }

            // Validate required parameters
            if (requestBean.getString("request_id") == null || requestBean.getString("request_id").trim().isEmpty()) {
                return createErrorResponse("400", "id parameter is required");
            }

            if (requestBean.getString("approval_status") == null || requestBean.getString("approval_status").trim().isEmpty()) {
                return createErrorResponse("400", "status parameter is required");
            }

            if (requestBean.getString("approval_message") == null || requestBean.getString("approval_message").trim().isEmpty()) {
                return createErrorResponse("400", "message parameter is required");
            }

            // Validate request_id format
            String requestId = requestBean.getString("request_id").trim();
            try {
                Long.parseLong(requestId);
            } catch (NumberFormatException e) {
                return createErrorResponse("400", "Invalid id format. Must be a valid number.");
            }

            // Validate status value
            String status = requestBean.getString("approval_status").trim().toLowerCase();
            if (!status.equals("true") && !status.equals("false")) {
                return createErrorResponse("400", "status must be either 'true' or 'false'");
            }

            // Validate message length
            String approvalMessage = requestBean.getString("approval_message").trim();
            if (approvalMessage.length() > 255) {
                return createErrorResponse("400", "message exceeds maximum length of 255 characters");
            }

            // Set audit fields
            requestBean.setString("approved_by", currentUser);
            requestBean.setString("action_id", actionId);

            LOG.info("Processing timetable approval request for request_id: {}, status: {}, user: {}",
                    requestId, status, currentUser);

            // Process the approval/rejection
            if (status.equals("true")) {
                return processApproval(requestBean, currentUser, actionId);
            } else {
                return processRejection(requestBean, currentUser, actionId);
            }

        } catch (Exception e) {
            LOG.error("Error processing timetable approval request for request_id: {}, user: {}",
                    requestBean.getString("request_id"), currentUser, e);

            // Create audit log for exception
            MessageDbHelper.createMessageHelper(
                    "Timetable Approval Error",
                    "Error occurred during timetable approval request: " + e.getMessage(),
                    currentUser,
                    AppModules.TRANSACTION_MANAGEMENT,
                    currentUser
            );

            return createErrorResponse("500", "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Process timetable approval
     */
    private String processApproval(BaseBean requestBean, String currentUser, String actionId) {
        try {
            // Update SWITCH_REQUESTS table and move data to main table
            boolean success = TimetableDbHelper.approveTimetableRequest(requestBean);

            if (success) {
                LOG.info("Timetable request {} approved successfully by user: {}",
                        requestBean.getString("request_id"), currentUser);

                // Create audit log for successful approval
                MessageDbHelper.createMessageHelper(
                        "Timetable Request Approved",
                        "Timetable request " + requestBean.getString("request_id") + " approved successfully",
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createSuccessResponse("00", "Timetable approved successfully");
            } else {
                String message = requestBean.getString("message") != null ?
                        requestBean.getString("message") : "Failed to approve timetable";

                // Create audit log for failed approval
                MessageDbHelper.createMessageHelper(
                        "Timetable Approval Failed",
                        "Failed to approve timetable request " + requestBean.getString("request_id") + " - " + message,
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createErrorResponse("500", "Timetable approved failed");
            }

        } catch (Exception e) {
            LOG.error("Error during timetable approval process for request_id: {}",
                    requestBean.getString("request_id"), e);
            return createErrorResponse("500", "Timetable approved failed: " + e.getMessage());
        }
    }

    /**
     * Process timetable rejection
     */
    private String processRejection(BaseBean requestBean, String currentUser, String actionId) {
        try {
            // Update SWITCH_REQUESTS table only (no data movement)
            boolean success = TimetableDbHelper.rejectTimetableRequest(requestBean);

            if (success) {
                LOG.info("Timetable request {} rejected successfully by user: {}",
                        requestBean.getString("request_id"), currentUser);

                // Create audit log for successful rejection
                MessageDbHelper.createMessageHelper(
                        "Timetable Request Rejected",
                        "Timetable request " + requestBean.getString("request_id") + " rejected successfully",
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createSuccessResponse("00", "Timetable rejected successfully");
            } else {
                String message = requestBean.getString("message") != null ?
                        requestBean.getString("message") : "Failed to reject timetable";

                // Create audit log for failed rejection
                MessageDbHelper.createMessageHelper(
                        "Timetable Rejection Failed",
                        "Failed to reject timetable request " + requestBean.getString("request_id") + " - " + message,
                        currentUser,
                        AppModules.TRANSACTION_MANAGEMENT,
                        currentUser
                );

                return createErrorResponse("500", "Timetable rejected failed");
            }

        } catch (Exception e) {
            LOG.error("Error during timetable rejection process for request_id: {}",
                    requestBean.getString("request_id"), e);
            return createErrorResponse("500", "Timetable rejected failed: " + e.getMessage());
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