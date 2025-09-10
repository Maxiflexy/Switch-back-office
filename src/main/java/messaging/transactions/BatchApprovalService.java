package messaging.transactions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.BatchApprovalDbHelper;
import services.executors.RequestExecutor;
import util.BaseBean;
import util.JsonUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class BatchApprovalService implements RequestExecutor {

    final static Logger LOG = LogManager.getLogger(BatchApprovalService.class);

    // Define valid module types
    private static final List<String> VALID_MODULE_TYPES = Arrays.asList("INFLOW", "OUTFLOW", "AIRTIME");

    @Override
    public String execute(String request, String currentUser, String actionId) {
        BaseBean requestBean = new BaseBean();
        JsonObject jsonRequest = JsonUtil.toJsonObject(request);

        try {
            // Extract parameters from request
            if (jsonRequest != null) {
                requestBean.setString("batch_id", JsonUtil.getJsonObjValue2(jsonRequest, "batch_id"));
                requestBean.setString("status", JsonUtil.getJsonObjValue2(jsonRequest, "status"));
                requestBean.setString("approval_message", JsonUtil.getJsonObjValue2(jsonRequest, "message"));
                requestBean.setString("module_type", JsonUtil.getJsonObjValue2(jsonRequest, "module_type"));
            }

            // Validate required parameters
            if (requestBean.getString("batch_id") == null || requestBean.getString("batch_id").trim().isEmpty()) {
                return createErrorResponse("400", "batch_id parameter is required");
            }

            if (requestBean.getString("status") == null || requestBean.getString("status").trim().isEmpty()) {
                return createErrorResponse("400", "status parameter is required");
            }

            if (requestBean.getString("approval_message") == null || requestBean.getString("approval_message").trim().isEmpty()) {
                return createErrorResponse("400", "message parameter is required");
            }

            if (requestBean.getString("module_type") == null || requestBean.getString("module_type").trim().isEmpty()) {
                return createErrorResponse("400", "module_type parameter is required");
            }

            // Validate status value
            String status = requestBean.getString("status").trim().toLowerCase();
            if (!status.equals("true") && !status.equals("false")) {
                return createErrorResponse("400", "status must be either 'true' or 'false'");
            }

            // Validate module_type value
            String moduleType = requestBean.getString("module_type").trim().toUpperCase();
            if (!VALID_MODULE_TYPES.contains(moduleType)) {
                return createErrorResponse("400", "Invalid module_type. Must be one of: " + String.join(", ", VALID_MODULE_TYPES));
            }
            requestBean.setString("module_type", moduleType);

            // Validate batch_id format (assuming it should be alphanumeric)
            String batchId = requestBean.getString("batch_id").trim();
            if (batchId.length() > 24) {
                return createErrorResponse("400", "batch_id exceeds maximum length of 24 characters");
            }

            // Validate message length
            String approvalMessage = requestBean.getString("approval_message").trim();
            if (approvalMessage.length() > 1000) {
                return createErrorResponse("400", "message exceeds maximum length of 1000 characters");
            }

            // Set audit fields
            requestBean.setString("approved_by", currentUser);
            requestBean.setString("action_id", actionId);

            LOG.info("Processing batch approval request for batch: {}, status: {}, module_type: {}, user: {}",
                    batchId, status, moduleType, currentUser);

            // First, validate that the batch exists and is in a valid state
            boolean batchExists = BatchApprovalDbHelper.validateBatchExists(requestBean);
            if (!batchExists) {
                String message = requestBean.getString("message") != null ?
                        requestBean.getString("message") : "Batch validation failed";
                return createErrorResponse("404", message);
            }

            if (status.equals("true")) {
                return processBatchApproval(requestBean, currentUser, actionId);
            } else {
                return processBatchRejection(requestBean, currentUser, actionId);
            }

        } catch (Exception e) {
            LOG.error("Error processing batch approval request for batch: {}, module_type: {}",
                    requestBean.getString("batch_id"), requestBean.getString("module_type"), e);

            // Create audit log for exception
//            MessageDbHelper.createMessageHelper(
//                    "Batch Approval Error",
//                    "Error occurred during batch approval: " + e.getMessage(),
//                    currentUser,
//                    AppModules.TRANSACTION_MANAGEMENT,
//                    currentUser
//            );

            return createErrorResponse("500", "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Process batch approval - includes external API call
     */
    private String processBatchApproval(BaseBean requestBean, String currentUser, String actionId) {
        try {
            // Update database to APPROVED status and get service_type
            boolean approvalSuccess = BatchApprovalDbHelper.approveBatch(requestBean);

            if (!approvalSuccess) {
                String message = requestBean.getString("message") != null ?
                        requestBean.getString("message") : "Failed to approve batch";

                return createErrorResponse("500", message);
            }

            // Make external API call with module_type
            String serviceType = requestBean.getString("service_type");
            String moduleType = requestBean.getString("module_type");
            boolean externalApiSuccess = callExternalService(requestBean.getString("batch_id"), serviceType, moduleType);

            if (!externalApiSuccess) {
                LOG.error("External API call failed for batch: {}, module_type: {}",
                        requestBean.getString("batch_id"), moduleType);

                return createErrorResponse("500", "error posting to esb service");
            }

            LOG.info("Batch {} approved successfully by user: {} for module_type: {}",
                    requestBean.getString("batch_id"), currentUser, moduleType);
            return createSuccessResponse("00", "Batch approved successfully");

        } catch (Exception e) {
            LOG.error("Error during batch approval process for batch: {}, module_type: {}",
                    requestBean.getString("batch_id"), requestBean.getString("module_type"), e);
            return createErrorResponse("500", "Error processing batch approval: " + e.getMessage());
        }
    }

    /**
     * Process batch rejection
     */
    private String processBatchRejection(BaseBean requestBean, String currentUser, String actionId) {
        try {
            // Update database to REJECTED status
            boolean rejectionSuccess = BatchApprovalDbHelper.rejectBatch(requestBean);

            if (!rejectionSuccess) {
                String message = requestBean.getString("message") != null ?
                        requestBean.getString("message") : "Failed to reject batch";

                return createErrorResponse("500", message);
            }

            LOG.info("Batch {} rejected successfully by user: {} for module_type: {}",
                    requestBean.getString("batch_id"), currentUser, requestBean.getString("module_type"));
            return createSuccessResponse("00", "Batch cancelled successfully");

        } catch (Exception e) {
            LOG.error("Error during batch rejection process for batch: {}, module_type: {}",
                    requestBean.getString("batch_id"), requestBean.getString("module_type"), e);
            return createErrorResponse("500", "Error processing batch rejection: " + e.getMessage());
        }
    }

    /**
     * Make external API call to ESB service with module_type included as module_name
     */
    private boolean callExternalService(String batchId, String serviceType, String moduleType) {
        HttpURLConnection connection = null;

        try {
            // Get ESB service URL from system properties
            String esbServiceUrl = System.getProperty("esb-service");
            if (esbServiceUrl == null || esbServiceUrl.trim().isEmpty()) {
                LOG.error("ESB service URL not configured in system properties");
                return false;
            }

            LOG.info("Making external API call to: {} for batch: {}, module_type: {}",
                    esbServiceUrl, batchId, moduleType);

            // Create connection
            URL url = new URL(esbServiceUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(30000); // 30 seconds

            // Create request payload with module_name included
            JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                    .add("operation_type", serviceType != null ? serviceType.toLowerCase() : "pending")
                    .add("batch_id", batchId);

            // Add module_name based on module_type
            if (moduleType != null && !moduleType.trim().isEmpty()) {
                payloadBuilder.add("module_name", moduleType.toUpperCase());
            }

            JsonObject requestPayload = payloadBuilder.build();

            String jsonInputString = JsonUtil.toStr(requestPayload);
            LOG.info("External API request payload: {}", jsonInputString);

            // Send request
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                wr.write(input, 0, input.length);
                wr.flush();
            }

            // Get response
            int responseCode = connection.getResponseCode();
            LOG.info("External API response code: {} for batch: {}, module_type: {}",
                    responseCode, batchId, moduleType);

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300 ?
                                    connection.getInputStream() : connection.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            String responseBody = response.toString();
            LOG.info("External API response body: {} for batch: {}, module_type: {}",
                    responseBody, batchId, moduleType);

            // Parse response
            if (responseCode >= 200 && responseCode < 300) {
                JsonObject responseJson = JsonUtil.toJsonObject(responseBody);
                if (responseJson != null) {
                    String status = JsonUtil.getJsonObjValue2(responseJson, "status");
                    String message = JsonUtil.getJsonObjValue2(responseJson, "message");

                    if ("00".equals(status) && "Success".equalsIgnoreCase(message)) {
                        LOG.info("External API call successful for batch: {}, module_type: {}", batchId, moduleType);
                        return true;
                    } else {
                        LOG.error("External API returned error status: {} message: {} for batch: {}, module_type: {}",
                                status, message, batchId, moduleType);
                        return false;
                    }
                } else {
                    LOG.error("Failed to parse external API response for batch: {}, module_type: {}", batchId, moduleType);
                    return false;
                }
            } else {
                LOG.error("External API call failed with HTTP code: {} for batch: {}, module_type: {}",
                        responseCode, batchId, moduleType);
                return false;
            }

        } catch (Exception e) {
            LOG.error("Exception during external API call for batch: {}, module_type: {}", batchId, moduleType, e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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