package services.outflow;

import exceptions.CustomException;
import messaging.outflow.SwitchAlgorithmActivate;
import services.servlets.CustomBaseServlet;
import util.ResponseUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static util.ResponseUtil.createDefaultResponse;

public class SwitchAlgorithmDeactivate extends CustomBaseServlet {

    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

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
                setExecutor(new SwitchAlgorithmActivate());
                respStr = getExecutor().execute(requestString, email, "");
                LOG.info("Switch Algorithm deactivate Response: {}", respStr);
                out.print(respStr);
            } else {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                LOG.info("Switch Algorithm deactivate Response: {}", respStr);
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
