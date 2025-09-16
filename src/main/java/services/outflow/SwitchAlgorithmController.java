package services.outflow;

import exceptions.CustomException;
import messaging.outflow.FetchSwitchAlgorithmRequest;
import messaging.outflow.SwitchAlgorithmRequest;
import messaging.outflow.SwitchAlgorithmUpdate;
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

public class SwitchAlgorithmController extends CustomBaseServlet {

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
            addObject(builder, "start_date", servletRequest.getParameter("start-date"));
            addObject(builder, "end_date", servletRequest.getParameter("end-date"));
            addObject(builder, "id", servletRequest.getParameter("id"));
            addObject(builder, "page", servletRequest.getParameter("page"));
            addObject(builder, "size", servletRequest.getParameter("size"));
            addObject(builder, "details", servletRequest.getParameter("details"));
            addObject(builder, "status", servletRequest.getParameter("status"));

            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            setExecutor(new FetchSwitchAlgorithmRequest());


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

            if (servletRequest.getContentType().equalsIgnoreCase(APPLICATION_JSON)) {

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new SwitchAlgorithmRequest());
                respStr = getExecutor().execute(requestString, email, "");
                LOG.info("Switch Algorithm Response: {}", respStr);
                out.print(respStr);
            } else {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                LOG.info("Switch Algorithm Response: {}", respStr);
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
    protected void doPut(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        PrintWriter out = null;
        String requestString;
        try {

            requestString = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            String email = (String) servletRequest.getAttribute("username");
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);

            if (servletRequest.getContentType().equalsIgnoreCase(APPLICATION_JSON)) {

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new SwitchAlgorithmUpdate());
                respStr = getExecutor().execute(requestString, email, "");
                LOG.info("Switch Algorithm Response: {}", respStr);
                out.print(respStr);
            } else {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                LOG.info("Switch Algorithm Response: {}", respStr);
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
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    }
}
