package persistence;

import util.BaseBean;

import javax.json.JsonObject;
import java.sql.Connection;

public interface FileUploadOps extends FetchRequest {
    boolean updateApprovedRecord(BaseBean requestBean, JsonObject request, Connection cnn);
    boolean approveDeleteRecord(BaseBean requestBean, JsonObject request, Connection cnn);
    boolean createModuleRequest(BaseBean requestBean, BaseBean row, Connection cnn);
    boolean createDeleteRequest(BaseBean requestBean, Connection cnn);

}
