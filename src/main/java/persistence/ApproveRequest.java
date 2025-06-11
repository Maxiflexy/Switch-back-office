package persistence;

import util.BaseBean;

import javax.json.JsonObject;

public interface ApproveRequest {

    boolean approveOrCancelRequest(BaseBean requestBean, JsonObject requestObject);
}
