package messaging.fileUtils.services.transactions;

import exceptions.CustomException;
import messaging.transactions.FailedRetrialService;
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

public class FailedRetrialServlet extends CustomBaseServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            String actionId = UUID.randomUUID().toString();

            JsonObjectBuilder builder = Json.createObjectBuilder();

            // Extract query parameters - handle null values gracefully
            String serviceType = servletRequest.getParameter("service_type");
            String requestStatus = servletRequest.getParameter("request_status");
            String retrialStartDate = servletRequest.getParameter("retrial_start_date");
            String retrialEndDate = servletRequest.getParameter("retrial_end_date");
            String batchId = servletRequest.getParameter("batch_id");
            String module = servletRequest.getParameter("module_type");
            String page = servletRequest.getParameter("page");
            String size = servletRequest.getParameter("size");

            LOG.info("Received request parameters - service_type: {}, request_status: {}, retrial_start_date: {}, retrial_end_date: {}, batch_id: {}, module: {}, page: {}, size: {}",
                    serviceType, requestStatus, retrialStartDate, retrialEndDate, batchId, module, page, size);

            // Add parameters to JSON builder (addObject handles null values)
            addObject(builder, "service_type", serviceType);
            addObject(builder, "request_status", requestStatus);
            addObject(builder, "retrial_start_date", retrialStartDate);
            addObject(builder, "retrial_end_date", retrialEndDate);
            addObject(builder, "batch_id", batchId);
            addObject(builder, "module", module);
            addObject(builder, "page", page);
            addObject(builder, "size", size);

            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new FailedRetrialService());

            respStr = getExecutor().execute(builder.build().toString(), user, actionId);
            out.print(respStr);

        } catch (CustomException e) {
            assert out != null;
            servletResponse.setStatus(e.getStatusCode());
            out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            LOG.error(e.getMessage(), e);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }
        try {
            assert out != null;
            out.flush();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        // This endpoint only supports GET requests
        servletResponse.setStatus(405); // Method Not Allowed
        servletResponse.getWriter().print("{\"status\":\"405\",\"message\":\"Method not allowed here try to . Use GET request.\"}");
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