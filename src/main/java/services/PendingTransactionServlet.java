package services;

import exceptions.CustomException;
import messaging.transactions.FetchPendingTransactionService;
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
        String requestStr = null;
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();
            String respStr = null;

            String user = (String) servletRequest.getAttribute("username");
            String actionId = UUID.randomUUID().toString();

            JsonObjectBuilder builder = Json.createObjectBuilder();
            addObject(builder, "operation_type", servletRequest.getParameter("service_type"));
            addObject(builder, "switch", servletRequest.getParameter("switch"));
            addObject(builder, "module", servletRequest.getParameter("module"));
            addObject(builder, "start_date", servletRequest.getParameter("start_date"));
            addObject(builder, "end_date", servletRequest.getParameter("end_date"));
            addObject(builder, "page", servletRequest.getParameter("page"));
            addObject(builder, "size", servletRequest.getParameter("size"));

            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new FetchPendingTransactionService());


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
            out.flush();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}
