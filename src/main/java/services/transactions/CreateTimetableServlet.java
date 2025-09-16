package services.transactions;

import exceptions.CustomException;
import messaging.transactions.CreateTimetableService;
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

public class CreateTimetableServlet extends CustomBaseServlet {

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
            System.out.println(jsonBody);
            LOG.info("Received timetable creation request body: {}", jsonBody);

            // Parse JSON body
            JsonObject requestJson = toJsonObject(jsonBody);
            if (requestJson == null) {
                getError(jsonBody);
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Invalid JSON in request body\"}");
                return;
            }

            // Validate required fields in request body
            if (!requestJson.containsKey("channelCode")) {
                LOG.error("Missing required field: channelCode");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Missing required field: channelCode\"}");
                return;
            }

            if (!requestJson.containsKey("switchDetails")) {
                LOG.error("Missing required field: switchDetails");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Missing required field: switchDetails\"}");
                return;
            }

            // Extract fields from request body
            String channelCode = requestJson.getString("channelCode", "").trim();
            if (channelCode.isEmpty()) {
                LOG.error("channelCode cannot be empty");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"channelCode parameter cannot be empty\"}");
                return;
            }

            // Validate switchDetails is an array
            if (requestJson.get("switchDetails").getValueType() != javax.json.JsonValue.ValueType.ARRAY) {
                LOG.error("switchDetails must be an array");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"switchDetails must be an array\"}");
                return;
            }

            // Create request object for service layer
            JsonObjectBuilder requestBuilder = Json.createObjectBuilder();
            addObject(requestBuilder, "channelCode", channelCode);
            requestBuilder.add("switchDetails", requestJson.getJsonArray("switchDetails"));

            JsonObject serviceRequest = requestBuilder.build();

            LOG.info("Processing timetable creation - channelCode: {}, user: {}", channelCode, user);

            // Set response headers
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);

            setExecutor(new CreateTimetableService());
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

            LOG.info("Timetable creation service execution completed for user: {}, actionId: {}, channelCode: {}",
                    user, actionId, channelCode);
            out.print(respStr);

        } catch (CustomException e) {
            LOG.error("Custom exception in CreateTimetableServlet: {}", e.getMessage(), e);
            if (out != null) {
                servletResponse.setStatus(e.getStatusCode());
                out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            }

        } catch (Exception e) {
            LOG.error("Unexpected error in CreateTimetableServlet: {}", e.getMessage(), e);
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
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            if (user == null || user.trim().isEmpty()) {
                user = "system"; // Default user if not available
            }

            String actionId = UUID.randomUUID().toString();

            // Extract query parameters
            String page = servletRequest.getParameter("page");
            String size = servletRequest.getParameter("size");
            String id = servletRequest.getParameter("id");
            String channelCode = servletRequest.getParameter("channelCode");

            LOG.info("Processing timetable fetch request - user: {}, page: {}, size: {}, id: {}, channelCode: {}",
                    user, page, size, id, channelCode);

            JsonObjectBuilder requestBuilder = Json.createObjectBuilder();
            requestBuilder.add("action", "fetch");

            if (page != null && !page.trim().isEmpty()) {
                requestBuilder.add("page", page.trim());
            }
            if (size != null && !size.trim().isEmpty()) {
                requestBuilder.add("size", size.trim());
            }
            if (id != null && !id.trim().isEmpty()) {
                requestBuilder.add("id", id.trim());
            }
            if (channelCode != null && !channelCode.trim().isEmpty()) {
                requestBuilder.add("channelCode", channelCode.trim());
            }

            JsonObject fetchRequest = requestBuilder.build();

            // Set response headers
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);

            setExecutor(new CreateTimetableService());
            respStr = getExecutor().execute(fetchRequest.toString(), user, actionId);

            // Determine HTTP status based on response
            JsonObject responseJson = toJsonObject(respStr);
            if (responseJson != null) {
                String responseStatus = responseJson.getString("status", "500");
                if ("00".equals(responseStatus)) {
                    servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
                } else {
                    servletResponse.setStatus(500);
                }
            } else {
                servletResponse.setStatus(500);
            }

            LOG.info("Timetable fetch service execution completed for user: {}, actionId: {}", user, actionId);
            out.print(respStr);

        } catch (CustomException e) {
            LOG.error("Custom exception in CreateTimetableServlet GET: {}", e.getMessage(), e);
            if (out != null) {
                servletResponse.setStatus(e.getStatusCode());
                out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            }

        } catch (Exception e) {
            LOG.error("Unexpected error in CreateTimetableServlet GET: {}", e.getMessage(), e);
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
    protected void doPut(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            if (user == null || user.trim().isEmpty()) {
                user = "system";
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
            LOG.info("Received timetable update request body: {}", jsonBody);

            // Parse JSON body
            JsonObject requestJson = toJsonObject(jsonBody);
            if (requestJson == null) {
                getError(jsonBody);
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Invalid JSON in request body\"}");
                return;
            }

            // Validate required fields in request body
            if (!requestJson.containsKey("channelCode")) {
                LOG.error("Missing required field: channelCode");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Missing required field: channelCode\"}");
                return;
            }

            if (!requestJson.containsKey("switchDetails")) {
                LOG.error("Missing required field: switchDetails");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"Missing required field: switchDetails\"}");
                return;
            }

            // Extract fields from request body
            String channelCode = requestJson.getString("channelCode", "").trim();
            if (channelCode.isEmpty()) {
                LOG.error("channelCode cannot be empty");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"channelCode parameter cannot be empty\"}");
                return;
            }

            // Validate switchDetails is an array
            if (requestJson.get("switchDetails").getValueType() != javax.json.JsonValue.ValueType.ARRAY) {
                LOG.error("switchDetails must be an array");
                servletResponse.setStatus(400);
                out.print("{\"status\":\"400\",\"message\":\"switchDetails must be an array\"}");
                return;
            }

            // Create request object for service layer
            JsonObjectBuilder requestBuilder = Json.createObjectBuilder();
            addObject(requestBuilder, "channelCode", channelCode);
            requestBuilder.add("switchDetails", requestJson.getJsonArray("switchDetails"));
            requestBuilder.add("action", "update");

            JsonObject serviceRequest = requestBuilder.build();

            LOG.info("Processing timetable update - channelCode: {}, user: {}", channelCode, user);

            // Set response headers
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);

            setExecutor(new CreateTimetableService());
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

            LOG.info("Timetable update service execution completed for user: {}, actionId: {}, channelCode: {}",
                    user, actionId, channelCode);
            out.print(respStr);

        } catch (CustomException e) {
            LOG.error("Custom exception in CreateTimetableServlet PUT: {}", e.getMessage(), e);
            if (out != null) {
                servletResponse.setStatus(e.getStatusCode());
                out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            }

        } catch (Exception e) {
            LOG.error("Unexpected error in CreateTimetableServlet PUT: {}", e.getMessage(), e);
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
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This endpoint only supports POST requests
        resp.setStatus(405); // Method Not Allowed
        resp.setContentType(APPLICATION_JSON);
        resp.setCharacterEncoding(UTF_8);
        resp.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed. Use POST request.\"}");
        LOG.warn("DELETE request attempted on CreateTimetableServlet - returning 405");
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

    private static void getError(String jsonBody) {
        LOG.error("Invalid JSON in request body: {}", jsonBody);
    }
}
