package messaging;

import constants.AppConstants;
import persistence.DBHelper;
import services.executors.Executor;
import util.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static util.CustomUtil.createJsonToken;
import static util.CustomUtil.getUserDetailsAndPersistUserData;

public class AuthenticationService extends Common implements Executor {

    @Override
    public String execute(String request) {
        BaseBean requestBean = new BaseBean();
        String requestString = validateRequestBody(request, requestBean);
        if (requestBean.containsKey("validationcode")) {
            return createReply(requestBean, false);
        }
        if (!DBHelper.checkUserDetails(requestBean)) {
            return createReply(requestBean, true);
        }
        requestBean.setString("ad_request", request);
        BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
        requestBean.setString("private_key", configBean.get("private_key"));
        boolean status = sendRequest(requestBean, configBean, requestString);
        return createReply(requestBean, status);
    }

    @Override
    protected String validateRequestBody(String request, BaseBean requestBean) {
        //validate email is firstname.lastname
        JsonObject authRequest = null;
        try {
            authRequest = JsonUtil.toJsonObject(request);
            validateParameter(authRequest, requestBean, "username");
            validateParameter(authRequest, requestBean, "password");
            validateParameter(authRequest, requestBean, "token");
            String username = requestBean.getString("username");
            if (username.contains("@ubagroup.com")) {
                username = username.replace("@ubagroup.com", "");
                requestBean.setString("username", username);
            }
        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return request;
    }

    @Override
    protected void processResponse(BaseBean requestBean) {
        try {
            if (requestBean.getString("auth-status").equals("true")) {
                BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
                BaseBean entrustBean = new BaseBean();
                entrustBean.setString("username", requestBean.getString("username"));
                entrustBean.setString("token", requestBean.getString("token"));
                String request = TokenService.generateEntrustRequest(entrustBean);
                String username = CryptoUtils.decrypt(configBean.getString("entrust_name"));
                String password = CryptoUtils.decrypt(configBean.getString("entrust_word"));
                String auth = "Basic ".concat(CryptoUtils.base64Encode(username.concat(":").concat(password)));
                entrustBean.setString("auth_details", auth);
                entrustBean.setString("path", "entrust");
                entrustBean.setString("ad_request", request);
                String env = configBean.getString("cmp");
                boolean isError = false;
                if (env.equals(AppConstants.Constants.LOCAL)) {
                    isError =  validateToken(entrustBean, request);
                } else if (env.equals(AppConstants.Constants.UBA)) {
                    isError = sendRequest(entrustBean, configBean, "");
                }

                if (!isError && entrustBean.getString("code").equals(ResponseUtil.SUCCESS)) {
                    createJsonToken(requestBean);
                    getUserDetailsAndPersistUserData(requestBean);
                } else {
                    requestBean.setString("status_type", ResponseUtil.FAIL);
                    requestBean.setString("status_code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);
                }
            } else {
                requestBean.setString("status_type", ResponseUtil.FAIL);
                requestBean.setString("status_code", ResponseUtil.HTTP_UNAUTHORIZED_STATUS);
            }
        } catch (Exception ex) {
            requestBean.setString("status_type", ResponseUtil.FAIL);
            requestBean.setString("status_code", ResponseUtil.HTTP_INTERNAL_SERVER_ERROR);
            ex.printStackTrace();
        }
    }

    private boolean validateToken(BaseBean requestBean, String request) {
        requestBean.setString("code", ResponseUtil.SUCCESS);
        return !DBHelper.validateToken(requestBean, request);
    }


    @Override
    protected String createReply(BaseBean requestBean, Boolean procErr) {
        JsonObject jsonResp = null;

        JsonObjectBuilder jObjBuil = Json.createObjectBuilder();


        if (procErr) {

            requestBean.setString("status_type", ResponseUtil.FAIL);
            requestBean.setString("status_code", ResponseUtil.GENERIC_PROCESSING_ERROR);

        }
        if (requestBean.getString("status_type").equals(ResponseUtil.FAIL)) {
            jsonResp = jObjBuil.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
            ).build();
            return JsonUtil.toStr(jsonResp);
        }

        JsonObjectBuilder response = Json.createObjectBuilder();
        if (    requestBean.get("x-http-status-code").equals(ResponseUtil.HTTP_OK_STATUS) && requestBean.getString("auth-status").equals("true")) {
            response.add("token", requestBean.getString("jwt"))
                    .add("details", Json.createObjectBuilder()
                            .add("username", requestBean.getString("user"))
                            .add("role", requestBean.getString("role_name"))
                            .add("refresh-token", requestBean.getString("refresh-token"))
                            .add("token-expiration", requestBean.getString("token-expiration"))
                            .build()
                    );
            jsonResp = response.add("status", Json.createObjectBuilder()
                    .add("type", requestBean.getString("status_type"))
                    .add("code", requestBean.getString("status_code"))
            ).build();
        }
//        LOG.info("Response is: " + jsonResp);
        return JsonUtil.toStr(jsonResp);
    }


}

