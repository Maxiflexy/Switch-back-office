package messaging.fileUtils.persistence;

import messaging.fileUtils.util.BaseBean;

import javax.json.JsonObject;

public interface ApproveRequest {

    boolean approveOrCancelRequest(BaseBean requestBean, JsonObject requestObject);
}
