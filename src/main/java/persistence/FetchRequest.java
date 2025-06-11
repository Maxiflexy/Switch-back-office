package persistence;

import util.BaseBean;

import javax.json.JsonObject;

public interface FetchRequest {

    boolean getUnapprovedOrCanceledRequest(BaseBean requestBean, JsonObject request);

}
