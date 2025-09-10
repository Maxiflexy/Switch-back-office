package messaging.fileUtils.messaging;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import exceptions.CustomException;
import messaging.ValidatorUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;
import util.BaseBean;
import util.JsonServiceConfig;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.JsonObject;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static util.CustomUtil.getPrivateKey;
import static util.CustomUtil.getPublicKey;

public class RequestValidator extends ValidatorUtils {

//    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");



    public static boolean validateIPAddress(String ip) {
        InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValid(ip);
    }

    public static boolean validateURL(String url) {
        UrlValidator validator = new UrlValidator();
        return validator.isValid(url);
    }

    public final JsonObject validateRequestFormat(String request, BaseBean valBean) throws IOException {

        JsonObject jobj = JsonUtil.toJsonObject(request);
        valBean.setString(VALIDATION_REPORT, "");

        if (jobj == null) {

            LOG.info("Invalid message format");
            valBean.setString("status_type", ResponseUtil.FAIL);
            valBean.setString("status_code", ResponseUtil.INVALID_MSG_FORMAT);
            valBean.setString(VALIDATION_REPORT, VALIDATION_FAILURE);
            valBean.setString(VALIDATION_MESSAGE, createDefaultResponse(valBean));
            throw new IOException("Invalid Request Format");
        }

        return jobj;

    }

    JsonObject validateBillerId(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateBillerId(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    JsonObject validateSessionId(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateSessionId(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    JsonObject validateTransactionRequest(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateTransactionRequest(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    JsonObject validateCategory(String request, BaseBean requestBean) {
        JsonObject jobj = null;
        try {
            jobj = validateRequestFormat(request, requestBean);
            validateTransactionRequest(jobj, requestBean);
        } catch (Exception ex) {
            LOG.error(ex.toString());
        }
        return jobj;
    }

    protected void validateParameter(JsonObject request, BaseBean requestBean, String parameter, boolean isString) throws IOException {
        if (!request.containsKey(parameter)) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", parameter + " not present");
            throw new IOException(requestBean.get("message"));
        }
        String value = "";
        if (!isString) {
            value = String.valueOf(request.get(parameter));
        } else {
            value = request.getString(parameter);
        }
        requestBean.setString(parameter, value);
    }

    public void validateParameter(JsonObject request, BaseBean requestBean, String parameter) throws IOException {
        validateParameter(request, requestBean, parameter, true);
    }

    public void validateDateParameter(JsonObject request, BaseBean requestBean, String parameter, DateTimeFormatter format) throws IOException {
        if (!request.containsKey(parameter)) {
            requestBean.setString("validationcode", "01");
            requestBean.setString("message", parameter + " not present");
            throw new IOException(requestBean.get("message"));
        }
        try {
            LocalDateTime.parse(request.getString(parameter), format);
            requestBean.setString(parameter, request.getString(parameter));
        }catch (Exception ex) {
            requestBean.setString("message", ex.getMessage());
            throw new IOException(requestBean.get("message"));

        }

    }
    public void validateOptionalDateParameter(JsonObject request, BaseBean requestBean, String parameter, DateTimeFormatter format) throws IOException {
        if (request.containsKey(parameter)) {
            try {
                LocalDateTime.parse(request.getString(parameter), format);
                requestBean.setString(parameter, request.getString(parameter));
            } catch (Exception ex) {
                requestBean.setString("message", ex.getMessage());
                throw new IOException(requestBean.get("message"));

            }
        }

    }

    public void validateOptionalParameter(JsonObject request, BaseBean requestBean, String parameter, boolean isString) {
        try {
            String value = "";
            if (request.containsKey(parameter)) {
                if (!isString) {
                    value = String.valueOf(request.get(parameter));
                } else {
                    value = request.getString(parameter);
                }
                requestBean.setString(parameter, value);
            }

        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void verifyRefreshToken(BaseBean requestBean, String token) throws IOException {
        boolean verified = false;
        if (token != null) {

            try {
                BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");

                RSAPublicKey publickey = (RSAPublicKey) getPublicKey(configBean.getString("public_key"));
                RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(configBean.getString("private_key"));

                JWTVerifier jwtV = JWT.require(Algorithm.RSA256(publickey, privateKey)).build();
                DecodedJWT jwtD = jwtV.verify(token);

            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "unable to verify refresh token");
                throw new IOException(requestBean.get("message"));
            }
        }
    }

    public static void verifyToken(BaseBean requestBean, String token) throws IOException {
        if (token != null) {

            try {
                BaseBean configBean = JsonServiceConfig.getInstance().getProperty("UBA");
                RSAPublicKey publicKey = (RSAPublicKey) getPublicKey(configBean.getString("public_key"));
                RSAPrivateKey privateKey = (RSAPrivateKey) getPrivateKey(configBean.getString("private_key"));
                JWTVerifier jwtV = JWT
                        .require(Algorithm.RSA256(publicKey, privateKey))
                        .build();
                try {
                    DecodedJWT jwtD = jwtV.verify(token);
                } catch (JWTVerificationException e) {
                    LOG.info(e.getMessage());
                    return;
                }
                throw new IOException("Token is still valid");

            } catch (Exception ex) {
                LOG.error(ex.getMessage());
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", ex.getMessage());
                throw new IOException(requestBean.get("message"));
            }
        }
    }

    public static void validateFieldNotEmpty(String fieldName, BaseBean row, BaseBean requestBean) {
        if (row.getString(fieldName).isEmpty()) {
            requestBean.setString("message", "Invalid field: "+fieldName);
            throw new CustomException(requestBean);
        }
    }



    public static void validateDateRange(String startDateStr, String endDateStr) throws IOException {
        try {
            LocalDateTime startDate = LocalDateTime.parse(startDateStr, FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr, FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sevenDaysAgo = now.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);

            if (startDate.isBefore(sevenDaysAgo) || startDate.isAfter(now)) {
                throw new IOException("Start date must be within the last 7 days and not in the future.");
            }

            if (endDate.isAfter(now)) {
                throw new IOException("End date must not be in the future.");
            }

            if (startDate.isAfter(endDate)) {
                throw new IOException("Start date must not be after end date.");
            }

        } catch (DateTimeParseException e) {
            throw new IOException("Invalid date format. Expected format: yyyy-MM-ddTHH:mm:ss");
        }
    }


}