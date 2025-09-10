package messaging.fileUtils.services.outflow;

import exceptions.CustomException;
import services.servlets.CustomBaseServlet;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static util.JsonUtil.addObject;
import static util.ResponseUtil.createDefaultResponse;

public class OutflowSwitchSequence extends CustomBaseServlet {

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
            addObject(builder, "start-date", servletRequest.getParameter("start-date"));
            addObject(builder, "end-date", servletRequest.getParameter("end-date"));
            addObject(builder, "id", servletRequest.getParameter("id"));
            addObject(builder, "page", servletRequest.getParameter("page"));
            addObject(builder, "size", servletRequest.getParameter("size"));
            addObject(builder, "details", servletRequest.getParameter("details"));
            addObject(builder, "status", servletRequest.getParameter("status"));

            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
//            setExecutor(new FetchOutflowSequencceRequest());


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
        PrintWriter out = null;
        String requestString;
        try {

            requestString = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            String email = (String) servletRequest.getAttribute("username");
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            JsonObjectBuilder builder = Json.createObjectBuilder();
            JsonObject jsonRequest = JsonUtil.toJsonObject(requestString);
            if (jsonRequest != null) {

                addObject(builder, "name", jsonRequest.getString("service_type"));
                addObject(builder, "start_date", jsonRequest.getString("start_date"));
                addObject(builder, "end_date", jsonRequest.getString("end_date"));
                addObject(builder, "switch_type", jsonRequest.getString("switch_type"));
                addObject(builder, "module", jsonRequest.getString("module_type"));

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
//                setExecutor(new CreateOutflowSequence());
                respStr = getExecutor().execute(builder.build().toString(), email, "");
                LOG.info("Retrial Response: {}", respStr);
                out.print(respStr);
            } else {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                LOG.info("Retrial Response: {}", respStr);
                out.print(respStr);
            }
        } catch (CustomException e) {
            assert out != null;
            servletResponse.setStatus(e.getStatusCode());
            out.print(createDefaultResponse(e.getResponseCode(), e.getStatusCode(), e.getMessage()));
            LOG.error(e.getMessage(), e);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            String respStr = getInvalidFormatResponse();
            assert out != null;
            LOG.info("Retrial Response: {}", respStr);
            out.print(respStr);
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
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}
