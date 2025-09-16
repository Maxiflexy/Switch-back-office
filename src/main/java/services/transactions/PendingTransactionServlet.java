package services.transactions;

import exceptions.CustomException;
import messaging.transactions.PendingTransactionService;
import services.servlets.CustomBaseServlet;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static util.JsonUtil.addObject;
import static util.ResponseUtil.createDefaultResponse;

public class PendingTransactionServlet extends CustomBaseServlet {

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

            JsonObjectBuilder builder = Json.createObjectBuilder();

            // Extract query parameters - handle null values gracefully
            String batchId = servletRequest.getParameter("batch_id");
            String tranRef = servletRequest.getParameter("tran_ref");
            String startDate = servletRequest.getParameter("start_date");
            String endDate = servletRequest.getParameter("end_date");
            String requestType = servletRequest.getParameter("request_type");
            String moduleType = servletRequest.getParameter("module_type");
            String page = servletRequest.getParameter("page");
            String size = servletRequest.getParameter("size");

            LOG.info("Received request parameters - batch_id: {}, module_type: {}, start_date: {}, end_date: {}, request_type: {}, page: {}, size: {}",
                    batchId, moduleType, startDate, endDate, requestType, page, size);

            // Add parameters to JSON builder (addObject handles null values)
            addObject(builder, "batch_id", batchId);
            addObject(builder, "tran_ref", tranRef);
            addObject(builder, "start_date", startDate);
            addObject(builder, "end_date", endDate);
            addObject(builder, "request_type", requestType);
            addObject(builder, "module_type", moduleType);
            addObject(builder, "page", page);
            addObject(builder, "size", size);

            // Set response headers
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);

            setExecutor(new PendingTransactionService());
            respStr = getExecutor().execute(builder.build().toString(), user, actionId);

            LOG.info("Service execution completed for user: {}, actionId: {}", user, actionId);
            out.print(respStr);

        } catch (CustomException e) {
            LOG.error("Custom exception in PendingTransactionServlet: {}", e.getMessage(), e);
            if (out != null) {
                servletResponse.setStatus(e.getStatusCode());
                out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            }

        } catch (Exception e) {
            LOG.error("Unexpected error in PendingTransactionServlet: {}", e.getMessage(), e);
            if (out != null) {
                servletResponse.setStatus(500);
                out.print("{\"status\":\"500\",\"message\":\"Internal server error: " + e.getMessage().replace("\"", "\\\"") + "\"}");
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
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        // This endpoint only supports GET requests
        servletResponse.setStatus(405); // Method Not Allowed
        servletResponse.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed. Use GET request.\"}");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This endpoint only supports GET requests
        resp.setStatus(405); // Method Not Allowed
        resp.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed. Use GET request.\"}");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // This endpoint only supports GET requests
        resp.setStatus(405); // Method Not Allowed
        resp.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed. Use GET request.\"}");
    }
}