package services;

import messaging.AuthenticationService;
import services.servlets.BaseServlet;
import util.CryptoUtils;
import util.Encrypter;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

import static util.JsonUtil.addObject;

public class AuthenticationController extends BaseServlet {


    @Override
    protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        doPost(servletRequest, servletResponse);
    }

    @Override
    protected void doPost(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
        PrintWriter out = null;
        String requestString;
        try {
            requestString = getBody(servletRequest);
            out = servletResponse.getWriter();
            String respStr = null;
            servletResponse.setStatus(ResponseUtil.HTTP_OK_STATUS_1_INT);
            JsonObjectBuilder builder = Json.createObjectBuilder();
            JsonObject jsonRequest = JsonUtil.toJsonObject(CryptoUtils.decrypt(requestString));
            if (jsonRequest != null) {
                addObject(builder, "username", jsonRequest.getString("username"));
                addObject(builder, "password", jsonRequest.getString("password"));
                addObject(builder, "token", jsonRequest.getString("token"));

                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                setExecutor(new AuthenticationService());
                respStr = getExecutor().execute(builder.build().toString());
                LOG.info("Auth Response: {}", respStr);
                out.print(CryptoUtils.encrypt(respStr));
            } else {
                servletResponse.setContentType(APPLICATION_JSON);
                servletResponse.setCharacterEncoding(UTF_8);
                respStr = getInvalidFormatResponse();
                LOG.info("Auth Response: {}", respStr);
                out.print(CryptoUtils.encrypt(respStr));
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
            servletResponse.setContentType(APPLICATION_JSON);
            servletResponse.setCharacterEncoding(UTF_8);
            String respStr = getInvalidFormatResponse();
            assert out != null;
            LOG.info("Auth Response: {}", respStr);
            out.print(CryptoUtils.encrypt(respStr));
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
        doPost(req, resp);
    }
}
