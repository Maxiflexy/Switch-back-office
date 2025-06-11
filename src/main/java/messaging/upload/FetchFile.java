package messaging.upload;

import exceptions.CustomException;
import messaging.RequestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.FileUploadDbHelper;
import services.executors.FileExecutor;
import util.BaseBean;
import util.JsonUtil;
import util.ResponseUtil;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.Part;
import java.util.List;

public class FetchFile extends RequestValidator implements FileExecutor {
    final static Logger LOG = LogManager.getLogger(FetchFile.class);

    @Override
    public String execute(String request, String currentUser, String actionId, List<Part> parts) {
        BaseBean requestBean = new BaseBean();
        JsonObject requestObject = JsonUtil.toJsonObject(request);
        boolean success = false;
        if (validateRequest(requestBean, request) && !"01".equals(requestBean.getString("validationcode"))) {
            if (requestBean.containsKey("fetch_content") && requestBean.getString("fetch_content").equals("true")) {
                success = FileUploadDbHelper.fetchFileContent(requestBean, requestObject);
            } else {
                success = FileUploadDbHelper.fetchUploadedFiles(requestBean, requestObject);
            }
            LOG.info("Fetching files {}", success);
        }
        return createReply(requestBean, success);
    }

    public boolean validateRequest(BaseBean requestBean, String request) {
        JsonObject jsonRequest = null;
        boolean response = false;
        try {
            jsonRequest = JsonUtil.toJsonObject(request);
            validateOptionalParameter(jsonRequest, requestBean, "document_id", true);
            validateParameter(jsonRequest, requestBean, "module_name", true);
            validateOptionalParameter(jsonRequest, requestBean, "action", true);
            validateOptionalParameter(jsonRequest, requestBean, "fetch_content", true);
            response = true;

        } catch (Exception Ex) {
            if (requestBean.get("message").isEmpty()) {
                requestBean.setString("validationcode", "01");
                requestBean.setString("message", "Bad request");
            }
        }
        return response;
    }

    public String createReply(BaseBean requestBean, Boolean error) {
        JsonObject jsonResp;
        if (!error) {
            throw new CustomException(requestBean);
        }

        String message = "Files fetched";
        jsonResp = Json.createObjectBuilder().add("status", ResponseUtil.SUCCESS)
                .add("message", message)
                .add("data", JsonUtil.toJsonArray(requestBean.getString("jsonBean")))
                .build();
        return jsonResp.toString();
    }

}
