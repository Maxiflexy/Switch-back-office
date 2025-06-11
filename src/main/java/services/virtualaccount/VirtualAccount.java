package services.virtualaccount;

import messaging.virtualAccount.*;
import services.servlets.CustomBaseServlet;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static util.JsonUtil.addObject;

public class VirtualAccount extends CustomBaseServlet {

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
            addObject(builder, "mc-status", servletRequest.getParameter("mc-status"));
            addObject(builder, "acct-enq-url", servletRequest.getParameter("acct-enq-url"));
            addObject(builder, "acct-prefix", servletRequest.getParameter("acct-prefix"));
            addObject(builder, "credt-trf-url", servletRequest.getParameter("credt-trf-url"));
            addObject(builder, "debt-trf-url", servletRequest.getParameter("debt-trf-url"));
            addObject(builder, "service-provider", servletRequest.getParameter("service-provider"));
            addObject(builder, "approval-comment", servletRequest.getParameter("approval-comment"));
            addObject(builder, "va-id", servletRequest.getParameter("va-id"));
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new GetAccountList());
            respStr = getExecutor().execute(builder.build().toString(), user, actionId);
            out.print(respStr);

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

    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            requestStr = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            String user = (String) servletRequest.getAttribute("username");
            String actionId = servletRequest.getParameter("action-id");
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            if (APPLICATION_JSON.equalsIgnoreCase(getContentType())) {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new CreateAccount());
                respStr = getExecutor().execute(requestStr, user, actionId);
                out.print(respStr);

            } else {

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                out.print(respStr);

            }
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
    protected void doPut(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        String requestStr = null;
        PrintWriter out = null;
        try {
            requestStr = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            String user = (String) servletRequest.getAttribute("username");
            String actionId = servletRequest.getParameter("action-id");
            String id = servletRequest.getParameter("id");
            String action = servletRequest.getParameter("action");
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            if (APPLICATION_JSON.equalsIgnoreCase(getContentType())) {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                switch (action) {
                    case "update":
                        setExecutor(new UpdateAccount());
                        break;
                    case "deactivate":
                        setExecutor(new DeactivateAccount());
                        break;

                    case "activate":
                        setExecutor(new ActivateAccount());
                        break;

                    default:
                        servletResponse.setContentType(APPLICATION_JSON);
                        servletResponse.setCharacterEncoding(UTF_8);
                        respStr = getInvalidFormatResponse();
                        out.print(respStr);
                }

                String request = JsonUtil.toBuilder(JsonUtil.toJsonObject(requestStr))
                        .add("update-id", id)
                        .build().toString();
                respStr = getExecutor().execute(request, user, actionId);
                out.print(respStr);

            } else {

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                out.print(respStr);

            }
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
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}