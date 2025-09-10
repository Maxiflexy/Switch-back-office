package messaging.fileUtils.persistence;

import util.BaseBean;

import java.sql.Connection;

public interface OutflowSwitchRequest {

    boolean createModuleRequest(BaseBean request, Connection connection);
    boolean updateModuleRequest(BaseBean request, Connection connection);
    boolean deactivateModuleRequest(BaseBean request, Connection connection);
    boolean approveModuleRequest(BaseBean request, Connection connection);
    BaseBean findApprovedById(long id);
    BaseBean findUnapprovedById(long id);
}
