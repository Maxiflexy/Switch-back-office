package services.transactions;

import exceptions.CustomException;
import messaging.transactions.BatchApprovalService;
import services.servlets.CustomBaseServlet;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static util.JsonUtil.addObject;
import static util.JsonUtil.toJsonObject;
import static util.ResponseUtil.createDefaultResponse;

public class BatchApprovalServlet extends CustomBaseServlet {

    // Pattern to extract batch_id from URL path like /transactions/approve/123456
    private static final Pattern BATCH_ID_PATTERN = Pattern.compile(".*/approve/([^/]+)$");

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            if (user == null || user.trim().isEmpty()) {
                user = "system";
            }

            String actionId = UUID.randomUUID().toString();

            // Extract batch_id from URL path
            String batchId = extractBatchIdFromPath(servletRequest.getRequestURI());
            if (batchId == null || batchId.trim().isEmpty()) {
                LOG.error("batch_id not found in URL path: {}", servletRequest.getRequestURI());
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Invalid URL path. Expected format: /transactions/approve/{batch_id}\"}");
                return;
            }

            LOG.info("Extracted batch_id from path: {}", batchId);

            // Read request body
            StringBuilder requestBody = new StringBuilder();
            String line;
            try (BufferedReader reader = servletRequest.getReader()) {
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
            }

            String jsonBody = requestBody.toString();
            LOG.info("Received request body: {}", jsonBody);

            // Parse JSON body
            JsonObject requestJson = toJsonObject(jsonBody);
            if (requestJson == null) {
                LOG.error("Invalid JSON in request body: {}", jsonBody);
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Invalid JSON in request body\"}");
                return;
            }

            // Validate required fields in request body
            if (!requestJson.containsKey("status") || !requestJson.containsKey("message")) {
                LOG.error("Missing required fields in request body. Expected: status, message");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Missing required fields: status and message\"}");
                return;
            }

            // Create combined request object with path variable and body data
            JsonObjectBuilder combinedBuilder = Json.createObjectBuilder();
            addObject(combinedBuilder, "batch_id", batchId);
            addObject(combinedBuilder, "status", requestJson.getString("status", ""));
            addObject(combinedBuilder, "message", requestJson.getString("message", ""));

            JsonObject combinedRequest = combinedBuilder.build();

            LOG.info("Processing batch approval - batch_id: {}, status: {}, user: {}",
                    batchId, requestJson.getString("status", ""), user);

            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);

            // Execute the service
            setExecutor(new BatchApprovalService());
            respStr = getExecutor().execute(combinedRequest.toString(), user, actionId);

            // Determine HTTP status based on response
            JsonObject responseJson = toJsonObject(respStr);
            if (responseJson != null) {
                String status = responseJson.getString("status", "500");
                if ("00".equals(status)) {
                    servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
                } else if ("400".equals(status)) {
                    servletResponse.setStatus(400);
                } else if ("404".equals(status)) {
                    servletResponse.setStatus(404);
                } else {
                    servletResponse.setStatus(500);
                }
            } else {
                servletResponse.setStatus(500);
            }

            LOG.info("Service execution completed for user: {}, actionId: {}, batch_id: {}", user, actionId, batchId);
            out.print(respStr);

        } catch (CustomException e) {
            LOG.error("Custom exception in BatchApprovalServlet: {}", e.getMessage(), e);
            if (out != null) {
                servletResponse.setStatus(e.getStatusCode());
                out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            }

        } catch (Exception e) {
            LOG.error("Unexpected error in BatchApprovalServlet: {}", e.getMessage(), e);
            if (out != null) {
                servletResponse.setStatus(500);
                String safeMessage = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Internal server error";
                out.print("{\"status\":\"500\",\"message\":\"Internal server error: " + safeMessage + "\"}");
            }
        } finally {
            if (out != null) {
                try {
                    out.flush();
                } catch (Exception e) {
                    LOG.error("Error flushing response", e);
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        // This endpoint only supports POST requests
        servletResponse.setStatus(405); // Method Not Allowed
        servletResponse.setContentType(APPLICATION_JSON);
        servletResponse.setCharacterEncoding(UTF_8);
        servletResponse.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed. Use POST request.\"}");
        LOG.warn("GET request attempted on BatchApprovalServlet - returning 405");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This endpoint only supports POST requests
        resp.setStatus(405); // Method Not Allowed
        resp.setContentType(APPLICATION_JSON);
        resp.setCharacterEncoding(UTF_8);
        resp.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed. Use POST request.\"}");
        LOG.warn("PUT request attempted on BatchApprovalServlet - returning 405");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This endpoint only supports POST requests
        resp.setStatus(405); // Method Not Allowed
        resp.setContentType(APPLICATION_JSON);
        resp.setCharacterEncoding(UTF_8);
        resp.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed. Use POST request.\"}");
        LOG.warn("DELETE request attempted on BatchApprovalServlet - returning 405");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Handle CORS preflight requests
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setStatus(200);
        LOG.debug("OPTIONS request handled for CORS");
    }

    /**
     * Extract batch_id from URL path
     * Expected format: /app/transactions/approve/{batch_id}
     * @param requestURI The full request URI
     * @return The extracted batch_id or null if not found
     */
    private String extractBatchIdFromPath(String requestURI) {
        if (requestURI == null || requestURI.trim().isEmpty()) {
            return null;
        }

        try {
            Matcher matcher = BATCH_ID_PATTERN.matcher(requestURI);
            if (matcher.find()) {
                String batchId = matcher.group(1);
                LOG.debug("Extracted batch_id: '{}' from URI: '{}'", batchId, requestURI);
                return batchId.trim();
            } else {
                LOG.warn("No batch_id found in URI: '{}'. Expected pattern: .../approve/{{batch_id}}", requestURI);
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error extracting batch_id from URI: '{}'", requestURI, e);
            return null;
        }
    }
}