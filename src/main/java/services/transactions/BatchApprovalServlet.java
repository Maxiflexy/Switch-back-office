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

import static util.JsonUtil.addObject;
import static util.JsonUtil.toJsonObject;
import static util.ResponseUtil.createDefaultResponse;

public class BatchApprovalServlet extends CustomBaseServlet {

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            if (user == null || user.trim().isEmpty()) {
                user = "system"; // Default user if not available
            }

            String actionId = UUID.randomUUID().toString();

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
            if (!requestJson.containsKey("batch_id") || !requestJson.containsKey("status") || !requestJson.containsKey("message")) {
                LOG.error("Missing required fields in request body. Expected: batch_id, status, message");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Missing required fields: batch_id, status and message\"}");
                return;
            }

            // Extract fields from request body
            String batchId = requestJson.getString("batch_id", "").trim();
            String status = requestJson.getString("status", "").trim();
            String message = requestJson.getString("message", "").trim();

            // Basic validation
            if (batchId.isEmpty()) {
                LOG.error("batch_id cannot be empty");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"batch_id parameter cannot be empty\"}");
                return;
            }

            if (status.isEmpty()) {
                LOG.error("status cannot be empty");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"status parameter cannot be empty\"}");
                return;
            }

            if (message.isEmpty()) {
                LOG.error("message cannot be empty");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"message parameter cannot be empty\"}");
                return;
            }

            // Create request object for service layer
            JsonObjectBuilder requestBuilder = Json.createObjectBuilder();
            addObject(requestBuilder, "batch_id", batchId);
            addObject(requestBuilder, "status", status);
            addObject(requestBuilder, "message", message);

            JsonObject serviceRequest = requestBuilder.build();

            LOG.info("Processing batch approval - batch_id: {}, status: {}, user: {}", batchId, status, user);

            // Set response headers
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);

            setExecutor(new BatchApprovalService());
            respStr = getExecutor().execute(serviceRequest.toString(), user, actionId);

            // Determine HTTP status based on response
            JsonObject responseJson = toJsonObject(respStr);
            if (responseJson != null) {
                String responseStatus = responseJson.getString("status", "500");
                if ("00".equals(responseStatus)) {
                    servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
                } else if ("400".equals(responseStatus)) {
                    servletResponse.setStatus(400);
                } else if ("404".equals(responseStatus)) {
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
}