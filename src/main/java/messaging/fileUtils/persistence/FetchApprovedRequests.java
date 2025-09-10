package messaging.fileUtils.persistence;

import util.BaseBean;

import javax.json.JsonObject;

public interface FetchApprovedRequests {

    boolean fetchApprovedRequests(BaseBean requestBean, JsonObject requestObject);

}
