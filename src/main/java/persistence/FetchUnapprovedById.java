package persistence;

import util.BaseBean;

import javax.json.JsonObject;

public interface FetchUnapprovedById {

    boolean fetchUnapprovedById(BaseBean requestBean, JsonObject requestObject);
}
