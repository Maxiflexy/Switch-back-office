package services.performance.endpoints;

import messaging.stats.endpoints.*;
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

public class Endpoint extends CustomBaseServlet {

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
            addObject(builder, "approval-status", servletRequest.getParameter("approval-status"));
            addObject(builder, "name", servletRequest.getParameter("name"));
            addObject(builder, "endpoint-code", servletRequest.getParameter("endpoint-code"));
            addObject(builder, "id", servletRequest.getParameter("id"));
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new GetEndpointList());
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

    @Override
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
                setExecutor(new CreateEndpoint());
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
        }    }

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
                        setExecutor(new UpdateEndpoint());
                        break;

                    case "deactivate":
                        setExecutor(new DeactivateEndpoint());
                        break;

                    case "activate":
                        setExecutor(new ActivateEndpoint());
                        break;

                    default:
                        servletResponse.setContentType(APPLICATION_JSON);
                        servletResponse.setCharacterEncoding(UTF_8);
                        respStr = getInvalidFormatResponse();
                        out.print(respStr);
                }

                String request = JsonUtil.toBuilder(JsonUtil.toJsonObject(requestStr))
                        .add("endpoint-id", id)
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
